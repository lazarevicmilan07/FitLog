package com.workoutlog.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.workoutlog.MainActivity
import com.workoutlog.R

object BackupReminderNotificationHelper {

    private const val CHANNEL_ID = "backup_reminder"
    const val NOTIFICATION_ID = 2002

    private val dailyMessages = listOf(
        "Don't forget to back up your workout data today 💾",
        "Keep your data safe — create a backup now",
        "Daily reminder: back up your Workout Log data",
        "Your workouts are valuable — back them up!",
        "A quick backup keeps your progress safe 🏋️"
    )

    private val weeklyMessages = listOf(
        "Time for your weekly data backup 💾",
        "Weekly reminder: back up your workout data",
        "Keep this week's progress safe — create a backup",
        "Don't lose your hard work — back up your data now"
    )

    private val monthlyMessages = listOf(
        "Monthly backup reminder — protect your workout history 💾",
        "Time to back up this month's workout data",
        "Monthly check: have you backed up your Workout Log?",
        "Keep your monthly progress safe with a backup"
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backup Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to back up your workout data"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, frequency: BackupReminderFrequency) {
        createNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, buildNotification(context, frequency))
    }

    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun buildNotification(context: Context, frequency: BackupReminderFrequency): Notification {
        val message = when (frequency) {
            BackupReminderFrequency.DAILY   -> dailyMessages.random()
            BackupReminderFrequency.WEEKLY  -> weeklyMessages.random()
            BackupReminderFrequency.MONTHLY -> monthlyMessages.random()
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_SETTINGS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, REQUEST_CODE_OPEN, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Workout Log")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Back Up Now", pendingIntent)
            .build()
    }

    private const val REQUEST_CODE_OPEN = 200
    const val EXTRA_OPEN_SETTINGS = "extra_open_settings"
}
