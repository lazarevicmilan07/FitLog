package com.workoutlog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.workoutlog.data.local.entity.WorkoutTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutTypeDao {

    @Query("SELECT * FROM workout_types ORDER BY name ASC")
    fun getAllFlow(): Flow<List<WorkoutTypeEntity>>

    @Query("SELECT * FROM workout_types ORDER BY name ASC")
    suspend fun getAll(): List<WorkoutTypeEntity>

    @Query("SELECT * FROM workout_types WHERE id = :id")
    suspend fun getById(id: Long): WorkoutTypeEntity?

    @Query("SELECT * FROM workout_types WHERE id = :id")
    fun getByIdFlow(id: Long): Flow<WorkoutTypeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(workoutType: WorkoutTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(workoutTypes: List<WorkoutTypeEntity>)

    @Update
    suspend fun update(workoutType: WorkoutTypeEntity)

    @Delete
    suspend fun delete(workoutType: WorkoutTypeEntity)

    @Query("DELETE FROM workout_types WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM workout_types")
    suspend fun getCount(): Int

    @Query("DELETE FROM workout_types")
    suspend fun deleteAll()

    /** Returns the single workout type marked as rest day, or null if none exists. */
    @Query("SELECT * FROM workout_types WHERE isRestDay = 1 LIMIT 1")
    suspend fun getRestDayType(): WorkoutTypeEntity?

    /** Clears the rest day flag on every type except the one being saved. */
    @Query("UPDATE workout_types SET isRestDay = 0 WHERE id != :excludeId")
    suspend fun clearRestDayFlagExcept(excludeId: Long)
}
