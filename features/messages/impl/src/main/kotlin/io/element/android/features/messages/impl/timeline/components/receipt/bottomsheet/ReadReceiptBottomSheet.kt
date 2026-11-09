/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.messages.impl.timeline.components.receipt.bottomsheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import io.element.android.features.messages.impl.timeline.model.TimelineItem
import io.element.android.compound.theme.ElementTheme
import io.element.android.libraries.designsystem.components.avatar.AvatarSize
import io.element.android.libraries.designsystem.preview.ElementPreview
import io.element.android.libraries.designsystem.preview.PreviewsDayNight
import io.element.android.libraries.designsystem.theme.components.ListItem
import io.element.android.libraries.designsystem.theme.components.ModalBottomSheet
import io.element.android.libraries.designsystem.theme.components.Text
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.ui.components.MatrixUserRow
import io.element.android.libraries.ui.strings.CommonStrings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadReceiptBottomSheet(
    state: ReadReceiptBottomSheetState,
    onUserDataClick: (UserId) -> Unit,
    modifier: Modifier = Modifier,
    onOpenAttachment: (TimelineItem.Event) -> Unit = {},
) {
    val isVisible = state.selectedEvent != null

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
    )
    val coroutineScope = rememberCoroutineScope()
    if (isVisible) {
        ModalBottomSheet(
            modifier = modifier,
//            modifier = modifier.navigationBarsPadding() - FIXME after https://issuetracker.google.com/issues/275849044
//                    .imePadding()
            sheetState = sheetState,
            onDismissRequest = {
                coroutineScope.launch {
                    sheetState.hide()
                    state.eventSink(ReadReceiptBottomSheetEvent.Dismiss)
                }
            },
            scrollable = false,
        ) {
            ReadReceiptBottomSheetContent(
                state = state,
                onOpenAttachment = { event ->
                    coroutineScope.launch {
                        sheetState.hide()
                        state.eventSink(ReadReceiptBottomSheetEvent.Dismiss)
                        onOpenAttachment(event)
                    }
                },
                onUserDataClick = {
                    coroutineScope.launch {
                        sheetState.hide()
                        state.eventSink(ReadReceiptBottomSheetEvent.Dismiss)
                        onUserDataClick.invoke(it)
                    }
                },
            )
            // FIXME remove after https://issuetracker.google.com/issues/275849044
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
internal fun ReadReceiptBottomSheetContent(
    state: ReadReceiptBottomSheetState,
    onUserDataClick: (UserId) -> Unit,
    onOpenAttachment: (TimelineItem.Event) -> Unit = {},
) {
    val event = state.selectedEvent
    val receipts = event?.readReceiptState?.receipts.orEmpty()
    val reactions = remember(event?.reactionsState) {
        messageReactionReports(event?.reactionsState?.reactions.orEmpty())
    }
    LazyColumn {
        if (event?.content?.isMedia == true) {
            item {
                ListItem(onClick = { onOpenAttachment(event) }) {
                    Text(stringResource(CommonStrings.syncme_message_open_attachment))
                }
            }
        }
        item {
            ListItem { Text(stringResource(CommonStrings.common_seen_by)) }
        }
        if (receipts.isEmpty()) {
            item {
                Text(
                    stringResource(CommonStrings.syncme_message_no_receipts),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = ElementTheme.colors.textSecondary,
                )
            }
        }
        items(receipts, key = { "receipt:${it.avatarData.id}" }) { receipt ->
            val userId = UserId(receipt.avatarData.id)
            MatrixUserRow(
                modifier = Modifier.clickable { onUserDataClick(userId) },
                matrixUser = MatrixUser(userId, receipt.avatarData.name, receipt.avatarData.url),
                avatarSize = AvatarSize.ReadReceiptList,
                trailingContent = {
                    Text(receipt.formattedDate, style = ElementTheme.typography.fontBodySmRegular, color = ElementTheme.colors.textSecondary)
                },
            )
        }
        if (event?.isMine == true) {
            item {
                ListItem { Text(stringResource(CommonStrings.syncme_message_reactions)) }
            }
            if (reactions.isEmpty()) {
                item {
                    Text(
                        stringResource(CommonStrings.syncme_message_no_reactions),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = ElementTheme.colors.textSecondary,
                    )
                }
            }
            items(reactions, key = { "reaction:${it.user.userId.value}" }) { report ->
                Column(Modifier.clickable { onUserDataClick(report.user.userId) }) {
                    MatrixUserRow(matrixUser = report.user, avatarSize = AvatarSize.ReadReceiptList)
                    Text(
                        text = report.entries.joinToString("\n"),
                        modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 12.dp),
                        style = ElementTheme.typography.fontBodyMdRegular,
                    )
                }
            }
        }
    }
}

@PreviewsDayNight
@Composable
internal fun ReadReceiptBottomSheetPreview(@PreviewParameter(ReadReceiptBottomSheetStatePreviewParam::class) state: ReadReceiptBottomSheetState) {
    ElementPreview(fillMaxSize = true) {
        ReadReceiptBottomSheet(
            state = state,
            onUserDataClick = {},
        )
    }
}
