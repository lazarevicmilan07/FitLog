package com.workoutlog.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.R
import com.workoutlog.data.datastore.ReminderPreferences
import com.workoutlog.data.datastore.ReminderTime
import com.workoutlog.data.datastore.SettingsDataStore
import com.workoutlog.data.datastore.ThemeMode
import com.workoutlog.notifications.BackupReminderManager
import com.workoutlog.notifications.BackupReminderPreferences
import com.workoutlog.notifications.BackupReminderSettings
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
    data class Message(@StringRes val resId: Int, val arg: String? = null) : SettingsEvent()
    data object ShowPremiumRequired : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val reminderPreferences: ReminderPreferences,
    private val reminderManager: ReminderManager,
    private val backupReminderPreferences: BackupReminderPreferences,
    private val backupReminderManager: BackupReminderManager,
    private val typeRepository: WorkoutTypeRepository,
    private val entryRepository: WorkoutEntryRepository,
    private val goalRepository: WorkoutGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val isPremium: StateFlow<Boolean> = settingsDataStore.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isBackupRestoreUnlocked: StateFlow<Boolean> = settingsDataStore.isBackupRestoreUnlocked
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderEnabled: StateFlow<Boolean> = reminderPreferences.isEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val reminderTime: StateFlow<ReminderTime> = reminderPreferences.reminderTime
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReminderTime(20, 0))

    val backupReminderSettings: StateFlow<BackupReminderSettings> = backupReminderPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackupReminderSettings())

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

    fun unlockBackupRestore(code: String) {
        if (code == "unlock-backup-restore") {
            viewModelScope.launch { settingsDataStore.setBackupRestoreUnlocked(true) }
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

    fun setBackupReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (!settingsDataStore.isPremium.first()) {
                _events.emit(SettingsEvent.ShowPremiumRequired)
                return@launch
            }
            val updated = backupReminderPreferences.settings.first().copy(enabled = enabled)
            backupReminderPreferences.save(updated)
            if (enabled) backupReminderManager.scheduleReminder(updated)
            else backupReminderManager.cancelReminder()
        }
    }

    fun updateBackupReminderSettings(settings: BackupReminderSettings) {
        viewModelScope.launch {
            if (!settingsDataStore.isPremium.first()) {
                _events.emit(SettingsEvent.ShowPremiumRequired)
                return@launch
            }
            backupReminderPreferences.save(settings)
            if (settings.enabled) backupReminderManager.scheduleReminder(settings)
        }
    }

    fun backup(uri: Uri, isMonthly: Boolean, months: List<YearMonth>, years: List<Int>) {
        viewModelScope.launch {
            if (!settingsDataStore.isPremium.first() && !settingsDataStore.isBackupRestoreUnlocked.first()) {
                _events.emit(SettingsEvent.ShowPremiumRequired)
                return@launch
            }
            _uiState.value = _uiState.value.copy(isBackingUp = true)
            try {
                val types = typeRepository.getAll()
                val entries = if (isMonthly) {
                    months.flatMap { ym ->
                        entryRepository.getEntriesBetweenDates(
                            ym.atDay(1).toEpochMilli(),
                            ym.atEndOfMonth().toEpochMilli()
                        )
                    }.distinctBy { it.id }
                } else {
                    years.flatMap { year ->
                        entryRepository.getEntriesBetweenDates(
                            LocalDate.of(year, 1, 1).toEpochMilli(),
                            LocalDate.of(year, 12, 31).toEpochMilli()
                        )
                    }.distinctBy { it.id }
                }
                val goals = goalRepository.getAll()
                BackupUtil.createBackup(context, uri, types, entries, goals)
                _events.emit(SettingsEvent.Message(R.string.msg_backup_saved))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Message(R.string.msg_backup_failed, e.message))
            } finally {
                _uiState.value = _uiState.value.copy(isBackingUp = false)
            }
        }
    }

    fun restore(uri: Uri) {
        viewModelScope.launch {
            if (!settingsDataStore.isPremium.first() && !settingsDataStore.isBackupRestoreUnlocked.first()) {
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
                    _events.emit(SettingsEvent.Message(R.string.msg_restore_success))
                } else {
                    _events.emit(SettingsEvent.Message(R.string.msg_restore_invalid))
                }
            } catch (e: Exception) {
                _events.emit(SettingsEvent.Message(R.string.msg_restore_failed, e.message))
            } finally {
                _uiState.value = _uiState.value.copy(isRestoring = false)
            }
        }
    }

    fun exportToExcel(uri: Uri, isMonthly: Boolean, months: List<YearMonth>, years: List<Int>) {
        _uiState.value = _uiState.value.copy(isExportingExcel = true)
        viewModelScope.launch {
            try {
                val types = typeRepository.getAll().map { it.toDomain() }
                val typeMap = types.associateBy { it.id }
                if (isMonthly) {
                    val reports = months.map { buildMonthlyReport(it.year, it.monthValue, typeMap) }
                    ExportUtil.exportMonthlyToExcel(context, uri, reports)
                } else {
                    val reports = years.map { buildYearlyReport(it, typeMap) }
                    ExportUtil.exportYearlyToExcel(context, uri, reports)
                }
                _events.emit(SettingsEvent.Message(R.string.msg_excel_exported))
            } catch (e: Throwable) {
                _events.emit(SettingsEvent.Message(R.string.msg_export_failed, e.message))
            } finally {
                _uiState.value = _uiState.value.copy(isExportingExcel = false)
            }
        }
    }

    fun exportToPdf(uri: Uri, isMonthly: Boolean, months: List<YearMonth>, years: List<Int>) {
        _uiState.value = _uiState.value.copy(isExportingPdf = true)
        viewModelScope.launch {
            try {
                val types = typeRepository.getAll().map { it.toDomain() }
                val typeMap = types.associateBy { it.id }
                if (isMonthly) {
                    val reports = months.map { buildMonthlyReport(it.year, it.monthValue, typeMap) }
                    ExportUtil.exportMonthlyToPdf(context, uri, reports)
                } else {
                    val reports = years.map { buildYearlyReport(it, typeMap) }
                    ExportUtil.exportYearlyToPdf(context, uri, reports)
                }
                _events.emit(SettingsEvent.Message(R.string.msg_pdf_exported))
            } catch (e: Throwable) {
                _events.emit(SettingsEvent.Message(R.string.msg_export_failed, e.message))
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
            totalWorkouts = domainEntries.count { it.workoutType?.isRestDay != true },
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

        val domainYearEntries = entries.map { it.toDomain(typeMap[it.workoutTypeId]) }

        val monthlyGroups = domainYearEntries
            .filter { it.workoutType?.isRestDay != true }
            .groupBy { it.date.monthValue }
        val monthlyCounts = (1..12).map { month ->
            MonthlyCountData(month, monthlyGroups[month]?.size ?: 0)
        }
        val yearlyRestDays = domainYearEntries
            .filter { it.workoutType?.isRestDay == true }
            .map { it.date }
            .distinct()
            .size

        return YearlyReport(
            year = year,
            totalWorkouts = domainYearEntries.count { it.workoutType?.isRestDay != true },
            totalRestDays = yearlyRestDays,
            monthlyCounts = monthlyCounts,
            workoutTypeCounts = typeCounts.mapNotNull { tc ->
                typeMap[tc.workoutTypeId]?.let { WorkoutTypeCountData(it, tc.count) }
            }
        )
    }
}
