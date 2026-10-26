/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.preferences.impl.updates

import org.json.JSONObject

internal const val UPDATE_METADATA = "https://updates.syncme.ir/android/latest.json"
internal const val UPDATE_APK = "https://updates.syncme.ir/android/SyncMe.apk"

internal data class UpdateRelease(val version: String, val versionCode: Long, val size: Long, val sha256: String, val json: String) {
    companion object {
        fun parse(json: String): UpdateRelease {
            require(json.length <= 65536)
            val data = JSONObject(json)
            val version = data.getString("version")
            val code = data.getLong("versionCode")
            val size = data.getLong("size")
            val hash = data.getString("sha256").lowercase()
            require(version.matches(Regex("[0-9]+(\\.[0-9]+){1,3}")) && version.length <= 32)
            require(code in 1..2100000000L && size in 1..536870912L && hash.matches(Regex("[0-9a-f]{64}")))
            require(data.getString("url") == UPDATE_APK)
            return UpdateRelease(version, code, size, hash, json)
        }
    }
}

internal enum class UpdateStage { Idle, Checking, Current, Available, Downloading, Paused, Verifying, Ready, CheckFailed, DownloadFailed, InstallFailed }
internal data class UpdateState(val stage: UpdateStage = UpdateStage.Idle, val release: UpdateRelease? = null, val percent: Int? = null)

@Suppress("DEPRECATION")
internal fun requireCompatibleUpdate(
    archive: android.content.pm.PackageInfo,
    installed: android.content.pm.PackageInfo,
    versionCode: Long,
) {
    fun signatures(info: android.content.pm.PackageInfo): Set<String> =
        (if (android.os.Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
            .orEmpty().map { it.toCharsString() }.toSet()
    require(archive.packageName == installed.packageName)
    require(androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(archive) == versionCode)
    require(versionCode > androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(installed))
    require(signatures(archive).isNotEmpty() && signatures(archive) == signatures(installed))
}
