package com.workoutlog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.workoutlog.data.local.dao.WorkoutEntryDao
import com.workoutlog.data.local.dao.WorkoutGoalDao
import com.workoutlog.data.local.dao.WorkoutTypeDao
import com.workoutlog.data.local.entity.WorkoutEntryEntity
import com.workoutlog.data.local.entity.WorkoutGoalEntity
import com.workoutlog.data.local.entity.WorkoutTypeEntity

@Database(
    entities = [
        WorkoutTypeEntity::class,
        WorkoutEntryEntity::class,
        WorkoutGoalEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutTypeDao(): WorkoutTypeDao
    abstract fun workoutEntryDao(): WorkoutEntryDao
    abstract fun workoutGoalDao(): WorkoutGoalDao

    companion object {
        const val DATABASE_NAME = "workout_log.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_types ADD COLUMN isRestDay INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE workout_types SET isRestDay = 1 WHERE LOWER(name) = 'rest day'")
            }
        }

        // Adds unique constraint on date — recreates table, keeping latest entry per date
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE workout_entries_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        workoutTypeId INTEGER NOT NULL,
                        note TEXT,
                        durationMinutes INTEGER,
                        caloriesBurned INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (workoutTypeId) REFERENCES workout_types(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO workout_entries_new (id, date, workoutTypeId, note, durationMinutes, caloriesBurned, createdAt)
                    SELECT id, date, workoutTypeId, note, durationMinutes, caloriesBurned, createdAt
                    FROM workout_entries
                    WHERE id IN (SELECT MAX(id) FROM workout_entries GROUP BY date)
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX index_workout_entries_date ON workout_entries_new (date)")
                db.execSQL("CREATE INDEX index_workout_entries_workoutTypeId ON workout_entries_new (workoutTypeId)")
                db.execSQL("CREATE INDEX index_workout_entries_date_workoutTypeId ON workout_entries_new (date, workoutTypeId)")
                db.execSQL("DROP TABLE workout_entries")
                db.execSQL("ALTER TABLE workout_entries_new RENAME TO workout_entries")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE workout_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        period TEXT NOT NULL,
                        targetCount INTEGER NOT NULL,
                        workoutTypeId INTEGER,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_goals ADD COLUMN boundYear INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_goals ADD COLUMN boundMonth INTEGER")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM workout_goals WHERE period = 'WEEKLY'")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_goals ADD COLUMN showOnDashboard INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
