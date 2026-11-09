/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.messages.impl.timeline.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReactions
import io.element.android.features.messages.impl.timeline.aTimelineRoomInfo
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemImageContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemVideoContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemAudioContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemVoiceContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemFileContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemStickerContent
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemAttachmentsContent
import io.element.android.features.messages.impl.timeline.protection.aTimelineProtectionState
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.matrix.api.timeline.Timeline
import io.element.android.libraries.testtags.TestTags
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class TimelineMediaTapTest : RobolectricTest() {
    @Test fun `media controls keep their action while sent bubble footer opens details`() = runAndroidComposeUiTest<ComponentActivity> {
        val contents = listOf(aTimelineItemImageContent(), aTimelineItemVideoContent(), aTimelineItemAudioContent(),
            aTimelineItemVoiceContent(), aTimelineItemFileContent(), aTimelineItemStickerContent(), aTimelineItemAttachmentsContent())
        var event by mutableStateOf(aTimelineItemEvent(isMine = true, content = contents.first(), timelineItemReactions = aTimelineItemReactions(0)))
        var opened = 0
        var details = 0
        val openMedia: (TimelineItem.Event) -> Unit = { opened++ }
        setContent {
            ElementPreview {
                key(event.eventOrTransactionId) {
                    TimelineItemRow(
                        timelineItem = event, timelineMode = Timeline.Mode.Live, timelineRoomInfo = aTimelineRoomInfo(),
                        timelineProtectionState = aTimelineProtectionState(), isLastOutgoingMessage = false, focusedEventId = null,
                        displayThreadSummaries = false, onUserDataClick = {}, onLinkClick = {}, onLinkLongClick = {},
                        onContentClick = openMedia, onGalleryItemClick = { _, _ -> }, onLongClick = {}, inReplyToClick = {},
                        onReactionClick = { _, _ -> }, onReactionLongClick = { _, _ -> }, onMoreReactionsClick = {},
                        onReadReceiptClick = { details++ }, onJoinCallClick = {}, onSwipeToReply = {}, eventSink = {},
                        eventContentView = { current, modifier, _ ->
                            Box(modifier.size(160.dp, 80.dp).testTag("media_control").clickable { openMedia(current) })
                        },
                    )
                }
            }
        }
        contents.forEachIndexed { index, content ->
            runOnIdle { event = aTimelineItemEvent(isMine = true, content = content, timelineItemReactions = aTimelineItemReactions(0)) }
            onNodeWithTag("media_control", useUnmergedTree = true).performClick()
            assertThat(opened).isEqualTo(index + 1)
            assertThat(details).isEqualTo(index)
            onNodeWithTag(TestTags.messageBubble.value, useUnmergedTree = true).performTouchInput {
                click(Offset(center.x, height - 2f))
            }
            assertThat(opened).isEqualTo(index + 1)
            assertThat(details).isEqualTo(index + 1)
        }
    }
}
