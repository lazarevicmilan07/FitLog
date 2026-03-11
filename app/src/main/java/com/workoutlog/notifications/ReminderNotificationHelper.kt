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
import com.workoutlog.receivers.RestDayReceiver

object ReminderNotificationHelper {

    const val CHANNEL_ID = "daily_workout_reminder"
    const val NOTIFICATION_ID = 2001

    private val motivationalMessages = listOf(
        "Don't forget to log today's workout 💪",
        "Did you train today or is it a rest day?",
        "Log your workout to keep your streak alive!",
        "Stay consistent — log today's activity!",
        "Quick reminder to log your workout today 🏋️",
        "Every day counts. Did you work out today?",
        "Keep the momentum going — log today's session!"
    )

    /** Creates the notification channel (idempotent, safe to call multiple times). */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Workout Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to log your daily workout"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    /**
     * Shows the daily reminder notification if POST_NOTIFICATIONS permission is granted.
     * Silently does nothing if the permission is missing (Android 13+).
     *
     * @param showRestDayAction Whether to include the "Add Rest Day" action button.
     *   Only true when a workout type with [isRestDay = true] exists in the database.
     */
    fun showNotification(context: Context, showRestDayAction: Boolean) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        NotificationManagerCompat.from(context)
            .notify(NOTIFICATION_ID, buildNotification(context, showRestDayAction))
    }

    /** Cancels any currently displayed reminder notification. */
    fun cancelNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun buildNotification(context: Context, showRestDayAction: Boolean): Notification {
        val message = motivationalMessages.random()

        // Body tap / "Add Workout" action → open the app to the dashboard
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addWorkoutPendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_ADD_WORKOUT,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Rest Day" action → fires RestDayReceiver directly (no app open needed)
        val restDayIntent = Intent(context, RestDayReceiver::class.java).apply {
            action = RestDayReceiver.ACTION_LOG_REST_DAY
        }
        val restDayPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REST_DAY,
            restDayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Workout Log")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Add Workout", addWorkoutPendingIntent)
            .apply {
                // Only show the "Add Rest Day" action when a rest-day type actually exists
                if (showRestDayAction) {
                    addAction(0, "Add Rest Day", restDayPendingIntent)
                }
            }
            .build()
    }

    private const val REQUEST_CODE_OPEN_APP = 100
    private const val REQUEST_CODE_ADD_WORKOUT = 101
    private const val REQUEST_CODE_REST_DAY = 102
}
