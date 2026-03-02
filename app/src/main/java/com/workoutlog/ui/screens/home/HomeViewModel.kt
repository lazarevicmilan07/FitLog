package com.workoutlog.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.local.entity.WorkoutGoalEntity
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutGoalRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.GoalPeriod
import com.workoutlog.domain.model.WorkoutEntry
import com.workoutlog.domain.model.WorkoutGoal
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.getCurrentDateRange
import com.workoutlog.domain.model.label
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEpochMilli
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class GoalWithProgress(
    val goal: WorkoutGoal,
    val current: Int,
    val periodLabel: String
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val currentMonth: YearMonth = YearMonth.now(),
    val workoutTypes: List<WorkoutType> = emptyList(),
    val entries: List<WorkoutEntry> = emptyList(),
    val entriesByDate: Map<LocalDate, List<WorkoutEntry>> = emptyMap(),
    val workoutCount: Int = 0,
    val totalEntries: Int = 0,
    val daysElapsed: Int = 0,
    val workoutPercentage: Int = 0,
    val selectedFilters: Set<Long> = emptySet(),
    val goals: List<GoalWithProgress> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository,
    private val goalRepository: WorkoutGoalRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedFilters = MutableStateFlow<Set<Long>>(emptySet())

    // Covers the full current calendar year — used for goal progress calculation
    private val _yearlyEntriesFlow = run {
        val today = LocalDate.now()
        val yearStart = LocalDate.of(today.year, 1, 1).toEpochMilli()
        val yearEnd = LocalDate.of(today.year, 12, 31).toEpochMilli()
        entryRepository.getEntriesBetweenDatesFlow(yearStart, yearEnd)
    }

    private val _goalProgressFlow = _currentMonth.flatMapLatest { month ->
        combine(
            goalRepository.getGoalsForMonthFlow(month.year, month.monthValue),
            typeRepository.getAllFlow(),
            _yearlyEntriesFlow
        ) { goalEntities, typeEntities, yearlyEntries ->
            val typeMap = typeEntities.associate { it.id to it.toDomain() }

            goalEntities.map { goalEntity ->
                val goal = goalEntity.toDomain(typeMap[goalEntity.workoutTypeId])
                val (startMillis, endMillis) = goal.period.getCurrentDateRange()

                val count = yearlyEntries.count { entry ->
                    entry.date in startMillis..endMillis && when {
                        goal.workoutTypeId != null -> entry.workoutTypeId == goal.workoutTypeId
                        else -> typeMap[entry.workoutTypeId]?.isRestDay == false
                    }
                }

                GoalWithProgress(
                    goal = goal,
                    current = count,
                    periodLabel = goal.period.label()
                )
            }.sortedBy { it.goal.period.ordinal }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _currentMonth,
        _selectedFilters,
        typeRepository.getAllFlow(),
        _currentMonth.flatMapLatest { month ->
            val startDate = month.atDay(1).toEpochMilli()
            val endDate = month.atEndOfMonth().toEpochMilli()
            entryRepository.getEntriesBetweenDatesFlow(startDate, endDate)
        },
        _goalProgressFlow
    ) { month, filters, typesEntities, entryEntities, goalProgress ->
        val typeMap = typesEntities.associate { it.id to it.toDomain() }
        val types = typesEntities.map { it.toDomain() }
        val allEntries = entryEntities.map { it.toDomain(typeMap[it.workoutTypeId]) }

        val filteredEntries = if (filters.isEmpty()) allEntries
        else allEntries.filter { it.workoutTypeId in filters }

        val entriesByDate = filteredEntries.groupBy { it.date }

        val workoutCount = allEntries.count { it.workoutType?.isRestDay != true }
        val totalEntries = allEntries.size
        val today = LocalDate.now()
        val currentYearMonth = YearMonth.now()
        val daysElapsed = when {
            month > currentYearMonth -> 0
            month == currentYearMonth -> today.dayOfMonth
            else -> month.lengthOfMonth()
        }
        val workoutPercentage = if (daysElapsed > 0) workoutCount * 100 / daysElapsed else 0

        HomeUiState(
            isLoading = false,
            currentMonth = month,
            workoutTypes = types,
            entries = allEntries,
            entriesByDate = entriesByDate,
            workoutCount = workoutCount,
            totalEntries = totalEntries,
            daysElapsed = daysElapsed,
            workoutPercentage = workoutPercentage,
            selectedFilters = filters,
            goals = goalProgress
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun goToMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    fun toggleFilter(typeId: Long) {
        _selectedFilters.update { current ->
            if (typeId in current) current - typeId else current + typeId
        }
    }

    fun clearFilters() {
        _selectedFilters.value = emptySet()
    }

    fun deleteEntry(entry: WorkoutEntry) {
        viewModelScope.launch {
            entryRepository.deleteById(entry.id)
        }
    }

    fun addGoal(period: GoalPeriod, targetCount: Int, workoutTypeId: Long?) {
        viewModelScope.launch {
            val month = _currentMonth.value
            goalRepository.insert(
                WorkoutGoalEntity(
                    period = period.name,
                    targetCount = targetCount,
                    workoutTypeId = workoutTypeId,
                    boundYear = month.year,
                    boundMonth = if (period == GoalPeriod.YEARLY) null else month.monthValue
                )
            )
        }
    }

    fun updateGoal(goalId: Long, period: GoalPeriod, targetCount: Int, workoutTypeId: Long?) {
        viewModelScope.launch {
            val existing = goalRepository.getById(goalId) ?: return@launch
            goalRepository.update(
                existing.copy(
                    period = period.name,
                    targetCount = targetCount,
                    workoutTypeId = workoutTypeId
                )
            )
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            goalRepository.deleteById(goalId)
        }
    }
}
