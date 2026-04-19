package com.workoutlog.di

import android.content.Context
import androidx.room.Room
import com.workoutlog.data.local.WorkoutDatabase
import com.workoutlog.data.local.dao.WorkoutEntryDao
import com.workoutlog.data.local.dao.WorkoutGoalDao
import com.workoutlog.data.local.dao.WorkoutTypeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutDatabase {
        return Room.databaseBuilder(
            context,
            WorkoutDatabase::class.java,
            WorkoutDatabase.DATABASE_NAME
        )
            .addMigrations(
                WorkoutDatabase.MIGRATION_1_2,
                WorkoutDatabase.MIGRATION_2_3,
                WorkoutDatabase.MIGRATION_3_4,
                WorkoutDatabase.MIGRATION_4_5,
                WorkoutDatabase.MIGRATION_5_6,
                WorkoutDatabase.MIGRATION_6_7
            )
            .build()
    }

    @Provides
    fun provideWorkoutTypeDao(database: WorkoutDatabase): WorkoutTypeDao {
        return database.workoutTypeDao()
    }

    @Provides
    fun provideWorkoutEntryDao(database: WorkoutDatabase): WorkoutEntryDao {
        return database.workoutEntryDao()
    }

    @Provides
    fun provideWorkoutGoalDao(database: WorkoutDatabase): WorkoutGoalDao {
        return database.workoutGoalDao()
    }
}
