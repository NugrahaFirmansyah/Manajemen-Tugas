package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Task with title, description, due date, and completion status.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDateEpochMillis: Long, // Due date in epoch milliseconds
    val dueTimeString: String = "", // e.g. "09:00"
    val priority: String = PRIORITY_MEDIUM, // HIGH, MEDIUM, LOW
    val category: String = CATEGORY_WORK, // Pekerjaan, Pribadi, Belajar, Kesehatan, Keuangan, Lainnya
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val hasReminder: Boolean = false,
    val reminderMinutesBefore: Int = 0,
    val subtasksJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
) {
    // Convenient alias for dueDate
    val dueDate: Long
        get() = dueDateEpochMillis

    val subtasks: List<Subtask>
        get() = Subtask.jsonToList(subtasksJson)

    companion object {
        const val PRIORITY_HIGH = "HIGH"
        const val PRIORITY_MEDIUM = "MEDIUM"
        const val PRIORITY_LOW = "LOW"

        const val CATEGORY_WORK = "Pekerjaan"
        const val CATEGORY_PERSONAL = "Pribadi"
        const val CATEGORY_STUDY = "Belajar"
        const val CATEGORY_HEALTH = "Kesehatan"
        const val CATEGORY_FINANCE = "Keuangan"
        const val CATEGORY_OTHER = "Lainnya"

        val ALL_CATEGORIES = listOf(
            CATEGORY_WORK,
            CATEGORY_PERSONAL,
            CATEGORY_STUDY,
            CATEGORY_HEALTH,
            CATEGORY_FINANCE,
            CATEGORY_OTHER
        )

        val ALL_PRIORITIES = listOf(
            PRIORITY_HIGH,
            PRIORITY_MEDIUM,
            PRIORITY_LOW
        )
    }
}
