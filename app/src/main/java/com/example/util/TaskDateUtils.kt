package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskDateUtils {

    private val indonesianLocale = Locale("id", "ID")

    fun getStartOfDay(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDay(epochMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = epochMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun getTodayStartOfDay(): Long {
        return getStartOfDay(System.currentTimeMillis())
    }

    fun isSameDay(time1: Long, time2: Long): Boolean {
        return getStartOfDay(time1) == getStartOfDay(time2)
    }

    fun isToday(epochMillis: Long): Boolean {
        return isSameDay(epochMillis, System.currentTimeMillis())
    }

    fun isTomorrow(epochMillis: Long): Boolean {
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return isSameDay(epochMillis, tomorrow.timeInMillis)
    }

    fun isYesterday(epochMillis: Long): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return isSameDay(epochMillis, yesterday.timeInMillis)
    }

    fun formatShortDate(epochMillis: Long): String {
        return when {
            isToday(epochMillis) -> "Hari Ini"
            isTomorrow(epochMillis) -> "Besok"
            isYesterday(epochMillis) -> "Kemarin"
            else -> {
                val sdf = SimpleDateFormat("d MMM", indonesianLocale)
                sdf.format(Date(epochMillis))
            }
        }
    }

    fun formatFullDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatMediumDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("d MMMM yyyy", indonesianLocale)
        return sdf.format(Date(epochMillis))
    }

    fun formatMonthYear(year: Int, monthIndex0: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex0)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
        return sdf.format(cal.time)
    }

    fun getDayOfWeekShortName(dayIndex0To6StartingSunday: Int): String {
        return when (dayIndex0To6StartingSunday) {
            0 -> "Min"
            1 -> "Sen"
            2 -> "Sel"
            3 -> "Rab"
            4 -> "Kam"
            5 -> "Jum"
            6 -> "Sab"
            else -> ""
        }
    }

    /**
     * Calculates days of month for a calendar grid.
     * Returns list of CalendarDay objects representing cells (including leading and trailing days).
     */
    fun getCalendarGridDays(year: Int, monthIndex0: Int): List<CalendarDay> {
        val days = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthIndex0)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ...
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month days to fill start of week
        val prevMonthDaysCount = firstDayOfWeek - 1
        if (prevMonthDaysCount > 0) {
            val prevCal = cal.clone() as Calendar
            prevCal.add(Calendar.MONTH, -1)
            val maxPrevDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            for (i in (maxPrevDays - prevMonthDaysCount + 1)..maxPrevDays) {
                prevCal.set(Calendar.DAY_OF_MONTH, i)
                days.add(
                    CalendarDay(
                        dayOfMonth = i,
                        epochMillis = prevCal.timeInMillis,
                        isCurrentMonth = false
                    )
                )
            }
        }

        // Current month days
        for (i in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, i)
            days.add(
                CalendarDay(
                    dayOfMonth = i,
                    epochMillis = cal.timeInMillis,
                    isCurrentMonth = true
                )
            )
        }

        // Next month trailing days to complete a 35 or 42 grid
        val totalCells = if (days.size <= 35) 35 else 42
        val nextMonthDaysCount = totalCells - days.size
        if (nextMonthDaysCount > 0) {
            val nextCal = cal.clone() as Calendar
            nextCal.add(Calendar.MONTH, 1)
            for (i in 1..nextMonthDaysCount) {
                nextCal.set(Calendar.DAY_OF_MONTH, i)
                days.add(
                    CalendarDay(
                        dayOfMonth = i,
                        epochMillis = nextCal.timeInMillis,
                        isCurrentMonth = false
                    )
                )
            }
        }

        return days
    }

    /**
     * Parse date and time into exact trigger timestamp
     */
    fun calculateAlarmTime(dueDateMillis: Long, timeString: String, minutesBefore: Int = 0): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dueDateMillis
        }
        if (timeString.isNotBlank() && timeString.contains(":")) {
            val parts = timeString.split(":")
            val hour = parts[0].toIntOrNull() ?: 9
            val minute = parts[1].toIntOrNull() ?: 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        } else {
            // Default 09:00 AM if no time specified
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        if (minutesBefore > 0) {
            cal.add(Calendar.MINUTE, -minutesBefore)
        }
        return cal.timeInMillis
    }
}

data class CalendarDay(
    val dayOfMonth: Int,
    val epochMillis: Long,
    val isCurrentMonth: Boolean
)
