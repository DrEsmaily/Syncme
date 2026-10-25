/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.home.impl.components

import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.designsystem.utils.LocalUiTestMode
import io.element.android.libraries.ui.common.layout.ContentAvoidingLayout
import io.element.android.libraries.ui.common.layout.ContentAvoidingLayoutData
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ContentAvoidingTimestampTest : RobolectricTest() {
    private val persian = "\u0633\u0644\u0627\u0645 \u0646\u0633\u062e\u0647 \u062c\u062f\u06cc\u062f"
    private val link = "https://example.org/download/Ab3KqobLRSoQ6RGj"

    @Test
    fun `Persian message ending in wrapped link does not overlap left timestamp`() = check("$persian\n\n$link", LayoutDirection.Rtl)

    @Test
    fun `Persian and link on same paragraph do not overlap left timestamp`() = check("$persian $link", LayoutDirection.Rtl)

    @Test
    fun `English message ending in Persian does not overlap right timestamp`() = check("Hello\n$persian", LayoutDirection.Ltr)

    @Test
    fun `English wrapped link does not overlap right timestamp`() = check(link, LayoutDirection.Ltr)

    @Test
    fun `short Persian line retains inline timestamp when space is available`() = check(persian, LayoutDirection.Rtl, expectInline = true)

    @Test
    fun `short English line retains inline timestamp when space is available`() = check("Hello", LayoutDirection.Ltr, expectInline = true)

    private fun check(text: String, direction: LayoutDirection, expectInline: Boolean = false) = runAndroidComposeUiTest<ComponentActivity> {
        var result: TextLayoutResult? = null
        var contentBounds = Rect.Zero
        var timestampBounds = Rect.Zero
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction, LocalUiTestMode provides false) {
                ContentAvoidingLayout(
                    modifier = Modifier.width(240.dp),
                    spacing = 4.dp,
                    overlay = {
                        Box(Modifier.size(64.dp, 20.dp).onGloballyPositioned { timestampBounds = it.boundsInRoot() })
                    },
                ) {
                    Text(
                        text = text,
                        style = TextStyle(fontSize = 18.sp, textDirection = TextDirection.Content),
                        modifier = Modifier.onGloballyPositioned { contentBounds = it.boundsInRoot() },
                        onTextLayout = ContentAvoidingLayout.measureLastTextLine(onContentLayoutChange = { data ->
                            onContentLayoutChange(data)
                        }).let { measure -> { layout -> result = layout; measure(layout) } },
                    )
                }
            }
        }
        waitForIdle()
        val layout = requireNotNull(result)
        val lastLine = layout.lineCount - 1
        val occupied = Rect(
            contentBounds.left + layout.getLineLeft(lastLine),
            contentBounds.top + layout.getLineTop(lastLine),
            contentBounds.left + layout.getLineRight(lastLine),
            contentBounds.top + layout.getLineBottom(lastLine),
        )
        assertThat(occupied.overlaps(timestampBounds)).isFalse()
        if (expectInline) assertThat(timestampBounds.top).isLessThan(contentBounds.bottom)
    }

    @Test
    fun `legacy RTL measurement reserves link at the left edge`() = checkLegacy(LayoutDirection.Rtl)

    @Test
    fun `legacy LTR measurement reserves Persian at the right edge`() = checkLegacy(LayoutDirection.Ltr)

    private fun checkLegacy(direction: LayoutDirection) = runAndroidComposeUiTest<ComponentActivity> {
        val text = if (direction == LayoutDirection.Rtl) "$persian\n$link" else "Hello\n$persian"
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, TextPaint().apply { textSize = 18f }, 240).build()
        var measured: ContentAvoidingLayoutData? = null
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                val measure = ContentAvoidingLayout.measureLegacyLastTextLine({ measured = it })
                SideEffect { measure(layout) }
            }
        }
        waitForIdle()
        val last = layout.lineCount - 1
        val expected = if (direction == LayoutDirection.Rtl) layout.width - layout.getLineLeft(last) else layout.getLineRight(last)
        assertThat(requireNotNull(measured).nonOverlappingContentWidth.toFloat()).isWithin(1f).of(expected)
    }
}
