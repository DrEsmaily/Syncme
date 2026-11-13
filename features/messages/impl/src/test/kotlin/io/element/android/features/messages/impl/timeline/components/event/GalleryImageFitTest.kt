/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class, coil3.annotation.ExperimentalCoilApi::class)
package io.element.android.features.messages.impl.timeline.components.event

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GalleryImageFitTest : RobolectricTest() {
    @Test fun `landscape photos retain every corner in light theme`() = checkFit(400, 100, Theme.Light)
    @Test fun `portrait photos retain every corner in light theme`() = checkFit(100, 400, Theme.Light)
    @Test fun `landscape photos retain every corner in dark theme`() = checkFit(400, 100, Theme.Dark)
    @Test fun `portrait photos retain every corner in dark theme`() = checkFit(100, 400, Theme.Dark)
    @Test fun `landscape photos retain every corner in black theme`() = checkFit(400, 100, Theme.Black)
    @Test fun `portrait photos retain every corner in black theme`() = checkFit(100, 400, Theme.Black)

    private fun checkFit(width: Int, height: Int, theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val colors = listOf(android.graphics.Color.RED, android.graphics.Color.GREEN, android.graphics.Color.BLUE, android.graphics.Color.YELLOW)
        colors.forEachIndexed { index, color ->
            val left = if (index % 2 == 0) 0f else width * 7f / 8
            val top = if (index < 2) 0f else height * 7f / 8
            canvas.drawRect(left, top, left + width / 8f, top + height / 8f, Paint().apply { this.color = color })
        }
        var clicked = -1
        setContent {
            ElementPreview(theme = theme) {
                CompositionLocalProvider(
                    LocalInspectionMode provides true,
                    LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler { bitmap.asImage() },
                ) {
                    TimelineItemGalleryView(
                        eventId = null,
                        content = aTimelineItemGalleryContent(items = listOf(
                            aGalleryItem(filename = "first-photo", width = width, height = height),
                            aGalleryItem(filename = "second-photo", width = width, height = height),
                        )),
                        onGalleryItemClick = { clicked = it }, onLongClick = null,
                    )
                }
            }
        }
        for ((index, filename) in listOf("first-photo", "second-photo").withIndex()) {
            val node = onNodeWithContentDescription(filename, useUnmergedTree = true)
            val pixels = node.captureToImage().toPixelMap()
            val counts = IntArray(4)
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                val c = pixels[x, y]
                if (c.red > .9f && c.green < .1f && c.blue < .1f) counts[0]++
                if (c.red < .1f && c.green > .9f && c.blue < .1f) counts[1]++
                if (c.red < .1f && c.green < .1f && c.blue > .9f) counts[2]++
                if (c.red > .9f && c.green > .9f && c.blue < .1f) counts[3]++
            }
            counts.forEach { assertThat(it).isAtLeast(4) }
            node.performTouchInput { click() }
            runOnIdle { assertThat(clicked).isEqualTo(index) }
        }
    }
}
