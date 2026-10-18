/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.avatar.internal

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.element.android.libraries.designsystem.components.avatar.AvatarData
import timber.log.Timber

@Composable
internal fun ImageAvatar(
    avatarData: AvatarData,
    avatarShape: Shape,
    forcedAvatarSize: Dp?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val size = forcedAvatarSize ?: avatarData.size.dp
    // Session thumbnails are small, local images. Decode once so the fallback is ready in the
    // first frame instead of briefly displaying initials while a second Coil request starts.
    val thumbnailSizePx = with(LocalDensity.current) { size.roundToPx().coerceAtLeast(1) }
    val thumbnail = remember(avatarData.thumbnail, thumbnailSizePx) {
        decodeAvatarThumbnail(avatarData.thumbnail, thumbnailSizePx)
    }
    SubcomposeAsyncImage(
        model = avatarData,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(avatarShape)
    ) {
        val collectedState by painter.state.collectAsState()
        when (val state = collectedState) {
            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            is AsyncImagePainter.State.Error -> {
                SideEffect {
                    Timber.e(
                        state.result.throwable,
                        "Error loading avatar $state\n${state.result}"
                    )
                }
                ThumbnailOrInitialLetterAvatar(
                    avatarData = avatarData,
                    thumbnail = thumbnail,
                    avatarShape = avatarShape,
                    forcedAvatarSize = forcedAvatarSize,
                    contentDescription = contentDescription,
                )
            }
            else -> ThumbnailOrInitialLetterAvatar(
                avatarData = avatarData,
                thumbnail = thumbnail,
                avatarShape = avatarShape,
                forcedAvatarSize = forcedAvatarSize,
                contentDescription = contentDescription,
            )
        }
    }
}

/**
 * Render the locally stored thumbnail of the avatar if any, else the initial letter.
 */
@Composable
private fun ThumbnailOrInitialLetterAvatar(
    avatarData: AvatarData,
    thumbnail: Bitmap?,
    avatarShape: Shape,
    forcedAvatarSize: Dp?,
    contentDescription: String?,
) {
    if (thumbnail == null) {
        InitialLetterAvatar(
            avatarData = avatarData,
            avatarShape = avatarShape,
            forcedAvatarSize = forcedAvatarSize,
            contentDescription = contentDescription,
        )
    } else {
        Image(
            bitmap = thumbnail.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(forcedAvatarSize ?: avatarData.size.dp)
                .clip(avatarShape),
        )
    }
}
