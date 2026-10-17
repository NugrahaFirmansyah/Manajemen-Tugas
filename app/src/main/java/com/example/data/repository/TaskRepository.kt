package com.example.data.repository

import android.content.Context
import com.example.data.local.TaskDao
import com.example.data.model.Subtask
import com.example.data.model.TaskItem
import com.example.util.AlarmScheduler
import com.example.util.TaskDateUtils
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val context: Context
) {

    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()

    fun getTasksByDate(startOfDay: Long, endOfDay: Long): Flow<List<TaskItem>> =
        taskDao.getTasksByDate(startOfDay, endOfDay)

    suspend fun getTaskById(id: Long): TaskItem? = taskDao.getTaskById(id)

    suspend fun insertTask(task: TaskItem): Long {
        val id = taskDao.insertTask(task)
        val insertedTask = task.copy(id = id)
        if (insertedTask.hasReminder) {
            AlarmScheduler.scheduleTaskReminder(context, insertedTask)
        }
        return id
    }

    suspend fun updateTask(task: TaskItem) {
        taskDao.updateTask(task)
        if (task.hasReminder && !task.isCompleted) {
            AlarmScheduler.scheduleTaskReminder(context, task)
        } else {
            AlarmScheduler.cancelTaskReminder(context, task.id)
        }
    }

    suspend fun deleteTask(task: TaskItem) {
        taskDao.deleteTask(task)
        AlarmScheduler.cancelTaskReminder(context, task.id)
    }

    suspend fun setTaskCompleted(task: TaskItem, isCompleted: Boolean) {
        val updated = task.copy(
            isCompleted = isCompleted,
            completedAt = if (isCompleted) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
        if (isCompleted) {
            AlarmScheduler.cancelTaskReminder(context, task.id)
        } else if (task.hasReminder) {
            AlarmScheduler.scheduleTaskReminder(context, updated)
        }
    }

    suspend fun toggleSubtask(task: TaskItem, subtaskId: String) {
        val currentSubtasks = task.subtasks
        val updatedList = currentSubtasks.map {
            if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
        }
        val allDone = updatedList.isNotEmpty() && updatedList.all { it.isCompleted }
        val updatedTask = task.copy(
            subtasksJson = Subtask.listToJson(updatedList),
            isCompleted = if (allDone) true else task.isCompleted,
            completedAt = if (allDone) System.currentTimeMillis() else task.completedAt
        )
        taskDao.updateTask(updatedTask)
    }

    suspend fun prepopulateIfEmpty() {
        if (taskDao.getTaskCount() == 0) {
            val todayStart = TaskDateUtils.getTodayStartOfDay()
            val cal = Calendar.getInstance().apply { timeInMillis = todayStart }

            // Today + 0
            val subtasks1 = listOf(
                Subtask(title = "Siapkan slide presentasi", isCompleted = true),
                Subtask(title = "Kirim rekap ke tim", isCompleted = false)
            )
            insertTask(
                TaskItem(
                    title = "Review Laporan Proyek Akhir",
                    description = "Periksa feedback dari klien dan siapkan revisi final sprint",
                    dueDateEpochMillis = todayStart,
                    dueTimeString = "10:00",
                    priority = TaskItem.PRIORITY_HIGH,
                    category = TaskItem.CATEGORY_WORK,
                    hasReminder = true,
                    subtasksJson = Subtask.listToJson(subtasks1)
                )
            )

            insertTask(
                TaskItem(
                    title = "Olahraga Lari Pagi 30 Menit",
                    description = "Rute keliling taman komplek untuk menjaga kebugaran",
                    dueDateEpochMillis = todayStart,
                    dueTimeString = "07:00",
                    priority = TaskItem.PRIORITY_LOW,
                    category = TaskItem.CATEGORY_HEALTH,
                    isCompleted = true,
                    completedAt = System.currentTimeMillis() - 3600000L
                )
            )

            insertTask(
                TaskItem(
                    title = "Beli Kebutuhan Mingguan",
                    description = "Bahan sayur, buah segar, susu, dan detergen",
                    dueDateEpochMillis = todayStart,
                    dueTimeString = "16:30",
                    priority = TaskItem.PRIORITY_MEDIUM,
                    category = TaskItem.CATEGORY_PERSONAL,
                    hasReminder = true
                )
            )

            // Tomorrow
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowStart = cal.timeInMillis
            val subtasks2 = listOf(
                Subtask(title = "Baca modul Jetpack Compose Architecture", isCompleted = false),
                Subtask(title = "Latihan koding StateFlow & Coroutines", isCompleted = false)
            )
            insertTask(
                TaskItem(
                    title = "Belajar Modern Android Development",
                    description = "Eksplorasi Room Database, M3 components, dan UI testing",
                    dueDateEpochMillis = tomorrowStart,
                    dueTimeString = "14:00",
                    priority = TaskItem.PRIORITY_HIGH,
                    category = TaskItem.CATEGORY_STUDY,
                    hasReminder = true,
                    subtasksJson = Subtask.listToJson(subtasks2)
                )
            )

            // Day after tomorrow
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayAfterTomorrowStart = cal.timeInMillis
            insertTask(
                TaskItem(
                    title = "Bayar Tagihan Listrik & Internet",
                    description = "Transfer melalui mobile banking sebelum tanggal jatuh tempo",
                    dueDateEpochMillis = dayAfterTomorrowStart,
                    dueTimeString = "09:00",
                    priority = TaskItem.PRIORITY_MEDIUM,
                    category = TaskItem.CATEGORY_FINANCE,
                    hasReminder = true
                )
            )
        }
    }
}
