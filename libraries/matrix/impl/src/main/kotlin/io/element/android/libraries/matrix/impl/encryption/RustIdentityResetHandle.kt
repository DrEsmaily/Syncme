/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl.encryption

import io.element.android.libraries.core.extensions.runCatchingExceptions
import io.element.android.libraries.matrix.api.diagnostics.StartupTrace
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.encryption.IdentityOAuthResetHandle
import io.element.android.libraries.matrix.api.encryption.IdentityPasswordResetHandle
import io.element.android.libraries.matrix.api.encryption.IdentityResetHandle
import org.matrix.rustcomponents.sdk.AuthData
import org.matrix.rustcomponents.sdk.AuthDataPasswordDetails
import org.matrix.rustcomponents.sdk.CrossSigningResetAuthType

object RustIdentityResetHandleFactory {
    fun create(
        userId: UserId,
        identityResetHandle: org.matrix.rustcomponents.sdk.IdentityResetHandle?
    ): Result<IdentityResetHandle?> {
        return runCatchingExceptions {
            identityResetHandle?.let {
                when (val authType = identityResetHandle.authType()) {
                    is CrossSigningResetAuthType.OAuth -> RustIdentityOAuthResetHandle(identityResetHandle, authType.info.approvalUrl)
                    // User interactive authentication (user + password)
                    CrossSigningResetAuthType.Uiaa -> RustPasswordIdentityResetHandle(userId, identityResetHandle)
                }
            }
        }
    }
}

class RustPasswordIdentityResetHandle(
    private val userId: UserId,
    private val identityResetHandle: org.matrix.rustcomponents.sdk.IdentityResetHandle,
) : IdentityPasswordResetHandle {
    override suspend fun resetPassword(password: String): Result<Unit> {
        StartupTrace.markReset("rust_handle_reset_password_begin")
        return runCatchingExceptions {
            identityResetHandle.reset(AuthData.Password(AuthDataPasswordDetails(userId.value, password)))
        }.also { result ->
            StartupTrace.markReset("rust_handle_reset_password_" + if (result.isSuccess) "success" else "failure_" + result.exceptionOrNull()?.javaClass?.simpleName)
        }
    }

    override suspend fun cancel() {
        StartupTrace.markReset("rust_handle_cancel_begin")
        identityResetHandle.cancelAndDestroy()
        StartupTrace.markReset("rust_handle_cancel_end")
    }
}

class RustIdentityOAuthResetHandle(
    private val identityResetHandle: org.matrix.rustcomponents.sdk.IdentityResetHandle,
    override val url: String,
) : IdentityOAuthResetHandle {
    override suspend fun resetOAuth(): Result<Unit> {
        return runCatchingExceptions { identityResetHandle.reset(null) }
    }

    override suspend fun cancel() {
        identityResetHandle.cancelAndDestroy()
    }
}

private suspend fun org.matrix.rustcomponents.sdk.IdentityResetHandle.cancelAndDestroy() {
    cancel()
    destroy()
}
