/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

@file:OptIn(ExperimentalTestApi::class)

package io.element.android.features.home.impl.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import com.google.common.truth.Truth.assertThat
import io.element.android.features.home.impl.model.LatestEvent
import io.element.android.features.home.impl.model.aRoomListRoomSummary
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RoomSummaryRowDirectionTest : RobolectricTest() {
    @Test
    fun `Persian preview aligns right`() = checkPreview("\u0633\u0644\u0627\u0645", ResolvedTextDirection.Rtl)

    @Test
    fun `English preview aligns left`() = checkPreview("Hello", ResolvedTextDirection.Ltr)

    @Test
    fun `mixed preview follows first strong Persian character`() = checkPreview("123 \u0633\u0644\u0627\u0645 SyncMe", ResolvedTextDirection.Rtl)

    @Test
    fun `mixed preview follows first strong English character`() = checkPreview("Hello \u0633\u0644\u0627\u0645", ResolvedTextDirection.Ltr)

    @Test
    fun `long Persian preview remains one line`() = checkPreview("\u0633\u0644\u0627\u0645 ".repeat(100), ResolvedTextDirection.Rtl, truncated = true)

    private fun checkPreview(text: String, direction: ResolvedTextDirection, truncated: Boolean = false) = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            RoomSummaryRow(
                modifier = Modifier.width(320.dp),
                room = aRoomListRoomSummary(latestEvent = LatestEvent.Synced(text)),
                hideInviteAvatars = false,
                isInviteSeen = false,
                onClick = {},
                eventSink = {},
            )
        }
        val results = mutableListOf<TextLayoutResult>()
        onNodeWithText(text, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertThat(layout.getParagraphDirection(0)).isEqualTo(direction)
        assertThat(layout.lineCount).isEqualTo(1)
        if (truncated) {
            assertThat(layout.isLineEllipsized(0)).isTrue()
        } else if (direction == ResolvedTextDirection.Rtl) {
            assertThat(layout.getLineRight(0)).isWithin(1f).of(layout.size.width.toFloat())
        } else {
            assertThat(layout.getLineLeft(0)).isWithin(1f).of(0f)
        }
    }
}
