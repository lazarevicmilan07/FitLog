package com.workoutlog.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutGoalRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.GoalPeriod
import com.workoutlog.domain.model.WorkoutGoal
import com.workoutlog.domain.model.WorkoutType
import com.workoutlog.ui.screens.home.GoalWithProgress
import com.workoutlog.domain.model.getDateRangeForMonth
import com.workoutlog.domain.model.toDomain
import com.workoutlog.domain.model.toEpochMilli
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class MonthSlot(
    val month: Int,
    val achieved: Int,
    val target: Int,
    val isHit: Boolean,
    val isActive: Boolean,
    val hasGoal: Boolean
)

data class MonthlyGoalTypeGroup(
    val workoutTypeId: Long?,
    val workoutTypeName: String,
    val year: Int,
    val months: List<MonthSlot>,
    val activeGoalId: Long?,
    val activeShowOnDashboard: Boolean
)

data class YearSlot(
    val year: Int,
    val achieved: Int,
    val target: Int,
    val isHit: Boolean,
    val isActive: Boolean,
    val hasGoal: Boolean
)

data class YearlyGoalTypeGroup(
    val workoutTypeId: Long?,
    val workoutTypeName: String,
    val periodStart: Int,
    val years: List<YearSlot>,
    val activeGoalId: Long?,
    val activeShowOnDashboard: Boolean
)

data class PastGoalPeriod(
    val goal: WorkoutGoal,
    val achieved: Int,
    val isHit: Boolean,
    val isInProgress: Boolean,
    val periodLabel: String,
    val year: Int,
    val month: Int?
)

data class HistoryMonthGroup(
    val month: Int?,
    val monthLabel: String,
    val items: List<PastGoalPeriod>
)

data class HistoryPeriodGroup(
    val period: GoalPeriod,
    val periodLabel: String,
    val monthGroups: List<HistoryMonthGroup>
)

data class HistoryYearGroup(
    val year: Int,
    val periodGroups: List<HistoryPeriodGroup>
)

data class GoalsUiState(
    val isLoading: Boolean = true,
    val selectedMonthlyYear: Int = YearMonth.now().year,
    val monthlyGoalGroups: List<MonthlyGoalTypeGroup> = emptyList(),
    val yearlyPeriodStart: Int = YearMonth.now().year - 5,
    val yearlyGoalGroups: List<YearlyGoalTypeGroup> = emptyList(),
    val historyYearGroups: List<HistoryYearGroup> = emptyList(),
    val currentPeriodGoals: List<GoalWithProgress> = emptyList(),
    val workoutTypes: List<WorkoutType> = emptyList()
)

private data class GoalComputed(
    val goal: WorkoutGoal,
    val boundYM: YearMonth,
    val achieved: Int,
    val isPast: Boolean,
    val isHit: Boolean
)

