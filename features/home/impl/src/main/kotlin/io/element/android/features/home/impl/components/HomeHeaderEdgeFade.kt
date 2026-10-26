/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.home.impl.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** A draw-only overlay at the chat viewport edge; no layer, blur, layout or input handling. */
internal fun Modifier.homeHeaderEdgeFade(background: Color, isVisible: () -> Boolean): Modifier = drawWithCache {
    val fadeHeight = minOf(5f, size.height)
    val brush = Brush.verticalGradient(
        colors = listOf(background, background.copy(alpha = 0f)),
        startY = 0f,
        endY = fadeHeight.coerceAtLeast(1f),
    )
    onDrawWithContent {
        drawContent()
        // Read scroll state during drawing, without recomposing or laying out the list on each scroll offset.
        if (isVisible() && fadeHeight > 0f) {
            drawRect(brush = brush, size = Size(size.width, fadeHeight))
        }
    }
}
