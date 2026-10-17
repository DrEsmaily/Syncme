package io.element.android.libraries.matrix.api.user

/** Only an explicit server "online" response can show a green indicator. */
sealed interface UserPresence {
    data object Unknown : UserPresence
    data object Online : UserPresence
    data object Away : UserPresence
    data class Offline(val lastSeenAt: Long?) : UserPresence
}

/** English-only last seen using elapsed time, completed units, 30-day months and a 365-day year. */
fun UserPresence.presenceLabel(nowMillis: Long): String? = when (this) {
    UserPresence.Unknown -> null
    UserPresence.Online -> "Online"
    UserPresence.Away -> "Away"
    is UserPresence.Offline -> {
        val seen = lastSeenAt
        if (seen == null || seen <= 0L || seen > nowMillis) {
            null
        } else {
            val elapsed = nowMillis - seen
            val hours = elapsed / 3_600_000L
            val days = elapsed / 86_400_000L
            when {
                hours == 0L -> "Last seen recently"
                days == 0L -> lastSeenElapsed(hours, "hour")
                days < 7 -> lastSeenElapsed(days, "day")
                days < 30 -> lastSeenElapsed(days / 7, "week")
                days < 365 -> lastSeenElapsed(days / 30, "month")
                else -> "Last seen long time ago"
            }
        }
    }
}

private fun lastSeenElapsed(count: Long, unit: String): String =
    "Last seen $count $unit${if (count == 1L) "" else "s"} ago"

/** An incomplete count is a lower bound, and must never be presented as an exact total. */
data class OnlineMemberCount(val online: Int, val isComplete: Boolean)

fun Map<io.element.android.libraries.matrix.api.core.UserId, UserPresence>.onlineMemberCount(
    joinedUserIds: Set<io.element.android.libraries.matrix.api.core.UserId>,
    joinedMembersCount: Long,
): OnlineMemberCount? {
    val online = joinedUserIds.count { this[it] == UserPresence.Online }
    val complete = joinedUserIds.size.toLong() == joinedMembersCount && joinedUserIds.isNotEmpty() &&
        joinedUserIds.all { this[it] != null && this[it] != UserPresence.Unknown }
    return if (online > 0 || complete) OnlineMemberCount(online, complete) else null
}
