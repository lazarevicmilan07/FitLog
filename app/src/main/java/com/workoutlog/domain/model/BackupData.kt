package com.workoutlog.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val workoutTypes: List<BackupWorkoutType>,
    val workoutEntries: List<BackupWorkoutEntry>
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
