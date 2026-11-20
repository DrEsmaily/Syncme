/*
 * Copyright (c) 2026 SyncMe contributors.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 */

package io.element.android.features.messages.impl.timeline.components.event

import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.URLSpan
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.timeline.factories.event.formatBubbleCaption
import io.element.android.tests.testutils.robolectric.RobolectricTest
import io.element.android.wysiwyg.view.spans.InlineCodeSpan
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InteractiveBubbleTextTest : RobolectricTest() {
    @Test
    fun `links respond in left and right aligned lines`() {
        val url = "https://example.org"
        for ((prefix, label) in listOf("See " to "this link", "ببین " to "این لینک", "URL: " to url)) {
            val text = SpannableString(prefix + label).apply {
                setSpan(URLSpan(url), prefix.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val layout = makeLayout(text)
            val target = layout.bubbleTextActionAt(layout.pointInside(prefix.length + 2))
            val link = (target as? BubbleTextAction.Open)?.link
            assertThat(link?.url).isEqualTo(url)
            assertThat(link?.text).isEqualTo(label)
            assertThat(layout.bubbleTextActionAt(layout.pointInside(1))).isNull()
        }
    }

    @Test
    fun `a tagged URL copies only the tagged range while a regular URL opens`() {
        val caption = "Copy `https://example.org/a` or open https://example.net"
        val text = caption.formatBubbleCaption()
        val layout = makeLayout(text)
        val copy = layout.bubbleTextActionAt(layout.pointInside("Copy ".length + 3))
        assertThat((copy as? BubbleTextAction.Copy)?.value).isEqualTo("https://example.org/a")
        val open = layout.bubbleTextActionAt(layout.pointInside(text.indexOf("https://example.net") + 3))
        assertThat((open as? BubbleTextAction.Open)?.link?.url).isEqualTo("https://example.net")
        assertThat(layout.bubbleTextActionAt(layout.pointInside(1))).isNull()
    }

    @Test
    fun `link span inside inline code only copies its exact Persian text`() {
        val snippet = "کد ۴۲"
        val body = SpannableString("پیشوند $snippet پایان")
        val start = body.indexOf(snippet)
        body.setSpan(InlineCodeSpan(), start, start + snippet.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        body.setSpan(URLSpan("https://example.org"), start, start + snippet.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val layout = makeLayout(body)
        assertThat(layout.bubbleTextActionAt(layout.pointInside(start + 1)))
            .isEqualTo(BubbleTextAction.Copy(snippet, start))
    }

    private fun makeLayout(text: CharSequence): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, TextPaint().apply { textSize = 24f }, 900)
            .build()

    private fun Layout.pointInside(index: Int): Offset {
        val line = getLineForOffset(index)
        return Offset(
            (getPrimaryHorizontal(index) + getPrimaryHorizontal(index + 1)) / 2f,
            (getLineTop(line) + getLineBottom(line)) / 2f,
        )
    }
}
