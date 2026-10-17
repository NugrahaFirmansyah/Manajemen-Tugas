package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.receiver.ReminderReceiver

object NotificationHelper {

    const val CHANNEL_TASKS_ID = "channel_tasks_priority"
    const val CHANNEL_DAILY_ID = "channel_daily_briefing"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tasksChannel = NotificationChannel(
                CHANNEL_TASKS_ID,
                "Pengingat Tugas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat untuk tugas terjadwal"
                enableVibration(true)
                setShowBadge(true)
            }

            val dailyChannel = NotificationChannel(
                CHANNEL_DAILY_ID,
                "Pengingat Harian",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ringkasan agenda harian dan motivasi"
                enableVibration(true)
                setShowBadge(true)
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(tasksChannel)
            manager?.createNotificationChannel(dailyChannel)
        }
    }

    fun showTaskNotification(
        context: Context,
        taskId: Long,
        title: String,
        description: String,
        dueTime: String
    ) {
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EXTRA_TASK_ID", taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action to mark complete
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_MARK_COMPLETE
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + 100000).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val contentText = if (description.isNotBlank()) {
            if (dueTime.isNotBlank()) "Pukul $dueTime • $description" else description
        } else {
            if (dueTime.isNotBlank()) "Jatuh tempo pukul $dueTime" else "Waktunya menyelesaikan tugas ini"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_TASKS_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ Pengingat: $title")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "Tandai Selesai",
                completePendingIntent
            )

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(taskId.toInt(), builder.build())
        } catch (_: SecurityException) {
            // Handled if permission denied
        }
    }

    fun showDailyBriefingNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = 9999
    ) {
        createNotificationChannels(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_DAILY_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled if permission denied
        }
    }
}
