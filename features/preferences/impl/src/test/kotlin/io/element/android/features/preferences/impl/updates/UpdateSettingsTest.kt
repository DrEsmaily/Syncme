/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.preferences.impl.updates

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UpdateSettingsTest : RobolectricTest() {
    @Test
    fun `up to date and retry states use clear English labels`() = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            ElementTheme(theme = Theme.Light) {
                Column {
                    UpdateSettingsContent(UpdateState(UpdateStage.Current)) {}
                    UpdateSettingsContent(UpdateState(UpdateStage.CheckFailed)) {}
                    UpdateSettingsContent(UpdateState(UpdateStage.DownloadFailed)) {}
                    UpdateSettingsContent(UpdateState(UpdateStage.Ready)) {}
                }
            }
        }
        onNodeWithText("You’re up to date").assertIsDisplayed()
        onNodeWithText("Download failed").assertIsDisplayed()
        onNodeWithText("Install update").assertIsDisplayed()
    }

    @Test
    fun `active download shows progress and prevents duplicate download taps`() = runAndroidComposeUiTest<ComponentActivity> {
        setContent { ElementTheme(theme = Theme.Dark) { UpdateSettingsContent(UpdateState(UpdateStage.Downloading, percent = 42)) {} } }
        onNodeWithText("42%", useUnmergedTree = false).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun `paused download retains progress and accepts retry in every theme`() = runAndroidComposeUiTest<ComponentActivity> {
        var clicks = 0
        setContent {
            Column {
                listOf(Theme.Light, Theme.Dark, Theme.Black).forEach { theme ->
                    ElementTheme(theme = theme) {
                        UpdateSettingsContent(UpdateState(UpdateStage.Paused, percent = 42)) { clicks++ }
                    }
                }
            }
        }
        onAllNodesWithText("Waiting to reconnect · Tap to retry").fetchSemanticsNodes().also { assertThat(it).hasSize(3) }
        onAllNodesWithText("Download paused").apply {
            for (index in 0..2) get(index).assertIsEnabled().performClick()
        }
        assertThat(clicks).isEqualTo(3)
    }

    @Test
    fun `available update shows version and accepts a download action`() = runAndroidComposeUiTest<ComponentActivity> {
        val release = UpdateRelease("1.1.12", 202611042, 10, "a".repeat(64), "")
        var clicks = 0
        setContent { ElementTheme(theme = Theme.Black) { UpdateSettingsContent(UpdateState(UpdateStage.Available, release)) { clicks++ } } }
        onNodeWithText("Update available").assertIsDisplayed().performClick()
        onNodeWithText("Version 1.1.12 · Tap to download").assertIsDisplayed()
        assertThat(clicks).isEqualTo(1)
    }
}
