/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.preferences.impl.updates

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UpdateReadyDialogTest : RobolectricTest() {
    private val release = UpdateRelease("1.2.0", 202611092, 74000000, "a".repeat(64), "")
    @Test fun `later keeps installation optional in light theme`() = checkLater(Theme.Light)
    @Test fun `later keeps installation optional in dark theme`() = checkLater(Theme.Dark)
    @Test fun `later keeps installation optional in black theme`() = checkLater(Theme.Black)

    private fun checkLater(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var acknowledged by mutableStateOf<String?>(null)
        var installations = 0
        setContent {
            ElementPreview(theme = theme) {
                UpdateReadyDialogContent(UpdateState(UpdateStage.Ready, release), acknowledged, { installations++ }, {
                    acknowledged = release.sha256
                })
            }
        }
        onNodeWithText("Update downloaded").assertIsDisplayed()
        onNodeWithText("SyncMe 1.2.0 is ready to install. Install it now?").assertIsDisplayed()
        assertThat(installations).isEqualTo(0)
        onNodeWithText("Later").performClick()
        onNodeWithText("Update downloaded").assertDoesNotExist()
        assertThat(installations).isEqualTo(0)
    }

    @Test fun `only verified ready updates prompt and installation requires a tap`() = runAndroidComposeUiTest<ComponentActivity> {
        var state by mutableStateOf(UpdateState(UpdateStage.Downloading, release))
        var installations = 0
        setContent {
            ElementPreview {
                UpdateReadyDialogContent(state, null, { installations++ }, {})
            }
        }
        for (stage in UpdateStage.entries.filter { it != UpdateStage.Ready }) {
            runOnIdle { state = UpdateState(stage, release) }
            onNodeWithText("Update downloaded").assertDoesNotExist()
        }
        runOnIdle { state = UpdateState(UpdateStage.Ready, release) }
        onNodeWithText("Install").assertIsDisplayed()
        assertThat(installations).isEqualTo(0)
        onNodeWithText("Install").performClick()
        assertThat(installations).isEqualTo(1)
    }
}
