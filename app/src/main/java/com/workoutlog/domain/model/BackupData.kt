package com.workoutlog.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    val workoutTypes: List<BackupWorkoutType>,
    val workoutEntries: List<BackupWorkoutEntry>,
    val workoutGoals: List<BackupWorkoutGoal> = emptyList()
)

@Serializable
data class BackupWorkoutType(
    val id: Long,
    val name: String,
    val color: Long? = null,
    val icon: String? = null,
    val isDefault: Boolean = false,
    val isRestDay: Boolean = false
)

@Serializable
data class BackupWorkoutEntry(
    val id: Long,
    val date: Long,
    val workoutTypeId: Long,
    val note: String? = null,
    val durationMinutes: Int? = null,
    val caloriesBurned: Int? = null
)

@Serializable
data class BackupWorkoutGoal(
    val id: Long,
    val period: String,
    val targetCount: Int,
    val workoutTypeId: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long,
    val boundYear: Int = 0,
    val boundMonth: Int? = null,
    val showOnDashboard: Boolean = true
)
