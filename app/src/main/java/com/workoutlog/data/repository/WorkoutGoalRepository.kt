package com.workoutlog.data.repository

import com.workoutlog.data.local.dao.WorkoutGoalDao
import com.workoutlog.data.local.entity.WorkoutGoalEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutGoalRepository @Inject constructor(
    private val dao: WorkoutGoalDao
) {
    fun getGoalsForMonthFlow(year: Int, month: Int): Flow<List<WorkoutGoalEntity>> =
        dao.getGoalsForMonthFlow(year, month)

    suspend fun getById(id: Long): WorkoutGoalEntity? = dao.getById(id)

    suspend fun getAll(): List<WorkoutGoalEntity> = dao.getAll()

    suspend fun insert(goal: WorkoutGoalEntity): Long = dao.insert(goal)

    suspend fun update(goal: WorkoutGoalEntity) = dao.update(goal)

    suspend fun deleteById(id: Long) = dao.deleteById(id)
}
