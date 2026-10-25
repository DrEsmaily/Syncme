/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.messages.impl.timeline.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.theme.Theme
import io.element.android.features.messages.impl.timeline.model.anAggregatedReaction
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MessagesReactionBackgroundTest : RobolectricTest() {
    @Test
    fun `single selected reaction is uniform in Light`() = checkBackground(Theme.Light, 1)

    @Test
    fun `multiple selected reactions are uniform in Light`() = checkBackground(Theme.Light, 2)

    @Test
    fun `single selected reaction is uniform in Dark`() = checkBackground(Theme.Dark, 1)

    @Test
    fun `multiple selected reactions are uniform in Dark`() = checkBackground(Theme.Dark, 2)

    @Test
    fun `single selected reaction is uniform in Black`() = checkBackground(Theme.Black, 1)

    @Test
    fun `multiple selected reactions are uniform in Black`() = checkBackground(Theme.Black, 2)

    private fun checkBackground(theme: Theme, count: Int) = runAndroidComposeUiTest<ComponentActivity> {
        var expected = Color.Unspecified
        var paddingX = 0
        var contentX = 0
        setContent {
            ElementTheme(theme = theme, applySystemBarsUpdate = false) {
                expected = ElementTheme.colors.bgAccentSelected.compositeOver(ElementTheme.colors.bgCanvasDefault)
                with(LocalDensity.current) {
                    paddingX = 6.dp.roundToPx()
                    contentX = 14.dp.roundToPx()
                }
                Box(Modifier.background(ElementTheme.colors.bgCanvasDefault).padding(16.dp)) {
                    MessagesReactionButton(
                        modifier = Modifier.testTag("reaction"),
                        onClick = {},
                        onLongClick = {},
                        content = MessagesReactionsButtonContent.Reaction(
                            // Blank glyphs expose the background beneath the reaction content.
                            anAggregatedReaction(key = "\u2003\u2003", count = count, isHighlighted = true),
                        ),
                    )
                }
            }
        }
        val pixels = onNodeWithTag("reaction").captureToImage().toPixelMap()
        listOf(paddingX, contentX).forEach { x ->
            val actual = pixels[x, pixels.height / 2]
            assertThat(actual.red).isWithin(2f / 255).of(expected.red)
            assertThat(actual.green).isWithin(2f / 255).of(expected.green)
            assertThat(actual.blue).isWithin(2f / 255).of(expected.blue)
        }
    }
}
