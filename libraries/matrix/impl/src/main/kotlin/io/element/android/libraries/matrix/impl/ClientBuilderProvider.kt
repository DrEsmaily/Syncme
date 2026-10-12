/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.matrix.impl

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.matrix.api.diagnostics.TlsAudit
import org.matrix.rustcomponents.sdk.ClientBuilder

interface ClientBuilderProvider {
    fun provide(): ClientBuilder
}

@ContributesBinding(AppScope::class)
class RustClientBuilderProvider : ClientBuilderProvider {
    override fun provide(): ClientBuilder {
        // SyncMe 234 experiment: disable the Matrix Rust HTTP client's
        // certificate/hostname verification at its source rather than patching
        // Android's rustls-platform-verifier. TLS transport remains enabled.
        // This intentionally permits server impersonation (MITM).
        TlsAudit.record(
            "RUST_MATRIX_CLIENT_BUILDER",
            "disableSslVerification=true source=RustClientBuilderProvider.provide; native_requests_not_intercepted",
            caller = true,
        )
        return ClientBuilder().disableSslVerification()
    }
}
