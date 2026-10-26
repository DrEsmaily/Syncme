/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.home.impl.components

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text
import kotlin.math.roundToInt

@Composable
internal fun SyncMeTitle() {
    val density = LocalDensity.current
    val inkCenter = remember(density) {
        val bounds = Rect()
        Paint().apply {
            textSize = with(density) { 26.dp.toPx() }
            typeface = Typeface.DEFAULT_BOLD
        }.getTextBounds("SyncMe", 0, 6, bounds)
        (bounds.top + bounds.bottom) / 2f
    }
    val style = with(density) {
        ElementTheme.typography.fontHeadingLgBold.copy(fontSize = 26.dp.toSp(), lineHeight = 32.dp.toSp())
    }
    Layout(
        modifier = Modifier.testTag("syncme_home_title"),
        content = {
            Text("SyncMe", modifier = Modifier.semantics { heading() }, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
    ) { measurables, constraints ->
        val text = measurables.single().measure(constraints.copy(minHeight = 0))
        val height = constraints.constrainHeight(32.dp.roundToPx())
        val y = (height / 2f - inkCenter - text[FirstBaseline]).roundToInt()
        layout(text.width, height) { text.placeRelative(0, y) }
    }
}

@PreviewsDayNight
@Composable
internal fun SyncMeTitlePreview() = ElementPreview { SyncMeTitle() }
