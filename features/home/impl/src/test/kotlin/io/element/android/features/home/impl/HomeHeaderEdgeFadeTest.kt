/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.home.impl

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.features.home.impl.components.homeHeaderEdgeFade
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlin.math.abs
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeHeaderEdgeFadeTest : RobolectricTest() {
    @Test fun `fade follows light theme without changing layout or taps`() = checkFade(Theme.Light)
    @Test fun `fade follows dark theme without changing layout or taps`() = checkFade(Theme.Dark)
    @Test fun `fade follows black theme without changing layout or taps`() = checkFade(Theme.Black)

    private fun checkFade(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var scrolled by mutableStateOf(false)
        var background = Color.Black
        var taps = 0
        setContent {
            ElementTheme(theme = theme) {
                background = ElementTheme.colors.bgCanvasDefault
                Box(
                    Modifier.size(100.dp).testTag("viewport")
                        .homeHeaderEdgeFade(background) { scrolled }
                        .background(Color.Red)
                        .clickable { taps++ }
                )
            }
        }
        val viewport = onNodeWithTag("viewport")
        val bounds = viewport.fetchSemanticsNode().boundsInRoot
        fun distance(a: Color, b: Color) = abs(a.red - b.red) + abs(a.green - b.green) + abs(a.blue - b.blue)
        val resting = viewport.captureToImage().toPixelMap()
        assertThat(distance(resting[resting.width / 2, 0], Color.Red)).isLessThan(0.03f)
        runOnIdle { scrolled = true }
        val pixels = viewport.captureToImage().toPixelMap()
        val x = pixels.width / 2
        val upper = pixels[x, 0]
        val middle = pixels[x, 2]
        val below = pixels[x, pixels.height / 2]
        assertThat(distance(upper, background)).isLessThan(0.25f)
        assertThat(distance(middle, Color.Red)).isLessThan(distance(upper, Color.Red))
        assertThat(distance(below, Color.Red)).isLessThan(0.03f)
        assertThat(viewport.fetchSemanticsNode().boundsInRoot).isEqualTo(bounds)
        viewport.performClick()
        assertThat(taps).isEqualTo(1)
        runOnIdle { scrolled = false }
        val reset = viewport.captureToImage().toPixelMap()
        assertThat(distance(reset[x, 0], Color.Red)).isLessThan(0.03f)
    }
}
