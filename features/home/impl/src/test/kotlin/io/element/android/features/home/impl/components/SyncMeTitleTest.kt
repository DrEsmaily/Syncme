/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.home.impl.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SyncMeTitleTest : RobolectricTest() {
    @Test fun `brand letters are optically centered in light theme`() = checkTitle(Theme.Light)
    @Test fun `brand letters are optically centered in dark theme`() = checkTitle(Theme.Dark)
    @Test fun `brand letters are optically centered in black theme`() = checkTitle(Theme.Black)

    private fun checkTitle(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        setContent { ElementPreview(theme = theme) { SyncMeTitle() } }
        val pixels = onNodeWithTag("syncme_home_title").captureToImage().toPixelMap()
        val background = pixels[0, 0]
        val occupiedRows = (0 until pixels.height).filter { y ->
            (0 until pixels.width).any { x ->
                val pixel = pixels[x, y]
                abs(pixel.red - background.red) + abs(pixel.green - background.green) + abs(pixel.blue - background.blue) > 0.5f
            }
        }
        assertThat(occupiedRows).isNotEmpty()
        val center = (occupiedRows.first() + occupiedRows.last() + 1) / 2f
        assertThat(center).isWithin(2f).of(pixels.height / 2f)
    }
}
