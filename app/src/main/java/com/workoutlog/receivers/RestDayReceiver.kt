package com.workoutlog.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.workoutlog.data.local.entity.WorkoutEntryEntity
import com.workoutlog.data.repository.WorkoutEntryRepository
import com.workoutlog.data.repository.WorkoutTypeRepository
import com.workoutlog.domain.model.toEpochMilli
import com.workoutlog.notifications.ReminderNotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Handles the "Rest Day" action from the daily reminder notification.
 *
 * When triggered, it immediately inserts a rest-day entry for today without
 * requiring the user to open the app. If an entry already exists for today
 * (e.g. the user already logged a workout), it does nothing.
 */
@AndroidEntryPoint
class RestDayReceiver : BroadcastReceiver() {

    @Inject lateinit var entryRepository: WorkoutEntryRepository
    @Inject lateinit var typeRepository: WorkoutTypeRepository

    companion object {
        const val ACTION_LOG_REST_DAY = "com.workoutlog.action.LOG_REST_DAY"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_LOG_REST_DAY) return

        // goAsync() keeps the receiver alive while the coroutine runs
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todayMillis = LocalDate.now().toEpochMilli()

                // Guard: do nothing if an entry was already logged today
                if (entryRepository.getByDate(todayMillis) != null) return@launch

                // Use the type explicitly marked as rest day.
                // The notification action is only shown when this type exists,
                // so this lookup should always succeed when the receiver fires.
                val restDayType = typeRepository.getRestDayType()
                    ?: return@launch  // No rest-day type — nothing to insert

                entryRepository.insert(
                    WorkoutEntryEntity(
                        date = todayMillis,
                        workoutTypeId = restDayType.id
                    )
                )

                // Dismiss the notification now that the action is complete
                ReminderNotificationHelper.cancelNotification(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
