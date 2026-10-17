package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val allTasks = db.taskDao().getAllTasks().first()
                    for (task in allTasks) {
                        if (!task.isCompleted && task.hasReminder) {
                            AlarmScheduler.scheduleTaskReminder(context, task)
                        }
                    }
                    // Schedule default daily briefing at 08:00 AM
                    AlarmScheduler.scheduleDailyBriefing(context, 8, 0)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
