package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.TaskItem
import com.example.data.repository.TaskRepository
import com.example.util.AlarmScheduler
import com.example.util.NotificationHelper
import com.example.util.TaskDateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

enum class FilterTab(val label: String) {
    TODAY("Hari Ini"),
    ALL("Semua"),
    UPCOMING("Mendatang"),
    COMPLETED("Selesai")
}

enum class SortOption(val label: String) {
    DATE_ASC("Tanggal Terdekat"),
    PRIORITY_HIGH("Prioritas Tertinggi"),
    TITLE_ASC("Nama A-Z")
}

data class TaskUiState(
    val tasks: List<TaskItem> = emptyList(),
    val filteredTasks: List<TaskItem> = emptyList(),
    val selectedDateTasks: List<TaskItem> = emptyList(),
    val selectedDateEpoch: Long = TaskDateUtils.getTodayStartOfDay(),
    val calendarYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val calendarMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-11
    val activeFilterTab: FilterTab = FilterTab.TODAY,
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val sortOption: SortOption = SortOption.DATE_ASC,
    val dailyBriefingEnabled: Boolean = true,
    val dailyBriefingHour: Int = 8,
    val dailyBriefingMinute: Int = 0,
    val isTaskDialogOpen: Boolean = false,
    val editingTask: TaskItem? = null,
    val initialDateForDialog: Long = TaskDateUtils.getTodayStartOfDay()
) {
    val totalCount: Int get() = tasks.size
    val completedCount: Int get() = tasks.count { it.isCompleted }
    val todayTasks: List<TaskItem> get() = tasks.filter { TaskDateUtils.isToday(it.dueDateEpochMillis) }
    val todayCompletedCount: Int get() = todayTasks.count { it.isCompleted }
    val completionPercentage: Float
        get() = if (tasks.isEmpty()) 0f else (completedCount.toFloat() / tasks.size.toFloat())
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    private val _uiState = MutableStateFlow(TaskUiState())

    val uiState: StateFlow<TaskUiState>

    init {
        val db = AppDatabase.getInstance(application)
        repository = TaskRepository(db.taskDao(), application)

        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            // Set up daily briefing alarm by default
            AlarmScheduler.scheduleDailyBriefing(application, 8, 0)
        }

        uiState = combine(
            repository.allTasks,
            _uiState
        ) { allTasks, state ->
            val todayStart = TaskDateUtils.getTodayStartOfDay()

            // Filter by active tab
            val tabFiltered = when (state.activeFilterTab) {
                FilterTab.TODAY -> allTasks.filter {
                    TaskDateUtils.isToday(it.dueDateEpochMillis)
                }
                FilterTab.ALL -> allTasks
                FilterTab.UPCOMING -> allTasks.filter {
                    it.dueDateEpochMillis > todayStart && !it.isCompleted
                }
                FilterTab.COMPLETED -> allTasks.filter { it.isCompleted }
            }

            // Filter by category
            val categoryFiltered = if (state.selectedCategory != null) {
                tabFiltered.filter { it.category == state.selectedCategory }
            } else {
                tabFiltered
            }

            // Filter by search query
            val searchFiltered = if (state.searchQuery.isNotBlank()) {
                val query = state.searchQuery.trim().lowercase()
                categoryFiltered.filter {
                    it.title.lowercase().contains(query) ||
                    it.description.lowercase().contains(query) ||
                    it.category.lowercase().contains(query)
                }
            } else {
                categoryFiltered
            }

            // Sort
            val sortedList = when (state.sortOption) {
                SortOption.DATE_ASC -> searchFiltered.sortedWith(
                    compareBy<TaskItem> { it.isCompleted }
                        .thenBy { it.dueDateEpochMillis }
                        .thenBy { it.dueTimeString }
                )
                SortOption.PRIORITY_HIGH -> searchFiltered.sortedWith(
                    compareBy<TaskItem> { it.isCompleted }
                        .thenBy {
                            when (it.priority) {
                                TaskItem.PRIORITY_HIGH -> 0
                                TaskItem.PRIORITY_MEDIUM -> 1
                                else -> 2
                            }
                        }
                        .thenBy { it.dueDateEpochMillis }
                )
                SortOption.TITLE_ASC -> searchFiltered.sortedWith(
                    compareBy<TaskItem> { it.isCompleted }
                        .thenBy { it.title.lowercase() }
                )
            }

            // Tasks on the selected calendar date
            val selectedDateStart = TaskDateUtils.getStartOfDay(state.selectedDateEpoch)
            val tasksOnSelectedDate = allTasks.filter {
                TaskDateUtils.getStartOfDay(it.dueDateEpochMillis) == selectedDateStart
            }.sortedWith(
                compareBy<TaskItem> { it.isCompleted }
                    .thenBy { it.dueTimeString }
            )

            state.copy(
                tasks = allTasks,
                filteredTasks = sortedList,
                selectedDateTasks = tasksOnSelectedDate
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TaskUiState()
        )
    }

    fun selectDate(epochMillis: Long) {
        val startOfDay = TaskDateUtils.getStartOfDay(epochMillis)
        val cal = Calendar.getInstance().apply { timeInMillis = startOfDay }
        _uiState.update {
            it.copy(
                selectedDateEpoch = startOfDay,
                calendarYear = cal.get(Calendar.YEAR),
                calendarMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun nextMonth() {
        _uiState.update { current ->
            var year = current.calendarYear
            var month = current.calendarMonth + 1
            if (month > 11) {
                month = 0
                year += 1
            }
            current.copy(calendarYear = year, calendarMonth = month)
        }
    }

    fun previousMonth() {
        _uiState.update { current ->
            var year = current.calendarYear
            var month = current.calendarMonth - 1
            if (month < 0) {
                month = 11
                year -= 1
            }
            current.copy(calendarYear = year, calendarMonth = month)
        }
    }

    fun jumpToToday() {
        val today = TaskDateUtils.getTodayStartOfDay()
        val cal = Calendar.getInstance().apply { timeInMillis = today }
        _uiState.update {
            it.copy(
                selectedDateEpoch = today,
                calendarYear = cal.get(Calendar.YEAR),
                calendarMonth = cal.get(Calendar.MONTH)
            )
        }
    }

    fun setFilterTab(tab: FilterTab) {
        _uiState.update { it.copy(activeFilterTab = tab) }
    }

    fun setSelectedCategory(category: String?) {
        _uiState.update {
            it.copy(selectedCategory = if (it.selectedCategory == category) null else category)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortOption(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun openNewTaskDialog(initialDate: Long = TaskDateUtils.getTodayStartOfDay()) {
        _uiState.update {
            it.copy(
                isTaskDialogOpen = true,
                editingTask = null,
                initialDateForDialog = initialDate
            )
        }
    }

    fun openEditTaskDialog(task: TaskItem) {
        _uiState.update {
            it.copy(
                isTaskDialogOpen = true,
                editingTask = task,
                initialDateForDialog = task.dueDateEpochMillis
            )
        }
    }

    fun closeTaskDialog() {
        _uiState.update {
            it.copy(
                isTaskDialogOpen = false,
                editingTask = null
            )
        }
    }

    fun saveTask(task: TaskItem) {
        viewModelScope.launch {
            if (task.id == 0L) {
                repository.insertTask(task)
            } else {
                repository.updateTask(task)
            }
            closeTaskDialog()
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun toggleTaskComplete(task: TaskItem) {
        viewModelScope.launch {
            repository.setTaskCompleted(task, !task.isCompleted)
        }
    }

    fun toggleSubtask(task: TaskItem, subtaskId: String) {
        viewModelScope.launch {
            repository.toggleSubtask(task, subtaskId)
        }
    }

    fun updateDailyBriefing(enabled: Boolean, hour: Int, minute: Int) {
        _uiState.update {
            it.copy(
                dailyBriefingEnabled = enabled,
                dailyBriefingHour = hour,
                dailyBriefingMinute = minute
            )
        }
        val app = getApplication<Application>()
        if (enabled) {
            AlarmScheduler.scheduleDailyBriefing(app, hour, minute)
        } else {
            AlarmScheduler.cancelDailyBriefing(app)
        }
    }

    fun triggerTestNotification() {
        val app = getApplication<Application>()
        val todayCount = uiState.value.todayTasks.count { !it.isCompleted }
        NotificationHelper.showDailyBriefingNotification(
            context = app,
            title = "🔔 Uji Coba Pengingat TugasKu",
            message = "Notifikasi pengingat harian aktif! Ada $todayCount tugas tertunda hari ini.",
            notificationId = 12345
        )
    }
}
