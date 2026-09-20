package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.util.AttendanceUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceOvernightTest {

    private lateinit var db: AppDatabase
    private val istTz = TimeZone.getTimeZone("Asia/Kolkata")
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).apply {
        timeZone = istTz
    }

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    /**
     * Test 1: 10PM -> 12AM (Midnight) -> Session MUST remain ACTIVE.
     * Entry at 10:00 PM on 2026-09-19.
     * At 12:00 AM midnight (2026-09-20), resolveTodaySession must still return the active session.
     */
    @Test
    fun test1_entry10pm_midnight12am_remainsActive() {
        val entryTimeMs = sdf.parse("2026-09-19 22:00")!!.time
        val midnightMs = sdf.parse("2026-09-20 00:00")!!.time

        val activeRecord = AttendanceRecord(
            id = "ATT-101",
            studentId = "STU-001",
            studentName = "Rahul Sharma",
            mobile = "9876543210",
            seatNumber = "12",
            shiftTitle = "Shift 3 (06:00 PM - 12:00 AM)",
            dateStr = "19 Sep 2026",
            entryTime = "10:00 PM",
            entryTimestamp = entryTimeMs,
            exitTime = null,
            exitTimestamp = null,
            duration = "--",
            durationMinutes = 0,
            status = "ACTIVE",
            isInside = true
        )

        // At midnight (2026-09-20 00:00), check resolved session
        val resolved = AttendanceUtils.resolveTodaySession(listOf(activeRecord), now = midnightMs)

        assertNotNull("Active overnight session must not be reset at midnight", resolved)
        assertEquals("ATT-101", resolved!!.id)
        assertEquals("10:00 PM", resolved.entryTime)
        assertNull(resolved.exitTime)
        assertTrue("Session must remain inside", resolved.isInside)
        assertEquals("ACTIVE", resolved.status)
    }

    /**
     * Test 2: 10PM -> 4AM checkout -> 6h duration across midnight.
     * Entry at 10:00 PM on 2026-09-19.
     * Checkout at 04:00 AM on 2026-09-20.
     * Duration must be exactly 6h.
     */
    @Test
    fun test2_entry10pm_exit4am_exact6HoursDuration() {
        val entryTimeMs = sdf.parse("2026-09-19 22:00")!!.time
        val exitTimeMs = sdf.parse("2026-09-20 04:00")!!.time

        val (durationStr, durationMinutes) = AttendanceUtils.calculateDurationBetween(entryTimeMs, exitTimeMs)

        assertEquals("6h", durationStr)
        assertEquals(360L, durationMinutes)

        // Verify completed record behavior on exit day
        val completedRecord = AttendanceRecord(
            id = "ATT-101",
            studentId = "STU-001",
            studentName = "Rahul Sharma",
            mobile = "9876543210",
            seatNumber = "12",
            shiftTitle = "Shift 3",
            dateStr = "19 Sep 2026",
            entryTime = "10:00 PM",
            entryTimestamp = entryTimeMs,
            exitTime = "04:00 AM",
            exitTimestamp = exitTimeMs,
            duration = durationStr,
            durationMinutes = durationMinutes,
            status = "COMPLETED",
            isInside = false
        )

        // At 05:00 AM on 2026-09-20 (same day as checkout), today's attendance shows the completed session
        val checkTimeMs = sdf.parse("2026-09-20 05:00")!!.time
        val resolved = AttendanceUtils.resolveTodaySession(listOf(completedRecord), now = checkTimeMs)

        assertNotNull("Checkout on Sep 20 should be visible as completed today", resolved)
        assertEquals("10:00 PM", resolved!!.entryTime)
        assertEquals("04:00 AM", resolved.exitTime)
        assertEquals("6h", resolved.duration)
        assertFalse(resolved.isInside)
    }

    /**
     * Test 3: Completed session from previous day -> Next day fresh UI.
     * Checkout occurred on 2026-09-20 at 04:00 AM.
     * On 2026-09-21 (next day), attendance UI must be FRESH (returns null).
     */
    @Test
    fun test3_completedSession_nextDayFresh() {
        val entryTimeMs = sdf.parse("2026-09-19 22:00")!!.time
        val exitTimeMs = sdf.parse("2026-09-20 04:00")!!.time

        val completedRecord = AttendanceRecord(
            id = "ATT-101",
            studentId = "STU-001",
            studentName = "Rahul Sharma",
            mobile = "9876543210",
            seatNumber = "12",
            shiftTitle = "Shift 3",
            dateStr = "19 Sep 2026",
            entryTime = "10:00 PM",
            entryTimestamp = entryTimeMs,
            exitTime = "04:00 AM",
            exitTimestamp = exitTimeMs,
            duration = "6h",
            durationMinutes = 360,
            status = "COMPLETED",
            isInside = false
        )

        // On 2026-09-21 at 09:00 AM, query resolved session
        val nextDayMs = sdf.parse("2026-09-21 09:00")!!.time
        val resolved = AttendanceUtils.resolveTodaySession(listOf(completedRecord), now = nextDayMs)

        assertNull("Next day must return fresh state (null) when previous session is completed", resolved)
    }

    /**
     * Test 4: No previous attendance -> Fresh state.
     * When user has no records, resolveTodaySession returns null.
     */
    @Test
    fun test4_noPreviousAttendance_freshState() {
        val nowMs = sdf.parse("2026-09-20 10:00")!!.time
        val resolved = AttendanceUtils.resolveTodaySession(emptyList(), now = nowMs)

        assertNull("Fresh user with no attendance records must resolve to null (Entry: --, Exit: --, Duration: 0m)", resolved)
    }

    /**
     * Test 5: App restart / reload during active session -> Still ACTIVE.
     * When user enters, record is written to Room DB.
     * Even if app process is killed and reopened, querying DB recovers active session.
     */
    @Test
    fun test5_appRestart_duringActiveSession_persistedAndActive() = runBlocking {
        val entryTimeMs = sdf.parse("2026-09-19 22:00")!!.time

        val record = AttendanceRecord(
            id = "ATT-PERSISTED",
            studentId = "STU-002",
            studentName = "Priya Singh",
            mobile = "9123456780",
            seatNumber = "05",
            shiftTitle = "Shift 3",
            dateStr = "19 Sep 2026",
            entryTime = "10:00 PM",
            entryTimestamp = entryTimeMs,
            exitTime = null,
            exitTimestamp = null,
            duration = "--",
            durationMinutes = 0,
            status = "ACTIVE",
            isInside = true
        )

        // Persist to Room
        db.attendanceDao().insertAttendance(record)

        // Simulate app restart / cold launch: re-query database
        val active = db.attendanceDao().getActiveAttendanceForStudent("STU-002")
        assertNotNull("Active session must be retrieved from database after restart", active)
        assertEquals("ATT-PERSISTED", active!!.id)
        assertTrue(active.isInside)
        assertEquals("10:00 PM", active.entryTime)

        // Resolve session at 02:30 AM on Day 2
        val queryTimeMs = sdf.parse("2026-09-20 02:30")!!.time
        val resolved = AttendanceUtils.resolveTodaySession(listOf(active), now = queryTimeMs)

        assertNotNull("Session must still be active in resolved UI state", resolved)
        assertEquals("10:00 PM", resolved!!.entryTime)
        assertNull(resolved.exitTime)
        assertTrue(resolved.isInside)
    }
}
