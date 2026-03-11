package com.workoutlog.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.datastore.ReminderPreferences
import com.workoutlog.data.datastore.ReminderTime
import com.workoutlog.data.datastore.SettingsDataStore
import com.workoutlog.data.datastore.ThemeMode
import com.workoutlog.notifications.ReminderManager
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutGoalRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.DailyCountData
import com.workoutlog.domain.model.MonthlyCountData
import com.workoutlog.domain.model.MonthlyReport
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.WorkoutTypeCountData
import com.workoutlog.domain.model.YearlyReport
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEpochMilli
import com.workoutlog.util.BackupUtil
import com.workoutlog.util.ExportUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val isExportingExcel: Boolean = false,
    val isExportingPdf: Boolean = false
)

sealed class SettingsEvent {
    data class Message(val text: String) : SettingsEvent()
    data object ShowPremiumRequired : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val reminderPreferences: ReminderPreferences,
    private val reminderManager: ReminderManager,
    private val typeRepository: WorkoutTypeRepository,
    private val entryRepository: WorkoutEntryRepository,
    private val goalRepository: WorkoutGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val isPremium: StateFlow<Boolean> = settingsDataStore.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderEnabled: StateFlow<Boolean> = reminderPreferences.isEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderTime: StateFlow<ReminderTime> = reminderPreferences.reminderTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderTime(20, 0))

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val theme = settingsDataStore.themeMode.first()
            _uiState.value = SettingsUiState(themeMode = theme)
        }
    }

    fun setPremium(isPremium: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPremium(isPremium)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }

    /**
     * Enables or disables daily reminders.
     * When enabling, schedules the reminder at the current saved time.
     * When disabling, cancels any pending reminder.
     */
    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            reminderPreferences.setEnabled(enabled)
            if (enabled) {
                val time = reminderPreferences.reminderTime.first()
                reminderManager.scheduleReminder(time.hour, time.minute)
            } else {
                reminderManager.cancelReminder()
            }
        }
    }

    /**
     * Updates the reminder time and immediately reschedules the pending
     * WorkManager job so the new time takes effect from the next execution.
     */
    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            reminderPreferences.setReminderTime(hour, minute)
            // Only reschedule if reminders are currently enabled
            if (reminderPreferences.isEnabled.first()) {
                reminderManager.scheduleReminder(hour, minute)
            }
        }
    }

    fun backup(uri: Uri, isMonthly: Boolean, year: Int, month: Int) {
        viewModelScope.launch {
            if (!settingsDataStore.isPremium.first()) {
                _events.emit(SettingsEvent.ShowPremiumRequired)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isBackingUp = true)
            try {
                val types = typeRepository.getAll()
                val entries = if (isMonthly) {
                    val ym = YearMonth.of(year, month)
                    entryRepository.getEntriesBetweenDates(
                        ym.atDay(1).toEpochMilli(),
                        ym.atEndOfMonth().toEpochMilli()
                    )
                } else {
                    entryRepository.getEntriesBetweenDates(
                        LocalDate.of(year, 1, 1).toEpochMilli(),
                        LocalDate.of(year, 12, 31).toEpochMilli()
                    )
                }
                val goals = goalRepository.getAll()
                BackupUtil.createBackup(context, uri, types, entries, goals)
                _events.emit(SettingsEvent.Message("Backup saved successfully"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Message("Backup failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isBackingUp = false)
            }
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            if (!settingsDataStore.isPremium.first()) {
                _events.emit(SettingsEvent.ShowPremiumRequired)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isRestoring = true)
            try {
                val backupData = BackupUtil.readBackup(context, uri)
                if (backupData != null) {
                    BackupUtil.restoreBackup(
                        backupData,
                        typeRepository,
                        entryRepository,
                        goalRepository
                    )
                    loadSettings()
                    _events.emit(SettingsEvent.Message("Data restored successfully"))
                } else {
                    _events.emit(SettingsEvent.Message("Invalid backup file"))
                }
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Message("Restore failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isRestoring = false)
            }
        }
    }

    fun exportToExcel(uri: Uri, isMonthly: Boolean, year: Int, month: Int) {
        _uiState.value = _uiState.value.copy(isExportingExcel = true)
        viewModelScope.launch {
            try {
                val types = typeRepository.getAll().map { it.toDomain() }
                val typeMap = types.associateBy { it.id }
                if (isMonthly) {
                    val report = buildMonthlyReport(year, month, typeMap)
                    ExportUtil.exportMonthlyToExcel(context, uri, report)
                } else {
                    val report = buildYearlyReport(year, typeMap)
                    ExportUtil.exportYearlyToExcel(context, uri, report)
                }
                _events.emit(SettingsEvent.Message("Excel exported successfully"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Message("Export failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isExportingExcel = false)
            }
        }
    }

    fun exportToPdf(uri: Uri, isMonthly: Boolean, year: Int, month: Int) {
        _uiState.value = _uiState.value.copy(isExportingPdf = true)
        viewModelScope.launch {
            try {
                val types = typeRepository.getAll().map { it.toDomain() }
                val typeMap = types.associateBy { it.id }
                if (isMonthly) {
                    val report = buildMonthlyReport(year, month, typeMap)
                    ExportUtil.exportMonthlyToPdf(context, uri, report)
                } else {
                    val report = buildYearlyReport(year, typeMap)
                    ExportUtil.exportYearlyToPdf(context, uri, report)
                }
                _events.emit(SettingsEvent.Message("PDF exported successfully"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Message("Export failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isExportingPdf = false)
            }
        }
    }

    private suspend fun buildMonthlyReport(year: Int, month: Int, typeMap: Map<Long, WorkoutType>): MonthlyReport {
        val yearMonth = YearMonth.of(year, month)
        val startDate = yearMonth.atDay(1).toEpochMilli()
        val endDate = yearMonth.atEndOfMonth().toEpochMilli()

        val entries = entryRepository.getEntriesBetweenDates(startDate, endDate)
        val typeCounts = entryRepository.getWorkoutTypeCountsBetween(startDate, endDate)
        val dailyCounts = entryRepository.getDailyCountsBetween(startDate, endDate)

        val domainEntries = entries.map { it.toDomain(typeMap[it.workoutTypeId]) }
        val restDaysCount = domainEntries
            .filter { it.workoutType?.isRestDay == true }
            .map { it.date }
            .distinct()
            .size
        val totalDuration = entries.sumOf { it.durationMinutes ?: 0 }
        val totalCalories = entries.sumOf { it.caloriesBurned ?: 0 }

        return MonthlyReport(
            year = year,
            month = month,
            totalWorkouts = entries.size,
            totalRestDays = restDaysCount,
            totalDuration = totalDuration,
            totalCalories = totalCalories,
            workoutTypeCounts = typeCounts.mapNotNull { tc ->
                typeMap[tc.workoutTypeId]?.let { WorkoutTypeCountData(it, tc.count) }
            },
            dailyCounts = dailyCounts.map { dc ->
                val day = java.time.Instant.ofEpochMilli(dc.date)
                    .atZone(java.time.ZoneId.systemDefault()).dayOfMonth
                DailyCountData(day, dc.count)
            }
        )
    }

    private suspend fun buildYearlyReport(year: Int, typeMap: Map<Long, WorkoutType>): YearlyReport {
        val startDate = LocalDate.of(year, 1, 1).toEpochMilli()
        val endDate = LocalDate.of(year, 12, 31).toEpochMilli()

        val entries = entryRepository.getEntriesBetweenDates(startDate, endDate)
        val typeCounts = entryRepository.getWorkoutTypeCountsBetween(startDate, endDate)

        val monthlyGroups = entries.groupBy {
            java.time.Instant.ofEpochMilli(it.date)
                .atZone(java.time.ZoneId.systemDefault()).monthValue
        }
        val monthlyCounts = (1..12).map { month ->
            MonthlyCountData(month, monthlyGroups[month]?.size ?: 0)
        }

        val domainYearEntries = entries.map { it.toDomain(typeMap[it.workoutTypeId]) }
        val yearlyRestDays = domainYearEntries
            .filter { it.workoutType?.isRestDay == true }
            .map { it.date }
            .distinct()
            .size

        return YearlyReport(
            year = year,
            totalWorkouts = entries.size,
            totalRestDays = yearlyRestDays,
            monthlyCounts = monthlyCounts,
            workoutTypeCounts = typeCounts.mapNotNull { tc ->
                typeMap[tc.workoutTypeId]?.let { WorkoutTypeCountData(it, tc.count) }
            }
        )
    }
}
