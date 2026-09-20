package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Query("SELECT * FROM users WHERE role = 'STUDENT' ORDER BY id DESC")
    fun getAllStudents(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role = 'ADMIN' ORDER BY id ASC")
    fun getAllAdmins(): Flow<List<User>>

    @Query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")
    fun getAdminCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET passwordHash = :newPassword WHERE mobile = :mobile")
    suspend fun updatePassword(mobile: String, newPassword: String): Int

    @Query("SELECT * FROM users ORDER BY id DESC")
    suspend fun getAllUsersSync(): List<User>

    @Query("SELECT COUNT(*) FROM users WHERE role = 'STUDENT'")
    fun getStudentCount(): Flow<Int>

    @Update
    suspend fun updateUser(user: User)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUser(id: String)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY id ASC")
    fun getAllShifts(): Flow<List<Shift>>

    @Query("SELECT * FROM shifts ORDER BY id ASC")
    suspend fun getAllShiftsSync(): List<Shift>

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getShiftById(id: Int): Shift?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShifts(shifts: List<Shift>)

    @Update
    suspend fun updateShift(shift: Shift)
}

@Dao
interface SeatDao {
    @Query("SELECT * FROM seats ORDER BY seatNumber ASC")
    fun getAllSeats(): Flow<List<Seat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeats(seats: List<Seat>)

    @Query("UPDATE seats SET isMaintenance = :maintenance WHERE seatNumber = :seatNumber")
    suspend fun updateSeatMaintenance(seatNumber: String, maintenance: Boolean)
}

@Dao
interface SeatAllocationDao {
    @Query("SELECT * FROM seat_allocations WHERE shiftId = :shiftId")
    fun getAllocationsForShift(shiftId: Int): Flow<List<SeatAllocation>>

    @Query("SELECT * FROM seat_allocations WHERE shiftId = :shiftId AND status = :status")
    fun getAllocationsForShiftAndStatus(shiftId: Int, status: String): Flow<List<SeatAllocation>>

    @Query("SELECT * FROM seat_allocations WHERE seatNumber = :seatNumber")
    fun getAllocationsForSeat(seatNumber: String): Flow<List<SeatAllocation>>

    @Query("SELECT * FROM seat_allocations WHERE seatNumber = :seatNumber AND shiftId = :shiftId LIMIT 1")
    suspend fun getAllocation(seatNumber: String, shiftId: Int): SeatAllocation?

    @Query("SELECT * FROM seat_allocations")
    fun getAllAllocations(): Flow<List<SeatAllocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(allocation: SeatAllocation)

    @Query("UPDATE seat_allocations SET status = :status, membershipId = :membershipId WHERE seatNumber = :seatNumber AND shiftId = :shiftId")
    suspend fun updateAllocationStatus(seatNumber: String, shiftId: Int, status: String, membershipId: String)

    @Query("DELETE FROM seat_allocations WHERE studentId = :studentId")
    suspend fun deleteAllocationsForStudent(studentId: String)

    @Query("DELETE FROM seat_allocations WHERE seatNumber = :seatNumber AND shiftId = :shiftId")
    suspend fun releaseSeat(seatNumber: String, shiftId: Int)

    @Query("DELETE FROM seat_allocations WHERE studentId = :studentId AND status = 'PENDING'")
    suspend fun deletePendingAllocationsForStudent(studentId: String)

    @Query("DELETE FROM seat_allocations WHERE seatNumber = :seatNumber AND shiftId = :shiftId AND status = 'PENDING'")
    suspend fun deletePendingAllocationForSeat(seatNumber: String, shiftId: Int)
}

@Dao
interface MembershipDao {
    @Query("SELECT * FROM memberships WHERE studentId = :studentId ORDER BY id DESC LIMIT 1")
    fun getActiveMembershipForStudent(studentId: String): Flow<Membership?>

    @Query("SELECT * FROM memberships WHERE studentId = :studentId ORDER BY id DESC LIMIT 1")
    suspend fun getActiveMembershipSync(studentId: String): Membership?

    @Query("SELECT * FROM memberships ORDER BY id DESC")
    fun getAllMemberships(): Flow<List<Membership>>

    @Query("SELECT * FROM memberships ORDER BY id DESC")
    suspend fun getAllMembershipsSync(): List<Membership>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembership(membership: Membership)

