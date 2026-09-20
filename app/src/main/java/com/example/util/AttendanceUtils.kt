package com.example.util

import com.example.data.model.AttendanceRecord
import java.text.SimpleDateFormat
import java.util.*

object AttendanceUtils {

    /**
     * Calculates consecutive daily attendance streak based on attendance records.
     * If user attended today, counts consecutive days ending today.
     * If user did not attend today yet but attended yesterday, counts consecutive days ending yesterday (streak is active!).
     * If attended neither today nor yesterday, streak is 0.
     */
    fun calculateStreak(records: List<AttendanceRecord>): Int {
        if (records.isEmpty()) return 0
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = istTz }

        val sdfDateStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }

        val attendedDates = records.mapNotNull { rec ->
            try {
                if (rec.entryTimestamp > 0) {
                    sdf.format(Date(rec.entryTimestamp))
                } else if (!rec.dateStr.isNullOrBlank()) {
                    val d = sdfDateStr.parse(rec.dateStr)
                    if (d != null) sdf.format(d) else null
                } else null
            } catch (_: Exception) {
                null
            }
        }.toSet()

        val cal = Calendar.getInstance(istTz)
        val todayStr = sdf.format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(cal.time)

        var streak = 0
        val checkCal = Calendar.getInstance(istTz)

        if (attendedDates.contains(todayStr)) {
            while (true) {
                val d = sdf.format(checkCal.time)
                if (attendedDates.contains(d)) {
                    streak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        } else if (attendedDates.contains(yesterdayStr)) {
            checkCal.time = cal.time // start from yesterday
            while (true) {
                val d = sdf.format(checkCal.time)
                if (attendedDates.contains(d)) {
                    streak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        } else {
            streak = 0
        }

        return streak
    }

    /**
     * Dynamic motivational messages based on current attendance streak.
     */
    fun getStreakMotivationMessage(streak: Int): String {
        return when {
            streak >= 10 -> "🔥 You're on fire! $streak days strong. Come tomorrow to continue your streak!"
            streak in 6..9 -> "Great consistency! Don't break your streak. Keep it up!"
            streak in 3..5 -> "2 more days to build an even stronger streak! Consistency is key."
            streak in 1..2 -> "🔥 Keep going! Your streak is getting stronger."
            else -> "Start fresh today! Visit the library and build your learning streak. 🔥"
        }
    }

    /**
     * Resolves which attendance record represents the current session to display on
     * the "Today's Attendance" card.
     *
     * Rules:
     * 1. If the latest record is ACTIVE (exit is null / isInside == true / status == ACTIVE):
     *    NEVER reset it because of midnight/date change. Keep showing that same record across midnight.
     * 2. If the latest record is COMPLETED:
     *    Check whether that completed record belongs to the current attendance day
     *    (i.e. exited today or entered today in IST).
     *    If not (e.g. from yesterday or earlier), return null so the card displays fresh.
     * 3. If no record exists for the current attendance day, return null (fresh state).
     */
    fun resolveTodaySession(records: List<AttendanceRecord>, now: Long = System.currentTimeMillis()): AttendanceRecord? {
        val latest = records.firstOrNull() ?: return null

        val isActive = latest.exitTime.isNullOrBlank() ||
                latest.exitTimestamp == null ||
                latest.isInside ||
                latest.status.equals("ACTIVE", ignoreCase = true)

        if (isActive) {
            return latest
        }

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = istTz }
        val todayStr = sdf.format(Date(now))

        val exitDateStr = latest.exitTimestamp?.let { sdf.format(Date(it)) } ?: ""
        val entryDateStr = if (latest.entryTimestamp > 0) sdf.format(Date(latest.entryTimestamp)) else ""

        // Check if the completed record finished today or started today
        val belongsToToday = (exitDateStr == todayStr) || (entryDateStr == todayStr)
        return if (belongsToToday) latest else null
    }

    /**
     * Formats duration into a clean string matching user specification:
     * - "6h" if minutes % 60 == 0
     * - "6h 15m" if both hours and minutes exist
     * - "45m" if hours == 0
     * - "0m" if 0
     */
    fun formatDuration(totalMinutes: Long): String {
        if (totalMinutes <= 0) return "0m"
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    /**
     * Computes duration between two timestamps in milliseconds supporting crossing midnight.
     */
    fun calculateDurationBetween(startMs: Long, endMs: Long): Pair<String, Long> {
        val diffMs = (endMs - startMs).coerceAtLeast(0)
        val minutes = (diffMs / (60 * 1000)).coerceAtLeast(0)
        return Pair(formatDuration(minutes), minutes)
    }

    /**
     * Formats entry timestamp to Day name (e.g. Tuesday).
     */
    fun getDayOfWeek(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.ENGLISH).apply {
            timeZone = TimeZone.getTimeZone("Asia/Kolkata")
        }
        return try {
            sdf.format(Date(timestamp))
        } catch (_: Exception) {
            "Day"
        }
    }

    /**
     * Returns 7 days of current week (Mon..Sun) with day letter, attended status, and whether it's today.
     */
    fun getCurrentWeekDays(records: List<AttendanceRecord>): List<WeekDayStatus> {
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).apply { timeZone = istTz }
        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
        val sdfDateStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }

        val attendedDates = records.mapNotNull { rec ->
            try {
                if (rec.entryTimestamp > 0) {
                    sdf.format(Date(rec.entryTimestamp))
                } else if (!rec.dateStr.isNullOrBlank()) {
                    val d = sdfDateStr.parse(rec.dateStr)
                    if (d != null) sdf.format(d) else null
                } else null
            } catch (_: Exception) {
                null
            }
        }.toSet()

        val cal = Calendar.getInstance(istTz)
        val todayStr = sdf.format(cal.time)

        // Find Monday of this week
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        val result = mutableListOf<WeekDayStatus>()
        for (i in 0 until 7) {
            val dateStr = sdf.format(cal.time)
            val isAttended = attendedDates.contains(dateStr)
            val isToday = (dateStr == todayStr)
            result.add(
                WeekDayStatus(
                    dayLetter = dayLetters[i],
                    dateNumber = cal.get(Calendar.DAY_OF_MONTH),
                    isAttended = isAttended,
                    isToday = isToday
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }
}

data class WeekDayStatus(
    val dayLetter: String,
    val dateNumber: Int,
    val isAttended: Boolean,
    val isToday: Boolean
)