private data class AllGoalsData(
    val computed: List<GoalComputed>,
    val typeMap: Map<Long, WorkoutType>
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: WorkoutGoalRepository,
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository
) : ViewModel() {

    private val _selectedMonthlyYear = MutableStateFlow(YearMonth.now().year)
    private val _yearlyPeriodStart = MutableStateFlow(YearMonth.now().year - 5)

    private val _innerFlow = combine(
        goalRepository.getAllGoalsFlow(),
        entryRepository.getAllFlow(),
        typeRepository.getAllFlow()
    ) { goalEntities, entryEntities, typeEntities ->
        val typeMap = typeEntities.associate { it.id to it.toDomain() }
        val today = LocalDate.now()

        val computed = goalEntities.map { entity ->
            val goal = entity.toDomain(typeMap[entity.workoutTypeId])
            val boundYM = YearMonth.of(entity.boundYear, entity.boundMonth ?: 1)
            val (startMs, endMs) = goal.period.getDateRangeForMonth(boundYM)

            val count = entryEntities.count { entry ->
                entry.date in startMs..endMs && when {
                    goal.workoutTypeId != null -> entry.workoutTypeId == goal.workoutTypeId
                    else -> typeMap[entry.workoutTypeId]?.isRestDay == false
                }
            }

            GoalComputed(
                goal = goal,
                boundYM = boundYM,
                achieved = count,
                isPast = endMs < today.toEpochMilli(),
                isHit = count >= goal.targetCount
            )
        }

        AllGoalsData(computed = computed, typeMap = typeMap)
    }

    val uiState: StateFlow<GoalsUiState> = combine(
        _innerFlow,
        _selectedMonthlyYear,
        _yearlyPeriodStart
    ) { data, monthlyYear, periodStart ->
        val currentYM = YearMonth.now()
        val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        val allWorkoutsLabel = "All Workouts"

        // Monthly goal groups
        val monthlyComputed = data.computed.filter { it.goal.period == GoalPeriod.MONTHLY }
        val monthlyTypes = monthlyComputed
            .filter { it.boundYM.year == monthlyYear }
            .map { it.goal.workoutTypeId }
            .distinct()

        val monthlyGoalGroups = monthlyTypes.map { typeId ->
            val typeName = if (typeId == null) allWorkoutsLabel
                          else data.typeMap[typeId]?.name ?: allWorkoutsLabel
            val activeComputed = monthlyComputed.find {
                it.goal.workoutTypeId == typeId && it.boundYM == currentYM
            }
            val months = (1..12).map { m ->
                val slot = monthlyComputed.find {
                    it.goal.workoutTypeId == typeId &&
                    it.boundYM.year == monthlyYear &&
                    it.boundYM.monthValue == m
                }
                if (slot != null) {
                    MonthSlot(
                        month = m,
                        achieved = slot.achieved,
                        target = slot.goal.targetCount,
                        isHit = slot.isHit,
                        isActive = YearMonth.of(monthlyYear, m) == currentYM,
                        hasGoal = true
                    )
                } else {
                    MonthSlot(month = m, achieved = 0, target = 0, isHit = false,
                              isActive = false, hasGoal = false)
                }
            }
            MonthlyGoalTypeGroup(
                workoutTypeId = typeId,
                workoutTypeName = typeName,
                year = monthlyYear,
                months = months,
                activeGoalId = activeComputed?.goal?.id,
                activeShowOnDashboard = activeComputed?.goal?.showOnDashboard ?: true
            )
        }.sortedWith(compareBy({ it.workoutTypeId == null }, { it.workoutTypeName }))
            .sortedByDescending { it.workoutTypeId == null }

        // Yearly goal groups
        val yearlyComputed = data.computed.filter { it.goal.period == GoalPeriod.YEARLY }
        val periodEnd = periodStart + 5
        val yearlyTypes = yearlyComputed
            .filter { it.boundYM.year in periodStart..periodEnd }
            .map { it.goal.workoutTypeId }
            .distinct()

        val yearlyGoalGroups = yearlyTypes.map { typeId ->
            val typeName = if (typeId == null) allWorkoutsLabel
                          else data.typeMap[typeId]?.name ?: allWorkoutsLabel
            val activeComputed = yearlyComputed.find {
                it.goal.workoutTypeId == typeId && it.boundYM.year == currentYM.year
            }
            val years = (periodStart..periodEnd).map { y ->
                val slot = yearlyComputed.find {
                    it.goal.workoutTypeId == typeId && it.boundYM.year == y
                }
                if (slot != null) {
                    YearSlot(
                        year = y,
                        achieved = slot.achieved,
                        target = slot.goal.targetCount,
                        isHit = slot.isHit,
                        isActive = y == currentYM.year,
                        hasGoal = true
                    )
                } else {
                    YearSlot(year = y, achieved = 0, target = 0, isHit = false,
                             isActive = false, hasGoal = false)
                }
            }
            YearlyGoalTypeGroup(
                workoutTypeId = typeId,
                workoutTypeName = typeName,
                periodStart = periodStart,
                years = years,
                activeGoalId = activeComputed?.goal?.id,
                activeShowOnDashboard = activeComputed?.goal?.showOnDashboard ?: true
            )
        }.sortedByDescending { it.workoutTypeId == null }

        // History — includes current year's started periods (not future years)
        val historyComputed = data.computed.filter { g ->
            when (g.goal.period) {
                GoalPeriod.MONTHLY -> g.boundYM <= currentYM
                GoalPeriod.YEARLY  -> g.boundYM.year <= currentYM.year
            }
        }
        val pastGoals = historyComputed
            .sortedWith(
                compareByDescending<GoalComputed> { it.boundYM.year }
                    .thenByDescending { it.boundYM.monthValue }
            )
            .map { g ->
                val periodLabel = when (g.goal.period) {
                    GoalPeriod.MONTHLY -> g.boundYM.format(monthFormatter)
                    GoalPeriod.YEARLY  -> g.boundYM.year.toString()
                }
                PastGoalPeriod(
                    goal = g.goal,
                    achieved = g.achieved,
                    isHit = g.isHit,
                    isInProgress = !g.isPast,
                    periodLabel = periodLabel,
                    year = g.boundYM.year,
                    month = if (g.goal.period == GoalPeriod.MONTHLY) g.boundYM.monthValue else null
                )
            }

        val historyYearGroups = pastGoals
            .groupBy { it.year }
            .toSortedMap(reverseOrder())
            .map { (year, yearItems) ->
                val periodGroups = listOf(GoalPeriod.MONTHLY, GoalPeriod.YEARLY)
                    .mapNotNull { period ->
                        val periodItems = yearItems.filter { it.goal.period == period }
                        if (periodItems.isEmpty()) return@mapNotNull null
                        val periodLabel = if (period == GoalPeriod.MONTHLY) "Monthly" else "Yearly"
                        val monthGroups = periodItems
                            .groupBy { it.month }
                            .entries
                            .sortedWith(compareByDescending { it.key ?: -1 })
                            .map { (month, monthItems) ->
                                val label = if (month != null)
                                    LocalDate.of(year, month, 1).month
                                        .getDisplayName(TextStyle.FULL, Locale.getDefault())
                                else ""
                                HistoryMonthGroup(month = month, monthLabel = label, items = monthItems)
                            }
                        HistoryPeriodGroup(period = period, periodLabel = periodLabel, monthGroups = monthGroups)
                    }
                HistoryYearGroup(year = year, periodGroups = periodGroups)
            }

        val currentPeriodGoals = data.computed
            .filter { !it.isPast }
            .map { GoalWithProgress(goal = it.goal, current = it.achieved, isPast = false) }

        GoalsUiState(
            isLoading = false,
            selectedMonthlyYear = monthlyYear,
            monthlyGoalGroups = monthlyGoalGroups,
            yearlyPeriodStart = periodStart,
            yearlyGoalGroups = yearlyGoalGroups,
            historyYearGroups = historyYearGroups,
            currentPeriodGoals = currentPeriodGoals,
            workoutTypes = data.typeMap.values.toList()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    fun previousMonthlyYear()  { _selectedMonthlyYear.value-- }
    fun nextMonthlyYear()      { _selectedMonthlyYear.value++ }
    fun goToMonthlyYear(year: Int) { _selectedMonthlyYear.value = year }
    fun previousYearlyPeriod() { _yearlyPeriodStart.value -= 6 }
    fun nextYearlyPeriod()     { _yearlyPeriodStart.value += 6 }
    fun goToYearlyPeriod(year: Int) { _yearlyPeriodStart.value = year - 5 }

    fun setGoalShowOnDashboard(goalId: Long, show: Boolean) {
        viewModelScope.launch {
            goalRepository.updateShowOnDashboard(goalId, show)
        }
    }

    fun addGoal(period: GoalPeriod, targetCount: Int, workoutTypeId: Long?) {
        viewModelScope.launch {
            val now = YearMonth.now()
            goalRepository.insert(
                com.workoutlog.data.local.entity.WorkoutGoalEntity(
                    period = period.name,
                    targetCount = targetCount,
                    workoutTypeId = workoutTypeId,
                    boundYear = now.year,
                    boundMonth = if (period == GoalPeriod.YEARLY) null else now.monthValue
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