    @Update
    suspend fun updateMembership(membership: Membership)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY entryTimestamp DESC")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND (isInside = 1 OR status = 'ACTIVE' OR status = 'active' OR exitTime IS NULL) ORDER BY entryTimestamp DESC LIMIT 1")
    suspend fun getActiveAttendanceForStudent(studentId: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY entryTimestamp DESC LIMIT 1")
    suspend fun getLatestAttendanceForStudent(studentId: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND (status = 'completed' OR status = 'COMPLETED') ORDER BY entryTimestamp DESC LIMIT 1")
    suspend fun getLatestCompletedAttendanceForStudent(studentId: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND dateStr = :dateStr ORDER BY entryTimestamp DESC")
    fun getTodayAttendanceRecordsForStudent(studentId: String, dateStr: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId AND dateStr = :dateStr ORDER BY entryTimestamp DESC LIMIT 1")
    suspend fun getTodayAttendanceForStudent(studentId: String, dateStr: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE id = :id LIMIT 1")
    suspend fun getAttendanceRecordById(id: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records ORDER BY entryTimestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY entryTimestamp DESC")
    suspend fun getAllAttendanceSync(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE dateStr = :dateStr ORDER BY entryTimestamp DESC")
    fun getAttendanceByDate(dateStr: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE isInside = 1 ORDER BY entryTimestamp DESC")
    fun getCurrentlyInside(): Flow<List<AttendanceRecord>>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE isInside = 1")
    fun getInsideCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT studentId) FROM attendance_records WHERE dateStr = :dateStr")
    fun getTodayPresentCount(dateStr: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord)

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteAttendance(id: String)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY id DESC")
    fun getPaymentsForStudent(studentId: String): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments ORDER BY id DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments ORDER BY id DESC")
    suspend fun getAllPaymentsSync(): List<PaymentRecord>

    @Query("SELECT SUM(amount) FROM payments WHERE status = 'Paid'")
    fun getTotalRevenue(): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord)
}

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints WHERE studentId = :studentId ORDER BY id DESC")
    fun getComplaintsForStudent(studentId: String): Flow<List<Complaint>>

    @Query("SELECT * FROM complaints ORDER BY id DESC")
    fun getAllComplaints(): Flow<List<Complaint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: Complaint)

    @Update
    suspend fun updateComplaint(complaint: Complaint)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY createdAtMillis DESC, id DESC")
    fun getAllAnnouncements(): Flow<List<Announcement>>

    @Query("SELECT * FROM announcements WHERE expiryDateMillis IS NULL OR expiryDateMillis >= :nowMillis ORDER BY createdAtMillis DESC, id DESC")
    fun getActiveAnnouncements(nowMillis: Long): Flow<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement)

    @Update
    suspend fun updateAnnouncement(announcement: Announcement)

    @Query("DELETE FROM announcements WHERE id = :id")
    suspend fun deleteAnnouncement(id: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAtMillis DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notifications WHERE targetStudentId IS NULL OR targetStudentId = :studentId ORDER BY createdAtMillis DESC")
    fun getNotificationsForStudent(studentId: String): Flow<List<NotificationItem>>

    @Query("SELECT COUNT(*) FROM notifications WHERE (targetStudentId IS NULL OR targetStudentId = :studentId) AND isRead = 0")
    fun getUnreadCount(studentId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationItem>)

    @Query("UPDATE notifications SET isRead = 1 WHERE targetStudentId IS NULL OR targetStudentId = :studentId")
    suspend fun markAllAsRead(studentId: String)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: String)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface AdminAuthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AdminSession)

    @Query("SELECT * FROM admin_sessions WHERE token = :token AND isActive = 1 LIMIT 1")
    suspend fun getActiveSession(token: String): AdminSession?

    @Query("UPDATE admin_sessions SET isActive = 0 WHERE token = :token")
    suspend fun invalidateSession(token: String)

    @Query("UPDATE admin_sessions SET isActive = 0 WHERE adminId = :adminId")
    suspend fun invalidateAllSessions(adminId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuthLog(log: AdminAuthLog)

    @Query("SELECT * FROM admin_auth_logs ORDER BY timestampMillis DESC LIMIT :limit")
    fun getRecentAuthLogs(limit: Int = 50): Flow<List<AdminAuthLog>>

}

@Dao
interface PasswordResetRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: PasswordResetRequest)

    @Update
    suspend fun updateRequest(request: PasswordResetRequest)

    @Query("SELECT * FROM password_reset_requests ORDER BY requestTimestamp DESC")
    fun getAllRequestsFlow(): Flow<List<PasswordResetRequest>>

    @Query("SELECT * FROM password_reset_requests WHERE status = 'PENDING' ORDER BY requestTimestamp DESC")
    fun getPendingRequestsFlow(): Flow<List<PasswordResetRequest>>

    @Query("SELECT * FROM password_reset_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): PasswordResetRequest?

    @Query("SELECT * FROM password_reset_requests WHERE mobile = :mobile AND status = 'PENDING' LIMIT 1")
    suspend fun getPendingRequestByMobile(mobile: String): PasswordResetRequest?

    @Query("SELECT * FROM password_reset_requests WHERE userId = :userId ORDER BY requestTimestamp DESC")
    fun getRequestsByUserFlow(userId: String): Flow<List<PasswordResetRequest>>
}

@Dao
interface PaymentVerificationRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: PaymentVerificationRequest)

    @Update
    suspend fun updateRequest(request: PaymentVerificationRequest)

    @Query("SELECT * FROM payment_verification_requests ORDER BY requestTimestamp DESC")
    fun getAllRequestsFlow(): Flow<List<PaymentVerificationRequest>>

    @Query("SELECT * FROM payment_verification_requests WHERE status = 'PENDING' ORDER BY requestTimestamp DESC")
    fun getPendingRequestsFlow(): Flow<List<PaymentVerificationRequest>>

    @Query("SELECT * FROM payment_verification_requests WHERE id = :id LIMIT 1")
    suspend fun getRequestById(id: String): PaymentVerificationRequest?

    @Query("SELECT * FROM payment_verification_requests WHERE studentId = :studentId ORDER BY requestTimestamp DESC")
    fun getRequestsByStudentFlow(studentId: String): Flow<List<PaymentVerificationRequest>>

    @Query("SELECT * FROM payment_verification_requests WHERE studentId = :studentId AND status = 'PENDING' LIMIT 1")
    suspend fun getPendingRequestForStudent(studentId: String): PaymentVerificationRequest?

    @Query("SELECT * FROM payment_verification_requests WHERE seatNumber = :seatNumber AND status = 'PENDING' LIMIT 1")
    suspend fun getPendingRequestForSeat(seatNumber: String): PaymentVerificationRequest?
}

@Dao
interface PaymentConfigDao {
    @Query("SELECT * FROM payment_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<PaymentConfig?>

    @Query("SELECT * FROM payment_config WHERE id = 1 LIMIT 1")
    suspend fun getConfigSync(): PaymentConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: PaymentConfig)
}

