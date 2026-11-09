/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReactions
import io.element.android.features.messages.impl.timeline.model.TimelineItemReadReceipts
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemImageContent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class MessageDetailsContentTest : RobolectricTest() {
    @Test fun `sender can open attachments and sees truthful empty receipt and reaction states`() = runAndroidComposeUiTest<ComponentActivity> {
        val event = aTimelineItemEvent(isMine = true, content = aTimelineItemImageContent(),
            timelineItemReactions = aTimelineItemReactions(0), readReceiptState = TimelineItemReadReceipts(persistentListOf()))
        var opened = 0
        setContent {
            ElementPreview {
                ReadReceiptBottomSheetContent(ReadReceiptBottomSheetState(event, {}), {}, { opened++ })
            }
        }
        onNodeWithText("Seen by").assertIsDisplayed()
        onNodeWithText("No read receipts available").assertIsDisplayed()
        onNodeWithText("Reactions").assertDoesNotExist()
        onNodeWithText("No reactions yet").assertDoesNotExist()
        onNodeWithText("Open attachment").performClick()
        assertThat(opened).isEqualTo(1)
    }

    @Test fun `reaction report is not added to received message details`() = runAndroidComposeUiTest<ComponentActivity> {
        val event = aTimelineItemEvent(isMine = false, timelineItemReactions = aTimelineItemReactions(0))
        setContent { ElementPreview { ReadReceiptBottomSheetContent(ReadReceiptBottomSheetState(event, {}), {}) } }
        onNodeWithText("Reactions").assertDoesNotExist()
    }
    @Test fun `older message details show cumulative readers with reactions inline`() = runAndroidComposeUiTest<ComponentActivity> {
        val alice = io.element.android.libraries.matrix.api.core.UserId("@alice:test")
        val receipt = io.element.android.features.messages.impl.timeline.model.ReadReceiptData(
            io.element.android.libraries.designsystem.components.avatar.AvatarData(alice.value, "Alice", size = io.element.android.libraries.designsystem.components.avatar.AvatarSize.ReadReceiptList),
            "12:00",
        )
        val reaction = io.element.android.features.messages.impl.timeline.model.AggregatedReaction(alice, "❤️",
            persistentListOf(io.element.android.features.messages.impl.timeline.model.AggregatedReactionSender(alice, 0, "12:00")))
        val event = aTimelineItemEvent(isMine = true, readReceiptState = TimelineItemReadReceipts(persistentListOf()),
            timelineItemReactions = io.element.android.features.messages.impl.timeline.model.TimelineItemReactions(persistentListOf(reaction)))
            .copy(readReceiptDetailsState = TimelineItemReadReceipts(persistentListOf(receipt)))
        setContent { ElementPreview { ReadReceiptBottomSheetContent(ReadReceiptBottomSheetState(event, {}), {}) } }
        onNodeWithText("Alice").assertIsDisplayed()
        onNodeWithText("❤️").assertIsDisplayed()
        onNodeWithText("No read receipts available").assertDoesNotExist()
        onNodeWithText("Reactions").assertDoesNotExist()
        onNodeWithText("No reactions yet").assertDoesNotExist()
    }

}
