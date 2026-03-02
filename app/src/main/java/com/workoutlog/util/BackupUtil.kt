package com.workoutlog.util

import android.content.Context
import android.net.Uri
import com.workoutlog.data.local.entity.WorkoutEntryEntity
import com.workoutlog.data.local.entity.WorkoutTypeEntity
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.BackupData
import com.workoutlog.domain.model.BackupWorkoutEntry
import com.workoutlog.domain.model.BackupWorkoutType
import kotlinx.serialization.json.Json

object BackupUtil {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun createBackup(
        context: Context,
        uri: Uri,
        types: List<WorkoutTypeEntity>,
        entries: List<WorkoutEntryEntity>
    ) {
        val backupData = BackupData(
            workoutTypes = types.map { type ->
                BackupWorkoutType(
                    id = type.id,
                    name = type.name,
                    color = type.color,
                    icon = type.icon,
                    isDefault = type.isDefault,
                    isRestDay = type.isRestDay
                )
            },
            workoutEntries = entries.map { entry ->
                BackupWorkoutEntry(
                    id = entry.id,
                    date = entry.date,
                    workoutTypeId = entry.workoutTypeId,
                    note = entry.note,
                    durationMinutes = entry.durationMinutes,
                    caloriesBurned = entry.caloriesBurned
                )
            }
        )

        val jsonString = json.encodeToString(BackupData.serializer(), backupData)
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(jsonString.toByteArray())
        }
    }

    fun readBackup(context: Context, uri: Uri): BackupData? {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            } ?: return null

            val backupData = json.decodeFromString(BackupData.serializer(), jsonString)

            // Validation
            if (backupData.version < 1) return null
            if (backupData.workoutTypes.any { it.name.isBlank() }) return null

            backupData
        } catch (_: Exception) {
            null
        }
    }

    suspend fun restoreBackup(
        backupData: BackupData,
        typeRepository: WorkoutTypeRepository,
        entryRepository: WorkoutEntryRepository
    ) {
        // Clear existing data
        entryRepository.deleteAll()
        typeRepository.deleteAll()

        // Restore types
        val typeEntities = backupData.workoutTypes.map { bt ->
            WorkoutTypeEntity(
                id = bt.id,
                name = bt.name,
                color = bt.color,
                icon = bt.icon,
                isDefault = bt.isDefault,
                isRestDay = bt.isRestDay
            )
        }
        typeRepository.insertAll(typeEntities)

        // Restore entries
        val entryEntities = backupData.workoutEntries.map { be ->
            WorkoutEntryEntity(
                id = be.id,
                date = be.date,
                workoutTypeId = be.workoutTypeId,
                note = be.note,
                durationMinutes = be.durationMinutes,
                caloriesBurned = be.caloriesBurned
            )
        }
        entryRepository.insertAll(entryEntities)
    }
}
