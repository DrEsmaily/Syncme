/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
package io.element.android.features.messages.impl.timeline.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.compound.theme.Theme
import io.element.android.features.messages.impl.R
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReactions
import io.element.android.features.messages.impl.timeline.model.event.aTimelineItemTextContent
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class ReactionPlacementTest : RobolectricTest() {
    @Test fun `reaction edges in light theme`() = checkEdges(Theme.Light)
    @Test fun `reaction edges in dark theme`() = checkEdges(Theme.Dark)
    @Test fun `reaction edges in black theme`() = checkEdges(Theme.Black)

    private fun checkEdges(theme: Theme) = runAndroidComposeUiTest<ComponentActivity> {
        var mine by mutableStateOf(true)
        var rtl by mutableStateOf(false)
        var body by mutableStateOf("Message body")
        var density = 1f
        setContent {
            density = androidx.compose.ui.platform.LocalDensity.current.density
            ElementPreview(theme = theme) {
                CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    TimelineItemEventRow(
                        event = aTimelineItemEvent(isMine = mine, content = aTimelineItemTextContent(body = body),
                            timelineItemReactions = aTimelineItemReactions(2)),
                        timelineMode = io.element.android.libraries.matrix.api.timeline.Timeline.Mode.Live,
                        timelineRoomInfo = io.element.android.features.messages.impl.timeline.aTimelineRoomInfo(),
                        timelineProtectionState = io.element.android.features.messages.impl.timeline.protection.aTimelineProtectionState(),
                        isLastOutgoingMessage = false, displayThreadSummaries = false,
                        onEventClick = {}, onGalleryItemClick = {}, onLongClick = {}, onLinkClick = {}, onLinkLongClick = {},
                        onUserDataClick = {}, inReplyToClick = {}, onReactionClick = { _, _ -> }, onReactionLongClick = { _, _ -> },
                        onMoreReactionsClick = {}, onReadReceiptClick = {}, onSwipeToReply = {}, eventSink = {},
                        eventContentView = { modifier, _ -> Text(body, modifier.widthIn(min = 240.dp)) },
                    )
                }
            }
        }
        for (layoutRtl in listOf(false, true)) for (outgoing in listOf(false, true)) {
            for (text in listOf("Hello there", "نسخه جدید برای گفتگو", "😀 ❤️")) {
                runOnIdle { rtl = layoutRtl; mine = outgoing; body = text }
                val placed = SemanticsMatcher("placed") { it.layoutInfo.isPlaced }
                val bubble = onNode(hasTestTag(io.element.android.libraries.testtags.TestTags.messageBubble.value) and placed,
                    useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val reaction = onNode(hasContentDescription("👍", substring = true) and placed,
                    useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
                val addLabel = activity!!.getString(R.string.screen_room_timeline_add_reaction)
                val add = onAllNodes(hasContentDescription(addLabel) and placed, useUnmergedTree = true)
                    .fetchSemanticsNodes().maxBy { it.boundsInRoot.width }.boundsInRoot
                if (outgoing) {
                    assertThat(reaction.left - bubble.left).isWithin(density).of(10f * density)
                    assertThat(add.left).isGreaterThan(reaction.right)
                } else {
                    assertThat(bubble.right - reaction.right).isWithin(density).of(10f * density)
                    assertThat(add.right).isLessThan(reaction.left)
                }
            }
        }
    }
}
