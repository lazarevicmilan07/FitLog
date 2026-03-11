package com.workoutlog.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reminderDataStore: DataStore<Preferences> by preferencesDataStore(name = "reminder_settings")

data class ReminderTime(val hour: Int, val minute: Int) {
    /** Formats the time as HH:MM (e.g. "20:00"). */
    fun formatted(): String = "%02d:%02d".format(hour, minute)
}

@Singleton
class ReminderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val enabledKey = booleanPreferencesKey("reminder_enabled")
    private val hourKey = intPreferencesKey("reminder_hour")
    private val minuteKey = intPreferencesKey("reminder_minute")

    val isEnabled: Flow<Boolean> = context.reminderDataStore.data.map { prefs ->
        prefs[enabledKey] ?: false
    }

    val reminderTime: Flow<ReminderTime> = context.reminderDataStore.data.map { prefs ->
        ReminderTime(
            hour = prefs[hourKey] ?: 20,
            minute = prefs[minuteKey] ?: 0
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.reminderDataStore.edit { it[enabledKey] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.reminderDataStore.edit {
            it[hourKey] = hour
            it[minuteKey] = minute
        }
    }
}
