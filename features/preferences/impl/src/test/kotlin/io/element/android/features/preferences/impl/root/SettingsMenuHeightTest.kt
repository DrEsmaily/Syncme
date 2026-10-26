/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.preferences.impl.root

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.features.preferences.impl.updates.UpdateSettingsContent
import io.element.android.features.preferences.impl.updates.UpdateState
import io.element.android.features.preferences.impl.updates.UpdateStage
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.LocalSettingsMenuRowHeight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.ui.components.aMatrixUser
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SettingsMenuHeightTest : RobolectricTest() {
    @Test fun `menu rows match updates in light theme`() = checkRows(Theme.Light)
    @Test fun `menu rows match updates in dark theme`() = checkRows(Theme.Dark)
    @Test fun `menu rows match updates in black theme`() = checkRows(Theme.Black)

    private fun checkRows(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var clicks = 0
        setContent {
            ElementPreview(theme = theme) {
                Column {
                    ListItem(onClick = {}) { Text("Outside settings") }
                    CompositionLocalProvider(LocalSettingsMenuRowHeight provides 72.dp) {
                        ListItem(onClick = { clicks++ }) { Text("Settings action") }
                        ListItem(onClick = {}) { Text("Submenu action") }
                        UpdateSettingsContent(UpdateState(UpdateStage.Current)) {}
                        MultiAccountSection(aPreferencesRootState(otherSessions = listOf(aMatrixUser())), {})
                    }
                }
            }
        }
        fun height(label: String) = onNodeWithText(label).fetchSemanticsNode().boundsInRoot.height
        val reference = height("Updates")
        for (label in listOf("Settings action", "Submenu action", "Switch accounts")) {
            assertThat(height(label)).isWithin(1f).of(reference)
        }
        assertThat(height("Outside settings")).isLessThan(reference)
        onNodeWithText("Settings action").performClick()
        assertThat(clicks).isEqualTo(1)
    }
}
