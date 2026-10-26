/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.home.impl.updates

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.libraries.designsystem.theme.components.DropdownMenuItem
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.ui.strings.CommonStrings

@Composable
internal fun UpdateMenuItem(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val updater = remember(context) { SyncMeUpdater.get(context) }
    val state by updater.state.collectAsStateWithLifecycle()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()) updater.install(context)
    }
    UpdateMenuContent(state) {
        onDismiss()
        when (state.stage) {
            UpdateStage.Available, UpdateStage.DownloadFailed -> updater.download()
            UpdateStage.Ready, UpdateStage.InstallFailed -> {
                if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                    runCatching { permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())) }
                        .onFailure { updater.permissionUnavailable() }
                } else updater.install(context)
            }
            else -> updater.check()
        }
    }
}

@Composable
internal fun UpdateMenuContent(state: UpdateState, onClick: () -> Unit) {
    val title = when (state.stage) {
        UpdateStage.Available -> CommonStrings.syncme_update_available
        UpdateStage.Downloading -> CommonStrings.syncme_update_downloading
        UpdateStage.Paused -> CommonStrings.syncme_update_paused
        UpdateStage.Verifying -> CommonStrings.syncme_update_verifying
        UpdateStage.Ready, UpdateStage.InstallFailed -> CommonStrings.syncme_update_install
        UpdateStage.DownloadFailed -> CommonStrings.syncme_update_download_failed
        else -> CommonStrings.syncme_update_menu
    }
    val subtitle = when (state.stage) {
        UpdateStage.Idle -> stringResource(CommonStrings.syncme_update_tap_check)
        UpdateStage.Checking -> stringResource(CommonStrings.syncme_update_checking)
        UpdateStage.Current -> stringResource(CommonStrings.syncme_update_current)
        UpdateStage.Available -> stringResource(CommonStrings.syncme_update_version, state.release?.version.orEmpty())
        UpdateStage.Downloading -> state.percent?.let { "$it%" } ?: stringResource(CommonStrings.syncme_update_starting)
        UpdateStage.Paused -> stringResource(CommonStrings.syncme_update_waiting)
        UpdateStage.Verifying -> stringResource(CommonStrings.syncme_update_verifying_detail)
        UpdateStage.Ready -> stringResource(CommonStrings.syncme_update_ready)
        UpdateStage.InstallFailed -> stringResource(CommonStrings.syncme_update_install_failed)
        UpdateStage.CheckFailed -> stringResource(CommonStrings.syncme_update_retry)
        UpdateStage.DownloadFailed -> stringResource(CommonStrings.syncme_update_download_retry)
    }
    DropdownMenuItem(
        text = {
            Column {
                Text(stringResource(title))
                Text(subtitle, style = ElementTheme.typography.fontBodySmRegular, color = ElementTheme.colors.textSecondary)
            }
        },
        onClick = onClick,
        enabled = state.stage !in setOf(UpdateStage.Checking, UpdateStage.Downloading, UpdateStage.Paused, UpdateStage.Verifying),
        leadingIcon = { Icon(CompoundIcons.ArrowDown(), contentDescription = null, tint = ElementTheme.colors.iconSecondary) },
    )
}

internal class UpdateMenuPreviewStates : PreviewParameterProvider<UpdateState> {
    override val values = UpdateStage.entries.asSequence().map { stage ->
        UpdateState(stage, UpdateRelease("1.1.12", 202611042, 74000000, "a".repeat(64), ""), 42)
    }
}

@PreviewsDayNight
@Composable
internal fun UpdateMenuPreview(@PreviewParameter(UpdateMenuPreviewStates::class) state: UpdateState) = ElementPreview {
    UpdateMenuContent(state) {}
}
