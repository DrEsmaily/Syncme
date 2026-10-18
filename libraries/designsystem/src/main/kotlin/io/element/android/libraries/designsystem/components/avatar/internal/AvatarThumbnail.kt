/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.avatar.internal

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.element.android.libraries.core.extensions.runCatchingExceptions
import kotlin.io.encoding.Base64

internal fun decodeAvatarThumbnail(encodedThumbnail: String?, sizePx: Int): Bitmap? {
    if (encodedThumbnail == null) return null
    return runCatchingExceptions {
        val bytes = Base64.decode(encodedThumbnail)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inSampleSize = 1
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) return@runCatchingExceptions null
        // Keep Coil's existing animation and format support for all other thumbnail types.
        if (options.outMimeType !in setOf("image/png", "image/jpeg")) return@runCatchingExceptions null
        // A homeserver may ignore the requested thumbnail dimensions. Bound decoded bitmap memory.
        val longestSide = maxOf(options.outWidth, options.outHeight)
        while (longestSide / options.inSampleSize / 2 >= sizePx.coerceAtLeast(1)) {
            options.inSampleSize *= 2
        }
        options.inJustDecodeBounds = false
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }.getOrNull()
}
