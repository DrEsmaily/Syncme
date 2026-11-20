/*
 * Copyright (c) 2026 SyncMe contributors.
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 */

package io.element.android.features.messages.impl.timeline.components.event

import android.graphics.Path
import android.graphics.Region
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.widget.Toast
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.text.getSpans
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.androidutils.clipboard.AndroidClipboardHelper
import io.element.android.libraries.textcomposer.ElementRichTextEditorStyle
import io.element.android.libraries.ui.common.layout.ContentAvoidingLayout
import io.element.android.libraries.ui.common.layout.ContentAvoidingLayoutData
import io.element.android.libraries.ui.strings.CommonStrings
import io.element.android.wysiwyg.compose.EditorStyledText
import io.element.android.wysiwyg.link.Link
import io.element.android.wysiwyg.view.spans.CodeBlockSpan
import io.element.android.wysiwyg.view.spans.CustomMentionSpan
import io.element.android.wysiwyg.view.spans.InlineCodeSpan
import kotlin.math.floor

@Composable
internal fun InteractiveBubbleText(
    text: CharSequence,
    onLinkClick: (Link) -> Unit,
    onLinkLongClick: (Link) -> Unit,
    onContentLayoutChange: (ContentAvoidingLayoutData) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = remember(context) { AndroidClipboardHelper(context.applicationContext) }
    val linkColor = ElementTheme.colors.textLinkExternal.toArgb()
    val styled = remember(text, linkColor) { styleBubbleLinks(text, linkColor) }
    val layout = remember(styled) { mutableStateOf<Layout?>(null) }
    val open = rememberUpdatedState(onLinkClick)
    val openMenu = rememberUpdatedState(onLinkLongClick)
    val copy = rememberUpdatedState<(String) -> Unit> { value ->
        clipboard.copyPlainText(value, isSensitive = true) {
            Toast.makeText(context, CommonStrings.common_copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }
    Box(
        modifier.pointerInput(styled) {
            awaitEachGesture {
                // Claim an interactive span before the surrounding bubble or AndroidView sees the press.
                val pass = PointerEventPass.Initial
                val down = awaitFirstDown(pass = pass)
                val target = layout.value?.bubbleTextActionAt(down.position) ?: return@awaitEachGesture
                down.consume()
                try {
                    val up = withTimeout(viewConfiguration.longPressTimeoutMillis) {
                        waitForUpOrCancellation(pass)
                    } ?: return@awaitEachGesture
                    up.consume()
                    if (layout.value?.bubbleTextActionAt(up.position) != target) return@awaitEachGesture
                    when (target) {
                        is BubbleTextAction.Copy -> copy.value(target.value)
                        is BubbleTextAction.Open -> open.value(target.link)
                    }
                } catch (_: PointerEventTimeoutCancellationException) {
                    if (target is BubbleTextAction.Open) openMenu.value(target.link)
                }
            }
        },
    ) {
        val reportLayout = ContentAvoidingLayout.measureLegacyLastTextLine(onContentLayoutChange = onContentLayoutChange)
        EditorStyledText(
            text = styled,
            onLinkClickedListener = onLinkClick,
            onLinkLongClickedListener = onLinkLongClick,
            style = ElementRichTextEditorStyle.textStyle(),
            releaseOnDetach = false,
            onTextLayout = {
                layout.value = it
                reportLayout(it)
            },
        )
    }
}

internal sealed interface BubbleTextAction {
    data class Copy(val value: String, val start: Int) : BubbleTextAction
    data class Open(val link: Link, val start: Int) : BubbleTextAction
}

internal fun CharSequence.hasCopyableInlineCode(): Boolean =
    this is Spanned && getSpans<InlineCodeSpan>().isNotEmpty()

private fun styleBubbleLinks(text: CharSequence, color: Int): Spanned = SpannableString(text).apply {
    for (span in getSpans<URLSpan>()) {
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        if (getSpans<CodeBlockSpan>(start, end).isNotEmpty() || getSpans<InlineCodeSpan>(start, end).isNotEmpty()) {
            removeSpan(span)
        } else if (getSpans<CustomMentionSpan>(start, end).isEmpty()) {
            setSpan(ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

internal fun Layout.bubbleTextActionAt(point: Offset): BubbleTextAction? {
    val text = text as? Spanned ?: return null
    if (point.y < 0 || point.y >= height) return null
    val line = getLineForVertical(point.y.toInt())
    if (point.x < getLineLeft(line) || point.x >= getLineRight(line)) return null
    val lineStart = getLineStart(line)
    val lineEnd = getLineVisibleEnd(line)
    if (lineStart >= lineEnd) return null

    fun contains(span: Any): Boolean {
        val start = maxOf(text.getSpanStart(span), lineStart)
        val end = minOf(text.getSpanEnd(span), lineEnd)
        if (start >= end) return false
        val path = Path()
        getSelectionPath(start, end, path)
        return Region().apply { setPath(path, Region(0, 0, width, height)) }
            .contains(floor(point.x).toInt(), floor(point.y).toInt())
    }

    text.getSpans<InlineCodeSpan>(lineStart, lineEnd).firstOrNull(::contains)?.let { span ->
        val start = text.getSpanStart(span)
        return BubbleTextAction.Copy(text.substring(start, text.getSpanEnd(span)), start)
    }
    val span = text.getSpans<URLSpan>(lineStart, lineEnd).firstOrNull(::contains)
        ?: text.getSpans<CustomMentionSpan>(lineStart, lineEnd).firstOrNull(::contains)
        ?: return null
    val url = when (span) {
        is URLSpan -> span.url
        is CustomMentionSpan -> span.url
        else -> null
    } ?: return null
    val start = text.getSpanStart(span)
    return BubbleTextAction.Open(Link(url = url, text = text.substring(start, text.getSpanEnd(span))), start)
}
