/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.factories

import io.element.android.features.messages.impl.timeline.groups.canBeGrouped
import io.element.android.features.messages.impl.timeline.model.ReadReceiptData
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.features.messages.impl.timeline.model.TimelineItemReadReceipts
import io.element.android.features.messages.impl.timeline.model.TimelineItemThreadInfo
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemRedactedContent
import io.element.android.libraries.matrix.api.core.ThreadId
import kotlinx.collections.immutable.toImmutableList

/**
 * Input is newest first, before grouping. Display a receipt under the nearest outgoing message
 * at or before the SDK receipt event, never under a newer unread message or in another thread.
 * Source items and SDK read positions remain unchanged. Unloaded targets wait for pagination.
 */
internal fun List<TimelineItem>.withOutgoingReadReceipts(): List<TimelineItem> {
    val pending = mutableMapOf<ThreadId?, MutableList<ReadReceiptData>>()
    val seen = mutableSetOf<Pair<ThreadId?, String>>()
    return map { item ->
        if (item !is TimelineItem.Event) return@map item
        val thread = (item.threadInfo as? TimelineItemThreadInfo.ThreadResponse)?.threadRootId
        val readers = pending.getOrPut(thread) { mutableListOf() }
        item.readReceiptState.receipts.forEach { receipt ->
            if (seen.add(thread to receipt.avatarData.id)) readers.add(receipt)
        }
        val isTarget = item.isMine && item.eventId != null && !item.canBeGrouped() && item.content !is TimelineItemRedactedContent
        val receipts = if (isTarget) readers.toList().also { readers.clear() } else emptyList()
        if (receipts == item.readReceiptState.receipts) {
            item
        } else {
            item.copy(
            readReceiptState = TimelineItemReadReceipts(receipts.toImmutableList()),
        )
        }
    }
}
