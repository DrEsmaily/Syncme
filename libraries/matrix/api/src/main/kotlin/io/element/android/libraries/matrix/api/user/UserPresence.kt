package io.element.android.libraries.matrix.api.user

/** Only an explicit server "online" response can show a green indicator. */
sealed interface UserPresence {
    data object Unknown : UserPresence
    data object Online : UserPresence
    data object Away : UserPresence
    data class Offline(val lastSeenAt: Long?) : UserPresence
}

fun UserPresence.presenceLabel(nowMillis: Long): String? = when (this) {
    UserPresence.Unknown -> null
    UserPresence.Online -> "Online"
    UserPresence.Away -> "Away"
    is UserPresence.Offline -> {
        val seen = lastSeenAt
        when {
            seen == null || seen > nowMillis || seen <= 0L -> "Offline"
            nowMillis - seen < 60_000L -> "Last seen just now"
            nowMillis - seen < 3_600_000L -> "Last seen ${(nowMillis - seen) / 60_000L} min ago"
            nowMillis - seen < 86_400_000L -> "Last seen ${(nowMillis - seen) / 3_600_000L} hours ago"
            else -> "Last seen ${(nowMillis - seen) / 86_400_000L} days ago"
        }
    }
}

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
