/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.libraries.dateformatter.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Display calendar only. Never changes stored timestamps or event ordering. */
enum class CalendarSystem { Gregorian, Persian }

interface CalendarPreferences {
    val calendar: StateFlow<CalendarSystem>
    suspend fun setCalendar(calendar: CalendarSystem)
}

internal val defaultCalendar: StateFlow<CalendarSystem> = MutableStateFlow(CalendarSystem.Gregorian)
