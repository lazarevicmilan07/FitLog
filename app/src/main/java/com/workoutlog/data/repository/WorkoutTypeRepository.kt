package com.workoutlog.data.repository

import com.workoutlog.data.local.dao.WorkoutTypeDao
import com.workoutlog.data.local.entity.WorkoutTypeEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutTypeRepository @Inject constructor(
    private val dao: WorkoutTypeDao
) {
    fun getAllFlow(): Flow<List<WorkoutTypeEntity>> = dao.getAllFlow()

    suspend fun getAll(): List<WorkoutTypeEntity> = dao.getAll()

    suspend fun getById(id: Long): WorkoutTypeEntity? = dao.getById(id)

    fun getByIdFlow(id: Long): Flow<WorkoutTypeEntity?> = dao.getByIdFlow(id)

    suspend fun insert(workoutType: WorkoutTypeEntity): Long = dao.insert(workoutType)

    suspend fun insertAll(workoutTypes: List<WorkoutTypeEntity>) = dao.insertAll(workoutTypes)

    suspend fun update(workoutType: WorkoutTypeEntity) = dao.update(workoutType)

    suspend fun delete(workoutType: WorkoutTypeEntity) = dao.delete(workoutType)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getCount(): Int = dao.getCount()

    suspend fun deleteAll() = dao.deleteAll()

    /** Returns the single workout type marked as rest day, or null if none exists. */
    suspend fun getRestDayType(): WorkoutTypeEntity? = dao.getRestDayType()

    /** Clears the rest day flag on every type except [excludeId]. */
    suspend fun clearRestDayFlagExcept(excludeId: Long) = dao.clearRestDayFlagExcept(excludeId)

    suspend fun insertDefaults() {
        if (dao.getCount() == 0) {
            val defaults = listOf(
                WorkoutTypeEntity(name = "Chest", color = 0xFFE53935, icon = "fitness_center", isDefault = true),
                WorkoutTypeEntity(name = "Back", color = 0xFF1E88E5, icon = "fitness_center", isDefault = true),
                WorkoutTypeEntity(name = "Legs", color = 0xFF43A047, icon = "directions_run", isDefault = true),
                WorkoutTypeEntity(name = "Shoulders", color = 0xFFFB8C00, icon = "fitness_center", isDefault = true),
                WorkoutTypeEntity(name = "Arms", color = 0xFF8E24AA, icon = "fitness_center", isDefault = true),
                WorkoutTypeEntity(name = "Cardio", color = 0xFFD81B60, icon = "directions_run", isDefault = true),
                WorkoutTypeEntity(name = "Rest Day", color = 0xFF757575, icon = "hotel", isDefault = true),
            )
            dao.insertAll(defaults)
        }
    }
}
