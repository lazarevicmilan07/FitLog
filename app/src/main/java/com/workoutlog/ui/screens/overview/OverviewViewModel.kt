package com.workoutlog.ui.screens.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.WorkoutEntry
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEpochMilli
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class OverviewUiState(
    val isLoading: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val entries: List<WorkoutEntry> = emptyList(),
    val workoutTypes: List<WorkoutType> = emptyList(),
    val totalWorkouts: Int = 0,
    val totalRestDays: Int = 0,
    val mostCommonType: WorkoutType? = null,
    val entriesByDate: Map<LocalDate, List<WorkoutEntry>> = emptyMap(),
    val selectedFilter: Long? = null
)

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OverviewUiState())
    val uiState: StateFlow<OverviewUiState> = _uiState.asStateFlow()

    init {
        loadMonth(YearMonth.now())
    }

    fun refresh() {
        loadMonth(_uiState.value.currentMonth)
    }

    fun loadMonth(yearMonth: YearMonth) {
        _uiState.value = _uiState.value.copy(currentMonth = yearMonth, isLoading = true)

        viewModelScope.launch {
            val startDate = yearMonth.atDay(1).minusDays(6).toEpochMilli()
            val endDate = yearMonth.atEndOfMonth().plusDays(6).toEpochMilli()

            val types = typeRepository.getAll().map { it.toDomain() }
            val typeMap = types.associateBy { it.id }
            val entries = entryRepository.getEntriesBetweenDates(startDate, endDate)
                .map { it.toDomain(typeMap[it.workoutTypeId]) }

            val filteredEntries = _uiState.value.selectedFilter?.let { filterId ->
                entries.filter { it.workoutTypeId == filterId }
            } ?: entries

            val entriesByDate = filteredEntries.groupBy { it.date }

            // Scope stats to current month only (entries may include overflow days for the calendar)
            val monthEntries = entries.filter {
                it.date.year == yearMonth.year && it.date.monthValue == yearMonth.monthValue
            }
            val restDays = monthEntries
                .filter { it.workoutType?.isRestDay == true }
                .map { it.date }
                .distinct()
                .size
            val realWorkoutEntries = monthEntries.filter { it.workoutType?.isRestDay != true }

            val typeCounts = realWorkoutEntries.groupBy { it.workoutTypeId }.mapValues { it.value.size }
            val mostCommonTypeId = typeCounts.maxByOrNull { it.value }?.key

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                entries = entries,
                workoutTypes = types,
                totalWorkouts = monthEntries.size,
                totalRestDays = restDays,
                mostCommonType = mostCommonTypeId?.let { typeMap[it] },
                entriesByDate = entriesByDate
            )
        }
    }

    fun previousMonth() {
        loadMonth(_uiState.value.currentMonth.minusMonths(1))
    }

    fun nextMonth() {
        loadMonth(_uiState.value.currentMonth.plusMonths(1))
    }

    fun setFilter(typeId: Long?) {
        _uiState.value = _uiState.value.copy(selectedFilter = typeId)
        loadMonth(_uiState.value.currentMonth)
    }

    fun deleteEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            entryRepository.deleteById(entry.id)
            loadMonth(_uiState.value.currentMonth)
        }
    }
}
