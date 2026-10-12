/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.securebackup.impl.reset

import dev.zacsweers.metro.Inject
import io.element.android.libraries.architecture.AsyncData
import io.element.android.libraries.matrix.api.diagnostics.StartupTrace
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.encryption.BackupState
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.encryption.IdentityResetHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Inject
class ResetIdentityFlowManager(
    private val encryptionService: EncryptionService,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) {
    private val resetHandleFlow: MutableStateFlow<AsyncData<IdentityResetHandle?>> = MutableStateFlow(AsyncData.Uninitialized)
    val currentHandleFlow: StateFlow<AsyncData<IdentityResetHandle?>> = resetHandleFlow
    private var whenResetIsDoneWaitingJob: Job? = null

    fun whenResetIsDone(block: () -> Unit) {
        StartupTrace.markReset("reset_wait_registered_state_" + encryptionService.backupStateStateFlow.value)
        whenResetIsDoneWaitingJob = sessionCoroutineScope.launch {
            StartupTrace.markReset("reset_wait_started")
            encryptionService.backupStateStateFlow.onEach { StartupTrace.markReset("reset_wait_observed_" + it) }.first { it == BackupState.ENABLED }
            StartupTrace.markReset("reset_wait_enabled_callback")
            block()
        }
    }

    fun getResetHandle(): StateFlow<AsyncData<IdentityResetHandle?>> {
        return if (resetHandleFlow.value.isLoading() || resetHandleFlow.value.isSuccess()) {
            resetHandleFlow
        } else {
            resetHandleFlow.value = AsyncData.Loading()

            sessionCoroutineScope.launch {
                StartupTrace.markReset("reset_start_identity_request")
                encryptionService.startIdentityReset()
                    .onSuccess { handle ->
                        StartupTrace.markReset("reset_start_identity_success_handle_" + (handle != null))
                        resetHandleFlow.value = AsyncData.Success(handle)
                    }
                    .onFailure {
                        StartupTrace.markReset("reset_start_identity_failure_" + it.javaClass.simpleName)
                        resetHandleFlow.value = AsyncData.Failure(it)
                    }
            }

            resetHandleFlow
        }
    }

    suspend fun cancel() {
        StartupTrace.markReset("reset_flow_cancel")
        currentHandleFlow.value.dataOrNull()?.cancel()
        resetHandleFlow.value = AsyncData.Uninitialized

        whenResetIsDoneWaitingJob?.cancel()
        whenResetIsDoneWaitingJob = null
    }
}
