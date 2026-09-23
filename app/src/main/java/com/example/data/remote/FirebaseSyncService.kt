package com.example.data.remote

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object FirebaseSyncService {
    private const val TAG = "FirebaseSyncService"

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var seatAllocationsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private var membershipsListener: ListenerRegistration? = null
    private var attendanceListener: ListenerRegistration? = null
    private var paymentsListener: ListenerRegistration? = null
    private var complaintsListener: ListenerRegistration? = null
    private var paymentVerificationRequestsListener: ListenerRegistration? = null
    private var passwordResetRequestsListener: ListenerRegistration? = null
    private var shiftsListener: ListenerRegistration? = null
    private var announcementsListener: ListenerRegistration? = null
    private var isRealtimeSyncStarted = false

    /**
     * Uploads or updates an announcement in Firestore collection "announcements".
     */
    fun syncAnnouncement(announcement: Announcement) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to announcement.id,
                    "title" to announcement.title,
                    "description" to announcement.description,
                    "dateStr" to announcement.dateStr,
                    "priority" to announcement.priority,
                    "createdAtMillis" to announcement.createdAtMillis,
                    "expiryDays" to announcement.expiryDays,
                    "expiryDateStr" to (announcement.expiryDateStr ?: ""),
                    "expiryDateMillis" to (announcement.expiryDateMillis ?: 0L)
                )
                firestore.collection("announcements")
                    .document(announcement.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced announcement ${announcement.id} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing announcement: ${e.message}")
            }
        }
    }

    /**
     * Deletes an announcement from Firestore collection "announcements".
     */
    fun deleteAnnouncementRemote(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("announcements")
                    .document(id)
                    .delete()
                    .await()
                Log.d(TAG, "Deleted announcement $id from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting announcement from Firestore: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates a library shift in Firestore collection "shifts".
     */
    fun syncShift(shift: Shift) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val docId = "SHIFT_${shift.id}"
                val data = hashMapOf(
                    "id" to shift.id,
                    "title" to shift.title,
                    "timeRange" to shift.timeRange,
                    "startTime" to shift.startTime,
                    "endTime" to shift.endTime,
                    "monthlyFee" to shift.monthlyFee
                )
                firestore.collection("shifts")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced shift $docId to Firestore: title=${shift.title}, fee=${shift.monthlyFee}, time=${shift.timeRange}")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing shift: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates a seat allocation in Firestore collection "seat_allocations".
     */
    fun syncSeatAllocation(allocation: SeatAllocation) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val docId = "SEAT_${allocation.seatNumber}_SHIFT_${allocation.shiftId}"
                val data = hashMapOf(
                    "seatNumber" to allocation.seatNumber,
                    "shiftId" to allocation.shiftId,
                    "studentId" to allocation.studentId,
                    "studentName" to allocation.studentName,
                    "membershipId" to allocation.membershipId,
                    "status" to allocation.status
                )
                firestore.collection("seat_allocations")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced seat allocation $docId to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing seat allocation: ${e.message}")
            }
        }
    }

    /**
     * Releases or deletes a seat allocation from Firestore collection "seat_allocations".
     */
    fun syncSeatRelease(seatNumber: String, shiftId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val docId = "SEAT_${seatNumber}_SHIFT_$shiftId"
                firestore.collection("seat_allocations")
                    .document(docId)
                    .delete()
                    .await()
                Log.d(TAG, "Released seat allocation $docId from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing seat allocation in Firestore: ${e.message}")
            }
        }
    }

    /**
     * Deletes all seat allocations for a given student from Firestore.
     */
    fun syncDeleteAllocationsForStudent(studentId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val query = firestore.collection("seat_allocations")
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .await()
                for (doc in query.documents) {
                    doc.reference.delete().await()
                }
                Log.d(TAG, "Deleted Firestore seat allocations for student $studentId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting student allocations from Firestore: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates an attendance record in Firestore collection "attendance_records".
     */
    fun syncAttendance(record: AttendanceRecord) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to record.id,
                    "studentId" to record.studentId,
                    "studentName" to record.studentName,
                    "mobile" to record.mobile,
                    "seatNumber" to record.seatNumber,
                    "shiftTitle" to record.shiftTitle,
                    "libraryId" to record.libraryId,
                    "dateStr" to record.dateStr,
                    "entryTime" to record.entryTime,
                    "entryTimestamp" to record.entryTimestamp,
                    "exitTime" to record.exitTime,
                    "exitTimestamp" to record.exitTimestamp,
                    "duration" to record.duration,
                    "durationMinutes" to record.durationMinutes,
                    "status" to record.status,
                    "isInside" to record.isInside,
                    "method" to record.method,
                    "correctionReason" to record.correctionReason,
                    "correctedByAdminId" to record.correctedByAdminId,
                    "createdAtMillis" to record.createdAtMillis,
                    "updatedAtMillis" to record.updatedAtMillis
                )
                firestore.collection("attendance_records")
                    .document(record.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced attendance record ${record.id} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing attendance record: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates a user profile in Firestore collection "users".
     */
    fun syncUser(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to user.id,
                    "fullName" to user.fullName,
                    "mobile" to user.mobile,
                    "email" to user.email,
                    "gender" to user.gender,
                    "passwordHash" to user.passwordHash,
                    "role" to user.role,
                    "address" to user.address,
                    "isActive" to user.isActive
                )
                firestore.collection("users")
                    .document(user.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced user ${user.id} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user: ${e.message}")
            }
        }
    }

    /**
     * Deletes a user from Firestore collection "users".
     */
    fun deleteUser(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users")
                    .document(userId)
                    .delete()
                    .await()
                Log.d(TAG, "Deleted user $userId from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting user from Firestore: ${e.message}")
            }
        }
    }

    /**
     * Deletes a membership from Firestore collection "memberships".
     */
    fun deleteMembership(membershipId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("memberships")
                    .document(membershipId)
                    .delete()
                    .await()
                Log.d(TAG, "Deleted membership $membershipId from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting membership from Firestore: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates a membership record in Firestore collection "memberships".
     */
    fun syncMembership(membership: Membership) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to membership.id,
                    "studentId" to membership.studentId,
                    "studentName" to membership.studentName,
                    "seatNumber" to membership.seatNumber,
                    "shiftIdsCsv" to membership.shiftIdsCsv,
                    "shiftTitles" to membership.shiftTitles,
                    "startDate" to membership.startDate,
                    "expiryDate" to membership.expiryDate,
                    "amount" to membership.amount,
                    "status" to membership.status,
                    "durationMonths" to membership.durationMonths,
                    "startDateMillis" to membership.startDateMillis,
                    "expiryDateMillis" to membership.expiryDateMillis
                )
                firestore.collection("memberships")
                    .document(membership.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced membership ${membership.id} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing membership: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates a payment record in Firestore collection "payments".
     */
    fun syncPayment(payment: PaymentRecord) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to payment.id,
                    "studentId" to payment.studentId,
                    "studentName" to payment.studentName,
                    "amount" to payment.amount,
                    "shiftDescription" to payment.shiftDescription,
                    "dateStr" to payment.dateStr,
                    "status" to payment.status,
                    "upiRefId" to payment.upiRefId,
                    "paymentMode" to payment.paymentMode,
                    "remarks" to payment.remarks
                )
                firestore.collection("payments")
                    .document(payment.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced payment ${payment.id} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing payment: ${e.message}")
            }
        }
    }

    /**
     * Deletes a payment record from Firestore collection "payments".
     */
    fun deletePaymentRemote(paymentId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("payments").document(paymentId).delete().await()
                Log.d(TAG, "Deleted payment $paymentId from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting payment: ${e.message}")
            }
        }
    }

    /**
     * Deletes all payment records from Firestore collection "payments".
     */
    fun deleteAllPaymentsRemote() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = firestore.collection("payments").get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                }
                Log.d(TAG, "Deleted all payments from Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all payments: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates a complaint in Firestore collection "complaints".
     */
    fun syncComplaint(complaint: Complaint) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to complaint.id,
                    "studentId" to complaint.studentId,
                    "studentName" to complaint.studentName,
                    "category" to complaint.category,
                    "title" to complaint.title,
                    "description" to complaint.description,
                    "dateStr" to complaint.dateStr,
                    "status" to complaint.status,
                    "adminReply" to complaint.adminReply,
                    "imageUri" to complaint.imageUri
                )
                firestore.collection("complaints")
                    .document(complaint.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced complaint ${complaint.id} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing complaint: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates a payment verification request in Firestore collection "payment_verification_requests".
     */
    fun syncPaymentVerificationRequest(request: PaymentVerificationRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to request.id,
                    "studentId" to request.studentId,
                    "studentName" to request.studentName,
                    "studentMobile" to request.studentMobile,
                    "seatNumber" to request.seatNumber,
                    "shiftIdsCsv" to request.shiftIdsCsv,
                    "shiftTitles" to request.shiftTitles,
                    "durationMonths" to request.durationMonths,
                    "amount" to request.amount,
                    "proofImageUri" to request.proofImageUri,
                    "utrNumber" to request.utrNumber,
                    "remarks" to request.remarks,
                    "requestDateStr" to request.requestDateStr,
                    "requestTimestamp" to request.requestTimestamp,
                    "status" to request.status,
                    "adminNotes" to request.adminNotes,
                    "reviewedByAdminId" to request.reviewedByAdminId,
                    "reviewedTimestamp" to request.reviewedTimestamp
                )
                firestore.collection("payment_verification_requests")
                    .document(request.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced payment verification request ${request.id} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing payment verification request: ${e.message}")
            }
        }
    }

    /**
     * Uploads or updates a password reset request in Firestore collection "password_reset_requests".
     */
    fun syncPasswordResetRequest(request: PasswordResetRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = hashMapOf(
                    "id" to request.id,
                    "userId" to request.userId,
                    "userName" to request.userName,
                    "mobile" to request.mobile,
                    "pendingPasswordHash" to request.pendingPasswordHash,
                    "requestDateStr" to request.requestDateStr,
                    "requestTimestamp" to request.requestTimestamp,
                    "status" to request.status,
                    "adminNotes" to request.adminNotes,
                    "reviewedByAdminId" to request.reviewedByAdminId,
                    "reviewedTimestamp" to request.reviewedTimestamp,
                    "requestType" to request.requestType
                )
                firestore.collection("password_reset_requests")
                    .document(request.id)
                    .set(data, SetOptions.merge())
                    .await()
                Log.d(TAG, "Synced password reset request ${request.id} to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing password reset request: ${e.message}")
            }
        }
    }

    /**
     * Fetches a user directly from Firestore by mobile number as fallback during login.
     */
    suspend fun fetchUserByMobile(mobile: String): User? {
        return try {
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("mobile", mobile)
                .limit(1)
                .get()
                .await()
            if (!querySnapshot.isEmpty) {
                val doc = querySnapshot.documents[0]
                User(
                    id = doc.getString("id") ?: "",
                    fullName = doc.getString("fullName") ?: "",
                    mobile = doc.getString("mobile") ?: "",
                    email = doc.getString("email") ?: "",
                    gender = doc.getString("gender") ?: "",
                    passwordHash = doc.getString("passwordHash") ?: "",
                    role = doc.getString("role") ?: "STUDENT",
                    address = doc.getString("address") ?: "Mania Deval Road, Ghazipur",
                    isActive = doc.getBoolean("isActive") ?: true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user by mobile from Firestore: ${e.message}")
            null
        }
    }

    /**
     * Initial sync of local room database tables to Firestore.
     */
    fun syncInitialData(db: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allocations = db.seatAllocationDao().getAllAllocationsSync()
                for (alloc in allocations) {
                    syncSeatAllocation(alloc)
                }

                val users = db.userDao().getAllUsersSync()
                for (u in users) {
                    syncUser(u)
                }

                val attendances = db.attendanceDao().getAllAttendanceSync()
                for (a in attendances) {
                    syncAttendance(a)
                }

                val memberships = db.membershipDao().getAllMembershipsSync()
                for (m in memberships) {
                    syncMembership(m)
                }

                val payments = db.paymentDao().getAllPaymentsSync()
                for (p in payments) {
                    syncPayment(p)
                }

                val pvrRequests = db.paymentVerificationRequestDao().getAllRequestsSync()
                for (req in pvrRequests) {
                    syncPaymentVerificationRequest(req)
                }

                val prrRequests = db.passwordResetRequestDao().getAllRequestsSync()
                for (prr in prrRequests) {
                    syncPasswordResetRequest(prr)
                }

                // Initial sync for shifts: fetch latest from Firestore if exists, else seed from local
                try {
                    val remoteShifts = firestore.collection("shifts").get().await()
                    if (remoteShifts.isEmpty) {
                        val localShifts = db.shiftDao().getAllShiftsSync()
                        for (s in localShifts) {
                            syncShift(s)
                        }
                    } else {
                        for (doc in remoteShifts.documents) {
                            val id = doc.getLong("id")?.toInt() ?: continue
                            val title = doc.getString("title") ?: continue
                            val timeRange = doc.getString("timeRange") ?: ""
                            val startTime = doc.getString("startTime") ?: ""
                            val endTime = doc.getString("endTime") ?: ""
                            val monthlyFee = doc.getLong("monthlyFee")?.toInt() ?: 350
                            val shift = Shift(
                                id = id,
                                title = title,
                                timeRange = timeRange,
                                startTime = startTime,
                                endTime = endTime,
                                monthlyFee = monthlyFee
                            )
                            db.shiftDao().insertShift(shift)
                        }
                    }
                } catch (se: Exception) {
                    Log.e(TAG, "Error syncing initial shifts: ${se.message}")
                }

                // Initial sync for announcements
                try {
                    val remoteAnnouncements = firestore.collection("announcements").get().await()
                    if (remoteAnnouncements.isEmpty) {
                        val localAnnouncements = db.announcementDao().getAllAnnouncementsSync()
                        for (a in localAnnouncements) {
                            syncAnnouncement(a)
                        }
                    } else {
                        for (doc in remoteAnnouncements.documents) {
                            val id = doc.getString("id") ?: continue
                            val title = doc.getString("title") ?: continue
                            val description = doc.getString("description") ?: ""
                            val dateStr = doc.getString("dateStr") ?: ""
                            val priority = doc.getString("priority") ?: "Normal"
                            val createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                            val expiryDays = doc.getLong("expiryDays")?.toInt() ?: 7
                            val expiryDateStr = doc.getString("expiryDateStr")
                            val expiryDateMillis = doc.getLong("expiryDateMillis")

                            val ann = Announcement(
                                id = id,
                                title = title,
                                description = description,
                                dateStr = dateStr,
                                priority = priority,
                                createdAtMillis = createdAtMillis,
                                expiryDays = expiryDays,
                                expiryDateStr = expiryDateStr,
                                expiryDateMillis = expiryDateMillis
                            )
                            db.announcementDao().insertAnnouncement(ann)
                        }
                    }
                } catch (ae: Exception) {
                    Log.e(TAG, "Error syncing initial announcements: ${ae.message}")
                }
                Log.d(TAG, "Initial Firestore sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Initial Firestore sync error: ${e.message}")
            }
        }
    }

    /**
     * Starts persistent Realtime Listeners for all Firestore collections.
     * Ensures any update on any device (e.g. seat booking, registration, attendance)
     * is instantaneously reflected on all other devices in real time.
     */
    fun startRealtimeSync(db: AppDatabase) {
        if (isRealtimeSyncStarted) return
        isRealtimeSyncStarted = true
        Log.d(TAG, "Starting two-way Realtime Firestore Sync listeners")

        // 1. SEAT ALLOCATIONS REALTIME LISTENER
        seatAllocationsListener = firestore.collection("seat_allocations")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Seat allocations listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val seatNumber = doc.getString("seatNumber") ?: continue
                            val shiftId = doc.getLong("shiftId")?.toInt() ?: continue
                            val studentId = doc.getString("studentId") ?: ""
                            val studentName = doc.getString("studentName") ?: ""
                            val membershipId = doc.getString("membershipId") ?: ""
                            val status = doc.getString("status") ?: "CONFIRMED"

                            val existing = db.seatAllocationDao().getAllocation(seatNumber, shiftId)
                            db.seatAllocationDao().insertAllocation(
                                SeatAllocation(
                                    id = existing?.id ?: 0,
                                    seatNumber = seatNumber,
                                    shiftId = shiftId,
                                    studentId = studentId,
                                    studentName = studentName,
                                    membershipId = membershipId,
                                    status = status
                                )
                            )
                        }
                        for (change in snapshots.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                val seatNumber = change.document.getString("seatNumber")
                                val shiftId = change.document.getLong("shiftId")?.toInt()
                                if (seatNumber != null && shiftId != null) {
                                    db.seatAllocationDao().releaseSeat(seatNumber, shiftId)
                                }
                            }
                        }
                    }
                }
            }

        // 2. USERS REALTIME LISTENER
        usersListener = firestore.collection("users")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Users listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            val fullName = doc.getString("fullName") ?: ""
                            val mobile = doc.getString("mobile") ?: ""
                            val email = doc.getString("email") ?: ""
                            val gender = doc.getString("gender") ?: ""
                            val passwordHash = doc.getString("passwordHash") ?: ""
                            val role = doc.getString("role") ?: "STUDENT"
                            val address = doc.getString("address") ?: "Mania Deval Road, Ghazipur"
                            val isActive = doc.getBoolean("isActive") ?: true

                            val user = User(
                                id = id,
                                fullName = fullName,
                                mobile = mobile,
                                email = email,
                                gender = gender,
                                passwordHash = passwordHash,
                                role = role,
                                address = address,
                                isActive = isActive
                            )
                            db.userDao().insertUser(user)
                        }
                        for (change in snapshots.documentChanges) {
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.REMOVED) {
                                val id = change.document.getString("id")
                                if (id != null) {
                                    db.userDao().deleteUser(id)
                                }
                            }
                        }
                    }
                }
            }

        // 3. MEMBERSHIPS REALTIME LISTENER
        membershipsListener = firestore.collection("memberships")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Memberships listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            val studentId = doc.getString("studentId") ?: ""
                            val studentName = doc.getString("studentName") ?: ""
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

                            val membership = Membership(
                                id = id,
                                studentId = studentId,
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
                            db.membershipDao().insertMembership(membership)
                        }
                    }
                }
            }

        // 4. ATTENDANCE RECORDS REALTIME LISTENER
        attendanceListener = firestore.collection("attendance_records")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Attendance listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            val studentId = doc.getString("studentId") ?: ""
                            val studentName = doc.getString("studentName") ?: ""
                            val mobile = doc.getString("mobile") ?: ""
                            val seatNumber = doc.getString("seatNumber") ?: ""
                            val shiftTitle = doc.getString("shiftTitle") ?: ""
                            val libraryId = doc.getString("libraryId") ?: ""
                            val dateStr = doc.getString("dateStr") ?: ""
                            val entryTime = doc.getString("entryTime") ?: ""
                            val entryTimestamp = doc.getLong("entryTimestamp") ?: 0L
                            val exitTime = doc.getString("exitTime")
                            val exitTimestamp = doc.getLong("exitTimestamp")
                            val duration = doc.getString("duration") ?: "--"
                            val durationMinutes = doc.getLong("durationMinutes") ?: 0L
                            val status = doc.getString("status") ?: "ACTIVE"
                            val isInside = doc.getBoolean("isInside") ?: true
                            val method = doc.getString("method") ?: "QR"
                            val correctionReason = doc.getString("correctionReason")
                            val correctedByAdminId = doc.getString("correctedByAdminId")
                            val createdAtMillis = doc.getLong("createdAtMillis") ?: 0L
                            val updatedAtMillis = doc.getLong("updatedAtMillis") ?: 0L

                            val record = AttendanceRecord(
                                id = id,
                                studentId = studentId,
                                studentName = studentName,
                                mobile = mobile,
                                seatNumber = seatNumber,
                                shiftTitle = shiftTitle,
                                libraryId = libraryId,
                                dateStr = dateStr,
                                entryTime = entryTime,
                                entryTimestamp = entryTimestamp,
                                exitTime = exitTime,
                                exitTimestamp = exitTimestamp,
                                duration = duration,
                                durationMinutes = durationMinutes,
                                status = status,
                                isInside = isInside,
                                method = method,
                                correctionReason = correctionReason,
                                correctedByAdminId = correctedByAdminId,
                                createdAtMillis = createdAtMillis,
                                updatedAtMillis = updatedAtMillis
                            )
                            db.attendanceDao().insertAttendance(record)
                        }
                    }
                }
            }

        // 5. PAYMENTS REALTIME LISTENER
        paymentsListener = firestore.collection("payments")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Payments listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            val studentId = doc.getString("studentId") ?: ""
                            val studentName = doc.getString("studentName") ?: ""
                            val amount = doc.getLong("amount")?.toInt() ?: 0
                            val shiftDescription = doc.getString("shiftDescription") ?: ""
                            val dateStr = doc.getString("dateStr") ?: ""
                            val status = doc.getString("status") ?: "Paid"
                            val upiRefId = doc.getString("upiRefId") ?: ""
                            val paymentMode = doc.getString("paymentMode") ?: "UPI"
                            val remarks = doc.getString("remarks") ?: ""

                            val p = PaymentRecord(
                                id = id,
                                studentId = studentId,
                                studentName = studentName,
                                amount = amount,
                                shiftDescription = shiftDescription,
                                dateStr = dateStr,
                                status = status,
                                upiRefId = upiRefId,
                                paymentMode = paymentMode,
                                remarks = remarks
                            )
                            db.paymentDao().insertPayment(p)
                        }
                    }
                }
            }

        // 6. COMPLAINTS REALTIME LISTENER
        complaintsListener = firestore.collection("complaints")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Complaints listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            val studentId = doc.getString("studentId") ?: ""
                            val studentName = doc.getString("studentName") ?: ""
                            val category = doc.getString("category") ?: ""
                            val title = doc.getString("title") ?: ""
                            val description = doc.getString("description") ?: ""
                            val dateStr = doc.getString("dateStr") ?: ""
                            val status = doc.getString("status") ?: "PENDING"
                            val adminReply = doc.getString("adminReply")
                            val imageUri = doc.getString("imageUri")

                            val c = Complaint(
                                id = id,
                                studentId = studentId,
                                studentName = studentName,
                                category = category,
                                title = title,
                                description = description,
                                dateStr = dateStr,
                                status = status,
                                adminReply = adminReply,
                                imageUri = imageUri
                            )
                            db.complaintDao().insertComplaint(c)
                        }
                    }
                }
            }

        // 7. PAYMENT VERIFICATION REQUESTS REALTIME LISTENER
        paymentVerificationRequestsListener = firestore.collection("payment_verification_requests")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Payment verification requests listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            val studentId = doc.getString("studentId") ?: ""
                            val studentName = doc.getString("studentName") ?: ""
                            val studentMobile = doc.getString("studentMobile") ?: ""
                            val seatNumber = doc.getString("seatNumber") ?: ""
                            val shiftIdsCsv = doc.getString("shiftIdsCsv") ?: ""
                            val shiftTitles = doc.getString("shiftTitles") ?: ""
                            val durationMonths = doc.getLong("durationMonths")?.toInt() ?: 1
                            val amount = doc.getLong("amount")?.toInt() ?: 0
                            val proofImageUri = doc.getString("proofImageUri") ?: ""
                            val utrNumber = doc.getString("utrNumber") ?: ""
                            val remarks = doc.getString("remarks") ?: ""
                            val requestDateStr = doc.getString("requestDateStr") ?: ""
                            val requestTimestamp = doc.getLong("requestTimestamp") ?: 0L
                            val status = doc.getString("status") ?: "PENDING"
                            val adminNotes = doc.getString("adminNotes")
                            val reviewedByAdminId = doc.getString("reviewedByAdminId")
                            val reviewedTimestamp = doc.getLong("reviewedTimestamp")

                            val req = PaymentVerificationRequest(
                                id = id,
                                studentId = studentId,
                                studentName = studentName,
                                studentMobile = studentMobile,
                                seatNumber = seatNumber,
                                shiftIdsCsv = shiftIdsCsv,
                                shiftTitles = shiftTitles,
                                durationMonths = durationMonths,
                                amount = amount,
                                proofImageUri = proofImageUri,
                                utrNumber = utrNumber,
                                remarks = remarks,
                                requestDateStr = requestDateStr,
                                requestTimestamp = requestTimestamp,
                                status = status,
                                adminNotes = adminNotes,
                                reviewedByAdminId = reviewedByAdminId,
                                reviewedTimestamp = reviewedTimestamp
                            )
                            db.paymentVerificationRequestDao().insertRequest(req)
                        }
                    }
                }
            }

        // 8. PASSWORD RESET REQUESTS REALTIME LISTENER
        passwordResetRequestsListener = firestore.collection("password_reset_requests")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Password reset requests listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            val userId = doc.getString("userId") ?: ""
                            val userName = doc.getString("userName") ?: ""
                            val mobile = doc.getString("mobile") ?: ""
                            val pendingPasswordHash = doc.getString("pendingPasswordHash") ?: ""
                            val requestDateStr = doc.getString("requestDateStr") ?: ""
                            val requestTimestamp = doc.getLong("requestTimestamp") ?: 0L
                            val status = doc.getString("status") ?: "PENDING"
                            val adminNotes = doc.getString("adminNotes")
                            val reviewedByAdminId = doc.getString("reviewedByAdminId")
                            val reviewedTimestamp = doc.getLong("reviewedTimestamp")
                            val requestType = doc.getString("requestType") ?: "RESET"

                            val prr = PasswordResetRequest(
                                id = id,
                                userId = userId,
                                userName = userName,
                                mobile = mobile,
                                pendingPasswordHash = pendingPasswordHash,
                                requestDateStr = requestDateStr,
                                requestTimestamp = requestTimestamp,
                                status = status,
                                adminNotes = adminNotes,
                                reviewedByAdminId = reviewedByAdminId,
                                reviewedTimestamp = reviewedTimestamp,
                                requestType = requestType
                            )
                            db.passwordResetRequestDao().insertRequest(prr)
                        }
                    }
                }
            }

        // 9. SHIFTS REALTIME LISTENER
        shiftsListener = firestore.collection("shifts")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Shifts listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        for (doc in snapshots.documents) {
                            val id = doc.getLong("id")?.toInt() ?: continue
                            val title = doc.getString("title") ?: continue
                            val timeRange = doc.getString("timeRange") ?: ""
                            val startTime = doc.getString("startTime") ?: ""
                            val endTime = doc.getString("endTime") ?: ""
                            val monthlyFee = doc.getLong("monthlyFee")?.toInt() ?: 350
                            val shift = Shift(
                                id = id,
                                title = title,
                                timeRange = timeRange,
                                startTime = startTime,
                                endTime = endTime,
                                monthlyFee = monthlyFee
                            )
                            db.shiftDao().insertShift(shift)
                        }
                    }
                }
            }

        // 10. ANNOUNCEMENTS REALTIME LISTENER
        announcementsListener = firestore.collection("announcements")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e(TAG, "Announcements listener error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        // Handle deleted announcements
                        for (change in snapshots.documentChanges) {
                            if (change.type == DocumentChange.Type.REMOVED) {
                                val removedId = change.document.getString("id") ?: change.document.id
                                db.announcementDao().deleteAnnouncement(removedId)
                            }
                        }
                        // Handle added or modified announcements
                        for (doc in snapshots.documents) {
                            val id = doc.getString("id") ?: continue
                            val title = doc.getString("title") ?: continue
                            val description = doc.getString("description") ?: ""
                            val dateStr = doc.getString("dateStr") ?: ""
                            val priority = doc.getString("priority") ?: "Normal"
                            val createdAtMillis = doc.getLong("createdAtMillis") ?: System.currentTimeMillis()
                            val expiryDays = doc.getLong("expiryDays")?.toInt() ?: 7
                            val expiryDateStr = doc.getString("expiryDateStr")
                            val expiryDateMillis = doc.getLong("expiryDateMillis")

                            val ann = Announcement(
                                id = id,
                                title = title,
                                description = description,
                                dateStr = dateStr,
                                priority = priority,
                                createdAtMillis = createdAtMillis,
                                expiryDays = expiryDays,
                                expiryDateStr = expiryDateStr,
                                expiryDateMillis = expiryDateMillis
                            )
                            db.announcementDao().insertAnnouncement(ann)

                            // Also mirror to notifications
                            val notif = NotificationItem(
                                id = "NOTIF-ANN-$id",
                                title = "Announcement: $title",
                                description = description,
                                timestamp = dateStr,
                                type = "ANNOUNCEMENT",
                                isRead = false,
                                targetStudentId = null,
                                createdAtMillis = createdAtMillis
                            )
                            db.notificationDao().insertNotification(notif)
                        }
                    }
                }
            }
    }
}

