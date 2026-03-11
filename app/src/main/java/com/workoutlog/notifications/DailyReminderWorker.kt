package com.workoutlog.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.workoutlog.data.datastore.ReminderPreferences
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * WorkManager worker that fires once at the scheduled reminder time.
 *
 * On each execution it:
 *  1. Checks whether today already has a workout or rest day entry.
 *  2. If not, shows the reminder notification.
 *  3. Re-schedules itself for the next day using the current reminder time
 *     from preferences (so timezone / DST changes are picked up automatically).
 */
@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val entryRepository: WorkoutEntryRepository,
    private val typeRepository: WorkoutTypeRepository,
    private val reminderPreferences: ReminderPreferences,
    private val reminderManager: ReminderManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        // Always re-check the database before showing the notification so that
        // an entry logged after the worker was scheduled will suppress it.
        val hasEntry = entryRepository.hasEntryForToday()
        if (!hasEntry) {
            val hasRestDayType = typeRepository.getRestDayType() != null
            ReminderNotificationHelper.showNotification(appContext, hasRestDayType)
        }

        // Re-schedule for the next day using the current saved time.
        // Reading from preferences here (instead of WorkData) ensures that a
        // time change made by the user while the worker was waiting will be
        // picked up on the very next execution.
        val time = reminderPreferences.reminderTime.first()
        reminderManager.scheduleReminder(time.hour, time.minute)

        return Result.success()
    }
}
