/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.preferences.impl.updates

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.element.android.libraries.designsystem.components.dialogs.ConfirmationDialog
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.ui.strings.CommonStrings

/** Activity-level host: does not start installation or open a background window without a user tap. */
@Composable
fun UpdateReadyDialog() {
    val context = LocalContext.current
    val updater = remember(context) { SyncMeUpdater.get(context) }
    val state by updater.state.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    val acknowledged by updater.acknowledgedPrompt.collectAsStateWithLifecycle(minActiveState = Lifecycle.State.RESUMED)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Build.VERSION.SDK_INT < 26 || context.packageManager.canRequestPackageInstalls()) updater.install(context)
    }
    UpdateReadyDialogContent(
        state = state,
        acknowledgedHash = acknowledged,
        onLater = { state.release?.let(updater::acknowledgePrompt) },
        onInstall = {
            state.release?.let(updater::acknowledgePrompt)
            if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
                runCatching { permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())) }
                    .onFailure { updater.permissionUnavailable() }
            } else updater.install(context)
        },
    )
}

@Composable
internal fun UpdateReadyDialogContent(
    state: UpdateState,
    acknowledgedHash: String?,
    onInstall: () -> Unit,
    onLater: () -> Unit,
) {
    val release = state.release ?: return
    if (state.stage != UpdateStage.Ready || acknowledgedHash == release.sha256) return
    ConfirmationDialog(
        title = stringResource(CommonStrings.syncme_update_download_complete),
        content = stringResource(CommonStrings.syncme_update_install_question, release.version),
        submitText = stringResource(CommonStrings.syncme_update_install_now),
        cancelText = stringResource(CommonStrings.syncme_update_later),
        onSubmitClick = onInstall,
        onDismiss = onLater,
    )
}

@PreviewsDayNight
@Composable
internal fun UpdateReadyDialogPreview() = ElementPreview {
    UpdateReadyDialogContent(
        UpdateState(UpdateStage.Ready, UpdateRelease("1.2.0", 202611092, 74000000, "a".repeat(64), "")),
        acknowledgedHash = null,
        onInstall = {},
        onLater = {},
    )
}
