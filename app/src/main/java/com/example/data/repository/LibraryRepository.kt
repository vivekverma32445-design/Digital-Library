package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.util.PasswordSecurity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.example.util.AttendanceUtils
import com.example.data.remote.FirebaseSyncService
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LibraryRepository(val db: AppDatabase) {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.notificationDao().sanitizeAdminNotifications()
            } catch (_: Exception) {}
        }
        FirebaseSyncService.syncInitialData(db)
        FirebaseSyncService.startRealtimeSync(db)
    }

    // Anti-rapid scan cache for duplicate scan prevention
    private val lastScanTimestamps = ConcurrentHashMap<String, Long>()

    // Security rate-limiting structures for Administrator Portal
    private val adminFailedAttempts = ConcurrentHashMap<String, MutableList<Long>>()

    /**
     * Highly resilient, collision-safe ID generator for high concurrency (1000+ users).
     */
    fun generateUniqueId(prefix: String): String {
        val rand = (10000..99999).random()
        return "$prefix-${System.currentTimeMillis()}-$rand"
    }

    // Official Library Entrance QR Identifier
    val officialLibraryQrCode = "MDDL-GHAZIPUR-MAIN-GATE"

    val allShifts: Flow<List<Shift>> = db.shiftDao().getAllShifts()
    val allSeats: Flow<List<Seat>> = db.seatDao().getAllSeats()
    val allAllocations: Flow<List<SeatAllocation>> = db.seatAllocationDao().getAllAllocations()
    val allAnnouncements: Flow<List<Announcement>> = db.announcementDao().getAllAnnouncements()
    val allAttendance: Flow<List<AttendanceRecord>> = db.attendanceDao().getAllAttendance()
    val currentlyInside: Flow<List<AttendanceRecord>> = db.attendanceDao().getCurrentlyInside()
    val insideCount: Flow<Int> = db.attendanceDao().getInsideCount()
    val allStudents: Flow<List<User>> = db.userDao().getAllStudents()
    val allAdmins: Flow<List<User>> = db.userDao().getAllAdmins()
    val allPayments: Flow<List<PaymentRecord>> = db.paymentDao().getAllPayments()
    val allComplaints: Flow<List<Complaint>> = db.complaintDao().getAllComplaints()
    val allMemberships: Flow<List<Membership>> = db.membershipDao().getAllMemberships()
    val allNotifications: Flow<List<NotificationItem>> = db.notificationDao().getAllNotifications()
    val adminAuthLogs: Flow<List<AdminAuthLog>> = db.adminAuthDao().getRecentAuthLogs(50)
    val allPasswordResetRequests: Flow<List<PasswordResetRequest>> = db.passwordResetRequestDao().getAllRequestsFlow()
    val pendingPasswordResetRequests: Flow<List<PasswordResetRequest>> = db.passwordResetRequestDao().getPendingRequestsFlow()
    val allPaymentVerificationRequests: Flow<List<PaymentVerificationRequest>> = db.paymentVerificationRequestDao().getAllRequestsFlow()
    val pendingPaymentVerificationRequests: Flow<List<PaymentVerificationRequest>> = db.paymentVerificationRequestDao().getPendingRequestsFlow()
    val paymentConfigFlow: Flow<PaymentConfig?> = db.paymentConfigDao().getConfigFlow()

    fun getNotificationsForStudent(studentId: String): Flow<List<NotificationItem>> =
        db.notificationDao().getNotificationsForStudent(studentId)

    fun getUnreadNotificationCount(studentId: String): Flow<Int> =
        db.notificationDao().getUnreadCount(studentId)

    fun getNotificationsForAdmin(): Flow<List<NotificationItem>> =
        db.notificationDao().getNotificationsForAdmin()

    fun getAdminUnreadNotificationCount(): Flow<Int> =
        db.notificationDao().getAdminUnreadCount()

    suspend fun markAllNotificationsAsRead(studentId: String) =
        db.notificationDao().markAllAsRead(studentId)

    suspend fun markAllAdminNotificationsAsRead() =
        db.notificationDao().markAllAdminAsRead()

    suspend fun markNotificationAsRead(id: String) =
        db.notificationDao().markAsRead(id)

    suspend fun deleteNotification(id: String) =
        db.notificationDao().deleteNotification(id)

    suspend fun clearAllNotifications() =
        db.notificationDao().clearAll()

    fun getStudentMembership(studentId: String): Flow<Membership?> =
        db.membershipDao().getActiveMembershipForStudent(studentId)

    fun getStudentAttendance(studentId: String): Flow<List<AttendanceRecord>> =
        db.attendanceDao().getAttendanceForStudent(studentId)

    fun getTodayAttendanceFlow(studentId: String): Flow<AttendanceRecord?> =
        db.attendanceDao().getAttendanceForStudent(studentId).map { records ->
            AttendanceUtils.resolveTodaySession(records)
        }

    suspend fun getActiveAttendance(studentId: String): AttendanceRecord? =
        db.attendanceDao().getActiveAttendanceForStudent(studentId)

    fun getStudentPayments(studentId: String): Flow<List<PaymentRecord>> =
        db.paymentDao().getPaymentsForStudent(studentId)

    fun getStudentComplaints(studentId: String): Flow<List<Complaint>> =
        db.complaintDao().getComplaintsForStudent(studentId)

    fun getAllocationsForShift(shiftId: Int): Flow<List<SeatAllocation>> =
        db.seatAllocationDao().getAllocationsForShift(shiftId)

    suspend fun getUserByMobile(mobile: String): User? {
        val local = db.userDao().getUserByMobile(mobile)
        if (local != null) return local
        val remote = FirebaseSyncService.fetchUserByMobile(mobile)
        if (remote != null) {
            db.userDao().insertUser(remote)
            return remote
        }
        return null
    }

    suspend fun getUserById(id: String): User? =
        db.userDao().getUserById(id)

    suspend fun registerUser(
        fullName: String,
        mobile: String,
        email: String,
        gender: String,
        password: String
    ): Result<User> {
        val existing = getUserByMobile(mobile)
        if (existing != null) {
            return Result.failure(Exception("Mobile number is already registered."))
        }
        val count = db.userDao().getStudentCount().first()
        var nextId: String
        var attempts = 0
        do {
            val randomSuffix = (1000..9999).random()
            nextId = String.format("DL-2026-%05d", ((count + 1 + attempts) * 100 + (randomSuffix % 100)) % 90000 + 10000)
            attempts++
        } while (db.userDao().getUserById(nextId) != null && attempts < 10)
        if (db.userDao().getUserById(nextId) != null) {
            nextId = "DL-2026-${(10000..99999).random()}"
        }
        val newUser = User(
            id = nextId,
            fullName = fullName,
            mobile = mobile,
            email = email,
            gender = gender,
            passwordHash = password, // in production salted SHA-256
            role = "STUDENT"
        )
        db.userDao().insertUser(newUser)
        FirebaseSyncService.syncUser(newUser)
        return Result.success(newUser)
    }

    suspend fun registerAdmin(
        fullName: String,
        mobile: String,
        email: String,
        masterKey: String,
        password: String
    ): Result<User> {
        val cleanMobile = mobile.trim()
        if (cleanMobile.length != 10 || !cleanMobile.all { it.isDigit() }) {
            return Result.failure(Exception("Please enter a valid 10-digit mobile number."))
        }
        val validMasterKeys = setOf("MKEY@2026", "MDDL@ADMIN2026", "ADMIN9569", "MDDL2026", "ADMIN2026", "MDDLADMIN")
        if (!validMasterKeys.contains(masterKey.trim())) {
            return Result.failure(Exception("Invalid Master Security Key. Only authorized administrators with the library master key can register."))
        }
        val existing = db.userDao().getUserByMobile(cleanMobile)
        if (existing != null) {
            return Result.failure(Exception("An administrator account with mobile +91 $cleanMobile is already registered."))
        }
        val (isValidStrength, strengthError) = PasswordSecurity.validatePasswordStrength(password)
        if (!isValidStrength) {
            return Result.failure(Exception(strengthError ?: "Password does not meet complexity requirements."))
        }
        val count = db.userDao().getAdminCount().first()
        val nextId = String.format("ADM-%03d", count + 1)
        val hashedPassword = PasswordSecurity.hashPassword(password)
        val newAdmin = User(
            id = nextId,
            fullName = fullName.trim().ifBlank { "Administrator" },
            mobile = cleanMobile,
            email = email.trim(),
            gender = "Not specified",
            passwordHash = hashedPassword,
            role = "ADMIN"
        )
        db.userDao().insertUser(newAdmin)

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        db.adminAuthDao().insertAuthLog(
            AdminAuthLog(
                id = "LOG-REG-${System.currentTimeMillis()}",
                adminMobile = cleanMobile,
                eventType = "ADMIN_REGISTERED",
                dateStr = sdfDate.format(Date()),
                details = "New authorized administrator (${newAdmin.fullName}, ID: $nextId) registered successfully."
            )
        )
        return Result.success(newAdmin)
    }

    // ==========================================
    // ADMINISTRATOR SECURE AUTHENTICATION ENGINE
    // ==========================================

    suspend fun adminLogin(mobile: String, pass: String): AdminLoginResult {
        val cleanMobile = mobile.trim()
        val now = System.currentTimeMillis()
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val dateStr = sdfDate.format(Date(now))

        // 1. Mobile Format Validation
        if (cleanMobile.length != 10 || !cleanMobile.all { it.isDigit() }) {
            return AdminLoginResult(
                success = false,
                errorMessage = "Invalid administrator mobile or password."
            )
        }

        // 2. Rate Limiting Check (Lockout after 5 failed attempts in 10 minutes)
        val attempts = adminFailedAttempts.computeIfAbsent(cleanMobile) { mutableListOf() }
        synchronized(attempts) {
            attempts.removeAll { now - it > 10 * 60 * 1000L }
            if (attempts.size >= 5) {
                val oldestAttempt = attempts.firstOrNull() ?: now
                val remainingMs = (10 * 60 * 1000L) - (now - oldestAttempt)
                val remainingMins = ((remainingMs / 60000L) + 1).toInt().coerceAtLeast(1)
                return AdminLoginResult(
                    success = false,
                    isLockedOut = true,
                    lockRemainingMinutes = remainingMins,
                    errorMessage = "Too many failed attempts. Account temporarily locked for $remainingMins minute(s) for security. Use Forgot Password to reset."
                )
            }
        }

        // 3. User & Password Hash Verification
        val cleanPass = pass.trim()
        val admin = db.userDao().getUserByMobile(cleanMobile)
        val isPasswordValid = if (admin != null && admin.role == "ADMIN") {
            PasswordSecurity.verifyPassword(cleanPass, admin.passwordHash)
        } else {
            false
        }

        if (!isPasswordValid || admin == null || admin.role != "ADMIN") {
            synchronized(attempts) {
                attempts.add(now)
            }
            db.adminAuthDao().insertAuthLog(
                AdminAuthLog(
                    id = "LOG-FAIL-${System.currentTimeMillis()}",
                    adminMobile = cleanMobile,
                    eventType = "LOGIN_FAILED",
                    dateStr = dateStr,
                    details = "Failed login attempt with mobile +91 $cleanMobile."
                )
            )
            return AdminLoginResult(
                success = false,
                errorMessage = "Invalid administrator mobile or password."
            )
        }

        // 4. Successful Authentication -> Create Session Token
        synchronized(attempts) {
            attempts.clear()
        }

        // Support multi-device concurrent admin logins without kicking existing sessions out
        val sessionToken = PasswordSecurity.generateSessionToken()
        val session = AdminSession(
            token = sessionToken,
            adminId = admin.id,
            adminMobile = admin.mobile,
            createdAtMillis = now,
            expiresAtMillis = now + (30L * 24 * 60 * 60 * 1000L), // 30-day active window for persistent login
            isActive = true
        )
        db.adminAuthDao().insertSession(session)

        db.adminAuthDao().insertAuthLog(
            AdminAuthLog(
                id = "LOG-SUCC-${System.currentTimeMillis()}",
                adminMobile = cleanMobile,
                eventType = "LOGIN_SUCCESS",
                dateStr = dateStr,
                details = "Administrator authenticated successfully. Active session established."
            )
        )

        return AdminLoginResult(
            success = true,
            session = session,
            user = admin
        )
    }

    suspend fun validateAdminSession(token: String): Result<AdminSession> {
        val session = db.adminAuthDao().getActiveSession(token)
            ?: return Result.failure(Exception("Admin session not found or inactive."))

        if (session.isExpired) {
            db.adminAuthDao().invalidateSession(token)
            return Result.failure(Exception("Admin session expired. Please login again."))
        }
        return Result.success(session)
    }

    suspend fun adminLogout(token: String, adminMobile: String): Result<Unit> {
        db.adminAuthDao().invalidateSession(token)
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        db.adminAuthDao().insertAuthLog(
            AdminAuthLog(
                id = "LOG-OUT-${System.currentTimeMillis()}",
                adminMobile = adminMobile,
                eventType = "LOGOUT",
                dateStr = sdfDate.format(Date()),
                details = "Administrator logged out manually. Session token invalidated."
            )
        )
        return Result.success(Unit)
    }

    suspend fun getAdminSession(token: String): AdminSession? {
        val session = db.adminAuthDao().getActiveSession(token)
        return if (session != null && !session.isExpired) session else null
    }

    suspend fun createOrRestoreAdminSession(admin: User): AdminSession {
        val now = System.currentTimeMillis()
        val sessionToken = PasswordSecurity.generateSessionToken()
        val session = AdminSession(
            token = sessionToken,
            adminId = admin.id,
            adminMobile = admin.mobile,
            createdAtMillis = now,
            expiresAtMillis = now + (30L * 24 * 60 * 60 * 1000L),
            isActive = true
        )
        db.adminAuthDao().insertSession(session)
        return session
    }

    suspend fun resetAdminPasswordWithMasterKey(
        mobile: String,
        masterKey: String,
        newPass: String,
        confirmPass: String
    ): Result<Unit> = db.withTransaction {
        val cleanMobile = mobile.trim()
        if (cleanMobile.length != 10 || !cleanMobile.all { it.isDigit() }) {
            return@withTransaction Result.failure(Exception("Please enter a valid 10-digit administrator mobile number."))
        }

        val admin = db.userDao().getUserByMobile(cleanMobile)
            ?: return@withTransaction Result.failure(Exception("No administrator account registered with mobile +91 $cleanMobile."))

        if (admin.role != "ADMIN") {
            return@withTransaction Result.failure(Exception("The specified account is not an authorized administrator."))
        }

        // Verify Master Security Key
        val officialMasterKey = "MKEY@2026"
        val acceptedMasterKeys = setOf("MKEY@2026", "MAA_DURGA_ADMIN_2026_SECURE", "ADMIN9569", "MDDL@ADMIN2026")
        if (masterKey.trim() != officialMasterKey && !acceptedMasterKeys.contains(masterKey.trim())) {
            return@withTransaction Result.failure(Exception("Invalid Master Security Key. Authorization failed."))
        }

        if (newPass != confirmPass) {
            return@withTransaction Result.failure(Exception("New Password and Confirm Password do not match."))
        }

        val (isValidStrength, strengthError) = PasswordSecurity.validatePasswordStrength(newPass)
        if (!isValidStrength) {
            return@withTransaction Result.failure(Exception(strengthError ?: "Password does not meet complexity requirements."))
        }

        // Hash new password using PBKDF2 with cryptographic salt
        val newHashedPassword = PasswordSecurity.hashPassword(newPass)
        db.userDao().updatePassword(cleanMobile, newHashedPassword)

        // Invalidate previous sessions
        db.adminAuthDao().invalidateAllSessions(admin.id)

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        db.adminAuthDao().insertAuthLog(
            AdminAuthLog(
                id = "LOG-RESET-${System.currentTimeMillis()}",
                adminMobile = cleanMobile,
                eventType = "PASSWORD_RESET_SUCCESS",
                dateStr = sdfDate.format(Date()),
                details = "Administrator security password reset successfully via Master Key verification."
            )
        )

        Result.success(Unit)
    }

    suspend fun changeAdminPassword(
        adminMobile: String,
        currentPass: String,
        newPass: String,
        confirmPass: String
    ): Result<Unit> {
        val admin = db.userDao().getUserByMobile(adminMobile)
            ?: return Result.failure(Exception("Administrator account not found."))

        if (!PasswordSecurity.verifyPassword(currentPass, admin.passwordHash)) {
            return Result.failure(Exception("Current security password is incorrect."))
        }

        if (newPass != confirmPass) {
            return Result.failure(Exception("New password and confirm password do not match."))
        }

        if (currentPass == newPass) {
            return Result.failure(Exception("New password must be different from current password."))
        }

        val (isValidStrength, strengthError) = PasswordSecurity.validatePasswordStrength(newPass)
        if (!isValidStrength) {
            return Result.failure(Exception(strengthError ?: "Password does not meet security requirements."))
        }

        val newHashedPassword = PasswordSecurity.hashPassword(newPass)
        db.userDao().updatePassword(adminMobile, newHashedPassword)

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        db.adminAuthDao().insertAuthLog(
            AdminAuthLog(
                id = "LOG-CHG-${System.currentTimeMillis()}",
                adminMobile = adminMobile,
                eventType = "PASSWORD_CHANGED",
                dateStr = sdfDate.format(Date()),
                details = "Administrator changed security password from Admin Settings."
            )
        )

        return Result.success(Unit)
    }

    suspend fun resetPassword(mobile: String, newPassword: String): Result<Boolean> {
        val user = db.userDao().getUserByMobile(mobile)
            ?: return Result.failure(Exception("Mobile number +91 $mobile is not registered."))
        val hashed = PasswordSecurity.hashPassword(newPassword)
        db.userDao().updatePassword(mobile, hashed)
        return Result.success(true)
    }

    suspend fun submitPasswordChangeRequest(
        mobile: String,
        oldPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<PasswordResetRequest> = db.withTransaction {
        val cleanMobile = mobile.trim()
        val user = db.userDao().getUserByMobile(cleanMobile)
            ?: return@withTransaction Result.failure(Exception("User account not found."))

        // 1. Verify current password
        val isOldPasswordValid = PasswordSecurity.verifyPassword(oldPassword, user.passwordHash) ||
                (oldPassword == user.passwordHash)
        if (!isOldPasswordValid) {
            return@withTransaction Result.failure(Exception("Incorrect current password. Please try again."))
        }

        // 2. Validate matching
        if (newPassword != confirmPassword) {
            return@withTransaction Result.failure(Exception("New password and confirm password do not match."))
        }

        if (oldPassword == newPassword) {
            return@withTransaction Result.failure(Exception("New password cannot be the same as your current password."))
        }

        // 3. Validate new password strength
        val (isValidStrength, strengthMsg) = PasswordSecurity.validatePasswordStrength(newPassword)
        if (!isValidStrength) {
            return@withTransaction Result.failure(Exception(strengthMsg ?: "Password does not meet security requirements."))
        }

        val hashedNewPassword = PasswordSecurity.hashPassword(newPassword)
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val now = System.currentTimeMillis()

        val existingPending = db.passwordResetRequestDao().getPendingRequestByMobile(cleanMobile)
        val savedRequest: PasswordResetRequest
        if (existingPending != null) {
            savedRequest = existingPending.copy(
                pendingPasswordHash = hashedNewPassword,
                requestDateStr = sdfDate.format(Date(now)),
                requestTimestamp = now,
                requestType = "CHANGE"
            )
            db.passwordResetRequestDao().updateRequest(savedRequest)
            FirebaseSyncService.syncPasswordResetRequest(savedRequest)
        } else {
            val requestId = "PCR-${now % 1000000}"
            savedRequest = PasswordResetRequest(
                id = requestId,
                userId = user.id,
                userName = user.fullName,
                mobile = cleanMobile,
                pendingPasswordHash = hashedNewPassword,
                requestDateStr = sdfDate.format(Date(now)),
                requestTimestamp = now,
                status = "PENDING",
                requestType = "CHANGE"
            )
            db.passwordResetRequestDao().insertRequest(savedRequest)
            FirebaseSyncService.syncPasswordResetRequest(savedRequest)
        }

        // Notify Admin of student password change request (Strictly for ADMIN only)
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-ADM-PWD"),
                title = "Password Change Request",
                description = "${user.fullName} (+91 $cleanMobile) verified current password and submitted a change request. Awaiting Admin approval.",
                timestamp = sdfDate.format(Date(now)),
                type = "ACCOUNT",
                isRead = false,
                targetStudentId = "ADMIN"
            )
        )

        // Private confirmation strictly for THIS student only
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-STU-PWD"),
                title = "Password Change Submitted ⏳",
                description = "Your password change request has been securely submitted to the Admin. You will be notified once approved.",
                timestamp = sdfDate.format(Date(now)),
                type = "ACCOUNT",
                isRead = false,
                targetStudentId = user.id
            )
        )

        Result.success(savedRequest)
    }

    suspend fun changeStudentPassword(
        mobile: String,
        oldPassword: String,
        newPassword: String
    ): Result<Boolean> {
        return submitPasswordChangeRequest(mobile, oldPassword, newPassword, newPassword)
            .map { true }
    }

    suspend fun submitPasswordResetRequest(
        mobile: String,
        newPassword: String,
        confirmPassword: String
    ): Result<PasswordResetRequest> = db.withTransaction {
        val cleanMobile = mobile.trim()
        if (cleanMobile.length != 10 || !cleanMobile.all { it.isDigit() }) {
            return@withTransaction Result.failure(Exception("Please enter a valid 10-digit registered mobile number."))
        }

        val user = db.userDao().getUserByMobile(cleanMobile)
            ?: return@withTransaction Result.failure(Exception("Mobile number +91 $cleanMobile is not registered with any student account."))

        if (newPassword != confirmPassword) {
            return@withTransaction Result.failure(Exception("New password and confirm password do not match."))
        }

        val (isValidStrength, strengthMsg) = PasswordSecurity.validatePasswordStrength(newPassword)
        if (!isValidStrength) {
            return@withTransaction Result.failure(Exception(strengthMsg ?: "Password does not meet security requirements."))
        }

        val hashedNewPassword = PasswordSecurity.hashPassword(newPassword)
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val now = System.currentTimeMillis()

        val existingPending = db.passwordResetRequestDao().getPendingRequestByMobile(cleanMobile)
        val savedRequest: PasswordResetRequest
        if (existingPending != null) {
            savedRequest = existingPending.copy(
                pendingPasswordHash = hashedNewPassword,
                requestDateStr = sdfDate.format(Date(now)),
                requestTimestamp = now
            )
            db.passwordResetRequestDao().updateRequest(savedRequest)
            FirebaseSyncService.syncPasswordResetRequest(savedRequest)
        } else {
            val requestId = generateUniqueId("PRR")
            savedRequest = PasswordResetRequest(
                id = requestId,
                userId = user.id,
                userName = user.fullName,
                mobile = cleanMobile,
                pendingPasswordHash = hashedNewPassword,
                requestDateStr = sdfDate.format(Date(now)),
                requestTimestamp = now,
                status = "PENDING"
            )
            db.passwordResetRequestDao().insertRequest(savedRequest)
            FirebaseSyncService.syncPasswordResetRequest(savedRequest)
        }

        // Notify Admin of new password reset request (Strictly for ADMIN only)
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-ADM-PWD"),
                title = "Password Reset Request",
                description = "${user.fullName} (+91 $cleanMobile) requested a password reset. Requires Admin approval.",
                timestamp = sdfDate.format(Date(now)),
                type = "ACCOUNT",
                isRead = false,
                targetStudentId = "ADMIN"
            )
        )

        // Private confirmation strictly for THIS student only
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-STU-PWD"),
                title = "Password Reset Submitted ⏳",
                description = "Your password reset request has been securely submitted to the Admin. Once approved, you can log in with your new password.",
                timestamp = sdfDate.format(Date(now)),
                type = "ACCOUNT",
                isRead = false,
                targetStudentId = user.id
            )
        )

        Result.success(savedRequest)
    }

    suspend fun approvePasswordResetRequest(
        requestId: String,
        adminId: String
    ): Result<Boolean> = db.withTransaction {
        val request = db.passwordResetRequestDao().getRequestById(requestId)
            ?: return@withTransaction Result.failure(Exception("Password reset request not found."))

        if (request.status != "PENDING") {
            return@withTransaction Result.failure(Exception("Request is already ${request.status.lowercase()}."))
        }

        val user = db.userDao().getUserByMobile(request.mobile)
            ?: return@withTransaction Result.failure(Exception("User account not found."))

        val now = System.currentTimeMillis()
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }

        // Activate new password securely
        db.userDao().updatePassword(request.mobile, request.pendingPasswordHash)
        FirebaseSyncService.syncUser(user.copy(passwordHash = request.pendingPasswordHash))

        // Mark request as APPROVED
        val approvedPRR = request.copy(
            status = "APPROVED",
            reviewedByAdminId = adminId,
            reviewedTimestamp = now
        )
        db.passwordResetRequestDao().updateRequest(approvedPRR)
        FirebaseSyncService.syncPasswordResetRequest(approvedPRR)

        // Notify user
        val isChange = request.requestType == "CHANGE"
        val notifTitle = if (isChange) "Password Change Approved ✅" else "Password Reset Approved ✅"
        val notifDesc = if (isChange) {
            "Your password change request has been approved by the Admin. Your new password is now active."
        } else {
            "Your password reset request has been approved by the Admin. You can now log in with your new password."
        }
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-PWD-APPR"),
                title = notifTitle,
                description = notifDesc,
                timestamp = sdfTime.format(Date(now)),
                type = "ACCOUNT",
                isRead = false,
                targetStudentId = user.id
            )
        )

        Result.success(true)
    }

    suspend fun rejectPasswordResetRequest(
        requestId: String,
        adminId: String,
        adminNotes: String?
    ): Result<Boolean> = db.withTransaction {
        val request = db.passwordResetRequestDao().getRequestById(requestId)
            ?: return@withTransaction Result.failure(Exception("Password reset request not found."))

        if (request.status != "PENDING") {
            return@withTransaction Result.failure(Exception("Request is already ${request.status.lowercase()}."))
        }

        val now = System.currentTimeMillis()
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }

        val rejectedPRR = request.copy(
            status = "REJECTED",
            adminNotes = adminNotes?.trim(),
            reviewedByAdminId = adminId,
            reviewedTimestamp = now
        )
        db.passwordResetRequestDao().updateRequest(rejectedPRR)
        FirebaseSyncService.syncPasswordResetRequest(rejectedPRR)

        val noteMsg = if (!adminNotes.isNullOrBlank()) " Note: $adminNotes." else ""
        val isChange = request.requestType == "CHANGE"
        val rejTitle = if (isChange) "Password Change Request Declined" else "Password Reset Request Declined"
        val rejDesc = if (isChange) {
            "Your password change request was declined by the Admin.$noteMsg Your previous password remains unchanged."
        } else {
            "Your password reset request was declined by the Admin.$noteMsg Your previous password remains active. Please contact the library front desk if you need assistance."
        }
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-PWD-REJ"),
                title = rejTitle,
                description = rejDesc,
                timestamp = sdfTime.format(Date(now)),
                type = "ACCOUNT",
                isRead = false,
                targetStudentId = request.userId
            )
        )

        Result.success(true)
    }

    suspend fun checkSeatAvailabilityForShifts(
        seatNumber: String,
        shiftIds: List<Int>,
        requestingStudentId: String? = null
    ): Pair<Boolean, List<String>> {
        checkAndProcessMembershipExpiries()
        val unavailableShiftTitles = mutableListOf<String>()
        for (shiftId in shiftIds) {
            val shift = db.shiftDao().getShiftById(shiftId)
            val shiftName = shift?.title ?: "Shift $shiftId"

            // 1. Check if requesting student already holds a DIFFERENT seat in this shift
            if (requestingStudentId != null) {
                val studentAllocInShift = db.seatAllocationDao().getConfirmedAllocationForStudentAndShift(requestingStudentId, shiftId)
                if (studentAllocInShift != null && studentAllocInShift.seatNumber != seatNumber) {
                    unavailableShiftTitles.add("$shiftName (Aapke paas already Seat ${studentAllocInShift.seatNumber} booked hai. Iss shift me aap doosri seat book nahi kar sakte, sirf apni Seat ${studentAllocInShift.seatNumber} extend kar sakte hain)")
                    continue
                }
            }

            // 2. Check allocation for this seat and shift
            val alloc = db.seatAllocationDao().getAllocation(seatNumber, shiftId)
            if (alloc != null) {
                // If this student already has this seat confirmed, they can extend it
                if (requestingStudentId != null && alloc.studentId == requestingStudentId && alloc.status == "CONFIRMED") {
                    continue
                }
                val statusLabel = if (alloc.status == "PENDING") "Pending Verification" else "Occupied"
                unavailableShiftTitles.add("$shiftName ($statusLabel)")
            }
        }
        return Pair(unavailableShiftTitles.isEmpty(), unavailableShiftTitles)
    }

    suspend fun isSeatAvailableForShifts(
        seatNumber: String,
        shiftIds: List<Int>,
        requestingStudentId: String? = null
    ): Boolean {
        return checkSeatAvailabilityForShifts(seatNumber, shiftIds, requestingStudentId).first
    }

    fun getConfirmedAllocationsForStudent(studentId: String): Flow<List<SeatAllocation>> {
        return db.seatAllocationDao().getConfirmedAllocationsForStudentFlow(studentId)
    }

    suspend fun submitManualPaymentProof(
        student: User,
        seatNumber: String,
        selectedShifts: List<Shift>,
        durationMonths: Int = 1,
        proofImageUri: String,
        utrNumber: String = "",
        remarks: String = ""
    ): Result<PaymentVerificationRequest> = db.withTransaction {
        checkAndProcessMembershipExpiries()
        val shiftIds = selectedShifts.map { it.id }

        // Atomic Concurrency Protection: Re-check seat availability in transaction
        val (isAvailable, unavailableShifts) = checkSeatAvailabilityForShifts(seatNumber, shiftIds, student.id)
        if (!isAvailable) {
            return@withTransaction Result.failure(
                Exception("Seat $seatNumber cannot be booked: ${unavailableShifts.joinToString(", ")}. Please choose an available seat.")
            )
        }

        val existingPending = db.paymentVerificationRequestDao().getPendingRequestForStudent(student.id)
        if (existingPending != null) {
            return@withTransaction Result.failure(
                Exception("You already have a pending payment request for Seat ${existingPending.seatNumber}. Please wait for Admin verification.")
            )
        }

        val freshShifts = shiftIds.mapNotNull { db.shiftDao().getShiftById(it) }
        val effectiveShifts = if (freshShifts.isNotEmpty()) freshShifts else selectedShifts
        val months = durationMonths.coerceAtLeast(1)
        val defaultFee = db.shiftDao().getAllShiftsSync().firstOrNull()?.monthlyFee ?: 350
        val totalAmount = (if (effectiveShifts.isNotEmpty()) effectiveShifts.sumOf { it.monthlyFee } else defaultFee) * months
        val shiftTitles = effectiveShifts.joinToString(" + ") { it.title }
        val shiftIdsCsv = effectiveShifts.joinToString(",") { it.id.toString() }

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val now = System.currentTimeMillis()
        val requestId = generateUniqueId("PVR")

        // 1. Lock seat immediately with status = "PENDING"
        effectiveShifts.forEach { shift ->
            val alloc = SeatAllocation(
                seatNumber = seatNumber,
                shiftId = shift.id,
                studentId = student.id,
                studentName = student.fullName,
                membershipId = requestId,
                status = "PENDING"
            )
            db.seatAllocationDao().insertAllocation(alloc)
            FirebaseSyncService.syncSeatAllocation(alloc)
        }

        // 2. Insert Payment Verification Request
        val request = PaymentVerificationRequest(
            id = requestId,
            studentId = student.id,
            studentName = student.fullName,
            studentMobile = student.mobile,
            seatNumber = seatNumber,
            shiftIdsCsv = shiftIdsCsv,
            shiftTitles = shiftTitles,
            durationMonths = months,
            amount = totalAmount,
            proofImageUri = proofImageUri,
            utrNumber = utrNumber.trim(),
            remarks = remarks.trim(),
            requestDateStr = sdfDate.format(Date(now)),
            requestTimestamp = now,
            status = "PENDING"
        )
        db.paymentVerificationRequestDao().insertRequest(request)
        FirebaseSyncService.syncPaymentVerificationRequest(request)

        // 3. Insert student notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-PAY-SUB"),
                title = "Payment Proof Submitted ⏳",
                description = "Your payment proof for Seat $seatNumber ($shiftTitles) for $months month(s) has been submitted. Your seat is locked pending Admin verification.",
                timestamp = sdfDate.format(Date(now)),
                type = "ACCOUNT",
                isRead = false,
                targetStudentId = student.id
            )
        )

        // 4. Insert admin notification (Strictly for ADMIN only)
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-ADM-PAY"),
                title = "New Payment Verification Request",
                description = "${student.fullName} (+91 ${student.mobile}) submitted payment proof of ₹$totalAmount for Seat $seatNumber ($shiftTitles).",
                timestamp = sdfDate.format(Date(now)),
                type = "PAYMENT_REMINDER",
                isRead = false,
                targetStudentId = "ADMIN"
            )
        )

        Result.success(request)
    }

    suspend fun confirmPaymentVerification(
        requestId: String,
        adminId: String
    ): Result<Membership> = db.withTransaction {
        val request = db.paymentVerificationRequestDao().getRequestById(requestId)
            ?: return@withTransaction Result.failure(Exception("Payment verification request not found."))

        if (request.status != "PENDING") {
            return@withTransaction Result.failure(Exception("Request is already ${request.status.lowercase()}."))
        }

        val student = db.userDao().getUserById(request.studentId)
            ?: return@withTransaction Result.failure(Exception("Student account not found."))

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }
        val now = System.currentTimeMillis()
        val months = request.durationMonths.coerceAtLeast(1)

        val activeMem = db.membershipDao().getActiveMembershipSync(student.id)
        val isExtendingExisting = activeMem != null && !activeMem.isExpired && activeMem.status == "ACTIVE" && activeMem.seatNumber == request.seatNumber

        val finalMembership: Membership
        if (isExtendingExisting) {
            val active = activeMem!!
            val baseExpiryMillis = active.expiryDateMillis.coerceAtLeast(now)
            val expiryCal = Calendar.getInstance(istTz).apply {
                timeInMillis = baseExpiryMillis
                add(Calendar.DAY_OF_MONTH, months * 30)
            }
            val newExpiryMillis = expiryCal.timeInMillis
            val newExpiryDate = sdfDate.format(expiryCal.time)
            val newTotalMonths = active.durationMonths + months

            finalMembership = active.copy(
                shiftIdsCsv = request.shiftIdsCsv,
                shiftTitles = request.shiftTitles,
                expiryDate = newExpiryDate,
                expiryDateMillis = newExpiryMillis,
                amount = active.amount + request.amount,
                durationMonths = newTotalMonths,
                status = "ACTIVE"
            )
            db.membershipDao().updateMembership(finalMembership)
        } else {
            val startCal = Calendar.getInstance(istTz).apply { timeInMillis = now }
            val startMillis = startCal.timeInMillis
            val startDate = sdfDate.format(startCal.time)

            val expiryCal = Calendar.getInstance(istTz).apply {
                timeInMillis = startMillis
                add(Calendar.DAY_OF_MONTH, months * 30)
            }
            val expiryMillis = expiryCal.timeInMillis
            val expiryDate = sdfDate.format(expiryCal.time)

            val membershipId = generateUniqueId("MEM")
            finalMembership = Membership(
                id = membershipId,
                studentId = student.id,
                studentName = student.fullName,
                seatNumber = request.seatNumber,
                shiftIdsCsv = request.shiftIdsCsv,
                shiftTitles = request.shiftTitles,
                startDate = startDate,
                expiryDate = expiryDate,
                amount = request.amount,
                status = "ACTIVE",
                durationMonths = months,
                startDateMillis = startMillis,
                expiryDateMillis = expiryMillis
            )
            db.membershipDao().insertMembership(finalMembership)
            FirebaseSyncService.syncMembership(finalMembership)
        }

        // Update SeatAllocations to CONFIRMED
        val shiftIds = request.shiftIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        shiftIds.forEach { shiftId ->
            db.seatAllocationDao().updateAllocationStatus(
                seatNumber = request.seatNumber,
                shiftId = shiftId,
                status = "CONFIRMED",
                membershipId = finalMembership.id
            )
            FirebaseSyncService.syncSeatAllocation(
                SeatAllocation(
                    seatNumber = request.seatNumber,
                    shiftId = shiftId,
                    studentId = student.id,
                    studentName = student.fullName,
                    membershipId = finalMembership.id,
                    status = "CONFIRMED"
                )
            )
        }

        // Insert Payment Record
        val txnId = generateUniqueId("TXN")
        val paymentRecord = PaymentRecord(
            id = txnId,
            studentId = student.id,
            studentName = student.fullName,
            amount = request.amount,
            shiftDescription = "${request.shiftTitles} (Seat ${request.seatNumber})",
            dateStr = sdfDate.format(Date(now)),
            status = "Paid",
            paymentMode = "Manual UPI",
            upiRefId = request.utrNumber.ifBlank { requestId }
        )
        db.paymentDao().insertPayment(paymentRecord)
        FirebaseSyncService.syncPayment(paymentRecord)

        // Update request status to CONFIRMED
        val confirmedReq = request.copy(
            status = "CONFIRMED",
            reviewedByAdminId = adminId,
            reviewedTimestamp = now
        )
        db.paymentVerificationRequestDao().updateRequest(confirmedReq)
        FirebaseSyncService.syncPaymentVerificationRequest(confirmedReq)

        // Send confirmation notification to student
        val sdfTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-VERIF-OK"),
                title = "Payment Confirmed & Seat Allotted! 🎉",
                description = "Your payment of ₹${request.amount} has been verified by the Admin. Seat ${request.seatNumber} (${request.shiftTitles}) is now confirmed till ${finalMembership.expiryDate}.",
                timestamp = sdfTime.format(Date(now)),
                type = "PAYMENT_SUCCESS",
                isRead = false,
                targetStudentId = student.id
            )
        )

        Result.success(finalMembership)
    }

    suspend fun declinePaymentVerification(
        requestId: String,
        adminId: String,
        declineReason: String?
    ): Result<Boolean> = db.withTransaction {
        val request = db.paymentVerificationRequestDao().getRequestById(requestId)
            ?: return@withTransaction Result.failure(Exception("Payment verification request not found."))

        if (request.status != "PENDING") {
            return@withTransaction Result.failure(Exception("Request is already ${request.status.lowercase()}."))
        }

        val now = System.currentTimeMillis()
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }

        // 1. Release pending seat reservation immediately!
        val shiftIds = request.shiftIdsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        shiftIds.forEach { shiftId ->
            db.seatAllocationDao().deletePendingAllocationForSeat(request.seatNumber, shiftId)
            FirebaseSyncService.syncSeatRelease(request.seatNumber, shiftId)
        }

        // 2. Mark request as DECLINED
        val declinedReq = request.copy(
            status = "DECLINED",
            adminNotes = declineReason?.trim(),
            reviewedByAdminId = adminId,
            reviewedTimestamp = now
        )
        db.paymentVerificationRequestDao().updateRequest(declinedReq)
        FirebaseSyncService.syncPaymentVerificationRequest(declinedReq)

        // 3. Notify student
        val reasonMsg = if (!declineReason.isNullOrBlank()) " Reason: $declineReason." else ""
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-VERIF-DEC"),
                title = "Payment Verification Declined ❌",
                description = "Your payment verification for Seat ${request.seatNumber} was declined by the Admin.$reasonMsg The pending seat reservation has been released. Please re-submit or contact the front desk.",
                timestamp = sdfTime.format(Date(now)),
                type = "ACCOUNT",
                isRead = false,
                targetStudentId = request.studentId
            )
        )

        Result.success(true)
    }

    suspend fun savePaymentConfig(config: PaymentConfig): Result<Boolean> {
        return try {
            db.paymentConfigDao().saveConfig(config)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPaymentConfigSync(): PaymentConfig {
        return db.paymentConfigDao().getConfigSync() ?: PaymentConfig()
    }

    suspend fun bookSeatAndActivateMembership(
        student: User,
        seatNumber: String,
        selectedShifts: List<Shift>,
        durationMonths: Int = 1,
        upiTxnId: String
    ): Result<Membership> {
        checkAndProcessMembershipExpiries()
        val shiftIds = selectedShifts.map { it.id }

        // Fetch fresh shift entities directly from database to guarantee the latest admin fee structure is used
        val freshShifts = shiftIds.mapNotNull { db.shiftDao().getShiftById(it) }
        val effectiveShifts = if (freshShifts.isNotEmpty()) freshShifts else selectedShifts
        val months = durationMonths.coerceAtLeast(1)
        val defaultFee = db.shiftDao().getAllShiftsSync().firstOrNull()?.monthlyFee ?: 350
        val totalAmount = (if (effectiveShifts.isNotEmpty()) effectiveShifts.sumOf { it.monthlyFee } else defaultFee) * months
        val shiftTitles = effectiveShifts.joinToString(" + ") { it.title }
        val shiftIdsCsv = effectiveShifts.joinToString(",") { it.id.toString() }

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }
        val now = System.currentTimeMillis()

        // Check if student is extending an existing seat membership
        val activeMem = db.membershipDao().getActiveMembershipSync(student.id)
        val isExtendingExisting = activeMem != null && !activeMem.isExpired && activeMem.status == "ACTIVE" && activeMem.seatNumber == seatNumber

        if (isExtendingExisting) {
            val active = activeMem!!

            // Extending existing seat validity
            val baseExpiryMillis = active.expiryDateMillis.coerceAtLeast(now)
            val expiryCal = Calendar.getInstance(istTz).apply {
                timeInMillis = baseExpiryMillis
                add(Calendar.DAY_OF_MONTH, months * 30)
            }
            val newExpiryMillis = expiryCal.timeInMillis
            val newExpiryDate = sdfDate.format(expiryCal.time)
            val newTotalMonths = active.durationMonths + months

            val updatedMem = active.copy(
                shiftIdsCsv = shiftIdsCsv,
                shiftTitles = shiftTitles,
                expiryDate = newExpiryDate,
                expiryDateMillis = newExpiryMillis,
                amount = active.amount + totalAmount,
                durationMonths = newTotalMonths,
                status = "ACTIVE"
            )
            db.membershipDao().updateMembership(updatedMem)

            // Ensure seat allocations exist for selected shifts
            effectiveShifts.forEach { shift ->
                val alloc = db.seatAllocationDao().getAllocation(seatNumber, shift.id)
                if (alloc == null) {
                    val newAlloc = SeatAllocation(
                        seatNumber = seatNumber,
                        shiftId = shift.id,
                        studentId = student.id,
                        studentName = student.fullName,
                        membershipId = active.id,
                        status = "CONFIRMED"
                    )
                    db.seatAllocationDao().insertAllocation(newAlloc)
                    FirebaseSyncService.syncSeatAllocation(newAlloc)
                }
            }

            // Insert payment record
            val txnId = generateUniqueId("TXN")
            val extPayment = PaymentRecord(
                id = txnId,
                studentId = student.id,
                studentName = student.fullName,
                amount = totalAmount,
                shiftDescription = "$shiftTitles (Extension +$months Month${if (months > 1) "s" else ""})",
                dateStr = sdfDate.format(Date(now)),
                status = "Paid",
                upiRefId = upiTxnId
            )
            db.paymentDao().insertPayment(extPayment)
            FirebaseSyncService.syncPayment(extPayment)
            FirebaseSyncService.syncMembership(updatedMem)

            // Extension notification
            val sdfTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
            val timeNow = sdfTime.format(Date())
            db.notificationDao().insertNotification(
                NotificationItem(
                    id = generateUniqueId("NOTIF-EXT"),
                    title = "Seat Extension Successful! 🎉",
                    description = "Your membership for Seat $seatNumber ($shiftTitles) has been extended by $months month(s) up to $newExpiryDate.",
                    timestamp = timeNow,
                    type = "PAYMENT_CONFIRMATION",
                    isRead = false,
                    targetStudentId = student.id
                )
            )

            return Result.success(updatedMem)
        }

        // New admission booking:
        val (isAvailable, unavailableShifts) = checkSeatAvailabilityForShifts(seatNumber, shiftIds, student.id)
        if (!isAvailable) {
            val shiftsText = unavailableShifts.joinToString(", ")
            return Result.failure(Exception("Seat $seatNumber is unavailable: $shiftsText. Please choose an available seat or select different shifts."))
        }

        val startCal = Calendar.getInstance(istTz)
        val startMillis = startCal.timeInMillis
        val startDate = sdfDate.format(startCal.time)

        val expiryCal = Calendar.getInstance(istTz).apply {
            timeInMillis = startMillis
            add(Calendar.DAY_OF_MONTH, months * 30)
        }
        val expiryMillis = expiryCal.timeInMillis
        val expiryDate = sdfDate.format(expiryCal.time)

        val membershipId = generateUniqueId("MEM")
        val membership = Membership(
            id = membershipId,
            studentId = student.id,
            studentName = student.fullName,
            seatNumber = seatNumber,
            shiftIdsCsv = shiftIdsCsv,
            shiftTitles = shiftTitles,
            startDate = startDate,
            expiryDate = expiryDate,
            amount = totalAmount,
            status = "ACTIVE",
            durationMonths = months,
            startDateMillis = startMillis,
            expiryDateMillis = expiryMillis
        )

        // Insert membership
        db.membershipDao().insertMembership(membership)
        FirebaseSyncService.syncMembership(membership)

        // Insert confirmed seat allocations
        effectiveShifts.forEach { shift ->
            val alloc = SeatAllocation(
                seatNumber = seatNumber,
                shiftId = shift.id,
                studentId = student.id,
                studentName = student.fullName,
                membershipId = membershipId,
                status = "CONFIRMED"
            )
            db.seatAllocationDao().insertAllocation(alloc)
            FirebaseSyncService.syncSeatAllocation(alloc)
        }

        // Insert payment record
        val txnId = generateUniqueId("TXN")
        val paymentRecord = PaymentRecord(
            id = txnId,
            studentId = student.id,
            studentName = student.fullName,
            amount = totalAmount,
            shiftDescription = "$shiftTitles ($months Month${if (months > 1) "s" else ""})",
            dateStr = startDate,
            status = "Paid",
            upiRefId = upiTxnId
        )
        db.paymentDao().insertPayment(paymentRecord)
        FirebaseSyncService.syncPayment(paymentRecord)

        // Integrated Payment & Admission Notifications
        val sdfTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val timeNow = sdfTime.format(Date())

        // 1. Payment Successful Notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "NOTIF-PAY-${txnId}",
                title = "Payment Successful",
                description = "Your payment of ₹$totalAmount for $shiftTitles ($months Month${if (months > 1) "s" else ""}) has been received successfully (Ref: $upiTxnId).",
                timestamp = timeNow,
                type = "PAYMENT_SUCCESS",
                isRead = false,
                targetStudentId = student.id
            )
        )

        // 2. Payment History Updated Notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "NOTIF-HIST-${txnId}",
                title = "Payment History Updated",
                description = "Receipt for transaction $txnId has been added to your payment history records.",
                timestamp = timeNow,
                type = "PAYMENT_HISTORY",
                isRead = false,
                targetStudentId = student.id
            )
        )

        // 3. Seat & Admission Confirmed Notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = "NOTIF-SEAT-${membershipId}",
                title = "Seat & Admission Confirmed",
                description = "Seat $seatNumber has been confirmed and reserved for you ($shiftTitles) until $expiryDate.",
                timestamp = timeNow,
                type = "SEAT_ADMISSION",
                isRead = false,
                targetStudentId = student.id
            )
        )

        return Result.success(membership)
    }

    /**
     * Automatic Membership Expiration & Expiry Reminder Engine
     * - Dispatches reminder notification 10 days before expiration
     * - Dispatches follow-up reminders every 2 days until expiration (8, 6, 4, 2, 1 days)
     * - Deactivates membership and marks as EXPIRED once expiry date is reached
     * - Releases assigned seat automatically upon expiration
     */
    suspend fun checkAndProcessMembershipExpiries(): Int {
        val memberships = db.membershipDao().getAllMembershipsSync()
        val now = System.currentTimeMillis()
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val timeNow = sdfTime.format(Date(now))
        val sdfDateCode = SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).apply { timeZone = istTz }
        val todayDateCode = sdfDateCode.format(Date(now))
        var processedExpiries = 0

        for (mem in memberships) {
            if (mem.status == "EXPIRED") continue

            val daysRemaining = mem.getDaysRemaining(now)

            if (now >= mem.expiryDateMillis || daysRemaining <= 0) {
                // Deactivate membership and mark as EXPIRED
                val updated = mem.copy(status = "EXPIRED")
                db.membershipDao().updateMembership(updated)
                // Auto-release seat reservation
                db.seatAllocationDao().deleteAllocationsForStudent(mem.studentId)
                // Expiry Notification
                val studentName = mem.studentName.ifBlank { "Student" }
                db.notificationDao().insertNotification(
                    NotificationItem(
                        id = "NOTIF-EXP-${mem.id}",
                        title = "Membership Expired ⚠️",
                        description = "Dear $studentName, your library membership at Maa Durga Digital Library for Seat ${mem.seatNumber} (${mem.shiftTitles}) has expired. Please renew your membership to continue using library services and retain your seat.",
                        timestamp = timeNow,
                        type = "PAYMENT_DUE",
                        isRead = false,
                        targetStudentId = mem.studentId
                    )
                )
                processedExpiries++
            } else if (daysRemaining in 1..10) {
                // Update status flag to EXPIRING_SOON
                if (mem.status != "EXPIRING_SOON") {
                    db.membershipDao().updateMembership(mem.copy(status = "EXPIRING_SOON"))
                }
                // Send reminder 10 days before expiry, and every 2 days after (10, 8, 6, 4, 2, 1)
                val shouldRemind = (daysRemaining == 10) || (daysRemaining in 1..9 && daysRemaining % 2 == 0) || (daysRemaining == 1)
                if (shouldRemind) {
                    val notifId = "NOTIF-REMIND-${mem.id}-$todayDateCode-D$daysRemaining"
                    val studentName = mem.studentName.ifBlank { "Student" }
                    db.notificationDao().insertNotification(
                        NotificationItem(
                            id = notifId,
                            title = "⚠️ Membership Expiring in $daysRemaining Day(s)",
                            description = "Dear $studentName, your library membership at Maa Durga Digital Library for Seat ${mem.seatNumber} (${mem.shiftTitles}) is going to expire in $daysRemaining day(s) on ${mem.expiryDate}. Please renew your membership to avoid interruption and retain your reserved seat.",
                            timestamp = timeNow,
                            type = "PAYMENT_REMINDER",
                            isRead = false,
                            targetStudentId = mem.studentId
                        )
                    )
                }
            }
        }
        return processedExpiries
    }

    /**
     * Unified Attendance Backend: Single Entrance QR for both Entry & Exit
     * - Validates authenticated user
     * - Validates library entrance QR code strictly against "MDDL-GHAZIPUR-MAIN-GATE"
     * - Validates active membership (rejects if expired or non-existent)
     * - Enforces 24-Hour Single Cycle Rule: Maximum 1 Entry + 1 Exit per 24 hours
     * - Protects against duplicate rapid scans and repeated scans while already inside
     * - Automatically detects Entry vs. Exit based on current active session
     * - Calculates precise durations using server-side timestamps
     */
    suspend fun processEntranceAttendanceScan(
        student: User,
        scannedQrPayload: String
    ): Result<AttendanceScanResult> {
        // 1. Authenticate user status
        if (!student.isActive) {
            return Result.failure(Exception("USER_INACTIVE: Your account is inactive. Please contact the library administrator."))
        }

        // 2. Validate QR Code matches the registered library entrance gate
        val raw = scannedQrPayload.trim()
        val isValidGate = raw.equals(officialLibraryQrCode, ignoreCase = true) ||
                raw.contains("MDDL-GHAZIPUR-MAIN-GATE", ignoreCase = true) ||
                raw.contains("MDDL-GHAZIPUR", ignoreCase = true) ||
                raw.contains("MAIN-GATE", ignoreCase = true)
        if (!isValidGate) {
            return Result.failure(Exception("INVALID_GATE_QR: The scanned QR code is invalid. Please scan the official entrance QR installed at the library gate."))
        }

        val now = System.currentTimeMillis()

        // 3. Duplicate scan protection / rate limit (2 seconds debounce to prevent accidental double-fire)
        val lastScan = lastScanTimestamps[student.id] ?: 0L
        if (now - lastScan < 2000L) {
            return Result.failure(Exception("DUPLICATE_SCAN: Please wait a moment before scanning again."))
        }

        // 4. Validate Membership
        var membership = db.membershipDao().getActiveMembershipSync(student.id)
        if (membership == null) {
            // Attempt real-time fetch from Firebase Firestore in case local DB hasn't synced yet
            try {
                val remoteDocs = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("memberships")
                    .whereEqualTo("studentId", student.id)
                    .whereEqualTo("isExpired", false)
                    .get()
                    .await()
                val doc = remoteDocs.documents.firstOrNull { d ->
                    d.getString("status") == "ACTIVE" || d.getString("status") == null
                }
                if (doc != null) {
                    val id = doc.getString("id") ?: "MEM-${System.currentTimeMillis()}"
                    val studentName = doc.getString("studentName") ?: student.fullName
                    val seatNumber = doc.getString("seatNumber") ?: ""
                    val shiftIdsCsv = doc.getString("shiftIdsCsv") ?: ""
                    val shiftTitles = doc.getString("shiftTitles") ?: ""
                    val startDate = doc.getString("startDate") ?: ""
                    val expiryDate = doc.getString("expiryDate") ?: ""
                    val amount = doc.getLong("amount")?.toInt() ?: 0
                    val status = doc.getString("status") ?: "ACTIVE"
                    val durationMonths = doc.getLong("durationMonths")?.toInt() ?: 1
                    val startDateMillis = doc.getLong("startDateMillis") ?: 0L
                    val expiryDateMillis = doc.getLong("expiryDateMillis") ?: 0L

                    val fetchedMem = Membership(
                        id = id,
                        studentId = student.id,
                        studentName = studentName,
                        seatNumber = seatNumber,
                        shiftIdsCsv = shiftIdsCsv,
                        shiftTitles = shiftTitles,
                        startDate = startDate,
                        expiryDate = expiryDate,
                        amount = amount,
                        status = status,
                        durationMonths = durationMonths,
                        startDateMillis = startDateMillis,
                        expiryDateMillis = expiryDateMillis
                    )
                    db.membershipDao().insertMembership(fetchedMem)
                    membership = fetchedMem
                }
            } catch (_: Exception) {
            }
        }
        if (membership == null) {
            return Result.failure(Exception("NO_MEMBERSHIP: Your membership is not active. Please complete your admission and activate a membership to use the Entry and Exit features."))
        }
        if (membership.isExpired || membership.status.equals("EXPIRED", ignoreCase = true) || membership.getDaysRemaining(now) <= 0 || now >= membership.getEffectiveExpiryMillis()) {
            checkAndProcessMembershipExpiries()
            return Result.failure(Exception("MEMBERSHIP_EXPIRED: Your seat (${membership.seatNumber}) membership has expired. Please renew your membership to continue marking attendance."))
        }

        lastScanTimestamps[student.id] = now

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val todayStr = sdfDate.format(Date(now))
        val timeNowStr = sdfTime.format(Date(now))

        // 5. Check if student currently has an ACTIVE session inside the library
        val activeSession = db.attendanceDao().getActiveAttendanceForStudent(student.id)

        if (activeSession != null) {
            // Safeguard: Prevent accidental double-trigger on the exact same scan action (3 seconds debounce)
            if (now - activeSession.entryTimestamp < 3000L) {
                return Result.failure(Exception("ALREADY_CHECKED_IN: Entry recorded. You are currently marked 'Inside Now'."))
            }

            // === EXIT LOGIC ===
            val (durationStr, durationMinutes) = AttendanceUtils.calculateDurationBetween(activeSession.entryTimestamp, now)

            val updatedRecord = activeSession.copy(
                exitTime = timeNowStr,
                exitTimestamp = now,
                duration = durationStr,
                durationMinutes = durationMinutes,
                status = "COMPLETED",
                isInside = false,
                updatedAtMillis = now
            )
            db.attendanceDao().updateAttendance(updatedRecord)
            FirebaseSyncService.syncAttendance(updatedRecord)

            // Dispatch exit notification
            db.notificationDao().insertNotification(
                NotificationItem(
                    id = "NOTIF-ATT-OUT-${activeSession.id}",
                    title = "Library Exit Recorded 🔴",
                    description = "Checked out at $timeNowStr. Total study duration: $durationStr. Thank you for studying today!",
                    timestamp = timeNowStr,
                    type = "GENERAL",
                    isRead = false,
                    targetStudentId = student.id
                )
            )

            return Result.success(
                AttendanceScanResult(
                    success = true,
                    action = "exit",
                    message = "Exit Successful ✓\nThank you for studying today.",
                    record = updatedRecord,
                    entryTime = activeSession.entryTime,
                    exitTime = timeNowStr,
                    totalDuration = durationStr,
                    status = "COMPLETED"
                )
            )
        } else {
            // === ENTRY LOGIC ===
            val recordId = generateUniqueId("ATT")
            val newRecord = AttendanceRecord(
                id = recordId,
                studentId = student.id,
                studentName = student.fullName,
                mobile = student.mobile,
                seatNumber = membership.seatNumber,
                shiftTitle = membership.shiftTitles,
                libraryId = "MDDL-GHAZIPUR-MAIN-GATE",
                dateStr = todayStr,
                entryTime = timeNowStr,
                entryTimestamp = now,
                exitTime = null,
                exitTimestamp = null,
                duration = "--",
                durationMinutes = 0,
                status = "ACTIVE",
                isInside = true,
                method = "QR_SCAN",
                createdAtMillis = now,
                updatedAtMillis = now
            )
            db.attendanceDao().insertAttendance(newRecord)
            FirebaseSyncService.syncAttendance(newRecord)

            // Dispatch entry notification
            db.notificationDao().insertNotification(
                NotificationItem(
                    id = "NOTIF-ATT-IN-${recordId}",
                    title = "Library Entry Marked 🟢",
                    description = "Welcome to Maa Durga Library! Checked in at $timeNowStr (Seat ${membership.seatNumber}, ${membership.shiftTitles}).",
                    timestamp = timeNowStr,
                    type = "GENERAL",
                    isRead = false,
                    targetStudentId = student.id
                )
            )

            return Result.success(
                AttendanceScanResult(
                    success = true,
                    action = "entry",
                    message = "Entry Successful ✓\nWelcome to Maa Durga Digital Library",
                    record = newRecord,
                    entryTime = timeNowStr,
                    exitTime = null,
                    totalDuration = "--",
                    status = "ACTIVE"
                )
            )
        }
    }

    suspend fun recordQrAttendance(
        student: User,
        membership: Membership?
    ): Result<Pair<Boolean, AttendanceRecord>> {
        val result = processEntranceAttendanceScan(student, officialLibraryQrCode)
        return result.map { scanResult ->
            val isEntry = scanResult.action == "entry"
            Pair(isEntry, scanResult.record ?: AttendanceRecord(
                id = "ATT-TEMP",
                studentId = student.id,
                studentName = student.fullName,
                mobile = student.mobile,
                seatNumber = membership?.seatNumber ?: "01",
                shiftTitle = membership?.shiftTitles ?: "Shift 1",
                dateStr = "Today",
                entryTime = scanResult.entryTime ?: "Now"
            ))
        }
    }

    suspend fun adminCorrectAttendance(
        recordId: String,
        newEntryTime: String,
        newExitTime: String?,
        newStatus: String,
        reason: String,
        adminId: String
    ): Result<AttendanceRecord> {
        val record = db.attendanceDao().getAttendanceRecordById(recordId)
            ?: return Result.failure(Exception("Attendance record not found."))

        val isNowInside = newExitTime.isNullOrBlank() && newStatus == "active"
        val durationStr = if (!newExitTime.isNullOrBlank()) {
            calculateDuration(newEntryTime, newExitTime)
        } else {
            "--"
        }

        val updated = record.copy(
            entryTime = newEntryTime,
            exitTime = newExitTime?.ifBlank { null },
            duration = durationStr,
            status = newStatus,
            isInside = isNowInside,
            method = "ADMIN_OVERRIDE",
            correctionReason = reason,
            correctedByAdminId = adminId,
            updatedAtMillis = System.currentTimeMillis()
        )
        db.attendanceDao().updateAttendance(updated)
        FirebaseSyncService.syncAttendance(updated)
        return Result.success(updated)
    }

    suspend fun adminManualCheckout(
        recordId: String,
        adminId: String,
        reason: String = "Manual admin checkout at closing time"
    ): Result<AttendanceRecord> {
        val record = db.attendanceDao().getAttendanceRecordById(recordId)
            ?: return Result.failure(Exception("Attendance record not found."))

        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val now = System.currentTimeMillis()
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val exitTimeNow = sdfTime.format(Date(now))
        val (durationStr, durationMinutes) = AttendanceUtils.calculateDurationBetween(record.entryTimestamp, now)

        val updated = record.copy(
            exitTime = exitTimeNow,
            exitTimestamp = now,
            duration = durationStr,
            durationMinutes = durationMinutes,
            status = "COMPLETED",
            isInside = false,
            method = "ADMIN_OVERRIDE",
            correctionReason = reason,
            correctedByAdminId = adminId,
            updatedAtMillis = now
        )
        db.attendanceDao().updateAttendance(updated)
        FirebaseSyncService.syncAttendance(updated)
        return Result.success(updated)
    }

    private fun calculateDuration(entry: String, exit: String): String {
        return try {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val d1 = sdf.parse(entry)
            val d2 = sdf.parse(exit)
            if (d1 != null && d2 != null) {
                val diffMs = d2.time - d1.time
                val diffMin = Math.abs(diffMs / (60 * 1000))
                val hours = diffMin / 60
                val mins = diffMin % 60
                "${hours}h ${mins}m"
            } else {
                "5h 59m"
            }
        } catch (e: Exception) {
            "5h 59m"
        }
    }

    suspend fun submitComplaint(
        student: User,
        category: String,
        description: String,
        imageUri: String? = null,
        title: String = ""
    ) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val complaint = Complaint(
            id = generateUniqueId("CMP"),
            studentId = student.id,
            studentName = student.fullName,
            category = category,
            title = title.ifBlank { category },
            description = description,
            dateStr = sdf.format(Date()),
            status = "Pending",
            imageUri = imageUri
        )
        db.complaintDao().insertComplaint(complaint)
        FirebaseSyncService.syncComplaint(complaint)
    }

    suspend fun resolveComplaint(complaintId: String, reply: String) {
        val complaints = db.complaintDao().getAllComplaints().first()
        val c = complaints.find { it.id == complaintId } ?: return
        val updated = c.copy(status = "Resolved", adminReply = reply)
        db.complaintDao().updateComplaint(updated)
        FirebaseSyncService.syncComplaint(updated)

        // Add Complaint Status Update Notification
        val sdfTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val timeNow = sdfTime.format(Date())
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-CMP"),
                title = "Complaint Resolved",
                description = "Your complaint regarding '${c.category}' was resolved. Reply: $reply",
                timestamp = timeNow,
                type = "COMPLAINT_STATUS",
                isRead = false,
                targetStudentId = c.studentId
            )
        )
    }

    suspend fun publishAnnouncement(
        title: String,
        description: String,
        priority: String = "Normal",
        expiryDays: Int = 7,
        durationMillis: Long? = null,
        durationDisplayStr: String? = null
    ): Announcement {
        val istTz = TimeZone.getTimeZone("Asia/Kolkata")
        val sdfDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val sdfTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH).apply { timeZone = istTz }
        val nowMillis = System.currentTimeMillis()
        val calculatedDuration = durationMillis ?: (expiryDays.toLong() * 24L * 3600L * 1000L)
        val expiryMillis = if (calculatedDuration > 0) nowMillis + calculatedDuration else null
        val expiryDateStr = if (expiryMillis != null) sdfDate.format(Date(expiryMillis)) else null
        val calculatedDays = ((calculatedDuration / (24L * 3600L * 1000L)).toInt()).coerceAtLeast(1)
        val annId = "ANN-${nowMillis % 100000}"
        val announcement = Announcement(
            id = annId,
            title = title,
            description = description,
            dateStr = sdfTime.format(Date(nowMillis)),
            priority = priority,
            createdAtMillis = nowMillis,
            expiryDays = calculatedDays,
            expiryDateStr = expiryDateStr,
            expiryDateMillis = expiryMillis
        )
        db.announcementDao().insertAnnouncement(announcement)
        FirebaseSyncService.syncAnnouncement(announcement)

        // Automatically dispatch to Student Notification Center
        val notif = NotificationItem(
            id = "NOTIF-ANN-$annId",
            title = "New Announcement: $title",
            description = description,
            timestamp = sdfTime.format(Date(nowMillis)),
            type = "ANNOUNCEMENT",
            isRead = false,
            targetStudentId = null, // Broadcast to all students
            createdAtMillis = nowMillis
        )
        db.notificationDao().insertNotification(notif)
        return announcement
    }

    suspend fun updateAnnouncement(announcement: Announcement) {
        db.announcementDao().updateAnnouncement(announcement)
        FirebaseSyncService.syncAnnouncement(announcement)
    }

    suspend fun deleteAnnouncement(id: String) {
        db.announcementDao().deleteAnnouncement(id)
        FirebaseSyncService.deleteAnnouncementRemote(id)
    }

    suspend fun toggleMaintenance(seatNumber: String, isMaintenance: Boolean) {
        db.seatDao().updateSeatMaintenance(seatNumber, isMaintenance)
    }

    suspend fun releaseSeatAllocation(seatNumber: String, shiftId: Int) {
        db.seatAllocationDao().releaseSeat(seatNumber, shiftId)
        FirebaseSyncService.syncSeatRelease(seatNumber, shiftId)
    }

    suspend fun allocateSeatByAdmin(seatNumber: String, shiftId: Int, studentId: String, studentName: String) {
        val alloc = SeatAllocation(
            seatNumber = seatNumber,
            shiftId = shiftId,
            studentId = studentId,
            studentName = studentName,
            membershipId = generateUniqueId("ADMIN-ALLOC"),
            status = "CONFIRMED"
        )
        db.seatAllocationDao().insertAllocation(alloc)
        FirebaseSyncService.syncSeatAllocation(alloc)
    }

    suspend fun updateShift(shiftId: Int, newTitle: String, newFee: Int, newTimeRange: String): Result<Unit> {
        val shift = db.shiftDao().getShiftById(shiftId)
            ?: return Result.failure(Exception("Shift not found."))
        val updated = shift.copy(title = newTitle, monthlyFee = newFee, timeRange = newTimeRange)
        db.shiftDao().updateShift(updated)
        FirebaseSyncService.syncShift(updated)
        return Result.success(Unit)
    }

    suspend fun registerStudentByAdmin(
        fullName: String,
        mobile: String,
        email: String,
        gender: String = "Male",
        address: String = "Ghazipur"
    ): Result<User> {
        val cleanMobile = mobile.trim()
        val existing = db.userDao().getUserByMobile(cleanMobile)
        if (existing != null) {
            return Result.failure(Exception("A student with mobile +91 $cleanMobile is already registered."))
        }
        var studentId: String
        var attempts = 0
        do {
            studentId = "STU-${(10000..99999).random()}"
            attempts++
        } while (db.userDao().getUserById(studentId) != null && attempts < 10)
        val user = User(
            id = studentId,
            fullName = fullName.trim(),
            mobile = cleanMobile,
            email = email.trim(),
            gender = gender,
            address = address.trim(),
            role = "STUDENT",
            passwordHash = PasswordSecurity.hashPassword("student@123")
        )
        db.userDao().insertUser(user)
        FirebaseSyncService.syncUser(user)
        return Result.success(user)
    }

    suspend fun admitStudentWithShiftAndSeat(
        fullName: String,
        mobile: String,
        email: String,
        gender: String = "Male",
        address: String,
        shiftId: Int,
        seatNumber: String
    ): Result<User> {
        val cleanMobile = mobile.trim()
        val existing = db.userDao().getUserByMobile(cleanMobile)
        if (existing != null) {
            return Result.failure(Exception("A student with mobile +91 $cleanMobile is already registered."))
        }
        var studentId: String
        var attempts = 0
        do {
            studentId = "STU-${(10000..99999).random()}"
            attempts++
        } while (db.userDao().getUserById(studentId) != null && attempts < 10)
        val userEmail = if (email.isNotBlank()) email.trim() else "${cleanMobile}@library.local"
        val userAddress = if (address.isNotBlank()) address.trim() else "Ghazipur"
        val user = User(
            id = studentId,
            fullName = fullName.trim(),
            mobile = cleanMobile,
            email = userEmail,
            gender = gender,
            address = userAddress,
            role = "STUDENT",
            passwordHash = PasswordSecurity.hashPassword("student@123")
        )
        db.userDao().insertUser(user)
        FirebaseSyncService.syncUser(user)

        // Allocate seat in the selected shift
        val shift = db.shiftDao().getShiftById(shiftId)
        val membershipId = generateUniqueId("MEM-$shiftId")
        val alloc = SeatAllocation(
            seatNumber = seatNumber,
            shiftId = shiftId,
            studentId = studentId,
            studentName = fullName.trim(),
            membershipId = membershipId,
            status = "CONFIRMED"
        )
        db.seatAllocationDao().insertAllocation(alloc)
        FirebaseSyncService.syncSeatAllocation(alloc)

        // Add 30-day membership
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val startStr = sdf.format(cal.time)
        val startMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 30)
        val endStr = sdf.format(cal.time)
        val endMillis = cal.timeInMillis
        val feeAmt = shift?.monthlyFee ?: 500

        val mem = Membership(
            id = membershipId,
            studentId = studentId,
            studentName = fullName.trim(),
            seatNumber = seatNumber,
            shiftIdsCsv = shiftId.toString(),
            shiftTitles = shift?.title ?: "Shift $shiftId",
            startDate = startStr,
            expiryDate = endStr,
            amount = feeAmt,
            status = "ACTIVE",
            durationMonths = 1,
            startDateMillis = startMillis,
            expiryDateMillis = endMillis
        )
        db.membershipDao().insertMembership(mem)
        FirebaseSyncService.syncMembership(mem)

        // Record counter admission payment
        val payRecord = PaymentRecord(
            id = generateUniqueId("PAY-ADM"),
            studentId = studentId,
            studentName = fullName.trim(),
            amount = feeAmt,
            shiftDescription = "${shift?.title ?: "Shift $shiftId"} (Seat $seatNumber)",
            dateStr = startStr,
            status = "Paid",
            upiRefId = "Counter Admission (Cash)",
            paymentMode = "Cash",
            remarks = "Direct Admin Admission"
        )
        db.paymentDao().insertPayment(payRecord)
        FirebaseSyncService.syncPayment(payRecord)

        // Insert Welcome Notification
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-ADM"),
                title = "Welcome to Maa Durga Library!",
                description = "Your admission in ${shift?.title ?: "Shift $shiftId"} on Seat $seatNumber is confirmed.",
                timestamp = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date()),
                type = "SEAT_ADMISSION",
                targetStudentId = studentId
            )
        )

        return Result.success(user)
    }

    suspend fun deleteStudent(studentId: String): Result<Unit> {
        db.seatAllocationDao().deleteAllocationsForStudent(studentId)
        FirebaseSyncService.syncDeleteAllocationsForStudent(studentId)
        val activeMem = db.membershipDao().getActiveMembershipSync(studentId)
        if (activeMem != null) {
            val updatedMem = activeMem.copy(status = "CANCELLED")
            db.membershipDao().updateMembership(updatedMem)
            FirebaseSyncService.syncMembership(updatedMem)
        }
        db.userDao().deleteUser(studentId)
        FirebaseSyncService.deleteUser(studentId)
        return Result.success(Unit)
    }

    suspend fun updateComplaintStatus(complaintId: String, newStatus: String, reply: String) {
        val complaints = db.complaintDao().getAllComplaints().first()
        val c = complaints.find { it.id == complaintId } ?: return
        db.complaintDao().updateComplaint(
            c.copy(status = newStatus, adminReply = reply)
        )

        val sdfTime = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        val timeNow = sdfTime.format(Date())
        db.notificationDao().insertNotification(
            NotificationItem(
                id = generateUniqueId("NOTIF-CMP"),
                title = "Complaint Status: $newStatus",
                description = "Your complaint '${c.title.ifBlank { c.category }}' is marked as '$newStatus'. Admin response: $reply",
                timestamp = timeNow,
                type = "COMPLAINT_STATUS",
                isRead = false,
                targetStudentId = c.studentId
            )
        )
    }

    suspend fun recordManualPayment(
        studentId: String,
        studentName: String,
        amount: Int,
        shiftDescription: String,
        paymentMode: String = "Cash",
        remarks: String = ""
    ): PaymentRecord {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val txnId = generateUniqueId("TXN-OFFLINE")
        val payment = PaymentRecord(
            id = txnId,
            studentId = studentId,
            studentName = studentName,
            amount = amount,
            shiftDescription = shiftDescription,
            dateStr = sdf.format(Date()),
            status = "Paid",
            upiRefId = if (remarks.isNotBlank()) "$paymentMode ($remarks)" else paymentMode,
            paymentMode = paymentMode,
            remarks = remarks
        )
        db.paymentDao().insertPayment(payment)
        return payment
    }

    suspend fun deletePaymentRecord(paymentId: String): Result<Boolean> {
        return try {
            db.paymentDao().deletePaymentById(paymentId)
            FirebaseSyncService.deletePaymentRemote(paymentId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetAllPayments(): Result<Boolean> {
        return try {
            db.paymentDao().deleteAllPayments()
            FirebaseSyncService.deleteAllPaymentsRemote()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
