/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.libraries.textcomposer.mentions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.tests.testutils.robolectric.RobolectricTest
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MentionSpanGeometryTest : RobolectricTest() {
    @Test
    fun `mention background follows its text baseline instead of filling a tall line`() {
        val styles = MentionSpanTheme(UserId("@me:example.org"))
        val span = MentionSpan(MentionType.User(UserId("@other:example.org")))
        span.updateTheme(styles)
        span.updateDisplayText(object : MentionSpanFormatter {
            override fun formatDisplayText(mentionType: MentionType): CharSequence = "@other"
        })
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 20f }
        val width = span.getSize(paint, null, 0, 0, null)
        val bitmap = Bitmap.createBitmap(width + 20, 80, Bitmap.Config.ARGB_8888)
        span.draw(Canvas(bitmap), null, 0, 0, 10f, 0, 40, 80, paint)
        val centerX = 10 + width / 2
        assertThat(Color.alpha(bitmap.getPixel(centerX, 10))).isEqualTo(0)
        assertThat(Color.alpha(bitmap.getPixel(centerX, 32))).isGreaterThan(0)
        assertThat(Color.alpha(bitmap.getPixel(centerX, 70))).isEqualTo(0)
    }
}
