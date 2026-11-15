/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.event

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.element.android.compound.theme.ElementTheme
import io.element.android.compound.tokens.generated.CompoundIcons
import io.element.android.features.messages.impl.timeline.model.event.GalleryItem
import io.element.android.features.messages.impl.timeline.model.event.TimelineItemGalleryContent
import io.element.android.libraries.designsystem.components.blurhash.blurHashBackground
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Icon
import io.element.android.libraries.matrix.api.core.EventId
import io.element.android.libraries.matrix.ui.media.contentvalidation.ContentValidationValue
import io.element.android.libraries.matrix.ui.media.contentvalidation.collectMediaState
import io.element.android.libraries.matrix.ui.media.contentvalidation.rememberEventContentValidationState
import io.element.android.libraries.ui.utils.time.formatShort

private const val MAX_TILES = 5
internal const val GALLERY_WIDTH_DP = 264f
internal const val GALLERY_GAP_DP = 2f
private val GROUP_CORNER_RADIUS = 6.dp

internal data class GalleryRow(val indices: List<Int>, val ratios: List<Float>, val heightDp: Float)

/** Justified rows preserve complete images instead of fitting portraits into fixed landscape slots. */
internal fun galleryRows(items: List<GalleryItem>, widthDp: Float = GALLERY_WIDTH_DP): List<GalleryRow> {
    val ratios = items.take(MAX_TILES).map { item ->
        item.aspectRatio?.takeIf { it.isFinite() && it > 0f }?.coerceIn(0.1f, 10f) ?: 1f
    }
    if (ratios.isEmpty()) return emptyList()
    fun row(start: Int, count: Int): GalleryRow {
        val rowRatios = ratios.subList(start, start + count)
        val width = (widthDp - GALLERY_GAP_DP * (count - 1)).coerceAtLeast(1f)
        return GalleryRow((start until start + count).toList(), rowRatios, width / rowRatios.sum())
    }
    var bestRows = emptyList<GalleryRow>()
    var bestScore = Double.POSITIVE_INFINITY
    // At most 13 partitions for the five visible tiles. No image decoding or pixel work is involved.
    fun choose(start: Int, rows: List<GalleryRow>, score: Double) {
        if (start == ratios.size) {
            if (score < bestScore) { bestScore = score; bestRows = rows }
            return
        }
        for (count in 1..minOf(3, ratios.size - start)) {
            val candidate = row(start, count)
            val distance = (candidate.heightDp - 160.0) / 160.0
            val tooShort = ((72.0 - candidate.heightDp) / 72.0).coerceAtLeast(0.0)
            choose(start + count, rows + candidate, score + distance * distance + 8 * tooShort * tooShort + 0.12)
        }
    }
    choose(0, emptyList(), 0.0)
    // Bound malformed/extreme media dimensions; ContentScale.Fit still keeps every pixel visible.
    return bestRows.map { it.copy(heightDp = it.heightDp.coerceIn(32f, 600f)) }
}

@Composable
fun TimelineItemGalleryView(
    eventId: EventId?,
    content: TimelineItemGalleryContent,
    onGalleryItemClick: (Int) -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val remainingCount = (content.items.size - MAX_TILES).coerceAtLeast(0)
    BoxWithConstraints(modifier = modifier.width(GALLERY_WIDTH_DP.dp)) {
        val widthDp = maxWidth.value
        val rows = remember(content.items, widthDp) { galleryRows(content.items, widthDp) }
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(GROUP_CORNER_RADIUS)),
            verticalArrangement = Arrangement.spacedBy(GALLERY_GAP_DP.dp),
        ) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(row.heightDp.dp),
                    horizontalArrangement = Arrangement.spacedBy(GALLERY_GAP_DP.dp),
                ) {
                    row.indices.forEachIndexed { position, index ->
                        GalleryItemCell(
                            eventId = eventId,
                            item = content.items[index],
                            isLast = remainingCount > 0 && index == MAX_TILES - 1,
                            remainingCount = remainingCount,
                            onClick = { onGalleryItemClick(index) },
                            onLongClick = onLongClick,
                            modifier = Modifier.weight(row.ratios[position]).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryItemCell(
    eventId: EventId?,
    item: GalleryItem,
    isLast: Boolean,
    remainingCount: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val eventContentValidationState = rememberEventContentValidationState(eventId, needsValidation = true)
    val thumbnailContentValidationState by eventContentValidationState.collectMediaState(item.thumbnailSource?.safeUrl)
    val mediaContentValidationState by eventContentValidationState.collectMediaState(item.mediaSource.safeUrl)

    val itemContentValidationState = remember(thumbnailContentValidationState, mediaContentValidationState) {
        if (thumbnailContentValidationState.isInvalid() || mediaContentValidationState.isInvalid()) {
            ContentValidationValue.Invalid
        } else if (thumbnailContentValidationState.hasUnrecoverableError() || mediaContentValidationState.hasUnrecoverableError()) {
            listOf(thumbnailContentValidationState, mediaContentValidationState).first { it is ContentValidationValue.UnrecoverableError }
        } else if (thumbnailContentValidationState.isLoading() || mediaContentValidationState.isLoading()) {
            ContentValidationValue.Loading
        } else {
            mediaContentValidationState
        }
    }

    Box(
        modifier = modifier
            .then(
                if (item.type == GalleryItem.Type.Image) Modifier.background(ElementTheme.colors.bgSubtleTertiary)
                else Modifier.blurHashBackground(item.blurhash, alpha = 0.9f)
            )
            .then(
                if (itemContentValidationState.isValid()) {
                    Modifier.combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            modifier = Modifier.fillMaxSize(),
            model = item.thumbnailMediaRequestData,
            contentScale = if (item.type == GalleryItem.Type.Image) ContentScale.Fit else ContentScale.Crop,
            alignment = Alignment.Center,
            contentDescription = item.filename,
        )

        if (item.type == GalleryItem.Type.Video) {
            VideoOverlay(duration = item.duration)
        }

        if (itemContentValidationState.isLoading()) {
            CircularProgressIndicator()
        } else if (itemContentValidationState.hasError()) {
            Box(
                modifier = Modifier.fillMaxSize().background(ElementTheme.colors.bgCriticalSubtle)
            ) {
                Icon(
                    modifier = Modifier.align(Alignment.Center),
                    imageVector = CompoundIcons.Error(),
                    tint = ElementTheme.colors.iconCriticalPrimary,
                    contentDescription = null,
                )
            }
        } else if (isLast && remainingCount > 0) {
            RemainingCountOverlay(count = remainingCount)
        }
    }
}

@Composable
private fun VideoOverlay(duration: kotlin.time.Duration) {
    val gradientColor = ElementTheme.colors.bgCanvasDefault

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(gradientColor.copy(alpha = 0f), gradientColor.copy(alpha = 1f))
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CompoundIcons.VideoCallSolid(),
                contentDescription = null,
                tint = ElementTheme.colors.textPrimary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = duration.formatShort(),
                color = ElementTheme.colors.textPrimary,
                style = ElementTheme.typography.fontBodySmMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RemainingCountOverlay(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ElementTheme.colors.bgSubtleTertiary.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$count",
            color = Color.White,
            style = ElementTheme.typography.fontHeadingSmMedium,
        )
    }
}

@PreviewsDayNight
@Composable
internal fun TimelineItemGalleryViewPreview(
    @PreviewParameter(TimelineItemGalleryContentPreviewParam::class) content: TimelineItemGalleryContent,
) = ElementPreview {
    TimelineItemGalleryView(
        eventId = null,
        content = content,
        onGalleryItemClick = {},
        onLongClick = {},
    )
}
