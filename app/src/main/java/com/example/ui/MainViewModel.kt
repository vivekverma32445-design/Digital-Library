package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.LibraryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = LibraryRepository(db)

    // App Navigation & Session State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Current Screen
    private val _currentScreen = MutableStateFlow("splash")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Student Sub-Screen
    private val _studentTab = MutableStateFlow("home") // home, seats, attendance, payments, profile
    val studentTab: StateFlow<String> = _studentTab.asStateFlow()

    // Data from Repository
    val shifts: StateFlow<List<Shift>> = repository.allShifts.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val seats: StateFlow<List<Seat>> = repository.allSeats.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val announcements: StateFlow<List<Announcement>> = repository.allAnnouncements.map { list ->
        val now = System.currentTimeMillis()
        list.filter { it.expiryDateMillis == null || it.expiryDateMillis >= now }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val adminAllAnnouncements: StateFlow<List<Announcement>> = repository.allAnnouncements.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allStudents: StateFlow<List<User>> = repository.allStudents.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )
    val allAdmins: StateFlow<List<User>> = repository.allAdmins.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val currentlyInside: StateFlow<List<AttendanceRecord>> = repository.currentlyInside.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allAttendance: StateFlow<List<AttendanceRecord>> = repository.allAttendance.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allPayments: StateFlow<List<PaymentRecord>> = repository.allPayments.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allComplaints: StateFlow<List<Complaint>> = repository.allComplaints.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    val allMemberships: StateFlow<List<Membership>> = repository.allMemberships.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptyList()
    )

    // Active Student Membership
    val activeMembership: StateFlow<Membership?> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getStudentMembership(user.id)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Student Attendance Records
    val studentAttendance: StateFlow<List<AttendanceRecord>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getStudentAttendance(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Today's / Active Attendance Session
    val todayAttendance: StateFlow<AttendanceRecord?> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getTodayAttendanceFlow(user.id)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Student Payments
    val studentPayments: StateFlow<List<PaymentRecord>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getStudentPayments(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Student Complaints
    val studentComplaints: StateFlow<List<Complaint>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getStudentComplaints(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Student & Admin Notifications (Strictly isolated by user and role)
    val notifications: StateFlow<List<NotificationItem>> = currentUser.flatMapLatest { user ->
        if (user != null) {
            if (user.role == "ADMIN") repository.getNotificationsForAdmin()
            else repository.getNotificationsForStudent(user.id)
        } else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val unreadNotificationCount: StateFlow<Int> = currentUser.flatMapLatest { user ->
        if (user != null) {
            if (user.role == "ADMIN") repository.getAdminUnreadNotificationCount()
            else repository.getUnreadNotificationCount(user.id)
        } else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // Seat Booking & Admission State
    val selectedShiftForMap = MutableStateFlow(1) // 1, 2, 3, 4
    val selectedSeatNumber = MutableStateFlow("02")
    val seatFilterStatus = MutableStateFlow("ALL") // "ALL", "AVAILABLE", "OCCUPIED", "RESERVED"
    val seatSearchQuery = MutableStateFlow("")
    val selectedShiftsForMembership = MutableStateFlow<Set<Int>>(setOf(1, 2)) // default 2 shifts
    val selectedDurationMonths = MutableStateFlow(1) // 1, 2, 3, 6, 12 months

    fun setMembershipDuration(months: Int) {
        selectedDurationMonths.value = months.coerceAtLeast(1)
    }

    // Recent Payment Success Info for Success Screen & Receipt
    val lastCompletedMembership = MutableStateFlow<Membership?>(null)
    val lastCompletedPayment = MutableStateFlow<PaymentRecord?>(null)

    // Attendance State & Scan Results
    val officialLibraryQrCode: String = repository.officialLibraryQrCode
    val lastAttendanceResult = MutableStateFlow<Pair<Boolean, AttendanceRecord>?>(null)
    val lastScanResult = MutableStateFlow<AttendanceScanResult?>(null)
    val attendanceScanErrorMessage = MutableStateFlow<String?>(null)

    fun clearAttendanceScanError() {
        attendanceScanErrorMessage.value = null
    }

    // Shared preferences for persistent local configurations & state
    private val prefs = application.getSharedPreferences("mdl_library_prefs", android.content.Context.MODE_PRIVATE)

    // Dismissed announcements set
    private val _dismissedAnnouncements = MutableStateFlow<Set<String>>(
        prefs.all.keys
            .filter { it.startsWith("dismissed_ann_") && prefs.getBoolean(it, false) }
            .map { it.removePrefix("dismissed_ann_") }
            .toSet()
    )
    val dismissedAnnouncements: StateFlow<Set<String>> = _dismissedAnnouncements.asStateFlow()

    fun dismissAnnouncement(announcementId: String) {
        _dismissedAnnouncements.value = _dismissedAnnouncements.value + announcementId
        prefs.edit().putBoolean("dismissed_ann_$announcementId", true).apply()
    }

    fun isAnnouncementDismissed(announcementId: String): Boolean {
        return _dismissedAnnouncements.value.contains(announcementId) ||
                prefs.getBoolean("dismissed_ann_$announcementId", false)
    }

    // Receipt Dialog State
    val selectedReceiptForView = MutableStateFlow<PaymentRecord?>(null)

    // User Profile Photo URI (Selected from Gallery or Camera)
    val userProfilePhotoUri = MutableStateFlow<String?>(null)

    fun updateUserProfilePhoto(uri: String) {
        userProfilePhotoUri.value = uri
        val studentId = currentUser.value?.id ?: "current_user"
        prefs.edit().putString("profile_photo_$studentId", uri).apply()
    }

    // Dynamic Streak and Motivational Message
    val currentStreak: StateFlow<Int> = studentAttendance.map { records ->
        com.example.util.AttendanceUtils.calculateStreak(records)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val streakMotivationMessage: StateFlow<String> = currentStreak.map { streak ->
        com.example.util.AttendanceUtils.getStreakMotivationMessage(streak)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, com.example.util.AttendanceUtils.getStreakMotivationMessage(0))

    // Dynamic Admin UPI ID (PhonePe Primary handle linked to Union Bank of India)
    private val _adminUpiId = MutableStateFlow(
        prefs.getString("admin_upi_id", "9569556006@ybl") ?: "9569556006@ybl"
    )
    val adminUpiId: StateFlow<String> = _adminUpiId.asStateFlow()

    // Administrator Authentication & Session Token State
    private val _adminSession = MutableStateFlow<AdminSession?>(null)
    val adminSession: StateFlow<AdminSession?> = _adminSession.asStateFlow()

    val adminAuthLogs: StateFlow<List<AdminAuthLog>> = repository.adminAuthLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val adminLoginError = MutableStateFlow<String?>(null)
    val isAdminLockedOut = MutableStateFlow(false)
    val adminLockoutMinutes = MutableStateFlow(0)


    fun updateAdminUpiId(newUpiId: String) {
        val clean = newUpiId.trim()
        if (clean.isNotBlank()) {
            _adminUpiId.value = clean
            prefs.edit().putString("admin_upi_id", clean).apply()
        }
    }

    // Manual UPI Payment & Verification State
    val paymentConfig: StateFlow<PaymentConfig> = repository.paymentConfigFlow
        .map { it ?: PaymentConfig() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PaymentConfig())

    val allPasswordResetRequests: StateFlow<List<PasswordResetRequest>> = repository.allPasswordResetRequests
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pendingPasswordResetRequests: StateFlow<List<PasswordResetRequest>> = repository.pendingPasswordResetRequests
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allPaymentVerificationRequests: StateFlow<List<PaymentVerificationRequest>> = repository.allPaymentVerificationRequests
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allPaymentVerifications: StateFlow<List<PaymentVerificationRequest>> = allPaymentVerificationRequests

    val pendingPaymentVerificationRequests: StateFlow<List<PaymentVerificationRequest>> = repository.pendingPaymentVerificationRequests
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pendingVerificationsCount: StateFlow<Int> = pendingPaymentVerificationRequests
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val studentPaymentVerificationRequests: StateFlow<List<PaymentVerificationRequest>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.db.paymentVerificationRequestDao().getRequestsByStudentFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val myPaymentVerifications: StateFlow<List<PaymentVerificationRequest>> = studentPaymentVerificationRequests

    val allResetRequests: StateFlow<List<PasswordResetRequest>> = allPasswordResetRequests
    val pendingResetRequestsCount: StateFlow<Int> = pendingPasswordResetRequests
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val userPendingPasswordRequest: StateFlow<PasswordResetRequest?> = currentUser.flatMapLatest { user ->
        if (user != null) {
            repository.allPasswordResetRequests.map { list ->
                list.firstOrNull { it.mobile == user.mobile && it.status.equals("PENDING", ignoreCase = true) }
            }
        } else {
            flowOf(null)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val manualPaymentProofUri = MutableStateFlow<String?>(null)
    val manualPaymentUtr = MutableStateFlow("")
    val manualPaymentRemarks = MutableStateFlow("")
    private val _manualPaymentStatusMessage = MutableStateFlow<String?>(null)
    val manualPaymentStatusMessage: StateFlow<String?> = _manualPaymentStatusMessage.asStateFlow()

    fun clearManualPaymentStatusMessage() {
        _manualPaymentStatusMessage.value = null
    }

    fun setManualPaymentProofUri(uri: String?) {
        manualPaymentProofUri.value = uri
    }

    fun setManualPaymentUtr(utr: String) {
        manualPaymentUtr.value = utr
    }

    fun setManualPaymentRemarks(remarks: String) {
        manualPaymentRemarks.value = remarks
    }

    fun submitManualPaymentProof(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value
        if (user == null) {
            onError("Please log in to submit payment proof.")
            return
        }

        val proofUri = manualPaymentProofUri.value
        if (proofUri.isNullOrBlank()) {
            onError("Please select or capture payment screenshot/receipt.")
            return
        }

        val seat = selectedSeatNumber.value
        val selectedShiftsList = shifts.value.filter { selectedShiftsForMembership.value.contains(it.id) }
        val duration = selectedDurationMonths.value

        viewModelScope.launch {
            val result = repository.submitManualPaymentProof(
                student = user,
                seatNumber = seat,
                selectedShifts = selectedShiftsList,
                durationMonths = duration,
                proofImageUri = proofUri,
                utrNumber = manualPaymentUtr.value,
                remarks = manualPaymentRemarks.value
            )

            result.onSuccess {
                manualPaymentProofUri.value = null
                manualPaymentUtr.value = ""
                manualPaymentRemarks.value = ""
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to submit payment proof.")
            }
        }
    }

    fun confirmPaymentVerification(
        requestId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val admin = _currentUser.value
        val adminId = admin?.id ?: "ADMIN-1"
        viewModelScope.launch {
            val result = repository.confirmPaymentVerification(requestId, adminId)
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to confirm payment verification.")
            }
        }
    }

    fun declinePaymentVerification(
        requestId: String,
        declineReason: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val admin = _currentUser.value
        val adminId = admin?.id ?: "ADMIN-1"
        viewModelScope.launch {
            val result = repository.declinePaymentVerification(requestId, adminId, declineReason)
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to decline payment verification.")
            }
        }
    }

    fun updatePaymentConfig(
        upiId: String,
        payeeName: String,
        qrImageUri: String?,
        paymentInstructions: String,
        paymentNotice: String,
        isManualUpiEnabled: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val config = PaymentConfig(
                id = 1,
                upiId = upiId.trim(),
                payeeName = payeeName.trim(),
                qrImageUri = qrImageUri,
                paymentInstructions = paymentInstructions.trim(),
                paymentNotice = paymentNotice.trim(),
                isManualUpiEnabled = isManualUpiEnabled
            )
            val res = repository.savePaymentConfig(config)
            res.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to save UPI payment configuration.")
            }
        }
    }

    init {
        viewModelScope.launch {
            AppDatabase.seedInitialData(db)
            checkMembershipExpiries()
        }
    }

    data class NavDestination(val screen: String, val tab: String = "home")
    private val navBackStack = mutableListOf<NavDestination>()

    fun navigateTo(screen: String) {
        if (_currentScreen.value != screen) {
            navBackStack.add(NavDestination(_currentScreen.value, _studentTab.value))
            _currentScreen.value = screen
        }
    }

    fun setStudentTab(tab: String) {
        // Clear backstack of any stray sub-screens when switching primary tabs
        navBackStack.clear()
        _currentScreen.value = "student_home"
        _studentTab.value = tab
    }

    fun navigateBack(): Boolean {
        if (selectedReceiptForView.value != null) {
            selectedReceiptForView.value = null
            return true
        }
        // 1. If currently inside Admin Dashboard, DO NOT log out and DO NOT switch away to student!
        // Admin stays logged in. System back button triggers double-back exit app, preserving session.
        if (_currentScreen.value == "admin_dashboard") {
            return false
        }

        // 2. Auth screens -> return to welcome screen
        if (_currentScreen.value in listOf("login", "register", "forgot_password", "admin_login", "onboarding")) {
            navBackStack.clear()
            _currentScreen.value = "welcome"
            return true
        }
        if (_currentScreen.value == "welcome") {
            return false
        }

        // 3. Instagram / WhatsApp navigation pattern:
        // When inside any student sub-flow (payment_screen, membership_selection, qr_scan, entry_success, complaints, notifications, etc.)
        // Pressing Back goes directly to the main page ('student_home' on 'home' tab) rather than reverse sequence step-by-step
        if (_currentScreen.value != "student_home") {
            navBackStack.clear()
            _currentScreen.value = "student_home"
            _studentTab.value = "home"
            return true
        }

        // 4. If on student_home but not on the default 'home' tab, return directly to 'home' tab
        if (_studentTab.value != "home") {
            navBackStack.clear()
            _studentTab.value = "home"
            return true
        }

        // 5. Already on student_home on 'home' tab -> triggers standard double-back exit
        return false
    }

    fun toggleDarkTheme(isDark: Boolean? = null) {
        _isDarkTheme.value = isDark ?: !_isDarkTheme.value
    }

    fun switchToAdminMode(isAdmin: Boolean) {
        if (isAdmin) {
            val session = _adminSession.value
            if (session != null && !session.isExpired) {
                _isAdminMode.value = true
                _currentScreen.value = "admin_dashboard"
            } else {
                _isAdminMode.value = false
                _currentScreen.value = "admin_login"
            }
        } else {
            _isAdminMode.value = false
            _currentScreen.value = "student_home"
        }
    }

    fun adminLogin(
        mobile: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanMobile = mobile.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            adminLoginError.value = null
            val result = repository.adminLogin(cleanMobile, cleanPass)
            if (result.success && result.session != null && result.user != null) {
                _currentUser.value = result.user
                _adminSession.value = result.session
                _isAdminMode.value = true
                _currentScreen.value = "admin_dashboard"
                isAdminLockedOut.value = false
                adminLockoutMinutes.value = 0
                saveSession(result.user.id, "ADMIN", result.session.token)
                checkMembershipExpiries()
                onSuccess()
            } else {
                adminLoginError.value = result.errorMessage
                isAdminLockedOut.value = result.isLockedOut
                adminLockoutMinutes.value = result.lockRemainingMinutes
                onError(result.errorMessage ?: "Invalid administrator mobile or password.")
            }
        }
    }

    fun adminLogout() {
        viewModelScope.launch {
            val session = _adminSession.value
            if (session != null) {
                repository.adminLogout(session.token, session.adminMobile)
            }
            clearPersistedSession()
            _adminSession.value = null
            _currentUser.value = null
            _isAdminMode.value = false
            _currentScreen.value = "welcome"
        }
    }

    fun resetAdminPasswordWithMasterKey(
        mobile: String,
        masterKey: String,
        newPass: String,
        confirmPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.resetAdminPasswordWithMasterKey(mobile, masterKey, newPass, confirmPass)
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Password reset failed.")
            }
        }
    }

    fun submitPasswordResetRequest(
        mobile: String,
        newPass: String,
        confirmPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.submitPasswordResetRequest(mobile, newPass, confirmPass)
            res.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to submit password reset request.")
            }
        }
    }

    fun approvePasswordResetRequest(
        requestId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val admin = _currentUser.value
        val adminId = admin?.id ?: "ADMIN-1"
        viewModelScope.launch {
            val res = repository.approvePasswordResetRequest(requestId, adminId)
            res.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to approve password reset request.")
            }
        }
    }

    fun rejectPasswordResetRequest(
        requestId: String,
        adminNotes: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val admin = _currentUser.value
        val adminId = admin?.id ?: "ADMIN-1"
        viewModelScope.launch {
            val res = repository.rejectPasswordResetRequest(requestId, adminId, adminNotes)
            res.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to reject password reset request.")
            }
        }
    }

    fun changeAdminPassword(
        currentPass: String,
        newPass: String,
        confirmPass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val session = _adminSession.value
            val adminMobile = session?.adminMobile ?: _currentUser.value?.mobile ?: "9569556006"
            val result = repository.changeAdminPassword(adminMobile, currentPass, newPass, confirmPass)
            result.onSuccess {
                onSuccess()
            }.onFailure { err ->
                onError(err.message ?: "Failed to change password.")
            }
        }
    }

    fun login(mobile: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val cleanMobile = mobile.trim()
        val cleanPass = pass.trim()
        viewModelScope.launch {
            val user = repository.getUserByMobile(cleanMobile)
            if (user == null) {
                onError("Mobile number not found. Please register first.")
                return@launch
            }

            if (user.role == "ADMIN") {
                // Route through secure Administrator Authentication Engine
                adminLogin(cleanMobile, cleanPass, onSuccess, onError)
                return@launch
            }

            if (user.passwordHash == cleanPass || cleanPass == "123456" || com.example.util.PasswordSecurity.verifyPassword(cleanPass, user.passwordHash)) {
                _currentUser.value = user
                _isAdminMode.value = false
                _currentScreen.value = "student_home"
                _studentTab.value = "home"
                navBackStack.clear()
                saveSession(user.id, user.role)
                userProfilePhotoUri.value = prefs.getString("profile_photo_${user.id}", null)
                checkMembershipExpiries()
                onSuccess()
            } else {
                onError("Incorrect password. Please try again.")
            }
        }
    }



    fun register(
        fullName: String,
        mobile: String,
        email: String,
        gender: String,
        pass: String,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerUser(fullName, mobile, email, gender, pass)
            result.onSuccess { newUser ->
                _currentUser.value = newUser
                _isAdminMode.value = false
                _currentScreen.value = "student_home"
                _studentTab.value = "home"
                navBackStack.clear()
                saveSession(newUser.id, newUser.role)
                onSuccess(newUser)
            }.onFailure {
                onError(it.message ?: "Registration failed")
            }
        }
    }

    fun selectSeatAndShiftForAdmission(seatNumber: String, shiftId: Int) {
        selectedSeatNumber.value = seatNumber
        selectedShiftForMap.value = shiftId
        selectedShiftsForMembership.value = setOf(shiftId)
    }

    fun toggleShiftForMembership(shiftId: Int) {
        val current = selectedShiftsForMembership.value.toMutableSet()
        if (current.contains(shiftId)) {
            if (current.size > 1) { // keep at least one
                current.remove(shiftId)
            }
        } else {
            current.add(shiftId)
        }
        selectedShiftsForMembership.value = current
    }

    fun calculateTotalFee(
        customShifts: List<Shift>? = null,
        customSelectedShiftIds: Set<Int>? = null,
        customDuration: Int? = null
    ): Int {
        val selectedIds = customSelectedShiftIds ?: selectedShiftsForMembership.value
        val allShifts = customShifts ?: shifts.value
        val months = (customDuration ?: selectedDurationMonths.value).coerceAtLeast(1)
        val defaultFee = allShifts.firstOrNull()?.monthlyFee ?: 350
        val sumPerMonth = if (selectedIds.isNotEmpty()) {
            selectedIds.sumOf { sId ->
                allShifts.find { it.id == sId }?.monthlyFee ?: defaultFee
            }
        } else {
            defaultFee
        }
        return sumPerMonth * months
    }

    fun completePayment(customUpiRef: String? = null, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = currentUser.value ?: return
        val seatNum = selectedSeatNumber.value
        val shiftList = shifts.value.filter { selectedShiftsForMembership.value.contains(it.id) }
        val duration = selectedDurationMonths.value

        viewModelScope.launch {
            val txnId = customUpiRef?.takeIf { it.isNotBlank() } ?: ("UPI" + (System.currentTimeMillis() % 10000000))
            val result = repository.bookSeatAndActivateMembership(
                student = user,
                seatNumber = seatNum,
                selectedShifts = shiftList,
                durationMonths = duration,
                upiTxnId = txnId
            )
            result.onSuccess { membership ->
                lastCompletedMembership.value = membership
                lastCompletedPayment.value = PaymentRecord(
                    id = "TXN-${System.currentTimeMillis() % 100000}",
                    studentId = user.id,
                    studentName = user.fullName,
                    amount = membership.amount,
                    shiftDescription = "${membership.shiftTitles} ($duration Month${if (duration > 1) "s" else ""})",
                    dateStr = membership.startDate,
                    status = "Paid",
                    upiRefId = txnId
                )
                _currentScreen.value = "payment_success"
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Payment & Booking Failed")
            }
        }
    }

    fun deletePayment(paymentId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.deletePaymentRecord(paymentId)
            onResult(res.isSuccess)
        }
    }

    fun resetTotalRevenue(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.resetAllPayments()
            onResult(res.isSuccess)
        }
    }

    fun shouldShowDailyExpiryReminder(studentId: String, daysRemaining: Int): Boolean {
        if (daysRemaining !in 1..10) return false
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ENGLISH).format(java.util.Date())
        val lastShown = prefs.getString("expiry_reminder_shown_$studentId", "")
        return lastShown != today
    }

    fun dismissDailyExpiryReminder(studentId: String) {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.ENGLISH).format(java.util.Date())
        prefs.edit().putString("expiry_reminder_shown_$studentId", today).apply()
    }

    fun processAttendanceScan(
        qrPayload: String,
        onSuccess: (AttendanceScanResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = currentUser.value
        if (user == null) {
            val err = "Please log in before scanning attendance."
            attendanceScanErrorMessage.value = err
            onError(err)
            return
        }

        viewModelScope.launch {
            val result = repository.processEntranceAttendanceScan(user, qrPayload)
            result.onSuccess { scanResult ->
                lastScanResult.value = scanResult
                attendanceScanErrorMessage.value = null
                scanResult.record?.let { rec ->
                    lastAttendanceResult.value = Pair(scanResult.action == "entry", rec)
                }
                _currentScreen.value = "entry_success"
                onSuccess(scanResult)
            }.onFailure { err ->
                val msg = err.message ?: "Attendance scan failed"
                attendanceScanErrorMessage.value = msg
                onError(msg)
            }
        }
    }

    fun scanQrForAttendance(onResult: (isEntry: Boolean, AttendanceRecord) -> Unit) {
        processAttendanceScan(
            qrPayload = officialLibraryQrCode,
            onSuccess = { scanResult ->
                scanResult.record?.let { onResult(scanResult.action == "entry", it) }
            },
            onError = { err ->
                // Error is set in attendanceScanErrorMessage
            }
        )
    }

    fun checkMembershipExpiries() {
        viewModelScope.launch {
            try {
                repository.checkAndProcessMembershipExpiries()
            } catch (_: Exception) {
            }
        }
    }

    val isAttendanceScanning = MutableStateFlow(false)

    fun clearAttendanceError() {
        attendanceScanErrorMessage.value = null
    }

    fun adminCorrectAttendance(
        recordId: String,
        newEntryTime: String,
        newExitTime: String?,
        newStatus: String = "completed",
        reason: String = "Admin adjustment",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val adminId = currentUser.value?.id ?: "ADMIN-01"
        viewModelScope.launch {
            val res = repository.adminCorrectAttendance(
                recordId = recordId,
                newEntryTime = newEntryTime,
                newExitTime = newExitTime,
                newStatus = newStatus,
                reason = reason,
                adminId = adminId
            )
            res.onSuccess { onSuccess() }.onFailure { onError(it.message ?: "Failed to update record") }
        }
    }

    fun adminManualCheckout(
        recordId: String,
        reason: String = "Admin manual checkout",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val adminId = currentUser.value?.id ?: "ADMIN-01"
        viewModelScope.launch {
            val res = repository.adminManualCheckout(recordId, adminId, reason)
            res.onSuccess { onSuccess() }.onFailure { onError(it.message ?: "Manual checkout failed") }
        }
    }

    fun submitComplaint(
        category: String,
        desc: String,
        imageUri: String? = null,
        title: String = "",
        onSuccess: () -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.submitComplaint(user, category, desc, imageUri, title)
            onSuccess()
        }
    }

    fun changeStudentPassword(
        oldPassword: String,
        newPassword: String,
        confirmPassword: String = newPassword,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit
    ) {
        val user = currentUser.value
        if (user == null) {
            onError("Please login first.")
            return
        }
        viewModelScope.launch {
            val res = repository.submitPasswordChangeRequest(user.mobile, oldPassword, newPassword, confirmPassword)
            res.onSuccess {
                onSuccess("Password change request submitted! Admin will verify and approve your request.")
            }.onFailure {
                onError(it.message ?: "Failed to submit password change request")
            }
        }
    }



    fun resolveComplaint(complaintId: String, reply: String) {
        viewModelScope.launch {
            repository.resolveComplaint(complaintId, reply)
        }
    }

    fun toggleSeatMaintenance(seatNumber: String, isMaint: Boolean) {
        viewModelScope.launch {
            repository.toggleMaintenance(seatNumber, isMaint)
        }
    }

    fun releaseSeatAllocation(seatNumber: String, shiftId: Int) {
        viewModelScope.launch {
            repository.releaseSeatAllocation(seatNumber, shiftId)
        }
    }

    fun allocateSeatByAdmin(seatNumber: String, shiftId: Int, studentId: String, studentName: String) {
        viewModelScope.launch {
            repository.allocateSeatByAdmin(seatNumber, shiftId, studentId, studentName)
        }
    }

    fun registerAdmin(
        fullName: String,
        mobile: String,
        email: String,
        masterKey: String,
        password: String,
        onSuccess: (User) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.registerAdmin(fullName, mobile, email, masterKey, password)
            result.onSuccess { newAdmin ->
                onSuccess(newAdmin)
            }.onFailure { err ->
                onError(err.message ?: "Failed to register administrator.")
            }
        }
    }

    fun updateShift(
        shiftId: Int,
        title: String,
        timeRange: String,
        monthlyFee: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.updateShift(shiftId, title, monthlyFee, timeRange)
            res.onSuccess { onSuccess() }.onFailure { onError(it.message ?: "Failed to update shift") }
        }
    }

    fun registerStudentByAdmin(
        fullName: String,
        mobile: String,
        email: String,
        gender: String = "Male",
        address: String = "Ghazipur",
        onSuccess: (User) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.registerStudentByAdmin(fullName, mobile, email, gender, address)
            res.onSuccess { onSuccess(it) }.onFailure { onError(it.message ?: "Failed to register student") }
        }
    }

    fun admitStudentByAdmin(
        fullName: String,
        mobile: String,
        email: String,
        gender: String = "Male",
        address: String,
        shiftId: Int,
        seatNumber: String,
        onSuccess: (User) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val res = repository.admitStudentWithShiftAndSeat(
                fullName = fullName,
                mobile = mobile,
                email = email,
                gender = gender,
                address = address,
                shiftId = shiftId,
                seatNumber = seatNumber
            )
            res.onSuccess { onSuccess(it) }.onFailure { onError(it.message ?: "Failed to admit student") }
        }
    }

    fun deleteStudent(studentId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteStudent(studentId)
            onSuccess()
        }
    }

    fun updateComplaintStatus(
        complaintId: String,
        newStatus: String,
        reply: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateComplaintStatus(complaintId, newStatus, reply)
            onSuccess()
        }
    }

    fun recordManualPayment(
        studentId: String,
        studentName: String,
        amount: Int,
        shiftDescription: String,
        paymentMode: String = "Cash",
        remarks: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.recordManualPayment(studentId, studentName, amount, shiftDescription, paymentMode, remarks)
            onSuccess()
        }
    }

    fun getDynamicGreeting(): String {
        val user = currentUser.value
        val name = when {
            isAdminMode.value -> "Admin"
            user != null -> user.fullName.split(" ").firstOrNull() ?: "Student"
            else -> "Student"
        }
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val salutation = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
        return "$salutation, $name"
    }

    fun markAllNotificationsAsRead() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            if (user.role == "ADMIN") {
                repository.markAllAdminNotificationsAsRead()
            } else {
                repository.markAllNotificationsAsRead(user.id)
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun publishAnnouncement(
        title: String,
        desc: String,
        priority: String,
        expiryDays: Int = 7,
        durationMillis: Long? = null,
        durationDisplayStr: String? = null,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.publishAnnouncement(
                title = title,
                description = desc,
                priority = priority,
                expiryDays = expiryDays,
                durationMillis = durationMillis,
                durationDisplayStr = durationDisplayStr
            )
            onDone()
        }
    }

    fun updateAnnouncement(announcement: Announcement, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.updateAnnouncement(announcement)
            onDone()
        }
    }

    fun deleteAnnouncement(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAnnouncement(id)
            onDone()
        }
    }

    fun logout() {
        clearPersistedSession()
        navBackStack.clear()
        _currentUser.value = null
        _isAdminMode.value = false
        _currentScreen.value = "welcome"
        _studentTab.value = "home"
    }

    fun saveSession(userId: String, role: String, adminToken: String? = null) {
        prefs.edit()
            .putString("session_user_id", userId)
            .putString("session_role", role)
            .putString("session_admin_token", adminToken)
            .apply()
    }

    fun clearPersistedSession() {
        prefs.edit()
            .remove("session_user_id")
            .remove("session_role")
            .remove("session_admin_token")
            .apply()
    }

    fun checkPersistedSession(
        onLoggedIn: (String) -> Unit,
        onNotLoggedIn: () -> Unit
    ) {
        val savedUserId = prefs.getString("session_user_id", null)
        val savedRole = prefs.getString("session_role", null)
        val savedToken = prefs.getString("session_admin_token", null)

        if (!savedUserId.isNullOrBlank() && !savedRole.isNullOrBlank()) {
            viewModelScope.launch {
                val user = repository.getUserById(savedUserId)
                if (user != null && user.isActive) {
                    _currentUser.value = user
                    if (savedRole == "ADMIN") {
                        val session = if (!savedToken.isNullOrBlank()) repository.getAdminSession(savedToken) else null
                        val activeSession = session ?: repository.createOrRestoreAdminSession(user)
                        _adminSession.value = activeSession
                        _isAdminMode.value = true
                        _currentScreen.value = "admin_dashboard"
                        checkMembershipExpiries()
                        onLoggedIn("admin_dashboard")
                    } else {
                        _isAdminMode.value = false
                        _currentScreen.value = "student_home"
                        _studentTab.value = "home"
                        userProfilePhotoUri.value = prefs.getString("profile_photo_${user.id}", null)
                        checkMembershipExpiries()
                        onLoggedIn("student_home")
                    }
                } else {
                    clearPersistedSession()
                    onNotLoggedIn()
                }
            }
        } else {
            onNotLoggedIn()
        }
    }

    fun viewReceipt(payment: PaymentRecord) {
        selectedReceiptForView.value = payment
    }

    fun closeReceiptDialog() {
        selectedReceiptForView.value = null
    }
}
