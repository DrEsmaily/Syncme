/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.home.impl

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.model.LatestEvent
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import io.element.android.features.home.impl.roomlist.aRoomListState
import io.element.android.features.home.impl.roomlist.aRoomsContentState
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.tests.testutils.robolectric.RobolectricTest
import kotlinx.collections.immutable.toImmutableList
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeListLayoutTest : RobolectricTest() {
    @Test
    fun `last conversation scrolls fully above the compose button`() = runAndroidComposeUiTest<ComponentActivity> {
        val rooms = (0..30).map {
            aRoomListRoomSummary(id = "!room$it:domain", name = "Conversation $it", latestEvent = LatestEvent.Synced("Preview $it"))
        }.toImmutableList()
        setContent {
            HomeView(
                homeState = aHomeState(roomListState = aRoomListState(contentState = aRoomsContentState(summaries = rooms))),
                modifier = Modifier.fillMaxSize(),
                onRoomClick = { _, _ -> },
                onSettingsClick = {},
                onAddAccountClick = {},
                onSetUpRecoveryClick = {},
                onConfirmRecoveryKeyClick = {},
                onStartChatClick = {},
                onCreateSpaceClick = {},
                onRoomSettingsClick = {},
                onMenuActionClick = {},
                onReportRoomClick = {},
                onDeclineInviteAndBlockUser = {},
                acceptDeclineInviteView = {},
                leaveRoomView = {},
            )
        }
        onNode(hasScrollAction() and hasAnyDescendant(hasText("Conversation 0")))
            .performScrollToNode(hasText("Conversation 30"))
        val preview = onNodeWithText("Preview 30", useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val compose = onNodeWithContentDescription(activity!!.getString(CommonStrings.action_create_room))
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertThat(preview.bottom).isAtMost(compose.top)
    }
}
