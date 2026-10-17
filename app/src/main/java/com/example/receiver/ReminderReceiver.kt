package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.AppDatabase
import com.example.util.AlarmScheduler
import com.example.util.NotificationHelper
import com.example.util.TaskDateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "com.example.tugasku.ACTION_TASK_REMINDER"
        const val ACTION_DAILY_BRIEFING = "com.example.tugasku.ACTION_DAILY_BRIEFING"
        const val ACTION_MARK_COMPLETE = "com.example.tugasku.ACTION_MARK_COMPLETE"

        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_TASK_DESC = "EXTRA_TASK_DESC"
        const val EXTRA_TASK_TIME = "EXTRA_TASK_TIME"
        const val EXTRA_BRIEFING_HOUR = "EXTRA_BRIEFING_HOUR"
        const val EXTRA_BRIEFING_MINUTE = "EXTRA_BRIEFING_MINUTE"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            ACTION_TASK_REMINDER -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Pengingat Tugas"
                val desc = intent.getStringExtra(EXTRA_TASK_DESC) ?: ""
                val time = intent.getStringExtra(EXTRA_TASK_TIME) ?: ""

                if (taskId != -1L) {
                    NotificationHelper.showTaskNotification(
                        context = context,
                        taskId = taskId,
                        title = title,
                        description = desc,
                        dueTime = time
                    )
                }
            }

            ACTION_DAILY_BRIEFING -> {
                val hour = intent.getIntExtra(EXTRA_BRIEFING_HOUR, 8)
                val minute = intent.getIntExtra(EXTRA_BRIEFING_MINUTE, 0)

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val todayStart = TaskDateUtils.getTodayStartOfDay()
                        val todayEnd = TaskDateUtils.getEndOfDay(todayStart)

                        val tasksToday = db.taskDao().getPendingTasksForDay(todayStart, todayEnd)
                        val count = tasksToday.size

                        val title = "🌅 Pengingat Harian TugasKu"
                        val message = if (count > 0) {
                            val firstFew = tasksToday.take(2).joinToString(", ") { it.title }
                            "Kamu punya $count tugas hari ini ($firstFew${if (count > 2) ", dll" else ""}). Yuk selesaikan!"
                        } else {
                            "Tidak ada tugas tertunda hari ini. Rencanakan harimu dengan produktif!"
                        }

                        NotificationHelper.showDailyBriefingNotification(
                            context = context,
                            title = title,
                            message = message
                        )

                        // Reschedule for tomorrow
                        AlarmScheduler.scheduleDailyBriefing(context, hour, minute)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_MARK_COMPLETE -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getInstance(context)
                            db.taskDao().updateCompleted(
                                id = taskId,
                                isCompleted = true,
                                completedAt = System.currentTimeMillis()
                            )
                            // Dismiss notification
                            NotificationManagerCompat.from(context).cancel(taskId.toInt())
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
