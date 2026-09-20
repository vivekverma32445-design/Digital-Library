package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.AttendanceRecord
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentVerificationRequest
import com.example.ui.MainViewModel
import com.example.ui.components.PasswordStrengthIndicator
import com.example.ui.theme.*
import com.example.util.AttendanceUtils
import com.example.util.PasswordSecurity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class AttendanceDayRecord(
    val dayNumber: Int,
    val dateStr: String,
    val dayOfWeek: String,
    val isPresent: Boolean,
    val isInside: Boolean = false,
    val entryTime: String = "--:--",
    val exitTime: String = "--:--",
    val duration: String = "0h 0m",
    val shiftTitle: String = "Shift",
    val seatNumber: String = "--"
)

@Composable
fun AttendanceScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val studentAttendance by viewModel.studentAttendance.collectAsState()
    val activeMembership by viewModel.activeMembership.collectAsState()
    val todayRecord by viewModel.todayAttendance.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val streakMsg by viewModel.streakMotivationMessage.collectAsState()

    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDayRecord by remember { mutableStateOf<Triple<Int, String, AttendanceRecord?>?>(null) }

    val istTz = remember { TimeZone.getTimeZone("Asia/Kolkata") }
    val userStartDateMillis = remember(activeMembership, studentAttendance) {
        var minTime: Long? = null
        if (activeMembership != null) {
            try {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }
                val parsed = sdf.parse(activeMembership!!.startDate)
                if (parsed != null) minTime = parsed.time
            } catch (_: Exception) {}
        }
        studentAttendance.lastOrNull()?.let { earliestRec ->
            if (earliestRec.entryTimestamp > 0) {
                val t = earliestRec.entryTimestamp
                if (minTime == null || t < minTime) minTime = t
            }
        }
        minTime
    }
    val displayedCal = remember(monthOffset) {
        Calendar.getInstance(istTz).apply {
            add(Calendar.MONTH, monthOffset)
        }
    }
    val todayCal = remember { Calendar.getInstance(istTz) }
    val isCurrentMonth = remember(monthOffset) {
        displayedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
        displayedCal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH)
    }
    val currentTodayDay = if (isCurrentMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1

    val monthHeaderStr = remember(displayedCal) {
        SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }.format(displayedCal.time)
    }
    val monthShortStr = remember(displayedCal) {
        SimpleDateFormat("MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }.format(displayedCal.time)
    }

    val maxDaysInMonth = remember(displayedCal) {
        displayedCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // Map day numbers (1..maxDays) to AttendanceRecord for the displayed month
    val monthAttendanceMap = remember(studentAttendance, monthOffset) {
        val map = mutableMapOf<Int, AttendanceRecord>()
        studentAttendance.forEach { record ->
            // record.dateStr is e.g. "12 Sep 2026" or "dd MMM yyyy"
            val parts = record.dateStr.trim().split(" ")
            if (parts.size >= 3) {
                val dayNum = parts[0].toIntOrNull()
                val monStr = "${parts[1]} ${parts[2]}"
                if (dayNum != null && monStr.equals(monthShortStr, ignoreCase = true)) {
                    map[dayNum] = record
                }
            }
        }
        map
    }

    val daysAttendedCount = monthAttendanceMap.size
    val monthlyProgressPercent = if (maxDaysInMonth > 0) ((daysAttendedCount * 100) / maxDaysInMonth) else 0

    val weekStatus = remember(studentAttendance) {
        AttendanceUtils.getCurrentWeekDays(studentAttendance)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Month Selector Header with Previous / Next Month Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthOffset-- }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }
                Text(
                    text = monthHeaderStr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { monthOffset++ }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
            if (monthOffset != 0) {
                TextButton(onClick = { monthOffset = 0 }) {
                    Text("Today", fontSize = 12.sp, color = PrimaryGreen)
                }
            } else {
                Spacer(Modifier.width(48.dp))
            }
        }

        Spacer(Modifier.height(14.dp))

        // Monthly Progress & Comparison Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$monthHeaderStr Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$daysAttendedCount / $maxDaysInMonth Days Attended",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (monthlyProgressPercent >= 60) PrimaryGreen.copy(alpha = 0.15f) else Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = "$monthlyProgressPercent% Attendance",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (monthlyProgressPercent >= 60) PrimaryGreen else Color(0xFFE65100),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { if (maxDaysInMonth > 0) (daysAttendedCount.toFloat() / maxDaysInMonth).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PrimaryGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (monthlyProgressPercent >= 75)
                        "🌟 Excellent consistency! You're performing in the top library study tier this month."
                    else if (monthlyProgressPercent >= 40)
                        "📈 Good progress! Regular library attendance helps you maintain focus."
                    else
                        "💡 Attend daily to build a regular study routine and increase your month progress.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Prominent Motivational Streak Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (currentStreak > 0) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (currentStreak > 0) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (currentStreak > 0) Color(0xFFFFE082) else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (currentStreak > 0) "🔥" else "⭐",
                                fontSize = 26.sp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "$currentStreak Day${if (currentStreak == 1) "" else "s"} Streak",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (currentStreak > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentStreak > 0) "Daily Attendance Consistency" else "Start your learning streak today",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentStreak > 0) Color(0xFFE65100) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (currentStreak > 0) "ACTIVE" else "INACTIVE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentStreak > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Motivational Message Callout
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentStreak > 0) Color(0xFFFFF3E0) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = streakMsg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFBF360C)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Weekly consistency progress dots (Mon - Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    weekStatus.forEach { day ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = day.dayLetter,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (day.isToday) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            day.isAttended -> PrimaryGreen
                                            day.isToday -> Color(0xFFFFCC80)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .border(
                                        width = if (day.isToday) 2.dp else 0.dp,
                                        color = if (day.isToday) Color(0xFFE65100) else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day.isAttended) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Attended",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${day.dateNumber}",
                                        fontSize = 10.sp,
                                        color = if (day.isToday) Color(0xFFBF360C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Dynamic Attendance Metrics (Clean zero state for new users)
        val totalDaysPresent = studentAttendance.size
        val totalStudyMinutes = remember(studentAttendance) {
            studentAttendance.sumOf { rec ->
                val duration = rec.duration
                val hMatch = Regex("(\\d+)h").find(duration)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val mMatch = Regex("(\\d+)m").find(duration)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                hMatch * 60 + mMatch
            }
        }
        val totalStudyTimeStr = if (totalStudyMinutes > 0) {
            "${totalStudyMinutes / 60}h ${totalStudyMinutes % 60}m"
        } else {
            "0h 0m"
        }

        // 3 Key Attendance Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AttendanceStatBox("Present", "$totalDaysPresent Days", Modifier.weight(1f))
            AttendanceStatBox("Total Study Time", totalStudyTimeStr, Modifier.weight(1.3f))
            AttendanceStatBox("Current Streak", "$currentStreak Days", Modifier.weight(1.1f))
        }

        Spacer(Modifier.height(18.dp))

        // Monthly Attendance Calendar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Day of week labels (M T W T F S S)
                val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    daysOfWeek.forEach { day ->
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(32.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Calendar Days Grid with dynamic present days from DB for the displayed month
                val startDayOfWeek = remember(displayedCal) {
                    val c = Calendar.getInstance(istTz).apply {
                        timeInMillis = displayedCal.timeInMillis
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    // Shift Sunday=1, Monday=2 ... to Monday=0, Sunday=6
                    (c.get(Calendar.DAY_OF_WEEK) + 5) % 7
                }
                val totalWeeks = remember(startDayOfWeek, maxDaysInMonth) {
                    ((startDayOfWeek + maxDaysInMonth) + 6) / 7
                }

                var dayCounter = 1

                for (week in 0 until totalWeeks) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = week * 7 + col
                            if (cellIndex >= startDayOfWeek && dayCounter <= maxDaysInMonth) {
                                val currentDay = dayCounter
                                val attendedRecord = monthAttendanceMap[currentDay]
                                val isPresent = attendedRecord != null
                                val isToday = isCurrentMonth && (currentDay == currentTodayDay)
                                val isSelected = selectedDayRecord?.first == currentDay

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(34.dp)
                                        .clickable {
                                            selectedDayRecord = Triple(currentDay, "$currentDay $monthShortStr", attendedRecord)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isPresent -> PrimaryGreen
                                                    isSelected -> PrimaryGreen.copy(alpha = 0.2f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = when {
                                                    isSelected -> 1.5.dp
                                                    isToday && !isPresent -> 1.dp
                                                    else -> 0.dp
                                                },
                                                color = when {
                                                    isSelected -> PrimaryGreen
                                                    isToday && !isPresent -> PrimaryGreen.copy(alpha = 0.5f)
                                                    else -> Color.Transparent
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$currentDay",
                                            fontSize = 11.sp,
                                            fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isPresent -> Color.White
                                                isToday -> PrimaryGreen
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                            }
                                        )
                                    }
                                    if (isPresent) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryGreen)
                                        )
                                    } else {
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                                dayCounter++
                            } else {
                                Spacer(Modifier.width(34.dp))
                            }
                        }
                    }
                }

                // If user taps on a date in calendar, show info card
                selectedDayRecord?.let { (dayNum, dateLabel, rec) ->
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (rec != null) Color(0xFFE8F8EE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (rec != null) PrimaryGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = dateLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (rec != null) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (rec != null) {
                                        "Entry: ${rec.entryTime} • Exit: ${rec.exitTime ?: "Inside"} (${rec.duration})"
                                    } else {
                                        "No attendance marked on this date"
                                    },
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { selectedDayRecord = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Today's Attendance Card
        val isInsideNow = todayRecord != null && (todayRecord!!.isInside || todayRecord!!.exitTime == null)
        val isCompletedToday = todayRecord != null && !isInsideNow && (todayRecord!!.status.equals("COMPLETED", ignoreCase = true) || todayRecord!!.exitTime != null)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = if (isInsideNow) androidx.compose.foundation.BorderStroke(1.2.dp, PrimaryGreen.copy(alpha = 0.5f)) else null
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Attendance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isInsideNow) Color(0xFFE8F8EE) else if (isCompletedToday) Color(0xFFEDE7F6) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isInsideNow) "🟢 INSIDE NOW" else if (isCompletedToday) "COMPLETED" else "NOT CHECKED IN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isInsideNow) PrimaryGreen else if (isCompletedToday) Color(0xFF512DA8) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AttendanceMetricItem(label = "Entry", value = if (todayRecord != null) todayRecord!!.entryTime else "--")
                    AttendanceMetricItem(label = "Exit", value = if (isInsideNow) "--" else if (isCompletedToday) (todayRecord?.exitTime ?: "--") else "--")
                    AttendanceMetricItem(label = "Duration", value = if (isInsideNow) "Running" else if (isCompletedToday) (todayRecord?.duration ?: "0m") else "0m")
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Day-by-Day Attendance and Absence Records
        var attendanceFilter by remember { mutableStateOf("ALL") } // ALL, PRESENT, ABSENT

        // Generate combined records for all dates for displayed month
        val combinedDayRecords = remember(studentAttendance, monthOffset, displayedCal, isCurrentMonth, currentTodayDay, maxDaysInMonth, userStartDateMillis) {
            val list = mutableListOf<com.example.ui.screens.AttendanceDayRecord>()
            val maxDayToDisplay = if (isCurrentMonth) currentTodayDay.coerceIn(1, maxDaysInMonth) else if (monthOffset <= 0) maxDaysInMonth else 0
            val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale.ENGLISH).apply { timeZone = istTz }
            val sdfDateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }

            val minDayToDisplay = if (userStartDateMillis != null) {
                val startCal = Calendar.getInstance(istTz).apply { timeInMillis = userStartDateMillis }
                val startYear = startCal.get(Calendar.YEAR)
                val startMonth = startCal.get(Calendar.MONTH)
                val dispYear = displayedCal.get(Calendar.YEAR)
                val dispMonth = displayedCal.get(Calendar.MONTH)
                when {
                    dispYear < startYear || (dispYear == startYear && dispMonth < startMonth) -> maxDaysInMonth + 1 // Prior month: no absent days
                    dispYear == startYear && dispMonth == startMonth -> startCal.get(Calendar.DAY_OF_MONTH) // Enrollment month: from start day onwards
                    else -> 1
                }
            } else {
                if (studentAttendance.isEmpty()) maxDaysInMonth + 1 else 1
            }

            for (d in maxDayToDisplay downTo minDayToDisplay) {
                val rec = monthAttendanceMap[d]
                val dayCal = Calendar.getInstance(istTz).apply {
                    timeInMillis = displayedCal.timeInMillis
                    set(Calendar.DAY_OF_MONTH, d)
                }
                val dayOfWeekStr = sdfDayOfWeek.format(dayCal.time)
                val dateFormatted = sdfDateFormatted.format(dayCal.time)

                if (rec != null) {
                    list.add(
                        com.example.ui.screens.AttendanceDayRecord(
                            dayNumber = d,
                            dateStr = rec.dateStr.ifBlank { dateFormatted },
                            dayOfWeek = dayOfWeekStr,
                            isPresent = true,
                            isInside = rec.isInside,
                            entryTime = rec.entryTime,
                            exitTime = rec.exitTime ?: if (rec.isInside) "Inside Now" else "--:--",
                            duration = if (rec.isInside) "Studying..." else rec.duration,
                            shiftTitle = rec.shiftTitle,
                            seatNumber = rec.seatNumber
                        )
                    )
                } else {
                    list.add(
                        com.example.ui.screens.AttendanceDayRecord(
                            dayNumber = d,
                            dateStr = dateFormatted,
                            dayOfWeek = dayOfWeekStr,
                            isPresent = false,
                            isInside = false,
                            entryTime = "--:--",
                            exitTime = "--:--",
                            duration = "0h 0m",
                            shiftTitle = "General",
                            seatNumber = "--"
                        )
                    )
                }
            }
            list
        }

        val presentRecordsCount = combinedDayRecords.count { it.isPresent }
        val absentRecordsCount = combinedDayRecords.count { !it.isPresent }

        val displayedRecords = remember(combinedDayRecords, attendanceFilter) {
            when (attendanceFilter) {
                "PRESENT" -> combinedDayRecords.filter { it.isPresent }
                "ABSENT" -> combinedDayRecords.filter { !it.isPresent }
                else -> combinedDayRecords
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Attendance Records",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = attendanceFilter == "ALL",
                    onClick = { attendanceFilter = "ALL" },
                    label = { Text("All (${combinedDayRecords.size})", fontSize = 10.5.sp) }
                )
                FilterChip(
                    selected = attendanceFilter == "PRESENT",
                    onClick = { attendanceFilter = "PRESENT" },
                    label = { Text("Present ($presentRecordsCount)", fontSize = 10.5.sp) }
                )
                FilterChip(
                    selected = attendanceFilter == "ABSENT",
                    onClick = { attendanceFilter = "ABSENT" },
                    label = { Text("Absent ($absentRecordsCount)", fontSize = 10.5.sp) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (displayedRecords.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No records in this filter.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                displayedRecords.forEach { rec ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (rec.isPresent) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = if (!rec.isPresent) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null,
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    rec.isInside -> Color(0xFFE8F8EE)
                                                    rec.isPresent -> Color(0xFFE8F5E9)
                                                    else -> Color(0xFFFFEBEE)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                rec.isInside -> Icons.Default.DirectionsRun
                                                rec.isPresent -> Icons.Default.CheckCircle
                                                else -> Icons.Default.Close
                                            },
                                            contentDescription = null,
                                            tint = if (rec.isPresent) PrimaryGreen else Color(0xFFC62828),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${rec.dateStr} (${rec.dayOfWeek})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (rec.isPresent) "${rec.shiftTitle} • Seat ${rec.seatNumber}" else "No library attendance recorded",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        rec.isInside -> Color(0xFFE8F8EE)
                                        rec.isPresent -> Color(0xFFE8F5E9)
                                        else -> Color(0xFFFFEBEE)
                                    }
                                ) {
                                    Text(
                                        text = when {
                                            rec.isInside -> "INSIDE NOW"
                                            rec.isPresent -> "PRESENT"
                                            else -> "ABSENT"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rec.isPresent) PrimaryGreen else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Entry Time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = rec.entryTime,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rec.isPresent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Exit Time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = rec.exitTime,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rec.isInside) PrimaryGreen else if (rec.isPresent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Duration", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = rec.duration,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rec.isPresent) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttendanceMetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

@Composable
private fun AttendanceStatBox(label: String, value: String, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
fun PaymentHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val payments by viewModel.studentPayments.collectAsState()
    val verificationRequests by viewModel.studentPaymentVerificationRequests.collectAsState()
    var selectedTab by remember { mutableIntStateOf(if (verificationRequests.any { it.status.equals("PENDING", ignoreCase = true) }) 0 else 0) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }

    val pendingCount = remember(verificationRequests) {
        verificationRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    }

    if (previewImageUri != null) {
        Dialog(onDismissRequest = { previewImageUri = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payment Proof",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { previewImageUri = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = previewImageUri,
                        contentDescription = "Payment Proof",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Payments & Verifications",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryGreen
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UPI Requests", fontWeight = FontWeight.Bold)
                        if (pendingCount > 0) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE65100),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = pendingCount.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Receipts", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Verification Requests Tab
            if (verificationRequests.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No UPI verification requests submitted",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(verificationRequests) { req ->
                        val isPending = req.status.equals("PENDING", ignoreCase = true)
                        val isConfirmed = req.status.equals("CONFIRMED", ignoreCase = true)
                        val isDeclined = req.status.equals("DECLINED", ignoreCase = true)

                        val borderColor = when {
                            isConfirmed -> PrimaryGreen
                            isDeclined -> Color(0xFFC62828)
                            else -> Color(0xFFFFA000)
                        }
                        val statusBg = when {
                            isConfirmed -> Color(0xFFE8F8EE)
                            isDeclined -> Color(0xFFFFEBEE)
                            else -> Color(0xFFFFF8E1)
                        }
                        val statusText = when {
                            isConfirmed -> "CONFIRMED & ALLOTTED"
                            isDeclined -> "DECLINED"
                            else -> "PENDING VERIFICATION"
                        }
                        val statusColor = when {
                            isConfirmed -> PrimaryGreen
                            isDeclined -> Color(0xFFC62828)
                            else -> Color(0xFFE65100)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "₹${req.amount}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusBg
                                    ) {
                                        Text(
                                            text = statusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = "Seat ${req.seatNumber} • ${req.shiftTitles} (${req.durationMonths} Month${if (req.durationMonths > 1) "s" else ""})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "UTR: ${req.utrNumber.ifBlank { "N/A" }} • Submitted: ${req.requestDateStr}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isPending) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFF8E1),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⏳ Seat ${req.seatNumber} is reserved in pending status. Once Admin confirms receipt of ₹${req.amount}, your membership and seat will be activated.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF5D4037),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                if (isDeclined && !req.adminNotes.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFEBEE),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⚠️ Reason for decline: ${req.adminNotes}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFC62828),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                if (!req.proofImageUri.isNullOrBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = { previewImageUri = req.proofImageUri },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("View Attached Screenshot", fontSize = 11.5.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Paid Receipts Tab
            if (payments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No payment history yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(payments) { payment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.viewReceipt(payment) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹${payment.amount}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFE8F8EE)
                                        ) {
                                            Text(
                                                text = payment.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "${payment.dateStr} • ${payment.shiftDescription}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "View Receipt",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RaiseComplaintScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val complaints by viewModel.studentComplaints.collectAsState()
    val categories = listOf("Wi-Fi", "Light", "Water/RO", "Cleanliness", "Noise", "Seat", "Washroom", "Other")

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var issueTitle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var selectedImageForPreview by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        attachedImageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Raise Complaint",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(16.dp))

        // Category dropdown
        Text(
            text = "Category",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = !categoryExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            selectedCategory = cat
                            categoryExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Optional Title
        Text(
            text = "Issue Subject / Title (Optional)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = issueTitle,
            onValueChange = { issueTitle = it },
            placeholder = { Text("e.g., Low Wi-Fi speed on 2nd floor") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Description
        Text(
            text = "Describe your issue *",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Provide details about the issue...") },
            minLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Attach Photo Box
        Text(
            text = "Attach Photo (Optional)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))

        if (attachedImageUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, PrimaryGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = attachedImageUri,
                    contentDescription = "Attached Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { attachedImageUri = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove Photo",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "+ Attach Photo Proof",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryGreen
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (description.isNotBlank()) {
                    viewModel.submitComplaint(
                        category = selectedCategory,
                        desc = description,
                        imageUri = attachedImageUri?.toString(),
                        title = issueTitle.ifBlank { "" }
                    ) {
                        description = ""
                        issueTitle = ""
                        attachedImageUri = null
                        isSubmitted = true
                    }
                }
            },
            enabled = description.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text("Submit Complaint", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (isSubmitted) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "✓ Complaint registered successfully. Admin will review soon.",
                color = PrimaryGreen,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))

        // Previous Complaints List
        Text(
            text = "My Complaints",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))

        if (complaints.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No complaints submitted yet.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            complaints.forEach { cmp ->
                val statusColor = when (cmp.status.lowercase()) {
                    "resolved" -> PrimaryGreen
                    "rejected" -> Color(0xFFD32F2F)
                    "in review", "in_review" -> Color(0xFF1976D2)
                    else -> Color(0xFFE65100) // Pending
                }
                val statusBg = when (cmp.status.lowercase()) {
                    "resolved" -> Color(0xFFE8F8EE)
                    "rejected" -> Color(0xFFFFEBEE)
                    "in review", "in_review" -> Color(0xFFE3F2FD)
                    else -> Color(0xFFFFF3E0) // Pending
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                if (!cmp.title.isNullOrBlank()) {
                                    Text(
                                        text = cmp.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${cmp.category} • ${cmp.dateStr}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = cmp.status.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = cmp.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Attached Photo Thumbnail if present
                        if (!cmp.imageUri.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .clickable { selectedImageForPreview = cmp.imageUri }
                            ) {
                                AsyncImage(
                                    model = cmp.imageUri,
                                    contentDescription = "Complaint image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        // Admin Reply Container
                        if (!cmp.adminReply.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8F3)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.SupportAgent,
                                        contentDescription = null,
                                        tint = PrimaryGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Admin Response",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                        Text(
                                            text = cmp.adminReply,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Image Preview Modal
        selectedImageForPreview?.let { imgUri ->
            AlertDialog(
                onDismissRequest = { selectedImageForPreview = null },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = imgUri,
                            contentDescription = "Full photo preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedImageForPreview = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToAttendance: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToComplaints: () -> Unit,
    onNavigateToSeats: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()
    val userProfilePhotoUri by viewModel.userProfilePhotoUri.collectAsState()
    val activeMembership by viewModel.activeMembership.collectAsState()

    var showPersonalInfoDialog by remember { mutableStateOf(false) }
    var showMembershipDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showHelpSupportDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateUserProfilePhoto(it.toString()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        // Avatar with real photo picker and unclipped camera badge
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F8EE))
                    .border(2.5.dp, PrimaryGreen, CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!userProfilePhotoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = userProfilePhotoUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            // Camera icon overlay badge positioned outside the inner clipped box
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(PrimaryGreen)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Change Profile Photo",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = currentUser?.fullName ?: "Library Student",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "+91 ${currentUser?.mobile ?: "---"}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(Modifier.height(24.dp))

        // Menu Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                ProfileMenuItem(
                    icon = Icons.Default.PersonOutline,
                    title = "Personal Information",
                    onClick = { showPersonalInfoDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.Default.Badge,
                    title = "Membership Details",
                    onClick = { showMembershipDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.Default.ReceiptLong,
                    title = "Payment History",
                    onClick = onNavigateToPayments
                )
                ProfileMenuItem(
                    icon = Icons.Default.Feedback,
                    title = "My Complaints",
                    onClick = onNavigateToComplaints
                )
                ProfileMenuItem(
                    icon = Icons.Default.NotificationsNone,
                    title = "Notifications",
                    onClick = onNavigateToNotifications
                )
                ProfileMenuItem(
                    icon = Icons.Default.LockReset,
                    title = "Change Password",
                    onClick = { showChangePasswordDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.Default.DarkMode,
                    title = "Theme",
                    trailingText = if (isDark) "Dark >" else "Light >",
                    onClick = { viewModel.toggleDarkTheme() }
                )
                ProfileMenuItem(
                    icon = Icons.Default.HelpOutline,
                    title = "Help & Support",
                    onClick = { showHelpSupportDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = "Logout",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { showLogoutDialog = true }
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }

    // 1. Personal Information Dialog (Never shows password!)
    if (showPersonalInfoDialog) {
        AlertDialog(
            onDismissRequest = { showPersonalInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow("Full Name", currentUser?.fullName ?: "---")
                    InfoRow("Mobile Number", "+91 ${currentUser?.mobile ?: "---"}")
                    InfoRow("Email", currentUser?.email?.ifBlank { "Not provided" } ?: "Not provided")
                    InfoRow("Gender", currentUser?.gender ?: "Not specified")
                    InfoRow("Role", "Library Student Member")
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Password is encrypted & secure.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonalInfoDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 2. Membership Details Dialog
    if (showMembershipDialog) {
        AlertDialog(
            onDismissRequest = { showMembershipDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Membership Details", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoRow("Plan Type", activeMembership?.shiftTitles?.let { "Monthly Plan ($it)" } ?: "Monthly Library Access")
                    InfoRow("Assigned Seat", activeMembership?.seatNumber?.let { "Seat #$it" } ?: "General / Unassigned")
                    InfoRow("Shift", activeMembership?.shiftTitles ?: "Full Day (8:00 AM - 8:00 PM)")
                    InfoRow("Membership Expiry", activeMembership?.expiryDate ?: "30 Sep 2026")
                    InfoRow("Status", activeMembership?.status ?: "Active Member")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMembershipDialog = false
                        onNavigateToSeats()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("View Seats")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMembershipDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 3. Change Password Dialog (Current Password Verification)
    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            viewModel = viewModel,
            onDismiss = { showChangePasswordDialog = false }
        )
    }

    // 4. Help & Support Dialog (WhatsApp direct integration + Phone)
    if (showHelpSupportDialog) {
        AlertDialog(
            onDismissRequest = { showHelpSupportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = PrimaryGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Help & Support", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Need help with seats, fees, or facilities? Contact the library admin directly:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // WhatsApp Chat Button
                    Button(
                        onClick = {
                            val url = "https://api.whatsapp.com/send?phone=919569556006&text=Hello%20Digital%20Library%20Admin,%20I%20am%20${currentUser?.fullName}%20(Mobile:%20${currentUser?.mobile}).%20I%20need%20assistance%20with:"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp: +91 95695 56006", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Chat on WhatsApp", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // Direct Call Button
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+919569556006"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Call: +91 95695 56006", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Call Admin: +91 95695 56006", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Column {
                        Text("Library Hours", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("6:00 AM - 11:00 PM (All 7 Days)", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpSupportDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 5. Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out from your account?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ChangePasswordDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var oldPassVisible by remember { mutableStateOf(false) }
    var newPassVisible by remember { mutableStateOf(false) }
    var confirmPassVisible by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val pendingRequest by viewModel.userPendingPasswordRequest.collectAsState()
    val criteria = remember(newPassword) { PasswordSecurity.checkPasswordCriteria(newPassword) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pending Request Banner if one is already awaiting admin approval
                pendingRequest?.let { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF90CAF9))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFF1565C0),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Request Pending Admin Approval",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D47A1)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "You submitted a password change request on ${req.requestDateStr}. Your new password will be activated as soon as the Admin approves it.",
                                fontSize = 11.5.sp,
                                color = Color(0xFF1565C0),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Text(
                    text = "Verify your current password and specify a new secure password. Your request will be sent to the Admin for approval before changing.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text("Current Password") },
                    singleLine = true,
                    visualTransformation = if (oldPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { oldPassVisible = !oldPassVisible }) {
                            Icon(
                                imageVector = if (oldPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Strong Password") },
                    singleLine = true,
                    visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newPassVisible = !newPassVisible }) {
                            Icon(
                                imageVector = if (newPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Strength Checklist
                PasswordStrengthIndicator(
                    password = newPassword,
                    title = "Required Password Standards"
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm New Password") },
                    singleLine = true,
                    visualTransformation = if (confirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { confirmPassVisible = !confirmPassVisible }) {
                            Icon(
                                imageVector = if (confirmPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                    Text("❌ Passwords do not match", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        if (newPassword != confirmPassword) {
                            statusMessage = Pair(false, "Passwords do not match.")
                            return@Button
                        }
                        val (isValid, errMsg) = PasswordSecurity.validatePasswordStrength(newPassword)
                        if (!isValid) {
                            statusMessage = Pair(false, errMsg ?: "Password does not meet complexity requirements.")
                            return@Button
                        }
                        isLoading = true
                        viewModel.changeStudentPassword(
                            oldPassword = oldPassword,
                            newPassword = newPassword,
                            confirmPassword = confirmPassword,
                            onSuccess = { msg ->
                                isLoading = false
                                statusMessage = Pair(true, "✅ $msg")
                                oldPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            },
                            onError = { err ->
                                isLoading = false
                                statusMessage = Pair(false, err)
                            }
                        )
                    },
                    enabled = !isLoading && oldPassword.isNotBlank() && criteria.isStrong && newPassword == confirmPassword,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text(if (isLoading) "Submitting Request..." else "Submit Request to Admin")
                }

                statusMessage?.let { (success, msg) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            color = if (success) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    trailingText: String? = null,
    tint: Color = PrimaryGreen,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface
            )
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
