/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2022-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.x.initializer

import android.annotation.SuppressLint
import android.content.Context
import android.system.Os
import android.util.Log
import androidx.startup.Initializer
import io.element.android.features.rageshake.api.logs.createWriteToFilesConfiguration
import io.element.android.libraries.architecture.bindings
import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.featureflag.api.FeatureFlags
import io.element.android.libraries.matrix.api.tracing.TracingConfiguration
import io.element.android.libraries.matrix.api.tracing.LogLevel
import io.element.android.libraries.matrix.api.diagnostics.TlsAudit
import io.element.android.x.di.AppBindings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

private const val ELEMENT_X_TARGET = "elementx"

class PlatformInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // Init the SDK with the Android Context, so it can be used with rustls and other Android specific features
        runCatchingExceptions {
            org.matrix.rustsdk.Android.init(context)
        }.onFailure {
            // We can't log this to Timber since the logs aren't initialized yet, so we log it to Logcat instead
            @SuppressLint("LogNotTimber")
            Log.e("PlatformInitializer", "Failed to init the SDK with an Android Context. The app probably won't work properly.", it)
        }

        val appBindings = context.bindings<AppBindings>()
        val tracingService = appBindings.tracingService()
        val platformService = appBindings.platformService()
        val bugReporter = appBindings.bugReporter()
        Timber.plant(tracingService.createTimberTree(ELEMENT_X_TARGET))
        val preferencesStore = appBindings.preferencesStore()
        val featureFlagService = appBindings.featureFlagService()
        val logLevel = runBlocking { preferencesStore.getTracingLogLevelFlow().first() }
        // Dedicated connectivity diagnostic build: enable supported Rust networking
        // tracing targets in the SDK's local file writer. Never raise to TRACE:
        // TRACE may include sensitive metadata and causes significant log volume.
        val auditTargets = listOf(
            "matrix_sdk::http_client",
            "matrix_sdk::client",
            "reqwest",
            "hyper",
            "rustls",
            "hickory_resolver",
        )
        TlsAudit.record(
            "RUST_TRACING_CONFIG",
            "network_targets=" + auditTargets.joinToString(",") +
                " level=DEBUG native_log_files=cache/logs",
        )
        val tracingConfiguration = TracingConfiguration(
            writesToLogcat = runBlocking { featureFlagService.isFeatureEnabled(FeatureFlags.PrintLogsToLogcat) },
            writesToFilesConfiguration = bugReporter.createWriteToFilesConfiguration(),
            logLevel = LogLevel.DEBUG,
            extraTargets = listOf(ELEMENT_X_TARGET) + auditTargets,
            traceLogPacks = runBlocking { preferencesStore.getTracingLogPacksFlow().first() },
            sdkSentryDsn = appBindings.sentrySdkDsn()?.value?.takeIf { it.isNotBlank() },
        )
        bugReporter.setCurrentTracingLogLevel(LogLevel.DEBUG.name)

        // Init the tracing service with the configuration so we have logging enabled as soon as possible
        platformService.init(tracingConfiguration)
        // Also set env variable for rust back trace
        Os.setenv("RUST_BACKTRACE", "1", true)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = mutableListOf()
}
