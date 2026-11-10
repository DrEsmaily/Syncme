/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.libraries.dateformatter.impl

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.element.android.libraries.dateformatter.api.CalendarSystem
import io.element.android.libraries.dateformatter.api.DateFormatterMode
import io.element.android.libraries.dateformatter.test.FakeCalendarPreferences
import io.element.android.tests.testutils.robolectric.RobolectricTest
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.time.Instant

@Config(sdk = [24, 35], qualifiers = "en")
class PersianDateFormatterTest : RobolectricTest() {
    private val clock = FakeClock().apply { givenInstant(Instant.parse("2026-11-10T11:05:00Z")) }
    private fun formatter(zone: TimeZone = TimeZone.UTC) = PersianDateFormatter({ zone }, clock)
    private fun millis(date: String) = Instant.parse(date).toEpochMilliseconds()

    @Test
    fun `official modern date anchors include leap Esfand and Nowruz`() {
        val anchors = listOf(
            "1970-01-01" to "1348-10-11", "2000-02-29" to "1378-12-10",
            "2020-03-20" to "1399-1-1", "2021-03-20" to "1399-12-30", "2021-03-21" to "1400-1-1",
            "2022-03-21" to "1401-1-1", "2023-03-21" to "1402-1-1",
            "2024-03-19" to "1402-12-29", "2024-03-20" to "1403-1-1",
            "2025-03-20" to "1403-12-30", "2025-03-21" to "1404-1-1",
            "2026-03-20" to "1404-12-29", "2026-03-21" to "1405-1-1",
            "2026-04-20" to "1405-1-31", "2026-04-21" to "1405-2-1",
            "2026-09-22" to "1405-6-31", "2026-09-23" to "1405-7-1",
            "2026-11-10" to "1405-8-19", "2026-12-21" to "1405-9-30", "2026-12-22" to "1405-10-1",
        )
        val formatter = formatter()
        for ((gregorian, expected) in anchors) {
            val value = formatter.date(millis("${gregorian}T12:00:00Z"))
            assertWithMessage(gregorian).that("${value.year}-${value.month + 1}-${value.day}").isEqualTo(expected)
        }
    }

    @Test
    fun `every civil day progresses once across twenty years of month and year boundaries`() {
        val formatter = formatter()
        var gregorian = LocalDate.of(2010, 1, 1)
        var previous: PersianDateFormatter.Date? = null
        while (gregorian < LocalDate.of(2031, 1, 1)) {
            val value = formatter.date(gregorian.toEpochDay() * 86400000L + 43200000L)
            previous?.let { old ->
                if (old.month == value.month && old.year == value.year) {
                    assertThat(value.day).isEqualTo(old.day + 1)
                } else {
                    assertThat(value.day).isEqualTo(1)
                    assertThat(value.month).isEqualTo((old.month + 1) % 12)
                    assertThat(value.year).isEqualTo(old.year + if (value.month == 0) 1 else 0)
                    if (old.month < 6) assertThat(old.day).isEqualTo(31)
                    else if (old.month < 11) assertThat(old.day).isEqualTo(30)
                    else assertThat(old.day).isAnyOf(29, 30)
                }
                assertThat(value.weekday).isEqualTo(old.weekday % 7 + 1)
            }
            previous = value
            gregorian = gregorian.plusDays(1)
        }
    }

    @Test
    fun `Tehran and fixed offsets change the civil date at local midnight`() {
        for (zone in listOf(TimeZone.of("Asia/Tehran"), TimeZone.of("+03:30"))) {
            val formatter = formatter(zone)
            assertThat(formatter.date(millis("2026-11-09T20:29:59.999Z")).day).isEqualTo(18)
            assertThat(formatter.date(millis("2026-11-09T20:30:00Z")).day).isEqualTo(19)
        }
        val la = formatter(TimeZone.of("America/Los_Angeles"))
        assertThat(la.date(millis("2026-11-10T07:59:59.999Z")).day).isEqualTo(18)
        assertThat(la.date(millis("2026-11-10T08:00:00Z")).day).isEqualTo(19)
    }

    @Test
    fun `approved Finglish spellings weekday and Latin digits are stable`() {
        val formatter = formatter(TimeZone.of("Asia/Tehran"))
        val timestamp = millis("2026-11-10T11:05:00Z")
        assertThat(formatter.format(timestamp, DateFormatterMode.Full)).isEqualTo("19 Aban 1405 · 14:35")
        assertThat(formatter.format(timestamp, DateFormatterMode.Day)).isEqualTo("Seshanbe, 19 Aban")
        assertThat(formatter.format(timestamp, DateFormatterMode.Month)).isEqualTo("Aban 1405")
    }

    @Test
    fun `Persian year comparison includes year across Nowruz within one Gregorian year`() {
        clock.givenInstant(Instant.parse("2026-03-21T12:00:00Z"))
        val formatter = formatter()
        assertThat(formatter.format(millis("2026-03-20T12:00:00Z"), DateFormatterMode.TimeOrDate)).isEqualTo("29 Esfand 1404")
    }

    @Test
    fun `calendar switch changes central formatter immediately and preserves nulls and time only`() = runTest {
        val preferences = FakeCalendarPreferences()
        val formatter = createFormatter("2026-11-10T11:05:00Z", preferences)
        val timestamp = millis("2026-11-10T11:05:00Z")
        val gregorian = formatter.format(timestamp)
        val originalTime = formatter.format(timestamp, DateFormatterMode.TimeOnly)
        preferences.setCalendar(CalendarSystem.Persian)
        assertThat(formatter.format(timestamp)).isEqualTo("19 Aban 1405 · 11:05")
        assertThat(formatter.format(timestamp, DateFormatterMode.TimeOnly)).isEqualTo(originalTime)
        assertThat(formatter.format(null)).isEmpty()
        preferences.setCalendar(CalendarSystem.Gregorian)
        assertThat(formatter.format(timestamp)).isEqualTo(gregorian)
    }

    @Test
    fun `calendar choice survives a new preference instance`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("syncme_calendar", Context.MODE_PRIVATE).edit().clear().commit()
        val first = DefaultCalendarPreferences(context)
        assertThat(first.calendar.value).isEqualTo(CalendarSystem.Gregorian)
        first.setCalendar(CalendarSystem.Persian)
        assertThat(DefaultCalendarPreferences(context).calendar.value).isEqualTo(CalendarSystem.Persian)
        first.setCalendar(CalendarSystem.Gregorian)
    }
}
