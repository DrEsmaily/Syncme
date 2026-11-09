/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet

import com.google.common.truth.Truth.assertThat
import io.element.android.features.messages.impl.timeline.model.AggregatedReaction
import io.element.android.features.messages.impl.timeline.model.AggregatedReactionSender
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test

class MessageReactionReportTest {
    @Test fun `all reactions are grouped by user without requiring read receipts`() {
        val alice = UserId("@alice:test")
        val bob = UserId("@bob:test")
        val reactions = listOf("❤️", "🎉", "👍", "😀").map { key ->
            AggregatedReaction(alice, key, persistentListOf(AggregatedReactionSender(bob, 0, "10:30")))
        } + AggregatedReaction(alice, "❤️", persistentListOf(AggregatedReactionSender(alice, 0, "10:31", MatrixUser(alice, "Alice"))))
        val report = messageReactionReports(reactions)
        assertThat(report).hasSize(2)
        assertThat(report.first { it.user.userId == bob }.entries).hasSize(4)
        assertThat(report.first { it.user.userId == alice }.user.displayName).isEqualTo("Alice")
        assertThat(report.first { it.user.userId == bob }.entries.first()).contains("10:30")
    }

    @Test fun `duplicate sender keys are reported once and empty reactions stay empty`() {
        val user = UserId("@user:test")
        val reaction = AggregatedReaction(user, "❤️", persistentListOf(AggregatedReactionSender(user, 0, ""), AggregatedReactionSender(user, 0, "")))
        assertThat(messageReactionReports(listOf(reaction)).single().entries).containsExactly("❤️")
        assertThat(messageReactionReports(emptyList())).isEmpty()
    }
}
