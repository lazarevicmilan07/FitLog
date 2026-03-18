package com.workoutlog.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.workoutlog.data.datastore.ReminderPreferences
import com.workoutlog.notifications.BackupReminderManager
import com.workoutlog.notifications.BackupReminderPreferences
import com.workoutlog.notifications.ReminderManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-schedules the daily reminder after the device reboots.
 *
 * WorkManager persists pending work across reboots for most cases, but
 * a OneTimeWorkRequest with a long delay can lose its schedule if the
 * device was powered off for an extended period. This receiver guarantees
 * correct rescheduling by recalculating the delay from the current local
 * time, which also handles timezone and DST changes that occurred while
 * the device was off.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderPreferences: ReminderPreferences
    @Inject lateinit var reminderManager: ReminderManager
    @Inject lateinit var backupReminderPreferences: BackupReminderPreferences
    @Inject lateinit var backupReminderManager: BackupReminderManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isEnabled = reminderPreferences.isEnabled.first()
                if (isEnabled) {
                    val time = reminderPreferences.reminderTime.first()
                    reminderManager.scheduleReminder(time.hour, time.minute)
                }

                val backupSettings = backupReminderPreferences.settings.first()
                if (backupSettings.enabled) {
                    backupReminderManager.scheduleReminder(backupSettings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
