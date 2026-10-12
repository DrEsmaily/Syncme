/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.pushproviders.firebase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class InMemoryFirebaseStore(
    installationId: String? = null
) : FirebaseStore {
    private val token = MutableStateFlow(installationId)

    override fun getInstallationId(): String? = token.value

    override fun fcmInstallationIdFlow(): Flow<String?> = token

    override fun storeInstallationId(installationId: String?) {
        token.value = installationId
    }
}
