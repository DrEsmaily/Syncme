/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.libraries.dateformatter.impl

import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import dev.zacsweers.metro.Inject
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.toLocalDateTime

/** ICU's Persian chronology handles Nowruz and leap years; timestamps are interpreted in the device time zone. */
@Inject
class PersianDateFormatter(private val timezoneProvider: TimezoneProvider, private val clock: Clock) {
    internal data class Date(val year: Int, val month: Int, val day: Int, val weekday: Int, val hour: Int, val minute: Int)

    internal fun date(timestamp: Long): Date {
        // A calendar per conversion avoids shared mutable ICU state between UI, history and background formatters.
        val local = Instant.fromEpochMilliseconds(timestamp).toLocalDateTime(timezoneProvider.provide())
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), ULocale("en_US@calendar=persian"))
        check(calendar.type == "persian")
        // Convert the already-local civil date at UTC noon, avoiding a second timezone/DST interpretation by ICU.
        calendar.timeInMillis = local.date.toEpochDays() * 86400000L + 43200000L
        return Date(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.DAY_OF_WEEK), local.hour, local.minute)
    }

    fun format(timestamp: Long, mode: DateFormatterMode): String {
        val value = date(timestamp)
        val today = date(clock.now().toEpochMilliseconds())
        val short = "${value.day} ${months[value.month]}"
        val full = "$short ${value.year}"
        val time = String.format(Locale.ROOT, "%02d:%02d", value.hour, value.minute)
        return when (mode) {
            DateFormatterMode.Full -> "$full · $time"
            DateFormatterMode.Month -> "${months[value.month]} ${value.year}"
            DateFormatterMode.Day -> "${weekdays[value.weekday - 1]}, " + if (today.year == value.year) short else full
            DateFormatterMode.TimeOrDate -> if (value.year == today.year && value.month == today.month && value.day == today.day) time
                else if (value.year == today.year) short else full
            DateFormatterMode.TimeOnly -> time
        }
    }

    private companion object {
        val months = listOf("Farvardin", "Ordibehesht", "Khordad", "Tir", "Mordad", "Shahrivar", "Mehr", "Aban", "Azar", "Dey", "Bahman", "Esfand")
        val weekdays = listOf("Yekshanbe", "Doshanbe", "Seshanbe", "Chaharshanbe", "Panjshanbe", "Jome", "Shanbe")
    }
}
