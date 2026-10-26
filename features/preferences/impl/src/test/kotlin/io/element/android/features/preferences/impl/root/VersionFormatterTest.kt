/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.preferences.impl.root

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.test.core.aBuildMeta
import io.element.android.services.toolbox.test.strings.FakeStringProvider
import kotlinx.coroutines.test.runTest
import org.junit.Test

class VersionFormatterTest {
    @Test
    fun `version formatter should return formatted version`() = runTest {
        val sut = DefaultVersionFormatter(
            stringProvider = FakeStringProvider(defaultResult = VERSION),
            buildMeta = aBuildMeta(
                gitBranchName = "main",
                versionName = "versionName",
                versionCode = 123
            )
        )
        assertThat(sut.get()).isEqualTo("SyncMe by 0x07c4 • Build versionName")
    }

    @Test
    fun `release version and build number are distinct`() {
        val sut = DefaultVersionFormatter(
            stringProvider = FakeStringProvider(),
            buildMeta = aBuildMeta(versionName = "1.1.14").copy(buildNumber = 270),
        )
        assertThat(sut.get()).isEqualTo("SyncMe by 0x07c4 • 1.1.14 • Build 270")
    }

    companion object {
        const val VERSION = "version: "
    }
}
