/*
 * Copyright (c) 2026 DrEsmaily (SyncMe Project).
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package io.element.android.libraries.dateformatter.impl

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import io.element.android.libraries.dateformatter.api.CalendarPreferences
import io.element.android.libraries.dateformatter.api.CalendarSystem
import io.element.android.libraries.di.annotations.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DefaultCalendarPreferences(@ApplicationContext context: Context) : CalendarPreferences {
    private val preferences = context.getSharedPreferences("syncme_calendar", Context.MODE_PRIVATE)
    private val mutableCalendar = MutableStateFlow(
        if (preferences.getString("calendar", null) == CalendarSystem.Persian.name) CalendarSystem.Persian else CalendarSystem.Gregorian
    )
    override val calendar = mutableCalendar.asStateFlow()
    private val lock = Mutex()

    override suspend fun setCalendar(calendar: CalendarSystem) = withContext(Dispatchers.IO) {
        lock.withLock {
            check(preferences.edit().putString("calendar", calendar.name).commit())
            mutableCalendar.value = calendar
        }
    }
}
