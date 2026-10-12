package io.element.android.libraries.matrix.api.user

import io.element.android.libraries.matrix.api.core.UserId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnlineMemberCountTest {
    private val alice = UserId("@alice:example.org")
    private val bob = UserId("@bob:example.org")
    private val carol = UserId("@carol:example.org")

    @Test
    fun `missing or unknown presence never reports zero online`() {
        assertNull(emptyMap<UserId, UserPresence>().onlineMemberCount(setOf(alice), 1))
        assertNull(mapOf(alice to UserPresence.Unknown).onlineMemberCount(setOf(alice), 1))
        assertNull(emptyMap<UserId, UserPresence>().onlineMemberCount(emptySet(), 0))
    }

    @Test
    fun `all joined members must have known presence for an exact total`() {
        val states = mapOf(alice to UserPresence.Online, bob to UserPresence.Away, carol to UserPresence.Online)
        assertEquals(OnlineMemberCount(1, true), states.onlineMemberCount(setOf(alice, bob), 2))
        assertEquals(OnlineMemberCount(1, false), states.onlineMemberCount(setOf(alice, bob), 3))
        assertEquals(OnlineMemberCount(1, false), states.onlineMemberCount(setOf(alice, bob, UserId("@unknown:example.org")), 3))
    }

    @Test
    fun `away is not included in online and known zero is allowed`() {
        assertEquals(OnlineMemberCount(0, true), mapOf(alice to UserPresence.Away).onlineMemberCount(setOf(alice), 1))
        assertNull(mapOf(alice to UserPresence.Away).onlineMemberCount(setOf(alice), 2))
    }

    @Test
    fun `nonmembers never contribute to a room total`() {
        assertNull(mapOf(carol to UserPresence.Online).onlineMemberCount(setOf(alice, bob), 2))
    }
}
