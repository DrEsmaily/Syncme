/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.messages.impl.timeline.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.features.messages.impl.timeline.aTimelineRoomInfo
import io.element.android.features.messages.impl.timeline.model.TimelineItemGroupPosition
import io.element.android.features.messages.impl.timeline.model.bubble.BubbleState
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BubblePaletteTest : RobolectricTest() {
    @Test fun `light sent background`() = check(Theme.Light, true, Color(0xFFDBEAFE))
    @Test fun `light received background`() = check(Theme.Light, false, Color(0xFFF4F4F5))
    @Test fun `dark sent background`() = check(Theme.Dark, true, Color(0xFF233B57))
    @Test fun `dark received background`() = check(Theme.Dark, false, Color(0xFF27272A))
    @Test fun `black sent background`() = check(Theme.Black, true, Color(0xFF142B43))
    @Test fun `black received background`() = check(Theme.Black, false, Color(0xFF18181B))
    @Test fun `custom warning background remains visible`() = check(Theme.Dark, true, Color.Red, custom = Color.Red)

    private fun check(theme: Theme, mine: Boolean, expected: Color, custom: Color? = null) = runAndroidComposeUiTest<ComponentActivity> {
        var timestampTint = Color.Unspecified
        setContent {
            ElementPreview(theme = theme) {
                timestampTint = MessageEventBubbleDefaults.timestampColor()
                MessageEventBubble(
                    state = BubbleState(TimelineItemGroupPosition.None, mine, aTimelineRoomInfo(isDm = true)),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {}, onLongClick = {}, customBackgroundColor = custom,
                    modifier = Modifier.testTag("palette-bubble"),
                ) { Box(Modifier.size(160.dp, 80.dp)) }
            }
        }
        if (custom == null) {
            val first = timestampTint.luminance()
            val second = expected.luminance()
            assertThat((maxOf(first, second) + .05f) / (minOf(first, second) + .05f)).isAtLeast(4.5f)
        }
        val pixels = onNodeWithTag("palette-bubble").captureToImage().toPixelMap()
        assertThat(pixels[pixels.width / 2, pixels.height / 2].toArgb()).isEqualTo(expected.toArgb())
    }
}
