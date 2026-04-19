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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode(val value: Int) {
    SYSTEM(0), LIGHT(1), DARK(2);

    companion object {
        fun fromValue(value: Int) = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = intPreferencesKey("theme_mode")
    private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
    private val showCaloriesKey = booleanPreferencesKey("show_calories")
    private val showDurationKey = booleanPreferencesKey("show_duration")
    private val isPremiumKey = booleanPreferencesKey("is_premium")
    private val showGoalsOnDashboardKey = booleanPreferencesKey("show_goals_on_dashboard")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.fromValue(prefs[themeModeKey] ?: ThemeMode.SYSTEM.value)
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[onboardingCompletedKey] ?: false
    }

    val showCalories: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[showCaloriesKey] ?: true
    }

    val showDuration: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[showDurationKey] ?: true
    }

    val isPremium: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[isPremiumKey] ?: false
    }

    val showGoalsOnDashboard: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[showGoalsOnDashboardKey] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeModeKey] = mode.value }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[onboardingCompletedKey] = completed }
    }

    suspend fun setShowCalories(show: Boolean) {
        context.dataStore.edit { it[showCaloriesKey] = show }
    }

    suspend fun setShowDuration(show: Boolean) {
        context.dataStore.edit { it[showDurationKey] = show }
    }

    suspend fun setPremium(isPremium: Boolean) {
        context.dataStore.edit { it[isPremiumKey] = isPremium }
    }

    suspend fun setShowGoalsOnDashboard(show: Boolean) {
        context.dataStore.edit { it[showGoalsOnDashboardKey] = show }
    }
}
