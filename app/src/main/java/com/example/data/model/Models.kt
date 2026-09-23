package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["mobile"], unique = true), Index(value = ["role"])]
)
data class User(
    @PrimaryKey val id: String, // e.g. "DL-2026-00001"
    val fullName: String,
    val mobile: String,
    val email: String,
    val gender: String,
    val passwordHash: String,
    val role: String = "STUDENT", // STUDENT or ADMIN
    val address: String = "Mania Deval Road, Ghazipur",
    val isActive: Boolean = true
)

@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey val id: Int, // 1, 2, 3, 4
    val title: String, // "Shift 1"
    val timeRange: String, // "6:00 AM - 12:00 PM"
    val startTime: String, // "06:00"
    val endTime: String, // "12:00"
    val monthlyFee: Int = 350,
    val isActive: Boolean = true
)

@Entity(tableName = "seats")
data class Seat(
    @PrimaryKey val seatNumber: String, // "01" to "36"
    val isMaintenance: Boolean = false,
    val notes: String = ""
)

@Entity(
    tableName = "memberships",
    indices = [Index(value = ["studentId"]), Index(value = ["seatNumber"]), Index(value = ["status"])]
)
data class Membership(
    @PrimaryKey val id: String,
    val studentId: String,
    val studentName: String,
    val seatNumber: String,
    val shiftIdsCsv: String, // e.g. "1,2"
    val shiftTitles: String, // e.g. "Shift 1 + 2"
    val startDate: String,
    val expiryDate: String, // e.g. "30 Sep 2026"
    val amount: Int,
    val status: String, // "ACTIVE", "EXPIRING_SOON", "EXPIRED", "PENDING"
    val durationMonths: Int = 1,
    val startDateMillis: Long = System.currentTimeMillis(),
    val expiryDateMillis: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
) {
    fun getEffectiveExpiryMillis(): Long {
        if (expiryDateMillis > 0) return expiryDateMillis
        return try {
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            }
            sdf.parse(expiryDate)?.time ?: (startDateMillis + (durationMonths * 30L * 24 * 60 * 60 * 1000))
        } catch (_: Exception) {
            startDateMillis + (durationMonths * 30L * 24 * 60 * 60 * 1000)
        }
    }

    fun getDaysRemaining(nowMillis: Long = System.currentTimeMillis()): Int {
        if (status.equals("EXPIRED", ignoreCase = true)) return 0
        val targetMillis = getEffectiveExpiryMillis()
        if (nowMillis >= targetMillis) return 0

        // Strict daily countdown calculated at midnight boundary in IST
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val nowCal = java.util.Calendar.getInstance(tz).apply { timeInMillis = nowMillis }
        val expCal = java.util.Calendar.getInstance(tz).apply { timeInMillis = targetMillis }

        val nowMidnight = java.util.Calendar.getInstance(tz).apply {
            set(nowCal.get(java.util.Calendar.YEAR), nowCal.get(java.util.Calendar.MONTH), nowCal.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val expMidnight = java.util.Calendar.getInstance(tz).apply {
            set(expCal.get(java.util.Calendar.YEAR), expCal.get(java.util.Calendar.MONTH), expCal.get(java.util.Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diffDays = ((expMidnight - nowMidnight) / (1000L * 60 * 60 * 24)).toInt()
        return if (diffDays <= 0) 0 else diffDays
    }

    val isExpired: Boolean
        get() = status.equals("EXPIRED", ignoreCase = true) || getDaysRemaining() <= 0

    val isExpiringSoon: Boolean
        get() = !isExpired && getDaysRemaining() in 1..5
}

@Entity(
    tableName = "seat_allocations",
    indices = [Index(value = ["seatNumber", "shiftId"]), Index(value = ["studentId"]), Index(value = ["status"])]
)
data class SeatAllocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seatNumber: String,
    val shiftId: Int,
    val studentId: String,
    val studentName: String,
    val membershipId: String,
    val status: String // "CONFIRMED", "HELD"
)

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["studentId"]), Index(value = ["dateStr"]), Index(value = ["status"]), Index(value = ["isInside"])]
)
data class AttendanceRecord(
    @PrimaryKey val id: String,
    val studentId: String,
    val studentName: String,
    val mobile: String,
    val seatNumber: String,
    val shiftTitle: String,
    val libraryId: String = "MDDL-GHAZIPUR-MAIN",
    val dateStr: String, // "07 Sep 2026"
    val entryTime: String, // "09:00 AM" (IST)
    val entryTimestamp: Long = System.currentTimeMillis(),
    val exitTime: String? = null, // "12:00 PM" (IST)
    val exitTimestamp: Long? = null,
    val duration: String = "--",
    val durationMinutes: Long = 0,
    val status: String = "active", // "active", "completed", "incomplete", "cancelled"
    val isInside: Boolean = true,
    val method: String = "QR_SCAN", // "QR_SCAN", "ADMIN_OVERRIDE", "AUTO_CLOSED"
    val correctionReason: String? = null,
    val correctedByAdminId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class AttendanceScanResult(
    val success: Boolean,
    val action: String, // "entry", "exit", "error"
    val message: String,
    val record: AttendanceRecord? = null,
    val entryTime: String? = null,
    val exitTime: String? = null,
    val totalDuration: String? = null,
    val status: String? = null,
    val errorCode: String? = null
)

@Entity(
    tableName = "payments",
    indices = [Index(value = ["studentId"]), Index(value = ["dateStr"])]
)
data class PaymentRecord(
    @PrimaryKey val id: String, // "TXN-10294"
    val studentId: String,
    val studentName: String,
    val amount: Int,
    val shiftDescription: String,
    val dateStr: String,
    val status: String = "Paid", // "Paid", "Pending", "Failed"
    val upiRefId: String = "",
    val paymentMode: String = "UPI",
    val remarks: String = ""
)

@Entity(
    tableName = "complaints",
    indices = [Index(value = ["studentId"]), Index(value = ["status"])]
)
data class Complaint(
    @PrimaryKey val id: String,
    val studentId: String,
    val studentName: String,
    val category: String, // Wi-Fi, Light, Water/RO, Cleanliness, Noise, Washroom, Seat, Other
    val description: String,
    val dateStr: String,
    val status: String = "Pending", // "Pending", "In Review", "Resolved", "Rejected"
    val adminReply: String? = null,
    val imageUri: String? = null,
    val title: String = ""
)

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dateStr: String,
    val priority: String = "Normal", // "Normal", "Important", "Urgent"
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiryDays: Int = 7, // duration in days selected by admin
    val expiryDateStr: String? = null, // e.g. "20 Sep 2026"
    val expiryDateMillis: Long? = null // epoch millis when announcement expires
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["targetStudentId"]), Index(value = ["isRead"])]
)
data class NotificationItem(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val type: String, // "ANNOUNCEMENT", "PAYMENT_DUE", "PAYMENT_SUCCESS", "PAYMENT_REMINDER", "PAYMENT_HISTORY", "SEAT_ADMISSION", "SEAT_AVAILABILITY", "LIBRARY_NOTICE", "COMPLAINT_STATUS", "ACCOUNT", "GENERAL", "SYSTEM"
    val isRead: Boolean = false,
    val targetStudentId: String? = null, // null means broadcast to all students
    val createdAtMillis: Long = System.currentTimeMillis()
)

