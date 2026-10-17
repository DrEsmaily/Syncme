package io.element.android.libraries.matrix.ui.presence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.OnlineMemberCount
import io.element.android.libraries.matrix.api.user.UserPresence
import io.element.android.libraries.matrix.api.user.presenceLabel
import io.element.android.libraries.ui.strings.CommonPlurals
import io.element.android.libraries.ui.strings.CommonStrings

/** Bounded snapshots for visible users, released on navigation; one shared session stream. */
@Composable
fun MatrixClient.observePresence(users: Set<UserId>): Map<UserId, UserPresence> {
    val visibleUsers = users.sortedBy { it.value }.take(128).toSet()
    DisposableEffect(this, visibleUsers) {
        trackPresence(visibleUsers)
        onDispose { releasePresence(visibleUsers) }
    }
    val states by presenceStates.collectAsState()
    return states
}

@Composable
fun UserPresence.toPresenceText(): String? = when (this) {
    UserPresence.Unknown -> null
    UserPresence.Online -> stringResource(CommonStrings.syncme_presence_online)
    UserPresence.Away -> stringResource(CommonStrings.syncme_presence_away)
    is UserPresence.Offline -> presenceLabel(System.currentTimeMillis())
}

@Composable
fun OnlineMemberCount?.toPresenceText(): String? = this?.let {
    pluralStringResource(
        if (isComplete) CommonPlurals.syncme_presence_online_count else CommonPlurals.syncme_presence_online_count_partial,
        online,
        online,
    )
}

@Composable
fun PresenceIndicator(presence: UserPresence, modifier: Modifier = Modifier) {
    if (presence != UserPresence.Online && presence != UserPresence.Away) return
    val label = presence.toPresenceText() ?: return
    val color = when (presence) {
        UserPresence.Online -> ElementTheme.colors.iconSuccessPrimary
        UserPresence.Away -> ElementTheme.colors.iconWarningPrimary
    }
    Box(modifier.size(10.dp).background(color, CircleShape).semantics { contentDescription = label })
}

@Composable
fun PresenceStatus(presence: UserPresence, modifier: Modifier = Modifier) {
    val label = presence.toPresenceText() ?: return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        PresenceIndicator(presence)
        Text(label, style = ElementTheme.typography.fontBodySmRegular, color = ElementTheme.colors.textSecondary)
    }
}

@PreviewsDayNight
@Composable
internal fun OnlinePresencePreview() = ElementPreview { PresenceStatus(UserPresence.Online) }

@PreviewsDayNight
@Composable
internal fun AwayPresencePreview() = ElementPreview { PresenceStatus(UserPresence.Away) }

@PreviewsDayNight
@Composable
internal fun OfflinePresencePreview() = ElementPreview { PresenceStatus(UserPresence.Offline(null)) }

@PreviewsDayNight
@Composable
internal fun RecentlySeenPresencePreview() = ElementPreview {
    PresenceStatus(UserPresence.Offline(System.currentTimeMillis() - 30 * 60_000L))
}

@PreviewsDayNight
@Composable
internal fun SeenDaysAgoPresencePreview() = ElementPreview {
    PresenceStatus(UserPresence.Offline(System.currentTimeMillis() - 5 * 86_400_000L))
}

@PreviewsDayNight
@Composable
internal fun SeenWeeksAgoPresencePreview() = ElementPreview {
    PresenceStatus(UserPresence.Offline(System.currentTimeMillis() - 15 * 86_400_000L))
}

@PreviewsDayNight
@Composable
internal fun SeenMonthsAgoPresencePreview() = ElementPreview {
    PresenceStatus(UserPresence.Offline(System.currentTimeMillis() - 40 * 86_400_000L))
}

@PreviewsDayNight
@Composable
internal fun SeenHoursAgoPresencePreview() = ElementPreview {
    PresenceStatus(UserPresence.Offline(System.currentTimeMillis() - 5 * 3_600_000L))
}

@PreviewsDayNight
@Composable
internal fun SeenLongTimeAgoPresencePreview() = ElementPreview {
    PresenceStatus(UserPresence.Offline(System.currentTimeMillis() - 400 * 86_400_000L))
}
