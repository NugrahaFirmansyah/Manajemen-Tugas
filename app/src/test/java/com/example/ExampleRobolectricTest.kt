package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.Subtask
import com.example.data.model.Task
import com.example.util.TaskDateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TugasKu", appName)
    }

    @Test
    fun `test Room database insert, retrieve, and update Task entity`() = runBlocking {
        val testDueDate = 1792240800000L
        val task = Task(
            title = "Test Room Database Task",
            description = "Detailed description of task",
            dueDateEpochMillis = testDueDate,
            dueTimeString = "14:00",
            isCompleted = false
        )

        val insertedId = db.taskDao().insertTask(task)
        assertTrue(insertedId > 0)

        val retrieved = db.taskDao().getTaskById(insertedId)
        assertNotNull(retrieved)
        assertEquals("Test Room Database Task", retrieved?.title)
        assertEquals("Detailed description of task", retrieved?.description)
        assertEquals(testDueDate, retrieved?.dueDateEpochMillis)
        assertEquals(testDueDate, retrieved?.dueDate)
        assertFalse(retrieved!!.isCompleted)

        // Update completion status
        db.taskDao().updateCompleted(insertedId, true, System.currentTimeMillis())
        val updated = db.taskDao().getTaskById(insertedId)
        assertTrue(updated!!.isCompleted)

        val allTasks = db.taskDao().getAllTasks().first()
        assertEquals(1, allTasks.size)
        assertEquals(insertedId, allTasks[0].id)
    }

    @Test
    fun `test subtask serialization and deserialization`() {
        val originalList = listOf(
            Subtask(id = "1", title = "Subtask A", isCompleted = false),
            Subtask(id = "2", title = "Subtask B", isCompleted = true)
        )
        val json = Subtask.listToJson(originalList)
        val restoredList = Subtask.jsonToList(json)

        assertEquals(2, restoredList.size)
        assertEquals("Subtask A", restoredList[0].title)
        assertFalse(restoredList[0].isCompleted)
        assertEquals("Subtask B", restoredList[1].title)
        assertTrue(restoredList[1].isCompleted)
    }

    @Test
    fun `test calendar grid generation`() {
        // October 2026
        val grid = TaskDateUtils.getCalendarGridDays(2026, 9)
        assertTrue(grid.isNotEmpty())
        assertTrue(grid.size == 35 || grid.size == 42)
    }
}
