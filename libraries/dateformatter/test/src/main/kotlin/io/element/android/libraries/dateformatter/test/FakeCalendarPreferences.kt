/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.libraries.dateformatter.test

import io.element.android.libraries.dateformatter.api.CalendarPreferences
import io.element.android.libraries.dateformatter.api.CalendarSystem
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCalendarPreferences(initial: CalendarSystem = CalendarSystem.Gregorian) : CalendarPreferences {
    override val calendar = MutableStateFlow(initial)
    override suspend fun setCalendar(calendar: CalendarSystem) { this.calendar.value = calendar }
}
