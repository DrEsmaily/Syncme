/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
@file:OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
package io.element.android.features.messages.impl.timeline.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.v2.runAndroidComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test

class TimelineItemReactionsLayoutTest : RobolectricTest() {
    @Test fun `spacing participates in wrapping LTR`() = checkWrap(LayoutDirection.Ltr)
    @Test fun `spacing participates in wrapping RTL`() = checkWrap(LayoutDirection.Rtl)

    private fun checkWrap(direction: LayoutDirection) = runAndroidComposeUiTest<ComponentActivity> {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                TimelineItemReactionsLayout(
                    modifier = Modifier.widthIn(max = 84.dp), itemSpacing = 5.dp, rowSpacing = 5.dp,
                    rowsBeforeCollapsible = null, expandButton = { Box(Modifier.size(40.dp)) }, addMoreButton = null,
                    reactions = { repeat(3) { Box(Modifier.size(40.dp).testTag("reaction-$it")) } },
                )
            }
        }
        val placed = SemanticsMatcher("placed") { it.layoutInfo.isPlaced }
        val bounds = (0..2).map { onNode(hasTestTag("reaction-$it") and placed, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot }
        assertThat(bounds[1].top).isGreaterThan(bounds[0].bottom)
        assertThat(bounds[2].top).isGreaterThan(bounds[1].bottom)
        bounds.drop(1).forEach { assertThat(it.left).isWithin(1f).of(bounds[0].left) }
    }
}
