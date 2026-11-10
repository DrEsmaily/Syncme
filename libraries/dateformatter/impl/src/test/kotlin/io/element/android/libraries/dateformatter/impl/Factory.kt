/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2024, 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.dateformatter.impl

import io.element.android.tests.testutils.InstrumentationStringProvider
import kotlinx.datetime.TimeZone
import java.util.Locale
import kotlin.time.Instant

/**
 * Create DefaultDateFormatter and set current time to the provided date.
 */
fun createFormatter(
    currentDate: String,
    calendarPreferences: io.element.android.libraries.dateformatter.api.CalendarPreferences = io.element.android.libraries.dateformatter.test.FakeCalendarPreferences(),
): DefaultDateFormatter {
    val clock = FakeClock().apply { givenInstant(Instant.parse(currentDate)) }
    val localDateTimeProvider = LocalDateTimeProvider(clock) { TimeZone.UTC }
    val dateFormatters = DateFormatters(
        localeChangeObserver = {},
        clock = clock,
        timeZoneProvider = { TimeZone.UTC },
        locale = Locale.getDefault(),
    )
    val stringProvider = InstrumentationStringProvider()
    val dateFormatterDay = DefaultDateFormatterDay(
        localDateTimeProvider = localDateTimeProvider,
        dateFormatters = dateFormatters,
    )
    return DefaultDateFormatter(
        dateTimeObserver = object : io.element.android.libraries.androidutils.system.DateTimeObserver {
            override val changes = kotlinx.coroutines.flow.emptyFlow<io.element.android.libraries.androidutils.system.DateTimeObserver.Event>()
        },
        calendarPreferences = calendarPreferences,
        persianDateFormatter = PersianDateFormatter({ TimeZone.UTC }, clock),
        dateFormatterFull = DateFormatterFull(
            stringProvider = stringProvider,
            localDateTimeProvider = localDateTimeProvider,
            dateFormatters = dateFormatters,
            dateFormatterDay = dateFormatterDay,
        ),
        dateFormatterMonth = DateFormatterMonth(
            stringProvider = stringProvider,
            localDateTimeProvider = localDateTimeProvider,
            dateFormatters = dateFormatters,
        ),
        dateFormatterDay = dateFormatterDay,
        dateFormatterTime = DateFormatterTime(
            localDateTimeProvider = localDateTimeProvider,
            dateFormatters = dateFormatters,
        ),
        dateFormatterTimeOnly = DateFormatterTimeOnly(
            localDateTimeProvider = localDateTimeProvider,
            dateFormatters = dateFormatters,
        ),
    )
}
