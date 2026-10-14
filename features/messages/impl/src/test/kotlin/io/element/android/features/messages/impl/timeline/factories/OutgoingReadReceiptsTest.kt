/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.factories

import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.timeline.aTimelineItemEvent
import io.element.android.features.messages.impl.timeline.aTimelineItemReadReceipts
import io.element.android.features.messages.impl.timeline.model.ReadReceiptData
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.TimelineItemThreadInfo
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemRedactedContent
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.matrix.api.core.ThreadId
import org.junit.Test

class OutgoingReadReceiptsTest {
    private fun reader(id: String) = ReadReceiptData(
        AvatarData(id = id, name = id, size = AvatarSize.TimelineReadReceipt),
        "12:00",
    )

    private fun message(mine: Boolean, vararg readers: ReadReceiptData) = aTimelineItemEvent(
        isMine = mine,
        readReceiptState = aTimelineItemReadReceipts(readers.toList()),
    )

    private fun List<TimelineItem>.readers(index: Int) = (get(index) as TimelineItem.Event).readReceiptState.receipts

    @Test
    fun `incoming reply keeps avatar under the previously read outgoing bubble`() {
        val alice = reader("alice")
        val outgoing = message(true, alice)
        val before = listOf(outgoing).withOutgoingReadReceipts()
        val after = listOf(message(false, alice), outgoing.copy(readReceiptState = aTimelineItemReadReceipts())).withOutgoingReadReceipts()
        assertThat(after.readers(0)).isEmpty()
        assertThat(after.readers(1)).isEqualTo(before.readers(0))
    }

    @Test
    fun `receipt is never moved forward to a newer unread outgoing message`() {
        val alice = reader("alice")
        val incoming = message(false, alice)
        val result = listOf(message(true), incoming, message(false), message(true), message(true)).withOutgoingReadReceipts()
        assertThat(result.readers(0)).isEmpty()
        assertThat(result.readers(1)).isEmpty()
        assertThat(result.readers(2)).isEmpty()
        assertThat(result.readers(3)).containsExactly(alice)
        assertThat(result.readers(4)).isEmpty()
        assertThat(incoming.readReceiptState.receipts).containsExactly(alice)
    }

    @Test
    fun `multiple readers are merged and each reader appears once`() {
        val alice = reader("alice")
        val bob = reader("bob")
        val result = listOf(message(false, alice), message(true, bob), message(true, alice)).withOutgoingReadReceipts()
        assertThat(result.readers(1)).containsExactly(alice, bob)
        assertThat(result.readers(2)).isEmpty()
    }

    @Test
    fun `missing loaded target waits for pagination instead of marking a newer message read`() {
        val incoming = message(false, reader("alice"))
        val newest = message(true)
        val initial = listOf(newest, incoming).withOutgoingReadReceipts()
        assertThat(initial.readers(0)).isEmpty()
        assertThat(initial.readers(1)).isEmpty()
        val paginated = listOf(newest, incoming, message(true)).withOutgoingReadReceipts()
        assertThat(paginated.readers(2)).hasSize(1)
    }

    @Test
    fun `receipt does not cross into another thread`() {
        val thread = TimelineItemThreadInfo.ThreadResponse(ThreadId("thread"))
        val result = listOf(
            message(false, reader("alice")).copy(threadInfo = thread),
            message(true),
            message(true).copy(threadInfo = thread),
        ).withOutgoingReadReceipts()
        assertThat(result.readers(1)).isEmpty()
        assertThat(result.readers(2)).hasSize(1)
    }

    @Test
    fun `deleted outgoing messages are not used as targets`() {
        val result = listOf(
            message(false, reader("alice")),
            message(true).copy(content = TimelineItemRedactedContent),
            message(true),
        ).withOutgoingReadReceipts()
        assertThat(result.readers(1)).isEmpty()
        assertThat(result.readers(2)).hasSize(1)
    }

    @Test
    fun `receipt on outgoing message remains unchanged`() {
        val outgoing = message(true, reader("alice"))
        assertThat(listOf(outgoing).withOutgoingReadReceipts()).containsExactly(outgoing)
    }
}
