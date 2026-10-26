/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.preferences.impl.root

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.HorizontalDivider
import io.element.android.libraries.matrix.ui.components.aMatrixUser
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SwitchAccountsBoundsTest : RobolectricTest() {
    @Test fun `switch row excludes preceding space in light theme`() = checkBounds(Theme.Light)
    @Test fun `switch row excludes preceding space in dark theme`() = checkBounds(Theme.Dark)
    @Test fun `switch row excludes preceding space in black theme`() = checkBounds(Theme.Black)

    private fun checkBounds(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var toggles = 0
        var pxPerDp = 1f
        setContent {
            pxPerDp = LocalDensity.current.density
            ElementPreview(theme = theme) {
                Column(Modifier.testTag("switch_section")) {
                    Spacer(Modifier.height(24.dp))
                    MultiAccountSection(
                        aPreferencesRootState(otherSessions = listOf(aMatrixUser()), eventSink = {
                            if (it == PreferencesRootEvent.ToggleOtherAccountsExpanded) toggles++
                        }),
                        onAddAccountClick = {},
                    )
                    HorizontalDivider(Modifier.testTag("following_divider"))
                }
            }
        }
        val row = onNodeWithText("Switch accounts").fetchSemanticsNode().boundsInRoot
        val section = onNodeWithTag("switch_section").fetchSemanticsNode().boundsInRoot
        val next = onNodeWithTag("following_divider").fetchSemanticsNode().boundsInRoot
        // 24dp leading spacer + 16dp section gap + 1dp divider precede the touch target.
        val expectedTop = 41f * pxPerDp
        assertThat(row.top - section.top).isWithin(1f).of(expectedTop)
        assertThat(row.bottom).isWithin(1f).of(next.top)
        onNodeWithTag("switch_section").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width / 2f, 30f * pxPerDp))
        }
        assertThat(toggles).isEqualTo(0)
        onNodeWithText("Switch accounts").performClick()
        assertThat(toggles).isEqualTo(1)
    }
}
