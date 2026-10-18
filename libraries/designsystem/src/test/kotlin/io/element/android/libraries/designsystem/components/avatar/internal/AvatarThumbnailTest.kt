/*
 * Copyright (c) 2026 Element Creations Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.designsystem.components.avatar.internal

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64

class AvatarThumbnailTest : RobolectricTest() {
    @Test
    fun `stored thumbnail is immediately available without an image loading request`() {
        val encoded = encodedBitmap(width = 32, height = 32)
        val bitmap = decodeAvatarThumbnail(encoded, sizePx = 32)
        assertThat(bitmap).isNotNull()
        assertThat(bitmap?.width).isEqualTo(32)
        assertThat(bitmap?.height).isEqualTo(32)
    }

    @Test
    fun `missing and corrupt thumbnails fall back safely`() {
        assertThat(decodeAvatarThumbnail(null, 32)).isNull()
        assertThat(decodeAvatarThumbnail("invalid base64!", 32)).isNull()
        assertThat(decodeAvatarThumbnail(Base64.encode(byteArrayOf(1, 2, 3)), 32)).isNull()
    }

    @Test
    fun `oversized thumbnails are sampled to bound bitmap memory`() {
        val bitmap = decodeAvatarThumbnail(encodedBitmap(width = 512, height = 256), sizePx = 32)
        assertThat(bitmap).isNotNull()
        assertThat(bitmap?.width).isEqualTo(32)
        assertThat(bitmap?.height).isEqualTo(16)
    }

    private fun encodedBitmap(width: Int, height: Int): String {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            Base64.encode(output.toByteArray())
        }
    }
}
