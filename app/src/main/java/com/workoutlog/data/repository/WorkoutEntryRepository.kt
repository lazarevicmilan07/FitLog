package com.workoutlog.data.repository

import com.workoutlog.data.local.dao.DailyCount
import com.workoutlog.data.local.dao.WorkoutEntryDao
import com.workoutlog.data.local.dao.WorkoutTypeCount
import com.workoutlog.data.local.entity.WorkoutEntryEntity
import com.workoutlog.domain.model.toEpochMilli
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutEntryRepository @Inject constructor(
    private val dao: WorkoutEntryDao
) {
    fun getAllFlow(): Flow<List<WorkoutEntryEntity>> = dao.getAllFlow()

    suspend fun getAll(): List<WorkoutEntryEntity> = dao.getAll()

    suspend fun getById(id: Long): WorkoutEntryEntity? = dao.getById(id)

    suspend fun getByDate(date: Long): WorkoutEntryEntity? = dao.getByDate(date)

    fun getByIdFlow(id: Long): Flow<WorkoutEntryEntity?> = dao.getByIdFlow(id)

    suspend fun insert(entry: WorkoutEntryEntity): Long = dao.insert(entry)

    suspend fun insertAll(entries: List<WorkoutEntryEntity>) = dao.insertAll(entries)

    suspend fun update(entry: WorkoutEntryEntity) = dao.update(entry)

    suspend fun delete(entry: WorkoutEntryEntity) = dao.delete(entry)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    fun getEntriesBetweenDatesFlow(startDate: Long, endDate: Long): Flow<List<WorkoutEntryEntity>> =
        dao.getEntriesBetweenDatesFlow(startDate, endDate)

    suspend fun getEntriesBetweenDates(startDate: Long, endDate: Long): List<WorkoutEntryEntity> =
        dao.getEntriesBetweenDates(startDate, endDate)

    suspend fun getWorkoutTypeCountsBetween(startDate: Long, endDate: Long): List<WorkoutTypeCount> =
        dao.getWorkoutTypeCountsBetween(startDate, endDate)

    suspend fun getDailyCountsBetween(startDate: Long, endDate: Long): List<DailyCount> =
        dao.getDailyCountsBetween(startDate, endDate)

    fun searchEntries(query: String): Flow<List<WorkoutEntryEntity>> = dao.searchEntries(query)

    suspend fun getCount(): Int = dao.getCount()

    suspend fun getCountBetween(startDate: Long, endDate: Long): Int =
        dao.getCountBetween(startDate, endDate)

    suspend fun deleteAll() = dao.deleteAll()

    /** Returns true if any entry (workout or rest day) exists for today's date. */
    suspend fun hasEntryForToday(): Boolean =
        dao.getByDate(LocalDate.now().toEpochMilli()) != null
}
