/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.preferences.impl.root

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalendarSettingsTest : RobolectricTest() {
    @Test fun `calendar selection in light theme`() = checkSelection(Theme.Light)
    @Test fun `calendar selection in dark theme`() = checkSelection(Theme.Dark)
    @Test fun `calendar selection in black theme`() = checkSelection(Theme.Black)

    private fun checkSelection(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var selected: CalendarOption? = null
        setContent {
            ElementPreview(theme = theme) {
                AppSettingsSection(aPreferencesRootState().copy(eventSink = {
                    if (it is PreferencesRootEvent.SetCalendar) selected = it.calendar
                }), {}, {}, {})
            }
        }
        onNodeWithText("Calendar").assertIsDisplayed().performClick()
        onNodeWithText("Persian (Solar Hijri)").assertIsDisplayed().performClick()
        assertThat(selected).isEqualTo(CalendarOption.Persian)
        onNodeWithText("Updates").assertIsDisplayed()
    }
}
