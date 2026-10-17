package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskItem
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PriorityHigh
import com.example.util.CalendarDay
import com.example.util.TaskDateUtils

@Composable
fun CalendarView(
    year: Int,
    monthIndex0: Int,
    selectedDateEpoch: Long,
    allTasks: List<TaskItem>,
    onSelectDate: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthTitle = remember(year, monthIndex0) {
        TaskDateUtils.formatMonthYear(year, monthIndex0)
    }

    val gridDays = remember(year, monthIndex0) {
        TaskDateUtils.getCalendarGridDays(year, monthIndex0)
    }

    // Precompute task counts and high-priority indicators per day
    val taskMapByDay = remember(allTasks) {
        val map = mutableMapOf<Long, MutableList<TaskItem>>()
        allTasks.forEach { task ->
            val dayStart = TaskDateUtils.getStartOfDay(task.dueDateEpochMillis)
            val list = map.getOrPut(dayStart) { mutableListOf() }
            list.add(task)
        }
        map
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calendar_view_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Month Header with Prev/Next and Today
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(36.dp).testTag("cal_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Bulan Sebelumnya",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        onClick = onJumpToToday,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("cal_today_button"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hari Ini",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(36.dp).testTag("cal_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Bulan Berikutnya",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Day of Week Labels (Min, Sen, Sel, Rab, Kam, Jum, Sab)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (i in 0..6) {
                    val dayName = TaskDateUtils.getDayOfWeekShortName(i)
                    val isWeekend = (i == 0 || i == 6)
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isWeekend) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Days Grid (Rows of 7)
            val chunkedDays = gridDays.chunked(7)
            chunkedDays.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    week.forEach { day ->
                        val isSelected = TaskDateUtils.isSameDay(day.epochMillis, selectedDateEpoch)
                        val isToday = TaskDateUtils.isToday(day.epochMillis)
                        val dayTasks = taskMapByDay[TaskDateUtils.getStartOfDay(day.epochMillis)] ?: emptyList()
                        val hasPending = dayTasks.any { !it.isCompleted }
                        val hasHighPriority = dayTasks.any { it.priority == TaskItem.PRIORITY_HIGH && !it.isCompleted }
                        val allCompleted = dayTasks.isNotEmpty() && dayTasks.all { it.isCompleted }

                        CalendarDayCell(
                            day = day,
                            isSelected = isSelected,
                            isToday = isToday,
                            taskCount = dayTasks.size,
                            hasPending = hasPending,
                            hasHighPriority = hasHighPriority,
                            allCompleted = allCompleted,
                            onClick = { onSelectDate(day.epochMillis) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    isToday: Boolean,
    taskCount: Int,
    hasPending: Boolean,
    hasHighPriority: Boolean,
    allCompleted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .testTag("cal_day_${day.dayOfMonth}_current_${day.isCurrentMonth}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${day.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            // Dots for tasks
            if (taskCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimary
                        hasHighPriority -> PriorityHigh
                        allCompleted -> EmeraldSuccess
                        hasPending -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    val displayDots = minOf(taskCount, 3)
                    for (i in 0 until displayDots) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}
