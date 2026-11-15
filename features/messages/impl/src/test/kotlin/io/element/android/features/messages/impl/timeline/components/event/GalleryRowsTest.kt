/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.features.messages.impl.timeline.components.event

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GalleryRowsTest {
    @Test fun `two portrait images share one row without letterboxing`() {
        val rows = galleryRows(List(2) { aGalleryItem(width = 1080, height = 2400) })
        assertThat(rows.map { it.indices }).containsExactly(listOf(0, 1))
        assertFillsRows(rows)
    }

    @Test fun `three portrait screenshots share one compact row`() {
        val rows = galleryRows(List(3) { aGalleryItem(width = 1080, height = 2400) })
        assertThat(rows.map { it.indices }).containsExactly(listOf(0, 1, 2))
        assertThat(rows.sumOf { it.heightDp.toDouble() }).isLessThan(220.0)
        assertFillsRows(rows)
    }

    @Test fun `four portrait screenshots use two filled rows`() {
        val rows = galleryRows(List(4) { aGalleryItem(width = 1080, height = 2400) })
        assertThat(rows.map { it.indices }).containsExactly(listOf(0, 1), listOf(2, 3)).inOrder()
        assertFillsRows(rows)
    }

    @Test fun `landscapes keep useful height instead of becoming thin strips`() {
        val rows = galleryRows(List(2) { aGalleryItem(width = 1920, height = 1080) })
        assertThat(rows.map { it.indices }).containsExactly(listOf(0), listOf(1)).inOrder()
        assertFillsRows(rows)
    }

    @Test fun `mixed sizes retain index order and exact image proportions`() {
        val items = listOf(aGalleryItem(width = 1080, height = 2400), aGalleryItem(width = 1920, height = 1080),
            aGalleryItem(width = 1080, height = 1920), aGalleryItem(width = 800, height = 800), aGalleryItem(width = 2400, height = 1080))
        val rows = galleryRows(items)
        assertThat(rows.flatMap { it.indices }).containsExactly(0, 1, 2, 3, 4).inOrder()
        assertFillsRows(rows)
    }

    @Test fun `narrow available width preserves complete tile proportions`() {
        val rows = galleryRows(List(3) { aGalleryItem(width = 1080, height = 2400) }, widthDp = 220f)
        assertFillsRows(rows, widthDp = 220f)
    }

    @Test fun `overflow leaves first five in source order and empty albums are safe`() {
        assertThat(galleryRows(List(60) { aGalleryItem() }).flatMap { it.indices }).containsExactly(0, 1, 2, 3, 4).inOrder()
        assertThat(galleryRows(emptyList())).isEmpty()
    }

    @Test fun `invalid and extreme dimensions stay finite and bounded`() {
        val items = listOf(aGalleryItem(width = 0, height = 0), aGalleryItem(width = -100, height = 20),
            aGalleryItem(width = 1, height = Int.MAX_VALUE), aGalleryItem(width = Int.MAX_VALUE, height = 1))
        val rows = galleryRows(items)
        assertThat(rows.flatMap { it.indices }).containsExactly(0, 1, 2, 3).inOrder()
        rows.forEach {
            assertThat(it.heightDp.isFinite()).isTrue()
            assertThat(it.heightDp).isAtLeast(32f)
            assertThat(it.heightDp).isAtMost(600f)
            it.ratios.forEach { ratio -> assertThat(ratio).isGreaterThan(0f) }
        }
    }

    private fun assertFillsRows(rows: List<GalleryRow>, widthDp: Float = GALLERY_WIDTH_DP) {
        rows.forEach { row ->
            val contentWidth = widthDp - GALLERY_GAP_DP * (row.indices.size - 1)
            row.ratios.forEach { ratio ->
                val tileWidth = contentWidth * ratio / row.ratios.sum()
                assertThat(tileWidth / row.heightDp).isWithin(.001f).of(ratio)
            }
        }
    }
}
