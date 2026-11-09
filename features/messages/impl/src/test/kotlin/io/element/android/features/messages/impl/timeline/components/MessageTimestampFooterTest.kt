/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.messages.impl.timeline.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReactions
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemTextContent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class MessageTimestampFooterTest : RobolectricTest() {
    @Test fun `timestamp footer in light theme`() = checkFooter(Theme.Light)
    @Test fun `timestamp footer in dark theme`() = checkFooter(Theme.Dark)
    @Test fun `timestamp footer in black theme`() = checkFooter(Theme.Black)

    private fun checkFooter(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var isMine by mutableStateOf(true)
        var direction by mutableStateOf(LayoutDirection.Ltr)
        var body by mutableStateOf("Message body")
        setContent {
            ElementPreview(theme = theme) {
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    TimelineItemEventRow(
                        event = aTimelineItemEvent(isMine = isMine, content = aTimelineItemTextContent(body = body, isEdited = true), timelineItemReactions = aTimelineItemReactions(0)),
                        timelineMode = io.element.android.libraries.matrix.api.timeline.Timeline.Mode.Live,
                        timelineRoomInfo = io.element.android.features.messages.impl.timeline.aTimelineRoomInfo(),
                        timelineProtectionState = io.element.android.features.messages.impl.timeline.protection.aTimelineProtectionState(),
                        isLastOutgoingMessage = false, displayThreadSummaries = false,
                        onEventClick = {}, onGalleryItemClick = {}, onLongClick = {}, onLinkClick = {}, onLinkLongClick = {},
                        onUserDataClick = {}, inReplyToClick = {}, onReactionClick = { _, _ -> }, onReactionLongClick = { _, _ -> },
                        onMoreReactionsClick = {}, onReadReceiptClick = {}, onSwipeToReply = {}, eventSink = {},
                        eventContentView = { modifier, _ -> Text(body, modifier.widthIn(min = 180.dp).testTag("message_body")) },
                    )
                }
            }
        }
        for (rtl in listOf(false, true)) for (mine in listOf(false, true)) {
            for (text in listOf("Short text", "متن فارسی با آخرین خط", "😀 ❤️ 🎉", "A wrapped link\nhttps://example.com/long/path")) {
                runOnIdle { isMine = mine; direction = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr; body = text }
                val content = onNodeWithTag("message_body", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val time = onNodeWithText("12:34", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                assertThat(time.top).isAtLeast(content.bottom)
                val edited = onNodeWithText(activity!!.getString(io.element.android.libraries.ui.strings.CommonStrings.common_edited_suffix),
                    useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                if (mine) {
                    assertThat(time.center.x).isLessThan(content.center.x)
                    assertThat(edited.left).isAtLeast(time.right)
                } else {
                    assertThat(time.center.x).isGreaterThan(content.center.x)
                    assertThat(edited.right).isAtMost(time.left)
                }
            }
        }
    }
    @Test fun `narrow reply images stay centered with compact footer in light theme`() = checkMedia(Theme.Light)
    @Test fun `narrow reply images stay centered with compact footer in dark theme`() = checkMedia(Theme.Dark)
    @Test fun `narrow reply images stay centered with compact footer in black theme`() = checkMedia(Theme.Black)

    private fun checkMedia(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var mine by mutableStateOf(true)
        var rtl by mutableStateOf(false)
        var reactionCount by mutableStateOf(0)
        var pixelsPerDp = 1f
        val quote = io.element.android.libraries.matrix.ui.messages.reply.InReplyToDetailsPreviewParam().values.elementAt(1)
        setContent {
            pixelsPerDp = androidx.compose.ui.platform.LocalDensity.current.density
            ElementPreview(theme = theme) {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    TimelineItemEventRow(
                        event = aTimelineItemEvent(isMine = mine,
                            content = io.element.android.features.messages.impl.timeline.model.event.aTimelineItemImageContent(aspectRatio = 0.5f),
                            inReplyTo = quote, timelineItemReactions = aTimelineItemReactions(reactionCount)),
                        timelineMode = io.element.android.libraries.matrix.api.timeline.Timeline.Mode.Live,
                        timelineRoomInfo = io.element.android.features.messages.impl.timeline.aTimelineRoomInfo(),
                        timelineProtectionState = io.element.android.features.messages.impl.timeline.protection.aTimelineProtectionState(),
                        isLastOutgoingMessage = false, displayThreadSummaries = false,
                        onEventClick = {}, onGalleryItemClick = {}, onLongClick = {}, onLinkClick = {}, onLinkLongClick = {},
                        onUserDataClick = {}, inReplyToClick = {}, onReactionClick = { _, _ -> }, onReactionLongClick = { _, _ -> },
                        onMoreReactionsClick = {}, onReadReceiptClick = {}, onSwipeToReply = {}, eventSink = {},
                        eventContentView = { modifier, _ ->
                            androidx.compose.foundation.layout.Box(modifier.size(80.dp, 160.dp).testTag("reply_media"))
                        },
                    )
                }
            }
        }
        for (direction in listOf(false, true)) for (outgoing in listOf(false, true)) for (count in listOf(0, 2)) {
            runOnIdle { rtl = direction; mine = outgoing; reactionCount = count }
            val media = onNodeWithTag("reply_media", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val bubble = onNodeWithTag(io.element.android.libraries.testtags.TestTags.messageBubble.value, useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val time = onNodeWithText("12:34", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertThat(media.center.x).isWithin(1f).of(bubble.center.x)
            assertThat(time.top).isAtLeast(media.bottom)
            assertThat(bubble.bottom - time.bottom).isWithin(1f).of(2f * pixelsPerDp)
            if (outgoing) {
                assertThat(time.center.x).isLessThan(bubble.center.x)
                assertThat(time.left - bubble.left).isWithin(1f).of(8f * pixelsPerDp)
            } else {
                assertThat(time.center.x).isGreaterThan(bubble.center.x)
                assertThat(bubble.right - time.right).isWithin(1f).of(8f * pixelsPerDp)
            }
        }
    }

}