// UI state models
enum class SeatAvailabilityState {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    SELECTED,
    MAINTENANCE
}

data class SeatVisualItem(
    val seatNumber: String,
    val state: SeatAvailabilityState,
    val occupantName: String? = null,
    val shiftTitle: String? = null
)

@Entity(tableName = "admin_sessions")
data class AdminSession(
    @PrimaryKey val token: String,
    val adminId: String,
    val adminMobile: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = System.currentTimeMillis() + (8L * 60 * 60 * 1000), // 8 hours validity
    val isActive: Boolean = true
) {
    val isExpired: Boolean
        get() = !isActive || System.currentTimeMillis() >= expiresAtMillis
}

@Entity(tableName = "admin_auth_logs")
data class AdminAuthLog(
    @PrimaryKey val id: String,
    val adminMobile: String,
    val eventType: String, // "LOGIN_SUCCESS", "LOGIN_FAILED", "LOGOUT", "PASSWORD_RESET_OTP_REQUESTED", "PASSWORD_RESET_SUCCESS", "PASSWORD_CHANGED"
    val timestampMillis: Long = System.currentTimeMillis(),
    val dateStr: String,
    val details: String = ""
)

@Entity(
    tableName = "password_reset_requests",
    indices = [Index(value = ["mobile"]), Index(value = ["status"])]
)
data class PasswordResetRequest(
    @PrimaryKey val id: String, // "PRR-${System.currentTimeMillis()}"
    val userId: String,
    val userName: String,
    val mobile: String,
    val pendingPasswordHash: String,
    val requestDateStr: String,
    val requestTimestamp: Long,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val adminNotes: String? = null,
    val reviewedByAdminId: String? = null,
    val reviewedTimestamp: Long? = null,
    val requestType: String = "RESET" // "RESET" (Forgot Password) or "CHANGE" (Profile Password Change)
)

@Entity(
    tableName = "payment_verification_requests",
    indices = [Index(value = ["studentId"]), Index(value = ["status"])]
)
data class PaymentVerificationRequest(
    @PrimaryKey val id: String, // "PVR-${System.currentTimeMillis()}"
    val studentId: String,
    val studentName: String,
    val studentMobile: String,
    val seatNumber: String,
    val shiftIdsCsv: String,
    val shiftTitles: String,
    val durationMonths: Int,
    val amount: Int,
    val proofImageUri: String,
    val utrNumber: String = "",
    val remarks: String = "",
    val requestDateStr: String,
    val requestTimestamp: Long,
    val status: String = "PENDING", // PENDING, CONFIRMED, DECLINED
    val adminNotes: String? = null,
    val reviewedByAdminId: String? = null,
    val reviewedTimestamp: Long? = null
)

@Entity(tableName = "payment_config")
data class PaymentConfig(
    @PrimaryKey val id: Int = 1,
    val upiId: String = "9569556006@ybl",
    val payeeName: String = "Maa Durga Digital Library",
    val qrImageUri: String? = null,
    val paymentInstructions: String = "1. Scan the QR code or use the UPI ID in your payment app.\n2. Pay the exact fee amount for your selected seat and duration.\n3. Take a screenshot of the payment receipt/confirmation.\n4. Upload the screenshot below and enter the 12-digit UTR/Ref number.\n5. Tap 'Submit Payment Proof' to reserve your seat while Admin verifies.",
    val paymentNotice: String = "Your seat will be reserved as 'Pending Payment Verification' until the Admin approves your payment.",
    val isManualUpiEnabled: Boolean = true
)

data class AdminLoginResult(
    val success: Boolean,
    val session: AdminSession? = null,
    val user: User? = null,
    val errorMessage: String? = null,
    val isLockedOut: Boolean = false,
    val lockRemainingMinutes: Int = 0
)

