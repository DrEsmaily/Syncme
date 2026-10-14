package io.element.android.libraries.matrix.api.user

/** Only an explicit server "online" response can show a green indicator. */
sealed interface UserPresence {
    data object Unknown : UserPresence
    data object Online : UserPresence
    data object Away : UserPresence
    data class Offline(val lastSeenAt: Long?) : UserPresence
}

enum class LastSeenCategory { Recently, WithinWeek, WithinMonth, LongAgo }

/** Approximate buckets from server time only; missing or invalid timestamps remain unknown. */
fun UserPresence.Offline.lastSeenCategory(nowMillis: Long): LastSeenCategory? {
    val seen = lastSeenAt ?: return null
    if (seen <= 0L || seen > nowMillis) return null
    val elapsed = nowMillis - seen
    val day = 86_400_000L
    return when {
        elapsed <= 3 * day -> LastSeenCategory.Recently
        elapsed <= 7 * day -> LastSeenCategory.WithinWeek
        elapsed <= 30 * day -> LastSeenCategory.WithinMonth
        else -> LastSeenCategory.LongAgo
    }
}

fun UserPresence.presenceLabel(nowMillis: Long): String? = when (this) {
    UserPresence.Unknown -> null
    UserPresence.Online -> "Online"
    UserPresence.Away -> "Away"
    is UserPresence.Offline -> when (lastSeenCategory(nowMillis)) {
        LastSeenCategory.Recently -> "Last seen recently"
        LastSeenCategory.WithinWeek -> "Last seen within a week"
        LastSeenCategory.WithinMonth -> "Last seen within a month"
        LastSeenCategory.LongAgo -> "Last seen a long time ago"
        null -> null
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
