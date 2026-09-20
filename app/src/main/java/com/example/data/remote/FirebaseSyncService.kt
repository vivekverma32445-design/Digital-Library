package com.example.data.remote

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
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
     * Initial sync of local room database tables to Firestore.
     */
    fun syncInitialData(db: AppDatabase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
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
                Log.d(TAG, "Initial Firestore sync completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Initial Firestore sync error: ${e.message}")
            }
        }
    }
}
