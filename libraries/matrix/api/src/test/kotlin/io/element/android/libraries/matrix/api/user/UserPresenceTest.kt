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
        val now = 1_000 * day
        val cases = listOf(
            0L to "Last seen recently",
            day - 1 to "Last seen recently",
            day to "Last seen 1 day ago",
            2 * day to "Last seen 2 days ago",
            7 * day - 1 to "Last seen 6 days ago",
            7 * day to "Last seen 1 week ago",
            14 * day - 1 to "Last seen 1 week ago",
            14 * day to "Last seen 2 weeks ago",
            30 * day - 1 to "Last seen 4 weeks ago",
            30 * day to "Last seen 1 month ago",
            60 * day to "Last seen 2 months ago",
            365 * day to "Last seen 12 months ago",
        )
        cases.forEach { (elapsed, expected) ->
            assertEquals(expected, UserPresence.Offline(now - elapsed).presenceLabel(now))
        }
        assertNull(UserPresence.Offline(0).presenceLabel(now))
        assertNull(UserPresence.Offline(-1).presenceLabel(now))
    }
}
