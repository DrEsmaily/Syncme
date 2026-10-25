/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.element.android.libraries.ui.common.layout

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.Text

@PreviewsDayNight
@Composable
internal fun ContentAvoidingTimestampPreview() = ElementPreview {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ContentAvoidingLayout(
            modifier = Modifier.width(240.dp),
            overlay = { Text("07:50") },
        ) {
            Text(
                text = "\u0633\u0644\u0627\u0645 \u0646\u0633\u062e\u0647 \u062c\u062f\u06cc\u062f\n\nhttps://example.org/download/Ab3KqobLRSoQ6RGj",
                style = TextStyle(textDirection = TextDirection.Content),
                onTextLayout = ContentAvoidingLayout.measureLastTextLine(onContentLayoutChange = this::onContentLayoutChange),
            )
        }
    }
}
