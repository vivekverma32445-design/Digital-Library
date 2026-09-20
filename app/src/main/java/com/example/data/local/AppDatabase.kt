package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Shift::class,
        Seat::class,
        Membership::class,
        SeatAllocation::class,
        AttendanceRecord::class,
        PaymentRecord::class,
        Complaint::class,
        Announcement::class,
        NotificationItem::class,
        AdminSession::class,
        AdminAuthLog::class,
        PasswordResetRequest::class,
        PaymentVerificationRequest::class,
        PaymentConfig::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun shiftDao(): ShiftDao
    abstract fun seatDao(): SeatDao
    abstract fun seatAllocationDao(): SeatAllocationDao
    abstract fun membershipDao(): MembershipDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun notificationDao(): NotificationDao
    abstract fun adminAuthDao(): AdminAuthDao
    abstract fun passwordResetRequestDao(): PasswordResetRequestDao
    abstract fun paymentVerificationRequestDao(): PaymentVerificationRequestDao
    abstract fun paymentConfigDao(): PaymentConfigDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "digital_library_prod_db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedInitialData(database)
                    }
                }
            }
        }

        suspend fun seedInitialData(database: AppDatabase) {
            // 1. Seed 36 seats - all clean & available by default (no demo maintenance/occupied)
            val seats = (1..36).map { num ->
                val formatted = String.format("%02d", num)
                Seat(
                    seatNumber = formatted,
                    isMaintenance = false
                )
            }
            database.seatDao().insertSeats(seats)

            // 2. Seed 4 shifts with academic timeframes
            val defaultShifts = listOf(
                Shift(1, "Shift 1", "6:00 AM - 12:00 PM", "06:00", "12:00", 350, true),
                Shift(2, "Shift 2", "12:00 PM - 6:00 PM", "12:00", "18:00", 350, true),
                Shift(3, "Shift 3", "6:00 PM - 12:00 AM", "18:00", "24:00", 350, true),
                Shift(4, "Shift 4", "12:00 AM - 6:00 AM", "00:00", "06:00", 350, true)
            )
            database.shiftDao().insertShifts(defaultShifts)

            // 3. Seed Initial Administrator Account (PBKDF2 salted hash, never plain text!)
            val existingAdmin = database.userDao().getUserByMobile("9569556006")
            val initialHashedPass = com.example.util.PasswordSecurity.hashPassword("M@n1shyadav")
            if (existingAdmin == null) {
                val admin = User(
                    id = "DL-ADMIN-01",
                    fullName = "Maa Durga Admin",
                    mobile = "9569556006",
                    email = "contact@maadurgalibrary.com",
                    gender = "Other",
                    passwordHash = initialHashedPass,
                    role = "ADMIN",
                    address = "Mania Deval Road, Ghazipur District, UP - 232333"
                )
                database.userDao().insertUser(admin)
            } else if (!existingAdmin.passwordHash.startsWith("pbkdf2:")) {
                // Upgrade plain text hash to cryptographic PBKDF2
                database.userDao().updatePassword("9569556006", initialHashedPass)
            }

            database.adminAuthDao().insertAuthLog(
                AdminAuthLog(
                    id = "LOG-INIT-${System.currentTimeMillis()}",
                    adminMobile = "9569556006",
                    eventType = "SYSTEM_INITIALIZED",
                    dateStr = "07 Sep 2026",
                    details = "Initial Administrator Account seeded with PBKDF2 cryptographic hash."
                )
            )

            // No dummy student memberships, allocations, attendance, or payments are pre-populated.
            // Everything starts empty & available for real users and real admissions!
        }
    }
}
