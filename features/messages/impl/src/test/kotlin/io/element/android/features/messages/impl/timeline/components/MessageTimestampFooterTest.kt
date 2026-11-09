/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.messages.impl.timeline.components

import androidx.activity.ComponentActivity
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
                        event = aTimelineItemEvent(isMine = isMine, content = aTimelineItemTextContent(body = body), timelineItemReactions = aTimelineItemReactions(0)),
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
                if (mine) assertThat(time.center.x).isLessThan(content.center.x)
                else assertThat(time.center.x).isGreaterThan(content.center.x)
            }
        }
    }
}
