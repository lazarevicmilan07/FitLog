package com.workoutlog.domain.model

import com.workoutlog.data.local.entity.WorkoutGoalEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class GoalPeriod { MONTHLY, YEARLY }

// Date range relative to the viewed month instead of always "now"
fun GoalPeriod.getDateRangeForMonth(viewedMonth: YearMonth): Pair<Long, Long> {
    val zoneId = ZoneId.systemDefault()
    val (start, end) = when (this) {
        GoalPeriod.MONTHLY -> viewedMonth.atDay(1) to viewedMonth.atEndOfMonth()
        GoalPeriod.YEARLY  -> LocalDate.of(viewedMonth.year, 1, 1) to
                              LocalDate.of(viewedMonth.year, 12, 31)
    }
    return start.atStartOfDay(zoneId).toInstant().toEpochMilli() to
           end.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

data class WorkoutGoal(
    val id: Long = 0,
    val period: GoalPeriod,
    val targetCount: Int,
    val workoutTypeId: Long?,
    val workoutType: WorkoutType? = null,
    val isActive: Boolean = true,
    val showOnDashboard: Boolean = true
)

fun WorkoutGoalEntity.toDomain(workoutType: WorkoutType? = null) = WorkoutGoal(
    id = id,
    period = GoalPeriod.valueOf(period),
    targetCount = targetCount,
    workoutTypeId = workoutTypeId,
    workoutType = workoutType,
    isActive = isActive,
    showOnDashboard = showOnDashboard
)

fun WorkoutGoal.toEntity() = WorkoutGoalEntity(
    id = id,
    period = period.name,
    targetCount = targetCount,
    workoutTypeId = workoutTypeId,
    isActive = isActive,
    showOnDashboard = showOnDashboard
)
