/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.libraries.matrix.ui.messages.reply

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.timeline.item.event.MessageContent
import io.element.android.libraries.matrix.api.timeline.item.event.TextMessageType
import io.element.android.libraries.matrix.ui.media.contentvalidation.ContentValidationValue
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InReplyToDirectionTest : RobolectricTest() {
    @Test
    fun `Persian quote aligns right`() = checkDirection("\u0633\u0644\u0627\u0645", ResolvedTextDirection.Rtl)

    @Test
    fun `English quote aligns left`() = checkDirection("Hello", ResolvedTextDirection.Ltr)

    @Test
    fun `mixed quote follows its first strong Persian character`() = checkDirection("123 \u0633\u0644\u0627\u0645 SyncMe", ResolvedTextDirection.Rtl)

    @Test
    fun `mixed quote follows its first strong English character`() = checkDirection("Hello \u0633\u0644\u0627\u0645", ResolvedTextDirection.Ltr)

    @Test
    fun `long Persian quote keeps the two-line limit`() = checkDirection("\u0633\u0644\u0627\u0645 ".repeat(100), ResolvedTextDirection.Rtl, truncated = true)

    @Test
    fun `short quotes retain their natural card width`() = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            Box(Modifier.width(320.dp).testTag("container")) {
                InReplyToView(details("Hi"), hideImage = false, contentValidationValue = ContentValidationValue.Valid,
                    modifier = Modifier.testTag("quote"))
            }
        }
        val width = onNodeWithTag("quote").fetchSemanticsNode().boundsInRoot.width
        val parentWidth = onNodeWithTag("container").fetchSemanticsNode().boundsInRoot.width
        assertThat(width).isLessThan(parentWidth)
    }

    private fun checkDirection(text: String, direction: ResolvedTextDirection, truncated: Boolean = false) =
        runAndroidComposeUiTest<ComponentActivity> {
            var inset = 0f
            setContent {
                inset = with(LocalDensity.current) { 8.dp.toPx() }
                InReplyToView(details(text), hideImage = false, contentValidationValue = ContentValidationValue.Valid,
                    modifier = Modifier.width(260.dp).testTag("quote"))
            }
            val results = mutableListOf<TextLayoutResult>()
            onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            val layout = results.single()
            val textBounds = onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val cardBounds = onNodeWithTag("quote").fetchSemanticsNode().boundsInRoot
            val senderBounds = onNodeWithText("Sender", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            if (direction == ResolvedTextDirection.Rtl) {
                assertThat(textBounds.right).isWithin(1f).of(cardBounds.right - inset)
                assertThat(senderBounds.right).isWithin(1f).of(cardBounds.right - inset)
            } else {
                assertThat(textBounds.left).isWithin(1f).of(cardBounds.left + inset)
                assertThat(senderBounds.left).isWithin(1f).of(cardBounds.left + inset)
            }
            assertThat(layout.getParagraphDirection(0)).isEqualTo(direction)
            if (truncated) {
                assertThat(layout.lineCount).isEqualTo(2)
                assertThat(layout.isLineEllipsized(1)).isTrue()
            } else if (direction == ResolvedTextDirection.Rtl) {
                assertThat(layout.getLineRight(0)).isWithin(1f).of(layout.size.width.toFloat())
            } else {
                assertThat(layout.getLineLeft(0)).isWithin(1f).of(0f)
            }
        }

    private fun details(text: String) = InReplyToDetails.Ready(
        eventId = EventId("\$quote"),
        senderId = UserId("@sender:example.org"),
        senderProfile = aProfileDetailsReady(displayName = "Sender"),
        eventContent = MessageContent(body = text, inReplyTo = null, isEdited = false, threadInfo = null, type = TextMessageType(text, null)),
        textContent = text,
    )
}
