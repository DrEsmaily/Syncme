/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.home.impl.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.eventformatter.api.RoomLatestEventFormatter
import io.element.android.features.home.impl.model.LatestEvent
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoomSummaryRowDirectionTest : RobolectricTest() {
    @Test
    fun `Persian preview aligns right`() = checkPreview("\u0633\u0644\u0627\u0645", ResolvedTextDirection.Rtl)

    @Test
    fun `English preview aligns left`() = checkPreview("Hello", ResolvedTextDirection.Ltr)

    @Test
    fun `mixed preview follows first strong Persian character`() = checkPreview("123 \u0633\u0644\u0627\u0645 SyncMe", ResolvedTextDirection.Rtl)

    @Test
    fun `mixed preview follows first strong English character`() = checkPreview("Hello \u0633\u0644\u0627\u0645", ResolvedTextDirection.Ltr)

    @Test
    fun `long Persian preview remains one line`() = checkPreview("\u0633\u0644\u0627\u0645 ".repeat(100), ResolvedTextDirection.Rtl, truncated = true)

    @Test
    fun `short and truncated Persian previews share the same width and right edge`() = runAndroidComposeUiTest<ComponentActivity> {
        val shortText = "\u0633\u0644\u0627\u0645"
        val longText = "$shortText ".repeat(100)
        setContent {
            Column {
                listOf(shortText, longText).forEach { text ->
                    RoomSummaryRow(
                        modifier = Modifier.width(320.dp),
                        room = aRoomListRoomSummary(latestEvent = LatestEvent.Synced(text)),
                        hideInviteAvatars = false,
                        isInviteSeen = false,
                        onClick = {},
                        eventSink = {},
                    )
                }
            }
        }
        val layouts = listOf(shortText, longText).map { text ->
            val results = mutableListOf<TextLayoutResult>()
            onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            results.single()
        }
        assertThat(layouts[0].size.width).isEqualTo(layouts[1].size.width)
        assertThat(layouts[0].getLineRight(0)).isWithin(1f).of(layouts[1].getLineRight(0))
        assertThat(layouts[1].isLineEllipsized(0)).isTrue()
    }

    @Test
    fun `You prefix does not clip the first Persian word`() = checkPrefixedPreview("You", "\u0646\u0633\u062e\u0647 ".repeat(100), ResolvedTextDirection.Rtl)

    @Test
    fun `sender name does not clip the first Persian word`() = checkPrefixedPreview("Alice", "\u0646\u0633\u062e\u0647 ".repeat(100), ResolvedTextDirection.Rtl)

    @Test
    fun `Persian sender name does not change English body direction`() =
        checkPrefixedPreview("\u2068\u0639\u0644\u06cc\u2069", "Version updated ".repeat(100), ResolvedTextDirection.Ltr)

    @Test
    fun `long sender name leaves space for the Persian body`() =
        checkPrefixedPreview("A very long sender display name".repeat(4), "\u0646\u0633\u062e\u0647 ".repeat(100), ResolvedTextDirection.Rtl)

    private fun checkPrefixedPreview(prefix: String, body: String, direction: ResolvedTextDirection) = runAndroidComposeUiTest<ComponentActivity> {
        val preview = buildAnnotatedString {
            append("$prefix: ")
            addStringAnnotation(RoomLatestEventFormatter.SENDER_PREFIX_TAG, "sender", 0, length)
            append(body)
        }
        setContent {
            RoomSummaryRow(
                modifier = Modifier.width(320.dp),
                room = aRoomListRoomSummary(latestEvent = LatestEvent.Synced(preview)),
                hideInviteAvatars = false,
                isInviteSeen = false,
                onClick = {},
                eventSink = {},
            )
        }
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText(body, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertThat(layout.getParagraphDirection(0)).isEqualTo(direction)
        assertThat(layout.lineCount).isEqualTo(1)
        assertThat(layout.isLineEllipsized(0)).isTrue()
        val firstWordLength = body.indexOf(' ')
        repeat(firstWordLength) { offset ->
            val bounds = layout.getBoundingBox(offset)
            assertThat(bounds.left).isAtLeast(-1f)
            assertThat(bounds.right).isAtMost(layout.size.width + 1f)
            assertThat(bounds.width).isGreaterThan(0f)
        }
        if (direction == ResolvedTextDirection.Rtl) {
            assertThat(layout.getBoundingBox(0).right).isWithin(1f).of(layout.size.width.toFloat())
        } else {
            assertThat(layout.getBoundingBox(0).left).isWithin(1f).of(0f)
        }
    }

    private fun checkPreview(text: String, direction: ResolvedTextDirection, truncated: Boolean = false) = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            RoomSummaryRow(
                modifier = Modifier.width(320.dp),
                room = aRoomListRoomSummary(latestEvent = LatestEvent.Synced(text)),
                hideInviteAvatars = false,
                isInviteSeen = false,
                onClick = {},
                eventSink = {},
            )
        }
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertThat(layout.getParagraphDirection(0)).isEqualTo(direction)
        assertThat(layout.lineCount).isEqualTo(1)
        if (truncated) {
            assertThat(layout.isLineEllipsized(0)).isTrue()
        } else if (direction == ResolvedTextDirection.Rtl) {
            assertThat(layout.getLineRight(0)).isWithin(1f).of(layout.size.width.toFloat())
        } else {
            assertThat(layout.getLineLeft(0)).isWithin(1f).of(0f)
        }
    }
}
