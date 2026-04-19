package com.workoutlog.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.datastore.SettingsDataStore
import com.workoutlog.data.local.entity.WorkoutGoalEntity
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutGoalRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.GoalPeriod
import com.workoutlog.domain.model.WorkoutEntry
import com.workoutlog.domain.model.WorkoutGoal
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.domain.model.getDateRangeForMonth
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEpochMilli
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val isPast: Boolean = false
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
    val goals: List<GoalWithProgress> = emptyList(),
    val showGoalsOnDashboard: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository,
    private val goalRepository: WorkoutGoalRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedFilters = MutableStateFlow<Set<Long>>(emptySet())

    private val _goalCompletedEvent = MutableSharedFlow<WorkoutGoal>(extraBufferCapacity = 1)
    val goalCompletedEvent: SharedFlow<WorkoutGoal> = _goalCompletedEvent.asSharedFlow()

    // Tracks the last known count per goal ID to detect when a goal crosses its target
    private val _previousGoalCounts = mutableMapOf<Long, Int>()

    private val _goalProgressFlow = _currentMonth.flatMapLatest { month ->
        val yearStart = LocalDate.of(month.year, 1, 1).toEpochMilli()
        val yearEnd = LocalDate.of(month.year, 12, 31).toEpochMilli()
        combine(
            goalRepository.getGoalsForMonthFlow(month.year, month.monthValue),
            typeRepository.getAllFlow(),
            entryRepository.getEntriesBetweenDatesFlow(yearStart, yearEnd)
        ) { goalEntities, typeEntities, viewedYearEntries ->
            val typeMap = typeEntities.associate { it.id to it.toDomain() }
            val nowMillis = LocalDate.now().toEpochMilli()

            goalEntities.map { goalEntity ->
                val goal = goalEntity.toDomain(typeMap[goalEntity.workoutTypeId])
                val (startMillis, endMillis) = goal.period.getDateRangeForMonth(month)

                val count = viewedYearEntries.count { entry ->
                    entry.date in startMillis..endMillis && when {
                        goal.workoutTypeId != null -> entry.workoutTypeId == goal.workoutTypeId
                        else -> typeMap[entry.workoutTypeId]?.isRestDay == false
                    }
                }

                GoalWithProgress(
                    goal = goal,
                    current = count,
                    isPast = endMillis < nowMillis
                )
            }.sortedBy { it.goal.period.ordinal }
        }
    }

    private val _baseUiState = combine(
        _currentMonth,
        _selectedFilters,
        typeRepository.getAllFlow(),
        _currentMonth.flatMapLatest { month ->
            val startDate = month.atDay(1).minusDays(6).toEpochMilli()
            val endDate = month.atEndOfMonth().plusDays(6).toEpochMilli()
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

        val monthOnlyEntries = allEntries.filter {
            it.date.year == month.year && it.date.monthValue == month.monthValue
        }
        val workoutCount = monthOnlyEntries.count { it.workoutType?.isRestDay != true }
        val totalEntries = monthOnlyEntries.size
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
    }

    val uiState: StateFlow<HomeUiState> = combine(
        _baseUiState,
        settingsDataStore.showGoalsOnDashboard
    ) { state, showGoals ->
        state.copy(showGoalsOnDashboard = showGoals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch {
            uiState.collect { state ->
                if (state.isLoading) return@collect
                state.goals.forEach { gwp ->
                    val prevCount = _previousGoalCounts[gwp.goal.id]
                    if (prevCount != null
                        && prevCount < gwp.goal.targetCount
                        && gwp.current >= gwp.goal.targetCount
                        && !gwp.isPast
                    ) {
                        _goalCompletedEvent.tryEmit(gwp.goal)
                    }
                    _previousGoalCounts[gwp.goal.id] = gwp.current
                }
            }
        }
    }

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
