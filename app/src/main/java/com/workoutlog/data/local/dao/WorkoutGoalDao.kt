package com.workoutlog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.workoutlog.data.local.entity.WorkoutGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutGoalDao {

    @Query("""
        SELECT * FROM workout_goals
        WHERE isActive = 1
          AND boundYear = :year
          AND (period = 'YEARLY' OR boundMonth = :month)
        ORDER BY createdAt ASC
    """)
    fun getGoalsForMonthFlow(year: Int, month: Int): Flow<List<WorkoutGoalEntity>>

    @Query("UPDATE workout_goals SET showOnDashboard = :show WHERE id = :id")
    suspend fun updateShowOnDashboard(id: Long, show: Boolean)

    @Query("SELECT * FROM workout_goals WHERE id = :id")
    suspend fun getById(id: Long): WorkoutGoalEntity?

    @Query("SELECT * FROM workout_goals ORDER BY createdAt ASC")
    suspend fun getAll(): List<WorkoutGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: WorkoutGoalEntity): Long

    @Update
    suspend fun update(goal: WorkoutGoalEntity)

    @Query("DELETE FROM workout_goals WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<WorkoutGoalEntity>)

    @Query("DELETE FROM workout_goals")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM workout_goals
        ORDER BY boundYear DESC, COALESCE(boundMonth, 13) DESC, createdAt DESC
    """)
    fun getAllGoalsFlow(): Flow<List<WorkoutGoalEntity>>
}
