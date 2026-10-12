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
import io.element.android.libraries.di.annotations.SessionCoroutineScope
import io.element.android.libraries.matrix.api.diagnostics.StartupTrace
import io.element.android.libraries.matrix.api.encryption.EncryptionService
import io.element.android.libraries.matrix.api.encryption.IdentityResetHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Inject
class ResetIdentityFlowManager(
    private val encryptionService: EncryptionService,
    @SessionCoroutineScope private val sessionCoroutineScope: CoroutineScope,
) {
    private val resetHandleFlow: MutableStateFlow<AsyncData<IdentityResetHandle?>> = MutableStateFlow(AsyncData.Uninitialized)
    val currentHandleFlow: StateFlow<AsyncData<IdentityResetHandle?>> = resetHandleFlow
    private var whenResetIsDoneWaitingJob: Job? = null
    private val resetCompleted = MutableStateFlow(false)

    fun whenResetIsDone(block: () -> Unit) {
        whenResetIsDoneWaitingJob?.cancel()
        whenResetIsDoneWaitingJob = sessionCoroutineScope.launch {
            resetCompleted.first { it }
            block()
        }
    }

    /** SDK reset success is distinct from backup activation, which may be disabled for this account. */
    fun onResetCompleted() {
        StartupTrace.markReset("reset_sdk_completed")
        resetCompleted.value = true
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
                        if (handle == null) onResetCompleted()
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
        whenResetIsDoneWaitingJob?.cancel()
        whenResetIsDoneWaitingJob = null
        resetCompleted.value = false
        currentHandleFlow.value.dataOrNull()?.cancel()
        resetHandleFlow.value = AsyncData.Uninitialized
    }
}
