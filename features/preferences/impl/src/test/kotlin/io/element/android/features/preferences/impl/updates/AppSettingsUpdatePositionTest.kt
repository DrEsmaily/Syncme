/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.preferences.impl.updates

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.features.preferences.impl.root.AppSettingsSection
import io.element.android.features.preferences.impl.root.aPreferencesRootState
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppSettingsUpdatePositionTest : RobolectricTest() {
    @Test fun `updates is the last app settings row in light theme`() = checkOrder(Theme.Light)
    @Test fun `updates is the last app settings row in dark theme`() = checkOrder(Theme.Dark)
    @Test fun `updates is the last app settings row in black theme`() = checkOrder(Theme.Black)

    private fun checkOrder(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var visible by mutableStateOf(true)
        setContent {
            ElementPreview(theme = theme) {
                Column {
                    if (visible) AppSettingsSection(aPreferencesRootState(), {}, {}, {})
                    Text("After app settings")
                }
            }
        }
        val lock = onNodeWithText("Screen lock").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val update = onNodeWithText("Updates").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val following = onNodeWithText("After app settings").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(update.top).isAtLeast(lock.bottom)
        assertThat(following.top).isAtLeast(update.bottom)
        onNodeWithText("Tap to check for updates").assertIsDisplayed()
        runOnIdle { visible = false }
        waitForIdle()
        runOnIdle { visible = true }
        onNodeWithText("Tap to check for updates").assertIsDisplayed()
    }
}
