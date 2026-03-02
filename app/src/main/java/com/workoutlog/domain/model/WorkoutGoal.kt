package com.workoutlog.domain.model

import com.workoutlog.data.local.entity.WorkoutGoalEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class GoalPeriod { WEEKLY, MONTHLY, YEARLY }

fun GoalPeriod.getCurrentDateRange(): Pair<Long, Long> {
    val today = LocalDate.now()
    val zoneId = ZoneId.systemDefault()
    val (start, end) = when (this) {
        GoalPeriod.WEEKLY -> {
            val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val sunday = monday.plusDays(6)
            monday to sunday
        }
        GoalPeriod.MONTHLY -> {
            val first = today.withDayOfMonth(1)
            val last = today.with(TemporalAdjusters.lastDayOfMonth())
            first to last
        }
        GoalPeriod.YEARLY -> {
            val first = today.withDayOfYear(1)
            val last = today.with(TemporalAdjusters.lastDayOfYear())
            first to last
        }
    }
    return start.atStartOfDay(zoneId).toInstant().toEpochMilli() to
            end.atStartOfDay(zoneId).toInstant().toEpochMilli()
}

fun GoalPeriod.label(): String = when (this) {
    GoalPeriod.WEEKLY -> "This Week"
    GoalPeriod.MONTHLY -> "This Month"
    GoalPeriod.YEARLY -> "This Year"
}

data class WorkoutGoal(
    val id: Long = 0,
    val period: GoalPeriod,
    val targetCount: Int,
    val workoutTypeId: Long?,
    val workoutType: WorkoutType? = null,
    val isActive: Boolean = true
)

fun WorkoutGoalEntity.toDomain(workoutType: WorkoutType? = null) = WorkoutGoal(
    id = id,
    period = GoalPeriod.valueOf(period),
    targetCount = targetCount,
    workoutTypeId = workoutTypeId,
    workoutType = workoutType,
    isActive = isActive
)

fun WorkoutGoal.toEntity() = WorkoutGoalEntity(
    id = id,
    period = period.name,
    targetCount = targetCount,
    workoutTypeId = workoutTypeId,
    isActive = isActive
)
