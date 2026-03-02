package com.workoutlog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_goals")
data class WorkoutGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val period: String,         // "WEEKLY" | "MONTHLY" | "YEARLY"
    val targetCount: Int,
    val workoutTypeId: Long?,   // null = all non-rest-day types
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val boundYear: Int = 0,     // year the goal belongs to
    val boundMonth: Int? = null // month for WEEKLY/MONTHLY goals; null for YEARLY
)
