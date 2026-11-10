/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.impl

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.dateformatter.api.DateFormatter
import io.element.android.libraries.dateformatter.api.DateFormatterMode

@ContributesBinding(AppScope::class)
class DefaultDateFormatter(
    private val dateFormatterFull: DateFormatterFull,
    private val dateFormatterMonth: DateFormatterMonth,
    private val dateFormatterDay: DateFormatterDay,
    private val dateFormatterTime: DateFormatterTime,
    private val dateFormatterTimeOnly: DateFormatterTimeOnly,
    private val calendarPreferences: io.element.android.libraries.dateformatter.api.CalendarPreferences,
    private val persianDateFormatter: PersianDateFormatter,
    private val dateTimeObserver: io.element.android.libraries.androidutils.system.DateTimeObserver,
) : DateFormatter {
    override val calendar get() = calendarPreferences.calendar
    override val changes get() = merge(calendar.map { Unit }, dateTimeObserver.changes.map { Unit })
    override fun format(
        timestamp: Long?,
        mode: DateFormatterMode,
        useRelative: Boolean,
    ): String {
        timestamp ?: return ""
        if (calendar.value == io.element.android.libraries.dateformatter.api.CalendarSystem.Persian && mode != DateFormatterMode.TimeOnly) {
            return persianDateFormatter.format(timestamp, mode)
        }
        return when (mode) {
            DateFormatterMode.Full -> {
                dateFormatterFull.format(timestamp, useRelative)
            }
            DateFormatterMode.Month -> {
                dateFormatterMonth.format(timestamp, useRelative)
            }
            DateFormatterMode.Day -> {
                dateFormatterDay.format(timestamp, useRelative)
            }
            DateFormatterMode.TimeOrDate -> {
                dateFormatterTime.format(timestamp, useRelative)
            }
            DateFormatterMode.TimeOnly -> {
                dateFormatterTimeOnly.format(timestamp)
            }
        }
    }
}
