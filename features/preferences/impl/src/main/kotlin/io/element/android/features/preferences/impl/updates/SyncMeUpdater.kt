/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.preferences.impl.updates

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.element.android.libraries.ui.strings.CommonStrings
import java.io.File
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Process-wide updater. DownloadManager owns transfers; only verified APKs reach the Android installer. */
class SyncMeUpdater private constructor(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = context.getSharedPreferences("syncme_updates", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(UpdateState())
    internal val state = mutableState.asStateFlow()
    private val mutableAcknowledgedPrompt = MutableStateFlow(preferences.getString("acknowledged_prompt", null))
    internal val acknowledgedPrompt = mutableAcknowledgedPrompt.asStateFlow()

    internal fun acknowledgePrompt(release: UpdateRelease) {
        mutableAcknowledgedPrompt.value = release.sha256
        scope.launch { preferences.edit().putString("acknowledged_prompt", release.sha256).apply() }
    }
    private val downloads get() = context.getSystemService(DownloadManager::class.java)
    private var task: Job? = null
    private var observing = false
    private var downloadId = preferences.getLong("download_id", -1)
    private val installedCode get() = PackageInfoCompat.getLongVersionCode(packageInfo(context.packageName))

    init {
        val saved = runCatching { UpdateRelease.parse(preferences.getString("release", null) ?: "") }.getOrNull()
        if (saved != null && saved.versionCode > installedCode && downloadId >= 0) {
            mutableState.value = UpdateState(UpdateStage.Downloading, saved)
            task = scope.launch { monitor(saved) }
        } else if (downloadId >= 0) {
            runCatching { downloads.remove(downloadId) }
            preferences.edit().clear().apply()
            downloadId = -1
        }
    }

    fun observeForeground() {
        if (observing) return
        observing = true
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { check() }
        })
    }

    internal fun check() {
        if (task?.isActive == true || state.value.stage in setOf(UpdateStage.Ready, UpdateStage.InstallFailed)) return
        task = scope.launch {
            mutableState.value = UpdateState(UpdateStage.Checking)
            try {
                val release = withContext(Dispatchers.IO) { fetchRelease() }
                mutableState.value = if (release.versionCode > installedCode) UpdateState(UpdateStage.Available, release)
                else UpdateState(UpdateStage.Current)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.value = UpdateState(UpdateStage.CheckFailed)
            }
        }
    }

    private fun fetchRelease(): UpdateRelease {
        val connection = URL(UPDATE_METADATA).openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache")
            require(connection.responseCode == 200)
            val bytes = connection.inputStream.use { stream ->
                val buffer = ByteArray(65537)
                var total = 0
                while (total < buffer.size) {
                    val count = stream.read(buffer, total, buffer.size - total)
                    if (count < 0) break
                    total += count
                }
                require(total <= 65536)
                buffer.copyOf(total)
            }
            return UpdateRelease.parse(bytes.toString(Charsets.UTF_8))
        } finally { connection.disconnect() }
    }

    internal fun download() {
        var release = state.value.release ?: return
        if (task?.isActive == true) return
        task = scope.launch {
            try {
                mutableState.value = UpdateState(UpdateStage.Checking, release)
                release = withContext(Dispatchers.IO) { fetchRelease() }
                if (release.versionCode <= installedCode) {
                    mutableState.value = UpdateState(UpdateStage.Current)
                    return@launch
                }
                if (downloadId >= 0) downloads.remove(downloadId)
                val target = apkFile()
                withContext(Dispatchers.IO) { if (target.exists()) require(target.delete()) }
                val request = DownloadManager.Request(UPDATE_APK.toUri())
                    .setTitle(context.getString(CommonStrings.syncme_update_download_title))
                    .setMimeType("application/vnd.android.package-archive")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "syncme-update.apk")
                downloadId = downloads.enqueue(request)
                preferences.edit().putLong("download_id", downloadId).putString("release", release.json).apply()
                mutableState.value = UpdateState(UpdateStage.Downloading, release)
                monitor(release)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                mutableState.value = UpdateState(UpdateStage.DownloadFailed, release)
            }
        }
    }

    private suspend fun monitor(release: UpdateRelease) {
        try {
            while (true) {
                val progress = withContext(Dispatchers.IO) {
                    downloads.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
                        require(cursor.moveToFirst())
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        status to ((bytes * 100 / release.size).coerceIn(0, 100).toInt())
                    }
                }
                when (progress.first) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        mutableState.value = UpdateState(UpdateStage.Verifying, release)
                        withContext(Dispatchers.IO) { verify(release) }
                        mutableState.value = UpdateState(UpdateStage.Ready, release)
                        return
                    }
                    DownloadManager.STATUS_FAILED -> error("Download failed")
                    DownloadManager.STATUS_PAUSED -> mutableState.value = UpdateState(UpdateStage.Paused, release, progress.second)
                    else -> mutableState.value = UpdateState(UpdateStage.Downloading, release, progress.second)
                }
                delay(1000)
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            runCatching { downloads.remove(downloadId) }
            preferences.edit().clear().apply()
            downloadId = -1
            mutableState.value = UpdateState(UpdateStage.DownloadFailed, release)
        }
    }

    internal fun permissionUnavailable() {
        mutableState.value = state.value.copy(stage = UpdateStage.InstallFailed)
    }

    internal fun install(activity: Context) {
        val release = state.value.release ?: return
        if (task?.isActive == true) return
        task = scope.launch {
            var verified = false
            try {
                mutableState.value = UpdateState(UpdateStage.Verifying, release)
                val uri = withContext(Dispatchers.IO) {
                    verify(release)
                    requireNotNull(downloads.getUriForDownloadedFile(downloadId))
                }
                verified = true
                withContext(Dispatchers.Main) {
                    activity.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                }
                mutableState.value = UpdateState(UpdateStage.Ready, release)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (!verified) {
                    runCatching { downloads.remove(downloadId) }
                    preferences.edit().clear().apply()
                    downloadId = -1
                }
                mutableState.value = UpdateState(if (verified) UpdateStage.InstallFailed else UpdateStage.DownloadFailed, release)
            }
        }
    }

    private fun apkFile(): File = File(requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)), "syncme-update.apk")

    private fun verify(release: UpdateRelease) {
        val file = apkFile()
        require(release.versionCode > installedCode && file.length() == release.size)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(65536)
            while (true) { val count = stream.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        require(digest.digest().joinToString("") { "%02x".format(it) } == release.sha256)
        val archive = requireNotNull(context.packageManager.getPackageArchiveInfo(file.path, signatureFlags()))
        requireCompatibleUpdate(archive, packageInfo(context.packageName), release.versionCode)
    }

    @Suppress("DEPRECATION")
    private fun packageInfo(name: String) = context.packageManager.getPackageInfo(name, signatureFlags())
    @Suppress("DEPRECATION")
    private fun signatureFlags() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    companion object {
        @Volatile private var instance: SyncMeUpdater? = null
        fun get(context: Context): SyncMeUpdater = instance ?: synchronized(this) {
            instance ?: SyncMeUpdater(context.applicationContext).also { instance = it }
        }
    }
}
