package io.element.android.libraries.matrix.api.user

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class UserPresenceTest {
    @Test fun onlineIsOnlyExplicitOnline() {
        assertEquals("Online", UserPresence.Online.presenceLabel(120_000))
        assertEquals("Away", UserPresence.Away.presenceLabel(120_000))
        assertNull(UserPresence.Unknown.presenceLabel(120_000))
        assertNull(UserPresence.Offline(null).presenceLabel(120_000))
    }

    @Test fun lastSeenFromValidServerTimestamp() {
        assertEquals("Last seen recently", UserPresence.Offline(90_000).presenceLabel(120_000))
        assertEquals("Last seen recently", UserPresence.Offline(120_000).presenceLabel(240_000))
    }

    @Test fun futureServerTimestampIsNotAccepted() {
        assertNull(UserPresence.Offline(200_000).presenceLabel(100_000))
    }
    @Test fun approximateLastSeenBoundaries() {
        val day = 86_400_000L
        val now = 100 * day
        val cases = listOf(
            0L to "Last seen recently",
            3 * day to "Last seen recently",
            3 * day + 1 to "Last seen within a week",
            7 * day to "Last seen within a week",
            7 * day + 1 to "Last seen within a month",
            30 * day to "Last seen within a month",
            30 * day + 1 to "Last seen a long time ago",
        )
        cases.forEach { (elapsed, expected) ->
            assertEquals(expected, UserPresence.Offline(now - elapsed).presenceLabel(now))
        }
        assertNull(UserPresence.Offline(0).presenceLabel(now))
        assertNull(UserPresence.Offline(-1).presenceLabel(now))
    }
}
