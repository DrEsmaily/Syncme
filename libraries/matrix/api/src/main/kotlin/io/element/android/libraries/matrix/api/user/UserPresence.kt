package io.element.android.libraries.matrix.api.user

/** Only an explicit server "online" response can show a green indicator. */
sealed interface UserPresence {
    data object Unknown : UserPresence
    data object Online : UserPresence
    data object Away : UserPresence
    data class Offline(val lastSeenAt: Long?) : UserPresence
}

/** English-only last seen, using completed 24-hour days and 30-day months. */
fun UserPresence.presenceLabel(nowMillis: Long): String? = when (this) {
    UserPresence.Unknown -> null
    UserPresence.Online -> "Online"
    UserPresence.Away -> "Away"
    is UserPresence.Offline -> {
        val seen = lastSeenAt
        if (seen == null || seen <= 0L || seen > nowMillis) {
            null
        } else {
            val days = (nowMillis - seen) / 86_400_000L
            when {
                days == 0L -> "Last seen recently"
                days < 7 -> lastSeenElapsed(days, "day")
                days < 30 -> lastSeenElapsed(days / 7, "week")
                else -> lastSeenElapsed(days / 30, "month")
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
