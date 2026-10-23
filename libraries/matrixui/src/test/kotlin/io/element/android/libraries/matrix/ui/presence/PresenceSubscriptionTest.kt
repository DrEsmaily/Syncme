package io.element.android.libraries.matrix.ui.presence

import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.UserId
import org.junit.Test

class PresenceSubscriptionTest {
    private val a = UserId("@a:example.org")
    private val b = UserId("@b:example.org")
    private val c = UserId("@c:example.org")
    private val d = UserId("@d:example.org")

    @Test
    fun `scrolling keeps overlapping users subscribed and releases once on disposal`() {
        val added = mutableListOf<Set<UserId>>()
        val removed = mutableListOf<Set<UserId>>()
        val subscription = PresenceSubscription({ added.add(it) }, { removed.add(it) })
        subscription.update(setOf(a, b, c))
        subscription.update(setOf(b, c, d))
        subscription.update(setOf(b, c, d))
        assertThat(added).containsExactly(setOf(a, b, c), setOf(d)).inOrder()
        assertThat(removed).containsExactly(setOf(a))
        subscription.dispose()
        subscription.dispose()
        assertThat(removed).containsExactly(setOf(a), setOf(b, c, d)).inOrder()
    }

    @Test
    fun `independent screens keep separate reference counts`() {
        val counts = mutableMapOf<UserId, Int>()
        fun track(users: Set<UserId>) { users.forEach { counts[it] = (counts[it] ?: 0) + 1 } }
        fun release(users: Set<UserId>) {
            users.forEach { user ->
                val count = requireNotNull(counts[user])
                if (count == 1) counts.remove(user) else counts[user] = count - 1
            }
        }
        val first = PresenceSubscription(::track, ::release)
        val second = PresenceSubscription(::track, ::release)
        first.update(setOf(a, b))
        second.update(setOf(b, c))
        first.update(setOf(b, d))
        assertThat(counts[b]).isEqualTo(2)
        first.dispose()
        assertThat(counts).containsExactly(b, 1, c, 1)
        second.update(emptySet())
        assertThat(counts).isEmpty()
    }
}
