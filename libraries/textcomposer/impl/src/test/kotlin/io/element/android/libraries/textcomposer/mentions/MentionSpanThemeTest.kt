/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.libraries.textcomposer.mentions

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.core.graphics.ColorUtils
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class MentionSpanThemeTest : RobolectricTest() {
    @Test
    fun `Light mentions use readable tinted colors`() = checkTheme(Theme.Light)

    @Test
    fun `Dark mentions use readable tinted colors`() = checkTheme(Theme.Dark)

    @Test
    fun `Black mentions use readable tinted colors`() = checkTheme(Theme.Black)

    private fun checkTheme(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        val styles = MentionSpanTheme(UserId("@me:example.org"))
        setContent {
            ElementTheme(theme = theme, applySystemBarsUpdate = false) {
                styles.updateStyles()
            }
        }
        waitForIdle()
        assertThat(styles.otherBackgroundColor).isNotEqualTo(Color.BLACK)
        assertThat(ColorUtils.calculateContrast(styles.otherTextColor, styles.otherBackgroundColor)).isAtLeast(4.5)
        assertThat(ColorUtils.calculateContrast(styles.currentUserTextColor, styles.currentUserBackgroundColor)).isAtLeast(4.5)
        assertThat(styles.paddingValuesPx.value.first).isEqualTo(styles.paddingValuesPx.value.second)
    }
}
