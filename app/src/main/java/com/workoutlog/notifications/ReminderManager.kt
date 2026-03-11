package com.workoutlog.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val WORK_NAME = "daily_workout_reminder"
    }

    /**
     * Schedules (or replaces) a daily reminder at the given local time.
     *
     * The first execution is delayed until the next occurrence of [hour]:[minute]
     * in local time. Each subsequent execution is scheduled by the worker itself
     * after it completes, ensuring the reminder re-fires every ~24 hours at the
     * correct local time regardless of timezone or DST changes.
     */
    fun scheduleReminder(hour: Int, minute: Int) {
        val delay = calculateDelay(hour, minute)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    /** Cancels any pending reminder. */
    fun cancelReminder() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /**
     * Calculates the millisecond delay from now until the next occurrence of
     * [hour]:[minute] in the device's current local time.
     *
     * If the target time is already past for today, returns the delay until
     * the same time tomorrow. This correctly handles timezone and DST changes
     * because [LocalDateTime.now()] always reflects the current local time.
     */
    private fun calculateDelay(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        return ChronoUnit.MILLIS.between(now, target)
    }
}
