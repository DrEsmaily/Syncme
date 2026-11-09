/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet

import io.element.android.features.messages.impl.timeline.model.AggregatedReaction
import io.element.android.libraries.matrix.api.user.MatrixUser

internal data class MessageReactionReport(val user: MatrixUser, val entries: List<String>)

/** Reactions are independent of read receipts; never infer a receipt from a reaction. */
internal fun messageReactionReports(reactions: List<AggregatedReaction>): List<MessageReactionReport> = reactions
    .flatMap { reaction -> reaction.senders.map { reaction to it } }
    .groupBy { (_, sender) -> sender.senderId }
    .map { (userId, entries) ->
        MessageReactionReport(
            user = entries.firstNotNullOfOrNull { (_, sender) -> sender.user } ?: MatrixUser(userId = userId),
            entries = entries.distinctBy { (reaction, _) -> reaction.key }.map { (reaction, sender) ->
                if (sender.sentTime.isBlank()) reaction.displayKey else "${reaction.displayKey}  ${sender.sentTime}"
            },
        )
    }
