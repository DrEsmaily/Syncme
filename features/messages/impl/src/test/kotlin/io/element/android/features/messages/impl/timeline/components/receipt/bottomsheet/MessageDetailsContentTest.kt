/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
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
    @Test fun `media details omit redundant attachment action and show truthful receipt states`() = runAndroidComposeUiTest<ComponentActivity> {
        val event = aTimelineItemEvent(isMine = true, content = aTimelineItemImageContent(),
            timelineItemReactions = aTimelineItemReactions(0), readReceiptState = TimelineItemReadReceipts(persistentListOf()))
        setContent {
            ElementPreview {
                ReadReceiptBottomSheetContent(ReadReceiptBottomSheetState(event, {}), {})
            }
        }
        onNodeWithText("Seen by").assertIsDisplayed()
        onNodeWithText("No read receipts available").assertIsDisplayed()
        onNodeWithText("Reactions").assertDoesNotExist()
        onNodeWithText("No reactions yet").assertDoesNotExist()
        onNodeWithText("Open attachment").assertDoesNotExist()
    }

    @Test fun `reaction report is not added to received message details`() = runAndroidComposeUiTest<ComponentActivity> {
        val event = aTimelineItemEvent(isMine = false, timelineItemReactions = aTimelineItemReactions(0))
        setContent { ElementPreview { ReadReceiptBottomSheetContent(ReadReceiptBottomSheetState(event, {}), {}) } }
        onNodeWithText("Reactions").assertDoesNotExist()
    }
    @Test fun `older message details show cumulative readers with reactions beside names in light theme`() = checkInline(io.element.android.compound.theme.Theme.Light)
    @Test fun `reactions sit beside names in dark theme`() = checkInline(io.element.android.compound.theme.Theme.Dark)
    @Test fun `reactions sit beside names in black theme`() = checkInline(io.element.android.compound.theme.Theme.Black)

    private fun checkInline(theme: io.element.android.compound.theme.Theme) = runAndroidComposeUiTest<ComponentActivity> {
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
        var pixelsPerDp = 1f
        setContent {
            pixelsPerDp = androidx.compose.ui.platform.LocalDensity.current.density
            ElementPreview(theme = theme) { ReadReceiptBottomSheetContent(ReadReceiptBottomSheetState(event, {}), {}) }
        }
        onNodeWithText("Alice").assertIsDisplayed()
        onNodeWithText("❤️").assertIsDisplayed()
        val name = onNodeWithText("Alice", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val reactionBounds = onNodeWithText("❤️", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val time = onNodeWithText("12:00", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertThat(reactionBounds.left - name.right).isWithin(1f).of(8f * pixelsPerDp)
        assertThat(reactionBounds.center.y).isWithin(1f).of(name.center.y)
        assertThat(reactionBounds.right).isLessThan(time.left)
        onNodeWithText("No read receipts available").assertDoesNotExist()
        onNodeWithText("Reactions").assertDoesNotExist()
        onNodeWithText("No reactions yet").assertDoesNotExist()
    }

}
