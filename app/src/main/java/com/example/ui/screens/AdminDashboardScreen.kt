package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.MainViewModel
import com.example.ui.components.DynamicUpiQrCanvas
import com.example.ui.theme.*
import com.example.util.QrDownloadUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onBackToStudent: () -> Unit
) {
    val context = LocalContext.current
    var adminTab by remember { mutableStateOf("Dashboard") }
    var selectedShiftForAdmin by remember { mutableIntStateOf(1) }
    var searchQuery by remember { mutableStateOf("") }
    var showSeatDetailDialog by remember { mutableStateOf<String?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val allStudents by viewModel.allStudents.collectAsState()
    val currentlyInside by viewModel.currentlyInside.collectAsState()
    val allPayments by viewModel.allPayments.collectAsState()
    val allComplaints by viewModel.allComplaints.collectAsState()
    val allMemberships by viewModel.allMemberships.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val seats by viewModel.seats.collectAsState()
    val allocations by viewModel.repository.getAllocationsForShift(selectedShiftForAdmin).collectAsState(emptyList())
    val allAllocations by viewModel.repository.allAllocations.collectAsState(emptyList())
    val pendingVerificationsCount by viewModel.pendingVerificationsCount.collectAsState()
    val pendingResetRequestsCount by viewModel.pendingResetRequestsCount.collectAsState()

    val istTz = TimeZone.getTimeZone("Asia/Kolkata")
    val todayDateStr = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = istTz }.format(Date())
    val todayPaidList = allPayments.filter { it.dateStr == todayDateStr && it.status == "Paid" }
    val todayCollectionVal = todayPaidList.sumOf { it.amount }
    val totalPaidVal = allPayments.filter { it.status == "Paid" }.sumOf { it.amount }
    val todayCollectionDisplay = if (todayCollectionVal > 0) "₹${String.format("%,d", todayCollectionVal)}" else "₹${String.format("%,d", totalPaidVal)}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Admin Header
        Surface(
            color = Color(0xFF0F3615),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("DL", fontWeight = FontWeight.ExtraBold, color = PrimaryGreen, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Maa Durga Digital Library",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Admin Management Console",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Switch to Student App Button
                    TextButton(
                        onClick = onBackToStudent,
                        colors = ButtonDefaults.textButtonColors(contentColor = EmeraldAccent)
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Student App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.width(6.dp))

                    IconButton(onClick = { viewModel.toggleDarkTheme() }) {
                        val isDark by viewModel.isDarkTheme.collectAsState()
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = Color.White
                        )
                    }

                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout Admin",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Logout", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        val tabs = listOf("Dashboard", "Verifications", "Password Requests", "Announcements", "Students", "Seats", "Shifts", "Attendance", "Payments", "Complaints")

        // Horizontal Navigation Tabs for Admin Sections
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(adminTab).coerceAtLeast(0),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryGreen
        ) {
            tabs.forEach { tabName ->
                val badgeCount = when (tabName) {
                    "Verifications" -> pendingVerificationsCount
                    "Password Requests" -> pendingResetRequestsCount
                    else -> 0
                }
                Tab(
                    selected = adminTab == tabName,
                    onClick = { adminTab = tabName },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tabName,
                                fontSize = 13.sp,
                                fontWeight = if (adminTab == tabName) FontWeight.Bold else FontWeight.Normal
                            )
                            if (badgeCount > 0) {
                                Spacer(Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = if (tabName == "Verifications") Color(0xFFE65100) else Color(0xFF1565C0),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (badgeCount > 99) "99+" else "$badgeCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // Tab Content
        when (adminTab) {
            "Dashboard" -> AdminOverviewContent(
                totalStudentsCount = allStudents.size,
                presentCount = currentlyInside.size,
                totalSeats = seats.size.coerceAtLeast(36),
                todayCollection = todayCollectionDisplay,
                totalRevenueAmount = totalPaidVal,
                allAllocations = allAllocations,
                currentlyInside = currentlyInside,
                dynamicGreeting = viewModel.getDynamicGreeting(),
                pendingVerificationsCount = pendingVerificationsCount,
                pendingResetRequestsCount = pendingResetRequestsCount,
                onNavigateTab = { adminTab = it },
                onLogoutClick = { showLogoutDialog = true }
            )
            "Verifications" -> AdminPaymentVerificationsContent(viewModel = viewModel)
            "Password Requests" -> AdminPasswordResetRequestsContent(viewModel = viewModel)
            "Announcements" -> AdminAnnouncementsContent(viewModel = viewModel)
            "Students" -> AdminStudentsContent(
                students = allStudents,
                memberships = allMemberships,
                allocations = allAllocations,
                viewModel = viewModel
            )
            "Seats" -> AdminSeatsContent(
                selectedShift = selectedShiftForAdmin,
                onSelectShift = { selectedShiftForAdmin = it },
                seats = seats,
                allocations = allocations,
                allStudents = allStudents,
                viewModel = viewModel,
                onSeatClick = { seatNum -> showSeatDetailDialog = seatNum }
            )
            "Shifts" -> AdminShiftsContent(shifts = shifts, viewModel = viewModel)
            "Attendance" -> AdminAttendanceContent(records = viewModel.allAttendance.collectAsState().value, viewModel = viewModel)
            "Payments" -> AdminPaymentsContent(payments = allPayments, viewModel = viewModel, students = allStudents)
            "Complaints" -> AdminComplaintsContent(complaints = allComplaints, viewModel = viewModel)
        }
    }

    // Confirmation Dialog for Admin Logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFC62828))
                    Spacer(Modifier.width(8.dp))
                    Text("Logout Admin", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text("Are you sure you want to log out from the Admin Console? You will return to the welcome screen.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.adminLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Yes, Logout", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full Admin Seat Management Dialog
    showSeatDetailDialog?.let { sNum ->
        val alloc = allocations.find { it.seatNumber == sNum }
        val seatObj = seats.find { it.seatNumber == sNum }
        val isMaint = seatObj?.isMaintenance == true
        val shiftTiming = shifts.find { it.id == selectedShiftForAdmin }?.timeRange ?: ""

        var allocateName by remember { mutableStateOf("") }
        var allocatePhone by remember { mutableStateOf("") }
        var showDirectAssignForm by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSeatDetailDialog = null },
            title = {
                Text(
                    text = "Manage Seat $sNum (Shift $selectedShiftForAdmin)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Timing: $shiftTiming",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(10.dp))

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isMaint -> Color(0xFFFFF3E0)
                            alloc != null -> Color(0xFFFFEBEE)
                            else -> Color(0xFFE8F8EE)
                        }
                    ) {
                        Text(
                            text = when {
                                isMaint -> "Status: LOCKED / MAINTENANCE (Non-bookable)"
                                alloc != null -> "Status: OCCUPIED"
                                else -> "Status: AVAILABLE (Bookable)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = when {
                                isMaint -> Color(0xFFE65100)
                                alloc != null -> Color(0xFFC62828)
                                else -> PrimaryGreen
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (alloc != null) {
                        val studentUser = allStudents.find { it.id == alloc.studentId || it.mobile == alloc.studentId || it.fullName.equals(alloc.studentName, ignoreCase = true) }
                        val sMobile = studentUser?.mobile ?: if (alloc.studentId.matches(Regex("\\d{10}"))) alloc.studentId else "9876543210"
                        val sEmail = studentUser?.email?.ifBlank { null } ?: "Not provided"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (studentUser?.fullName ?: alloc.studentName).take(1).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(text = studentUser?.fullName ?: alloc.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Student ID: ${alloc.studentId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Phone: ", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    Text(sMobile, fontSize = 11.5.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Email: ", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    Text(sEmail, fontSize = 11.5.sp)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Plan: ", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Shift $selectedShiftForAdmin • ${alloc.membershipId}", fontSize = 11.5.sp)
                                }

                                val mem = allMemberships.find { it.studentId == alloc.studentId || (it.seatNumber == sNum && it.status == "ACTIVE") }
                                    ?: allMemberships.find { it.studentId == alloc.studentId }
                                if (mem != null) {
                                    val daysRem = mem.getDaysRemaining(System.currentTimeMillis())
                                    val isExpired = mem.status == "EXPIRED" || daysRem <= 0
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Valid: ", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        Text("${mem.startDate} to ${mem.expiryDate}", fontSize = 11.5.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isExpired) Color(0xFFFFEBEE) else Color(0xFFE8F8EE)
                                        ) {
                                            Text(
                                                text = if (isExpired) "STATUS: EXPIRED" else "STATUS: ACTIVE ($daysRem days left)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExpired) Color(0xFFC62828) else PrimaryGreen,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Quick Call and WhatsApp Action buttons
                        val memForRem = allMemberships.find { it.studentId == alloc.studentId }
                        val isMemExpired = memForRem?.status == "EXPIRED" || ((memForRem?.getDaysRemaining(System.currentTimeMillis()) ?: 1) <= 0)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val clean = sMobile.filter { it.isDigit() }
                                    val full = if (clean.length == 10) "91$clean" else clean
                                    val customMsg = if (isMemExpired) {
                                        "Hello ${alloc.studentName}, your library membership at Maa Durga Digital Library for Seat $sNum has expired. Please renew your membership to continue access and retain your seat. Thank you."
                                    } else {
                                        "Hello ${alloc.studentName}, greetings from Maa Durga Digital Library regarding Seat $sNum."
                                    }
                                    val encoded = java.net.URLEncoder.encode(customMsg, "UTF-8")
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$full?text=$encoded"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = if (isMemExpired) Color(0xFFC62828) else Color(0xFF25D366)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (isMemExpired) "Expiry Reminder" else "WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sMobile"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Call", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Admin Action 1: Free Seat / Remove Student
                        Button(
                            onClick = {
                                viewModel.releaseSeatAllocation(sNum, selectedShiftForAdmin)
                                showSeatDetailDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Remove Student & Free Seat", fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Admin Action 2: Put on maintenance
                        OutlinedButton(
                            onClick = {
                                viewModel.toggleSeatMaintenance(sNum, true)
                                showSeatDetailDialog = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lock / Maintenance", fontSize = 12.sp)
                        }
                    } else if (isMaint) {
                        Text(
                            text = "This seat is locked or marked under maintenance. Students cannot select this seat for admission.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        // Admin Action: Unlock Seat
                        Button(
                            onClick = {
                                viewModel.toggleSeatMaintenance(sNum, false)
                                showSeatDetailDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Unlock Seat (Make Available)", fontSize = 12.sp)
                        }
                    } else {
                        // Seat is available
                        Text(
                            text = "Seat is open for student self-admission or you can directly allocate it or lock it.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))

                        if (!showDirectAssignForm) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showDirectAssignForm = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Assign to Student", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.toggleSeatMaintenance(sNum, true)
                                        showSeatDetailDialog = null
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Lock Seat", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Text("Allocate to Student:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = allocateName,
                                    onValueChange = { allocateName = it },
                                    label = { Text("Student Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = allocatePhone,
                                    onValueChange = { allocatePhone = it },
                                    label = { Text("Mobile / ID (optional)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (allocateName.isNotBlank()) {
                                            val studentId = if (allocatePhone.isNotBlank()) allocatePhone.trim() else "STUDENT-${System.currentTimeMillis() % 1000}"
                                            viewModel.allocateSeatByAdmin(sNum, selectedShiftForAdmin, studentId, allocateName.trim())
                                            showSeatDetailDialog = null
                                        }
                                    },
                                    enabled = allocateName.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Confirm Allocation", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeatDetailDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AdminOverviewContent(
    totalStudentsCount: Int,
    presentCount: Int,
    totalSeats: Int,
    todayCollection: String,
    totalRevenueAmount: Int,
    allAllocations: List<com.example.data.model.SeatAllocation>,
    currentlyInside: List<AttendanceRecord>,
    dynamicGreeting: String,
    pendingVerificationsCount: Int = 0,
    pendingResetRequestsCount: Int = 0,
    onNavigateTab: (String) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Tagline & Dynamic Greeting
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$dynamicGreeting 👋",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Manage Smarter. Grow Better.",
                fontSize = 12.sp,
                color = PrimaryGreen,
                fontWeight = FontWeight.Medium
            )
        }

        // Pending Action Alert Banners
        if (pendingVerificationsCount > 0) {
            Spacer(Modifier.height(12.dp))
            Card(
                onClick = { onNavigateTab("Verifications") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9800)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PendingActions,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⚡ $pendingVerificationsCount Pending Payment Verification${if (pendingVerificationsCount > 1) "s" else ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "Students have submitted UPI proofs. Review and allot seats now.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (pendingResetRequestsCount > 0) {
            Spacer(Modifier.height(10.dp))
            Card(
                onClick = { onNavigateTab("Password Requests") },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LockReset,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔑 $pendingResetRequestsCount Password Reset Request${if (pendingResetRequestsCount > 1) "s" else ""}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1565C0)
                        )
                        Text(
                            text = "Students have requested password resets. Review and approve/reject.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF37474F)
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 4 KPI Cards Grid - Clickable to corresponding admin tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminKpiCard(
                title = "Total Students",
                value = "$totalStudentsCount",
                icon = Icons.Default.Groups,
                color = Color(0xFF2E7D32),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab("Students") }
            )
            AdminKpiCard(
                title = "Present Today",
                value = "$presentCount",
                icon = Icons.Default.HowToReg,
                color = Color(0xFF0288D1),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab("Attendance") }
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AdminKpiCard(
                title = "Total Seats",
                value = "$totalSeats",
                icon = Icons.Default.EventSeat,
                color = Color(0xFFF57C00),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab("Seats") }
            )
            AdminKpiCard(
                title = "Today's Collection",
                value = todayCollection,
                icon = Icons.Default.AccountBalanceWallet,
                color = Color(0xFF1B5E20),
                modifier = Modifier.weight(1f),
                onClick = { onNavigateTab("Payments") }
            )
        }

        Spacer(Modifier.height(18.dp))

        // Seat Occupancy (Today) Bars calculated dynamically
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seat Occupancy (Live by Shift)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${allAllocations.size} Seats Allocated",
                        fontSize = 11.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    (1..4).forEach { sId ->
                        val shiftAllocCount = allAllocations.count { it.shiftId == sId }
                        val shiftPercent = if (totalSeats > 0) {
                            ((shiftAllocCount * 100) / totalSeats).coerceIn(0, 100)
                        } else 0
                        OccupancyBar("Shift $sId", shiftPercent)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Total Revenue Card calculated dynamically (tap to view Payments)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTab("Payments") },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Total Collected Revenue", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "₹${String.format(Locale.ENGLISH, "%,d", totalRevenueAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F8EE)
                    ) {
                        Text(
                            text = "Verified",
                            color = PrimaryGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Quick info
                Text(
                    text = "Reflects all real student admissions, UPI transactions, and manual cash receipts.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Currently Inside (Live) Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Attendance (Live Inside)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFE8F8EE)
                    ) {
                        Text(
                            text = "${currentlyInside.size} Inside",
                            color = PrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                currentlyInside.forEach { rec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = rec.seatNumber, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryGreen)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(text = rec.studentName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "${rec.shiftTitle} • Entry: ${rec.entryTime}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F8EE)
                        ) {
                            Text(
                                text = "Inside",
                                color = PrimaryGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Admin Session & Easy Logout Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Admin Session Active", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Maa Durga Digital Library • Ghazipur Console", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onLogoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out from Admin Console", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun OccupancyBar(shiftTitle: String, percentage: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$percentage%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height((percentage * 0.9).dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(PrimaryGreen)
        )
        Spacer(Modifier.height(6.dp))
        Text(text = shiftTitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun AdminStudentsContent(
    students: List<User>,
    memberships: List<Membership>,
    allocations: List<SeatAllocation>,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, WITH_SEAT, INACTIVE
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var selectedStudentForDetail by remember { mutableStateOf<User?>(null) }
    var studentToDelete by remember { mutableStateOf<User?>(null) }

    // Add Student Form State
    var newName by remember { mutableStateOf("") }
    var newMobile by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newGender by remember { mutableStateOf("Male") }
    var assignShiftImmediately by remember { mutableStateOf(false) }
    var selectedShiftId by remember { mutableIntStateOf(1) }
    var selectedSeatNumber by remember { mutableStateOf("") }
    var addErrorMsg by remember { mutableStateOf<String?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    val shifts by viewModel.shifts.collectAsState()
    val seats by viewModel.seats.collectAsState()

    val studentSeatMap = remember(allocations) {
        allocations.groupBy { it.studentId }
    }

    val studentMembershipMap = remember(memberships) {
        memberships.associateBy { it.studentId }
    }

    val filteredStudents = remember(students, searchQuery, selectedFilter, studentSeatMap) {
        students.filter { st ->
            val matchesQuery = st.fullName.contains(searchQuery, ignoreCase = true) ||
                    st.id.contains(searchQuery, ignoreCase = true) ||
                    st.mobile.contains(searchQuery, ignoreCase = true) ||
                    st.email.contains(searchQuery, ignoreCase = true)

            val hasSeat = studentSeatMap.containsKey(st.id)
            val matchesFilter = when (selectedFilter) {
                "WITH_SEAT" -> hasSeat
                "NO_SEAT" -> !hasSeat
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Action Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Student Directory",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${students.size} registered students (${studentSeatMap.size} with seats)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = {
                    newName = ""
                    newMobile = ""
                    newEmail = ""
                    newGender = "Male"
                    addErrorMsg = null
                    showAddStudentDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add Student", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, ID, or mobile number...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(Modifier.height(10.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All (${students.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "WITH_SEAT",
                onClick = { selectedFilter = "WITH_SEAT" },
                label = { Text("With Seat (${studentSeatMap.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == "NO_SEAT",
                onClick = { selectedFilter = "NO_SEAT" },
                label = { Text("Awaiting Seat (${(students.size - studentSeatMap.size).coerceAtLeast(0)})", fontSize = 11.sp) }
            )
        }

        Spacer(Modifier.height(12.dp))

        // Students List
        if (filteredStudents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "No students match your criteria",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStudents, key = { it.id }) { st ->
                    val allocs = studentSeatMap[st.id] ?: emptyList()
                    val membership = studentMembershipMap[st.id]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStudentForDetail = st },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Avatar with initials
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = st.fullName.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = PrimaryGreen
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = st.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${st.id} • ${st.mobile}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Quick communication buttons
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // WhatsApp Action
                                    IconButton(
                                        onClick = {
                                            val cleanNumber = st.mobile.filter { it.isDigit() }
                                            val fullNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
                                            val url = "https://wa.me/$fullNumber"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "WhatsApp Student",
                                            tint = Color(0xFF25D366),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Phone Call Action
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${st.mobile}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = "Call Student",
                                            tint = PrimaryGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Details Arrow
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "View Details",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(8.dp))

                            // Seat & Membership status row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (allocs.isNotEmpty()) {
                                    val seatNumbers = allocs.map { "Seat ${it.seatNumber}" }.distinct().joinToString(", ")
                                    val shiftIds = allocs.map { "Shift ${it.shiftId}" }.distinct().joinToString(", ")
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F8EE)
                                    ) {
                                        Text(
                                            text = "$seatNumbers ($shiftIds)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "No Seat Allocated",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                if (membership != null) {
                                    val now = System.currentTimeMillis()
                                    val daysLeft = membership.getDaysRemaining(now)
                                    val isExpired = membership.status == "EXPIRED" || daysLeft <= 0
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isExpired) Color(0xFFFFEBEE) else if (daysLeft <= 5) Color(0xFFFFF3E0) else Color(0xFFE8F8EE)
                                        ) {
                                            Text(
                                                text = if (isExpired) "EXPIRED" else "Valid: $daysLeft d left",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExpired) Color(0xFFC62828) else if (daysLeft <= 5) Color(0xFFE65100) else PrimaryGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "Exp: ${membership.expiryDate}",
                                            fontSize = 10.5.sp,
                                            color = if (isExpired) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Account: Active",
                                        fontSize = 11.sp,
                                        color = PrimaryGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // If membership is expired, show quick WhatsApp Reminder CTA right on the student card
                            if (membership != null && (membership.status == "EXPIRED" || membership.getDaysRemaining(System.currentTimeMillis()) <= 0)) {
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFEBEE),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Membership expired",
                                            fontSize = 11.sp,
                                            color = Color(0xFFC62828),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        TextButton(
                                            onClick = {
                                                val cleanNumber = st.mobile.filter { it.isDigit() }
                                                val fullNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
                                                val message = "Hello ${st.fullName}, your library membership at Maa Durga Digital Library has expired. Please renew your membership to continue accessing your seat. Thank you."
                                                val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
                                                val url = "https://wa.me/$fullNumber?text=$encodedMsg"
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                context.startActivity(intent)
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(13.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("WhatsApp Reminder", fontSize = 11.sp, color = Color(0xFF25D366), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 1. Add Student Dialog
    if (showAddStudentDialog) {
        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = {
                Text(
                    text = "Register New Student",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Create official library credentials. Student will be assigned a permanent ID automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newMobile,
                        onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) newMobile = it },
                        label = { Text("Mobile Number (10 digits) *") },
                        placeholder = { Text("9876543210") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email Address (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Gender Selector
                    Text("Gender:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Male", "Female", "Other").forEach { g ->
                            FilterChip(
                                selected = newGender == g,
                                onClick = { newGender = g },
                                label = { Text(g, fontSize = 11.sp) }
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Allocate Shift & Seat Now?", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Assign seat immediately upon registration", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = assignShiftImmediately,
                            onCheckedChange = { assignShiftImmediately = it }
                        )
                    }

                    if (assignShiftImmediately) {
                        Text("Select Shift:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            shifts.forEach { sh ->
                                FilterChip(
                                    selected = selectedShiftId == sh.id,
                                    onClick = {
                                        selectedShiftId = sh.id
                                        selectedSeatNumber = ""
                                    },
                                    label = { Text("Shift ${sh.id} (₹${sh.monthlyFee})", fontSize = 10.sp) }
                                )
                            }
                        }

                        val occupiedSeatsInShift = remember(allocations, selectedShiftId) {
                            allocations.filter { it.shiftId == selectedShiftId }.map { it.seatNumber }.toSet()
                        }
                        val maintSeats = remember(seats) {
                            seats.filter { it.isMaintenance }.map { it.seatNumber }.toSet()
                        }
                        val availableSeats = remember(occupiedSeatsInShift, maintSeats) {
                            (1..36).map { String.format("%02d", it) }
                                .filter { !occupiedSeatsInShift.contains(it) && !maintSeats.contains(it) }
                        }

                        Text("Select Seat (${availableSeats.size} available):", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        if (availableSeats.isEmpty()) {
                            Text("No seats available in this shift!", fontSize = 11.sp, color = Color(0xFFC62828))
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(availableSeats) { sNum ->
                                    FilterChip(
                                        selected = selectedSeatNumber == sNum,
                                        onClick = { selectedSeatNumber = sNum },
                                        label = { Text("Seat $sNum", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }

                    addErrorMsg?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = err,
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank() || newMobile.length != 10) {
                            addErrorMsg = "Please enter valid student name and 10-digit mobile number."
                            return@Button
                        }
                        if (assignShiftImmediately && selectedSeatNumber.isBlank()) {
                            addErrorMsg = "Please select an available seat for Shift $selectedShiftId."
                            return@Button
                        }
                        isAdding = true
                        addErrorMsg = null
                        val emailToUse = if (newEmail.isNotBlank()) newEmail.trim() else "${newMobile.trim()}@library.local"
                        if (assignShiftImmediately && selectedSeatNumber.isNotBlank()) {
                            viewModel.admitStudentByAdmin(
                                fullName = newName.trim(),
                                mobile = newMobile.trim(),
                                email = emailToUse,
                                gender = newGender,
                                address = "",
                                shiftId = selectedShiftId,
                                seatNumber = selectedSeatNumber,
                                onSuccess = {
                                    isAdding = false
                                    showAddStudentDialog = false
                                },
                                onError = { err ->
                                    isAdding = false
                                    addErrorMsg = err
                                }
                            )
                        } else {
                            viewModel.registerStudentByAdmin(
                                fullName = newName.trim(),
                                mobile = newMobile.trim(),
                                email = emailToUse,
                                gender = newGender,
                                address = "",
                                onSuccess = {
                                    isAdding = false
                                    showAddStudentDialog = false
                                },
                                onError = { err ->
                                    isAdding = false
                                    addErrorMsg = err
                                }
                            )
                        }
                    },
                    enabled = !isAdding && newName.isNotBlank() && newMobile.length == 10,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(if (isAdding) "Registering..." else if (assignShiftImmediately) "Register & Allocate Seat" else "Register Student")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Student Detail & Profile Dialog
    selectedStudentForDetail?.let { st ->
        val allocs = studentSeatMap[st.id] ?: emptyList()
        val membership = studentMembershipMap[st.id]

        AlertDialog(
            onDismissRequest = { selectedStudentForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Student Profile",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F8EE)) {
                        Text(
                            text = st.id,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = st.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(text = "Mobile: ${st.mobile}", fontSize = 13.sp)
                            Text(text = "Email: ${st.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Gender: ${st.gender}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Membership & Seat details
                    Text("Library Allocation:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    if (allocs.isNotEmpty() || membership != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F8EE)),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (allocs.isNotEmpty()) {
                                    val seatsStr = allocs.map { "Seat ${it.seatNumber}" }.distinct().joinToString(", ")
                                    val shiftsStr = allocs.map { "Shift ${it.shiftId}" }.distinct().joinToString(", ")
                                    Text(text = "Allocated: $seatsStr ($shiftsStr)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryGreen)
                                }
                                if (membership != null) {
                                    val daysLeft = membership.getDaysRemaining(System.currentTimeMillis())
                                    val isExpired = membership.status == "EXPIRED" || daysLeft <= 0
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = "Plan: ${membership.shiftTitles}", fontSize = 12.sp)
                                    Text(text = "Joined / Start Date: ${membership.startDate}", fontSize = 12.sp)
                                    Text(text = "Valid Until: ${membership.expiryDate}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (isExpired) "Status: EXPIRED (0 days left)" else "Status: ACTIVE ($daysLeft days remaining)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExpired) Color(0xFFC62828) else PrimaryGreen
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No active seat allocation. Student has not selected a seat or is on waitlist.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isExpiredStudent = membership != null && (membership.status == "EXPIRED" || membership.getDaysRemaining(System.currentTimeMillis()) <= 0)
                    if (isExpiredStudent) {
                        Button(
                            onClick = {
                                val cleanNumber = st.mobile.filter { it.isDigit() }
                                val fullNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
                                val message = "Hello ${st.fullName}, your library membership at Maa Durga Digital Library has expired. Please renew your membership to continue accessing your seat. Thank you."
                                val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
                                val url = "https://wa.me/$fullNumber?text=$encodedMsg"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send WhatsApp Expiry Reminder", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    // Contact action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val cleanNumber = st.mobile.filter { it.isDigit() }
                                val fullNumber = if (cleanNumber.length == 10) "91$cleanNumber" else cleanNumber
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$fullNumber"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${st.mobile}"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Call", fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Delete button
                    OutlinedButton(
                        onClick = {
                            studentToDelete = st
                            selectedStudentForDetail = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Student Account", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStudentForDetail = null }) {
                    Text("Close")
                }
            }
        )
    }

    // 3. Delete Confirmation Dialog
    studentToDelete?.let { st ->
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("Delete Student?", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
            text = {
                Text("Are you sure you want to permanently delete ${st.fullName} (${st.id})? Any seat allocated to this student will be immediately freed up for booking.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStudent(st.id) {
                            studentToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminSeatsContent(
    selectedShift: Int,
    onSelectShift: (Int) -> Unit,
    seats: List<com.example.data.model.Seat>,
    allocations: List<com.example.data.model.SeatAllocation>,
    allStudents: List<com.example.data.model.User>,
    viewModel: MainViewModel,
    onSeatClick: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Seat Allocations", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row {
                (1..4).forEach { sId ->
                    FilterChip(
                        selected = selectedShift == sId,
                        onClick = { onSelectShift(sId) },
                        label = { Text("Shift $sId", fontSize = 10.sp) }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        if (allocations.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FAF5)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "No Occupied Seats in Shift $selectedShift",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "All seats in this shift are available. Tap any seat below to allocate it or lock it for maintenance.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        } else {
            Text(
                text = "Occupied Seats (${allocations.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))

            allocations.forEach { alloc ->
                val student = allStudents.find { it.id == alloc.studentId || it.mobile == alloc.studentId || it.fullName.equals(alloc.studentName, ignoreCase = true) }
                val sMobile = student?.mobile ?: if (alloc.studentId.matches(Regex("\\d{10}"))) alloc.studentId else ""
                val sEmail = student?.email?.ifBlank { null } ?: "Not provided"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE8F8EE)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = alloc.seatNumber, fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 14.sp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(text = student?.fullName ?: alloc.studentName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                    Text(text = "ID: ${alloc.studentId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Button(
                                onClick = { onSeatClick(alloc.seatNumber) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Manage", fontSize = 11.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (sMobile.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(sMobile, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(sEmail, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (sMobile.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clean = sMobile.filter { it.isDigit() }
                                            val full = if (clean.length == 10) "91$clean" else clean
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$full"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sMobile"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = "Call", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            text = "Interactive Shift $selectedShift Seat Grid (Tap to Manage)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))

        val allocatedSet = allocations.map { it.seatNumber }.toSet()
        for (row in 0 until 6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0 until 6) {
                    val seatIdx = row * 6 + col + 1
                    val sNum = String.format("%02d", seatIdx)
                    val isOccupied = allocatedSet.contains(sNum)
                    val isMaint = seats.find { it.seatNumber == sNum }?.isMaintenance == true

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isMaint -> SeatMaintenanceBg
                                    isOccupied -> SeatOccupiedBg
                                    else -> SeatAvailableBg
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    isMaint -> SeatMaintenanceBorder
                                    isOccupied -> SeatOccupiedBorder
                                    else -> SeatAvailableBorder
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSeatClick(sNum) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sNum,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isMaint -> SeatMaintenanceText
                                isOccupied -> SeatOccupiedText
                                else -> SeatAvailableText
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminShiftsContent(
    shifts: List<com.example.data.model.Shift>,
    viewModel: MainViewModel
) {
    var shiftToEdit by remember { mutableStateOf<com.example.data.model.Shift?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editTimeRange by remember { mutableStateOf("") }
    var editFeeText by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Library Shifts & Fee Plans",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage shift timings and monthly subscription fees",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Tap on any shift to modify its hours or update the monthly fee. Changes reflect in real-time across student enrollment and UPI receipts.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shifts, key = { it.id }) { shift ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            shiftToEdit = shift
                            editTitle = shift.title
                            editTimeRange = shift.timeRange
                            editFeeText = shift.monthlyFee.toString()
                            editError = null
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PrimaryGreen.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "Shift ${shift.id}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = shift.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = shift.timeRange,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${shift.monthlyFee}",
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    fontSize = 18.sp
                                )
                                Text(
                                    text = "per month",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    shiftToEdit = shift
                                    editTitle = shift.title
                                    editTimeRange = shift.timeRange
                                    editFeeText = shift.monthlyFee.toString()
                                    editError = null
                                }
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Shift",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Shift Dialog
    shiftToEdit?.let { s ->
        AlertDialog(
            onDismissRequest = { shiftToEdit = null },
            title = {
                Text(
                    text = "Edit Shift ${s.id} Details",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Shift Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTimeRange,
                        onValueChange = { editTimeRange = it },
                        label = { Text("Timing (e.g. 06:00 AM - 12:00 PM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editFeeText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) editFeeText = it },
                        label = { Text("Monthly Fee (₹)") },
                        placeholder = { Text("500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    editError?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = err,
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fee = editFeeText.toIntOrNull()
                        if (editTitle.isBlank() || editTimeRange.isBlank() || fee == null || fee <= 0) {
                            editError = "Please enter valid title, timing, and monthly fee amount."
                            return@Button
                        }
                        viewModel.updateShift(
                            shiftId = s.id,
                            title = editTitle.trim(),
                            timeRange = editTimeRange.trim(),
                            monthlyFee = fee
                        )
                        shiftToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { shiftToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminAttendanceContent(
    records: List<AttendanceRecord>,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, INSIDE, COMPLETED
    var selectedShiftFilter by remember { mutableStateOf("ALL") } // ALL, Morning, Evening, Full Day, Dual Shift
    var selectedDateFilter by remember { mutableStateOf("ALL") } // ALL, TODAY, CUSTOM
    var customDateStr by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showGateQrDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<AttendanceRecord?>(null) }

    val istTz = remember { java.util.TimeZone.getTimeZone("Asia/Kolkata") }
    val sdfDate = remember {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).apply {
            timeZone = istTz
        }
    }
    val todayStr = remember { sdfDate.format(java.util.Date()) }

    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val todayRecords = remember(records, todayStr) { records.filter { it.dateStr == todayStr } }
    val totalStudentsPresentToday = remember(todayRecords) { todayRecords.map { it.studentId }.distinct().size }
    val insideRecords = remember(records) { records.filter { it.isInside } }
    val checkedOutTodayRecords = remember(todayRecords) { todayRecords.filter { !it.isInside } }

    val filteredRecords = remember(records, searchQuery, selectedFilter, selectedShiftFilter, selectedDateFilter, customDateStr, todayStr) {
        records.filter { rec ->
            val matchesQuery = rec.studentName.contains(searchQuery, ignoreCase = true) ||
                    rec.seatNumber.contains(searchQuery, ignoreCase = true) ||
                    rec.mobile.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "INSIDE" -> rec.isInside
                "COMPLETED" -> !rec.isInside
                else -> true
            }

            val matchesShift = when (selectedShiftFilter) {
                "ALL" -> true
                "Morning" -> rec.shiftTitle.contains("Morning", ignoreCase = true)
                "Evening" -> rec.shiftTitle.contains("Evening", ignoreCase = true)
                "Full Day" -> rec.shiftTitle.contains("Full Day", ignoreCase = true) || rec.shiftTitle.contains("24", ignoreCase = true)
                "Dual Shift" -> rec.shiftTitle.contains("Dual", ignoreCase = true) || rec.shiftTitle.contains("+", ignoreCase = true)
                else -> rec.shiftTitle.contains(selectedShiftFilter, ignoreCase = true)
            }

            val matchesDate = when (selectedDateFilter) {
                "TODAY" -> rec.dateStr == todayStr
                "CUSTOM" -> customDateStr == null || rec.dateStr == customDateStr
                else -> true
            }

            matchesQuery && matchesFilter && matchesShift && matchesDate
        }
    }

    if (showDatePicker) {
        val calendar = java.util.Calendar.getInstance(istTz)
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = java.util.Calendar.getInstance(istTz)
                cal.set(year, month, dayOfMonth)
                customDateStr = sdfDate.format(cal.time)
                selectedDateFilter = "CUSTOM"
                showDatePicker = false
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
            show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Attendance Overview & Headcount Cards (Requirement 5)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("PRESENT TODAY", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("$totalStudentsPresentToday", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Students Today", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("INSIDE NOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${insideRecords.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                        Text("Active in Library", fontSize = 10.sp, color = PrimaryGreen.copy(alpha = 0.8f))
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CHECKED OUT", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("${checkedOutTodayRecords.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Left Today", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Entrance Gate QR Code Management Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F3615)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.QrCode, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ENTRANCE GATE QR CODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldAccent)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Library Entrance Gate QR",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showGateQrDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EmeraldAccent.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Enlarge", tint = EmeraldAccent)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Embedded Live Entrance Gate QR
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, EmeraldAccent),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            DynamicUpiQrCanvas(
                                payload = "MDDL-GHAZIPUR-MAIN-GATE",
                                modifier = Modifier.size(180.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "MDDL-GHAZIPUR-MAIN-GATE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Actions: View Fullscreen, Download, Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                QrDownloadUtils.shareQrCode(
                                    context = context,
                                    payload = "MDDL-GHAZIPUR-MAIN-GATE",
                                    title = "Entrance Gate QR - Maa Durga Digital Library"
                                )
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = {
                                QrDownloadUtils.downloadQrToDevice(
                                    context = context,
                                    payload = "MDDL-GHAZIPUR-MAIN-GATE",
                                    filePrefix = "MDDL_Gate_QR"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Download", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        Button(
                            onClick = { showGateQrDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Enlarge", fontSize = 11.5.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Search & Filter Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search student name, seat number, mobile...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Filters & Calendar Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedDateFilter == "ALL",
                        onClick = { selectedDateFilter = "ALL" },
                        label = { Text("All Dates", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedDateFilter == "TODAY",
                        onClick = { selectedDateFilter = "TODAY" },
                        label = { Text("Today", fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = selectedDateFilter == "CUSTOM",
                        onClick = { showDatePicker = true },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(customDateStr ?: "Pick Date", fontSize = 11.5.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Status Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "ALL" to "All Status (${records.size})",
                        "INSIDE" to "Inside Now (${insideRecords.size})",
                        "COMPLETED" to "Checked Out (${records.count { !it.isInside }})"
                    ).forEach { (filterKey, label) ->
                        val isSelected = selectedFilter == filterKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filterKey },
                            label = { Text(label, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Shift Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        "ALL" to "All Shifts",
                        "Morning" to "Morning",
                        "Evening" to "Evening",
                        "Full Day" to "Full Day",
                        "Dual Shift" to "Dual Shifts"
                    ).forEach { (sKey, sLabel) ->
                        val isSelected = selectedShiftFilter == sKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedShiftFilter = sKey },
                            label = { Text(sLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.85f),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Attendance Records List
        if (filteredRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No records match '$searchQuery'" else "No attendance records found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = if (rec.isInside) androidx.compose.foundation.BorderStroke(1.2.dp, PrimaryGreen.copy(alpha = 0.4f)) else null
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = rec.studentName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = PrimaryGreen.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "Seat ${rec.seatNumber}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "📱 ${rec.mobile} • ${rec.shiftTitle}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (rec.isInside) Color(0xFFE8F8EE) else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    if (rec.isInside) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryGreen)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = if (rec.isInside) "Inside Now" else "Checked Out",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rec.isInside) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Date: ${rec.dateStr}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Entry: ${rec.entryTime}  |  Exit: ${rec.exitTime ?: "--"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (rec.isInside) {
                                    val durationMs = (currentTimeMillis - rec.entryTimestamp).coerceAtLeast(0)
                                    val spentMins = (durationMs / (60 * 1000)).toInt()
                                    val hrs = spentMins / 60
                                    val mins = spentMins % 60
                                    val durationStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                                    Text(
                                        text = "Duration so far: $durationStr",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                } else if (!rec.duration.isNullOrBlank() && rec.duration != "0h 0m") {
                                    Text(
                                        text = "Total Duration: ${rec.duration}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (rec.isInside) {
                                    OutlinedButton(
                                        onClick = { viewModel.adminManualCheckout(rec.id) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Checkout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                IconButton(
                                    onClick = { recordToEdit = rec },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Attendance",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog: Full Official Gate QR Code for Wall Print / Display
    if (showGateQrDialog) {
        AlertDialog(
            onDismissRequest = { showGateQrDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Entrance Gate QR Code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Maa Durga Digital Library",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(2.dp, PrimaryGreen),
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(18.dp)
                        ) {
                            DynamicUpiQrCanvas(
                                payload = "MDDL-GHAZIPUR-MAIN-GATE",
                                modifier = Modifier.size(200.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "MDDL-GHAZIPUR-MAIN-GATE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            QrDownloadUtils.shareQrCode(
                                context = context,
                                payload = "MDDL-GHAZIPUR-MAIN-GATE",
                                title = "Entrance Gate QR - Maa Durga Digital Library"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            QrDownloadUtils.downloadQrToDevice(
                                context = context,
                                payload = "MDDL-GHAZIPUR-MAIN-GATE",
                                filePrefix = "MDDL_Gate_QR"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Download QR", fontSize = 12.sp, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showGateQrDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Modal Dialog: Admin Attendance Correction
    recordToEdit?.let { rec ->
        var editEntry by remember { mutableStateOf(rec.entryTime) }
        var editExit by remember { mutableStateOf(rec.exitTime ?: "") }
        var editIsInside by remember { mutableStateOf(rec.isInside) }
        var editReason by remember { mutableStateOf("Admin adjustment") }

        AlertDialog(
            onDismissRequest = { recordToEdit = null },
            title = {
                Text(
                    text = "Correct Attendance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Student: ${rec.studentName} (Seat ${rec.seatNumber})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = editEntry,
                        onValueChange = { editEntry = it },
                        label = { Text("Entry Time (e.g. 06:00 AM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editExit,
                        onValueChange = { editExit = it },
                        label = { Text("Exit Time (e.g. 12:00 PM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = editIsInside,
                            onCheckedChange = { editIsInside = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Currently Inside Library", fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = editReason,
                        onValueChange = { editReason = it },
                        label = { Text("Reason for Correction") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminCorrectAttendance(
                            recordId = rec.id,
                            newEntryTime = editEntry.trim(),
                            newExitTime = if (editExit.isBlank()) null else editExit.trim(),
                            newStatus = if (editIsInside) "active" else "completed",
                            reason = editReason.trim()
                        )
                        recordToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Save Correction", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminPaymentsContent(
    payments: List<com.example.data.model.PaymentRecord>,
    viewModel: MainViewModel,
    students: List<com.example.data.model.User>
) {
    val currentUpiId by viewModel.adminUpiId.collectAsState()
    val shifts by viewModel.shifts.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editUpiText by remember { mutableStateOf(currentUpiId) }

    // Search and filter
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PAID, CASH, UPI

    // Cash Recording Dialog state
    var showRecordCashDialog by remember { mutableStateOf(false) }
    var selectedStudentId by remember { mutableStateOf("") }
    var manualStudentName by remember { mutableStateOf("") }
    var manualAmountText by remember { mutableStateOf("500") }
    var selectedShiftDesc by remember { mutableStateOf("Shift 1 (06:00 AM - 12:00 PM)") }
    var manualPaymentMode by remember { mutableStateOf("Cash") }
    var manualRemarks by remember { mutableStateOf("Monthly desk fee collected at reception") }
    var recordError by remember { mutableStateOf<String?>(null) }

    // Receipt Dialog state
    var selectedPaymentForReceipt by remember { mutableStateOf<com.example.data.model.PaymentRecord?>(null) }

    val totalCollected = remember(payments) {
        payments.filter { it.status.equals("Paid", ignoreCase = true) || it.status.equals("Completed", ignoreCase = true) }.sumOf { it.amount }
    }

    val filteredPayments = remember(payments, searchQuery, selectedFilter) {
        payments.filter { p ->
            val matchesQuery = p.studentName.contains(searchQuery, ignoreCase = true) ||
                    p.id.contains(searchQuery, ignoreCase = true) ||
                    p.shiftDescription.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "PAID" -> p.status.equals("Paid", ignoreCase = true) || p.status.equals("Completed", ignoreCase = true)
                "CASH" -> p.paymentMode.equals("Cash", ignoreCase = true)
                "UPI" -> p.paymentMode.contains("UPI", ignoreCase = true) || p.id.startsWith("PAY-UPI")
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header & Manual Record Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fee & Payment Records",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total Collected: ₹${String.format(Locale.ENGLISH, "%,d", totalCollected)} • ${payments.size} records",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        if (students.isNotEmpty()) {
                            selectedStudentId = students.first().id
                            manualStudentName = students.first().fullName
                        }
                        if (shifts.isNotEmpty()) {
                            val s = shifts.first()
                            selectedShiftDesc = "${s.title} (${s.timeRange})"
                            manualAmountText = s.monthlyFee.toString()
                        }
                        recordError = null
                        showRecordCashDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Collect Cash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Admin UPI Gateway Settings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ACTIVE UPI PAYMENT GATEWAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            Text("Bank Account Linked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            editUpiText = currentUpiId
                            showEditDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit UPI ID", tint = PrimaryGreen)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = currentUpiId,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Dynamic QR code generated on student payment screens automatically encodes this PhonePe UPI ID.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search & Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search transactions, student, receipt #...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All (${payments.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "PAID",
                        onClick = { selectedFilter = "PAID" },
                        label = { Text("Paid", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "CASH",
                        onClick = { selectedFilter = "CASH" },
                        label = { Text("Cash Receipts", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "UPI",
                        onClick = { selectedFilter = "UPI" },
                        label = { Text("UPI / Online", fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredPayments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No payment records match your filters",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredPayments, key = { it.id }) { p ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedPaymentForReceipt = p },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = p.studentName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (p.paymentMode.equals("Cash", ignoreCase = true)) Color(0xFFFFF3E0) else Color(0xFFE8F8EE)
                                ) {
                                    Text(
                                        text = p.paymentMode,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (p.paymentMode.equals("Cash", ignoreCase = true)) Color(0xFFE65100) else PrimaryGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(text = "${p.id} • ${p.dateStr}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = p.shiftDescription, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "₹${p.amount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryGreen)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE8F8EE)
                            ) {
                                Text(
                                    text = p.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Record Cash / Manual Payment
    if (showRecordCashDialog) {
        AlertDialog(
            onDismissRequest = { showRecordCashDialog = false },
            title = {
                Text(
                    text = "Collect Cash / Offline Fee",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Record payment collected at the library counter in cash or direct bank transfer. A receipt will be generated automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Student picker / entry
                    if (students.isNotEmpty()) {
                        Text("Select Student:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        var expandedStudentDropdown by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedStudentDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (manualStudentName.isNotBlank()) "$manualStudentName ($selectedStudentId)" else "Select a student...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedStudentDropdown,
                                onDismissRequest = { expandedStudentDropdown = false }
                            ) {
                                students.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.fullName} (${s.id})") },
                                        onClick = {
                                            selectedStudentId = s.id
                                            manualStudentName = s.fullName
                                            expandedStudentDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = manualStudentName,
                            onValueChange = { manualStudentName = it },
                            label = { Text("Student Name *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Shift selection
                    if (shifts.isNotEmpty()) {
                        Text("Select Shift Plan:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        var expandedShiftDropdown by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expandedShiftDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedShiftDesc,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = expandedShiftDropdown,
                                onDismissRequest = { expandedShiftDropdown = false }
                            ) {
                                shifts.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.title} (${s.timeRange}) - ₹${s.monthlyFee}") },
                                        onClick = {
                                            selectedShiftDesc = "${s.title} (${s.timeRange})"
                                            manualAmountText = s.monthlyFee.toString()
                                            expandedShiftDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Amount input
                    OutlinedTextField(
                        value = manualAmountText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) manualAmountText = it },
                        label = { Text("Amount Received (₹) *") },
                        placeholder = { Text("500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment Mode chips
                    Text("Payment Mode:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "Direct UPI", "Bank Transfer").forEach { mode ->
                            FilterChip(
                                selected = manualPaymentMode == mode,
                                onClick = { manualPaymentMode = mode },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Remarks
                    OutlinedTextField(
                        value = manualRemarks,
                        onValueChange = { manualRemarks = it },
                        label = { Text("Receipt Notes / Remarks") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    recordError?.let { err ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = err,
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = manualAmountText.toIntOrNull()
                        if (manualStudentName.isBlank() || amt == null || amt <= 0) {
                            recordError = "Please select a valid student and enter payment amount."
                            return@Button
                        }
                        val stuId = if (selectedStudentId.isNotBlank()) selectedStudentId else "STU-${(1000..9999).random()}"
                        viewModel.recordManualPayment(
                            studentId = stuId,
                            studentName = manualStudentName.trim(),
                            amount = amt,
                            shiftDescription = selectedShiftDesc,
                            paymentMode = manualPaymentMode,
                            remarks = manualRemarks.trim(),
                            onSuccess = {
                                showRecordCashDialog = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Issue Receipt & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordCashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Receipt Detail View
    selectedPaymentForReceipt?.let { rec ->
        AlertDialog(
            onDismissRequest = { selectedPaymentForReceipt = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Official Fee Receipt", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE8F8EE)) {
                        Text(
                            text = "PAID",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("MAA DURGA DIGITAL LIBRARY", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryGreen)
                            Text("Ghazipur, Uttar Pradesh", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("₹${rec.amount}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                            Text("Payment Receipt", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Receipt No:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rec.id, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Student Name:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rec.studentName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Date & Time:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rec.dateStr, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Plan / Shift:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rec.shiftDescription, fontSize = 12.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mode:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rec.paymentMode, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (rec.remarks.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remarks:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(rec.remarks, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPaymentForReceipt = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Update Library UPI Gateway ID", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        text = "Enter your verified PhonePe / Bank UPI ID (e.g. 9569556006@ybl or username@ybl):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editUpiText,
                        onValueChange = { editUpiText = it },
                        label = { Text("UPI ID") },
                        placeholder = { Text("9569556006@ybl") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editUpiText.isNotBlank()) {
                            viewModel.updateAdminUpiId(editUpiText.trim())
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminComplaintsContent(
    complaints: List<com.example.data.model.Complaint>,
    viewModel: MainViewModel
) {
    var replyText by remember { mutableStateOf("") }
    var selectedComplaintId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(complaints) { cmp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "${cmp.category} (${cmp.studentName})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (cmp.status == "Resolved") Color(0xFFE8F8EE) else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = cmp.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cmp.status == "Resolved") PrimaryGreen else Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(text = cmp.description, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)

                    // Display attached photo if student uploaded one
                    if (!cmp.imageUri.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Attached Photo / Evidence:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        ) {
                            AsyncImage(
                                model = cmp.imageUri,
                                contentDescription = "Complaint Attachment",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date: ${cmp.dateStr}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (cmp.status != "Resolved") {
                            Button(
                                onClick = {
                                    viewModel.resolveComplaint(cmp.id, "Resolved by Admin.")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Mark Resolved", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAnnouncementsContent(viewModel: MainViewModel) {
    val announcements by viewModel.adminAllAnnouncements.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<com.example.data.model.Announcement?>(null) }
    var deletingAnnouncementId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Library Announcements",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${announcements.size} active announcements broadcasted",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("New Notice", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))

        if (announcements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "No Announcements Published",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Broadcast library rules, timing changes, maintenance alerts, and exam notices to all students.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Create First Announcement")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(announcements, key = { it.id }) { ann ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (ann.priority) {
                                                    "Urgent" -> Color(0xFFFFEBEE)
                                                    "Important" -> Color(0xFFFFF3E0)
                                                    else -> Color(0xFFE8F8EE)
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Campaign,
                                            contentDescription = null,
                                            tint = when (ann.priority) {
                                                "Urgent" -> Color(0xFFC62828)
                                                "Important" -> Color(0xFFE65100)
                                                else -> PrimaryGreen
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(10.dp))

                                    Text(
                                        text = ann.title,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (ann.priority) {
                                        "Urgent" -> Color(0xFFFFEBEE)
                                        "Important" -> Color(0xFFFFF3E0)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = ann.priority,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (ann.priority) {
                                            "Urgent" -> Color(0xFFC62828)
                                            "Important" -> Color(0xFFE65100)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = ann.description,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Posted: ${ann.dateStr}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    val isExpired = ann.expiryDateMillis != null && ann.expiryDateMillis < System.currentTimeMillis()
                                    if (ann.expiryDateStr != null) {
                                        Text(
                                            text = if (isExpired) "Expired: ${ann.expiryDateStr}" else "Active till: ${ann.expiryDateStr} (${ann.expiryDays}d)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isExpired) Color(0xFFC62828) else PrimaryGreen
                                        )
                                    }
                                }

                                Row {
                                    TextButton(
                                        onClick = { editingAnnouncement = ann },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(15.dp),
                                            tint = PrimaryGreen
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit", fontSize = 12.sp, color = PrimaryGreen)
                                    }

                                    TextButton(
                                        onClick = { deletingAnnouncementId = ann.id },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(15.dp),
                                            tint = Color(0xFFC62828)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Delete", fontSize = 12.sp, color = Color(0xFFC62828))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Announcement Dialog
    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("Normal") }

        data class AnnDurationOption(val label: String, val chipLabel: String, val millis: Long)
        val durationPresets = remember {
            listOf(
                AnnDurationOption("6 Hours", "6h", 6L * 3600 * 1000),
                AnnDurationOption("12 Hours", "12h", 12L * 3600 * 1000),
                AnnDurationOption("1 Day", "1d", 24L * 3600 * 1000),
                AnnDurationOption("2 Days", "2d", 2L * 24 * 3600 * 1000),
                AnnDurationOption("3 Days", "3d", 3L * 24 * 3600 * 1000),
                AnnDurationOption("5 Days", "5d", 5L * 24 * 3600 * 1000),
                AnnDurationOption("7 Days", "7d", 7L * 24 * 3600 * 1000),
                AnnDurationOption("15 Days", "15d", 15L * 24 * 3600 * 1000),
                AnnDurationOption("30 Days", "30d", 30L * 24 * 3600 * 1000),
                AnnDurationOption("Permanent", "Permanent ♾️", 365L * 24 * 3600 * 1000)
            )
        }
        var selectedPresetIdx by remember { mutableIntStateOf(3) } // default 2 Days
        var isCustomDuration by remember { mutableStateOf(true) } // Open custom duration directly with no limits
        var customNumberInput by remember { mutableStateOf("2") }
        var customUnit by remember { mutableStateOf("Days") } // "Hours" or "Days"

        val effectiveDurationMillis: Long = remember(isCustomDuration, selectedPresetIdx, customNumberInput, customUnit) {
            if (isCustomDuration) {
                val num = customNumberInput.trim().toLongOrNull()?.coerceAtLeast(1) ?: 1L
                if (customUnit == "Hours") num * 3600 * 1000L
                else num * 24L * 3600 * 1000L
            } else {
                durationPresets[selectedPresetIdx].millis
            }
        }

        val effectiveDurationLabel: String = remember(isCustomDuration, selectedPresetIdx, customNumberInput, customUnit) {
            if (isCustomDuration) {
                val num = customNumberInput.trim().toLongOrNull()?.coerceAtLeast(1) ?: 1L
                "$num $customUnit"
            } else {
                durationPresets[selectedPresetIdx].label
            }
        }

        val calculatedExpiryDate = remember(effectiveDurationMillis, isCustomDuration, selectedPresetIdx) {
            val isPermanent = !isCustomDuration && durationPresets[selectedPresetIdx].label == "Permanent"
            if (isPermanent) {
                "Always Active (No auto-expiry)"
            } else {
                val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
                cal.timeInMillis = System.currentTimeMillis() + effectiveDurationMillis
                val sdf = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.ENGLISH).apply {
                    timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
                }
                sdf.format(cal.time)
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Publish New Announcement", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Announcement Title") },
                        placeholder = { Text("e.g. Sunday Revision Timing") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Details & Description") },
                        placeholder = { Text("Write full message here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Priority Level:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Normal", "Important", "Urgent").forEach { pLevel ->
                            val isSelected = priority == pLevel
                            FilterChip(
                                selected = isSelected,
                                onClick = { priority = pLevel },
                                label = { Text(pLevel, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (pLevel) {
                                        "Urgent" -> Color(0xFFFFEBEE)
                                        "Important" -> Color(0xFFFFF3E0)
                                        else -> PrimaryGreen.copy(alpha = 0.15f)
                                    },
                                    selectedLabelColor = when (pLevel) {
                                        "Urgent" -> Color(0xFFC62828)
                                        "Important" -> Color(0xFFE65100)
                                        else -> PrimaryGreen
                                    }
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Expiry Duration Selector (Presets & Custom Duration with no limits)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Display Duration / Expiry:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        TextButton(
                            onClick = { isCustomDuration = !isCustomDuration },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (isCustomDuration) "Use Presets" else "Set Custom Time ⚙️",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))

                    if (!isCustomDuration) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            durationPresets.forEachIndexed { index, opt ->
                                val isSelected = selectedPresetIdx == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPresetIdx = index },
                                    label = { Text(opt.chipLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.18f),
                                        selectedLabelColor = PrimaryGreen
                                    )
                                )
                            }
                        }
                    } else {
                        // Custom Duration Input: Admin sets exact number of Hours or Days
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Custom Duration (Enter any hours or days):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customNumberInput,
                                        onValueChange = { customNumberInput = it.filter { ch -> ch.isDigit() } },
                                        placeholder = { Text("e.g. 2") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryGreen,
                                            focusedLabelColor = PrimaryGreen
                                        )
                                    )

                                    // Unit Toggle (Hours / Days)
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surface)
                                            .padding(2.dp)
                                    ) {
                                        listOf("Hours", "Days").forEach { unit ->
                                            val isUnitSelected = customUnit == unit
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (isUnitSelected) PrimaryGreen else Color.Transparent)
                                                    .clickable { customUnit = unit }
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = unit,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isUnitSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isUnitSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Expires: $calculatedExpiryDate ($effectiveDurationLabel)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && description.isNotBlank()) {
                            val days = (effectiveDurationMillis / (24L * 3600 * 1000)).toInt().coerceAtLeast(1)
                            viewModel.publishAnnouncement(
                                title = title.trim(),
                                desc = description.trim(),
                                priority = priority,
                                expiryDays = days,
                                durationMillis = effectiveDurationMillis,
                                durationDisplayStr = effectiveDurationLabel
                            ) {
                                showCreateDialog = false
                            }
                        }
                    },
                    enabled = title.isNotBlank() && description.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Announcement Dialog
    editingAnnouncement?.let { ann ->
        var editTitle by remember { mutableStateOf(ann.title) }
        var editDesc by remember { mutableStateOf(ann.description) }
        var editPriority by remember { mutableStateOf(ann.priority) }
        var editDurationDays by remember { mutableStateOf(ann.expiryDays) }

        val editCalculatedExpiryDate = remember(editDurationDays) {
            val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            cal.add(java.util.Calendar.DAY_OF_YEAR, editDurationDays)
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
            }
            sdf.format(cal.time)
        }

        AlertDialog(
            onDismissRequest = { editingAnnouncement = null },
            title = { Text("Edit Announcement", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )

                    Spacer(Modifier.height(12.dp))

                    Text("Priority Level:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Normal", "Important", "Urgent").forEach { pLevel ->
                            val isSelected = editPriority == pLevel
                            FilterChip(
                                selected = isSelected,
                                onClick = { editPriority = pLevel },
                                label = { Text(pLevel, fontSize = 11.5.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (pLevel) {
                                        "Urgent" -> Color(0xFFFFEBEE)
                                        "Important" -> Color(0xFFFFF3E0)
                                        else -> PrimaryGreen.copy(alpha = 0.15f)
                                    },
                                    selectedLabelColor = when (pLevel) {
                                        "Urgent" -> Color(0xFFC62828)
                                        "Important" -> Color(0xFFE65100)
                                        else -> PrimaryGreen
                                    }
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text("Display Duration / Expiry in Days:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3, 5, 7, 15, 30).forEach { days ->
                            val isSelected = editDurationDays == days
                            FilterChip(
                                selected = isSelected,
                                onClick = { editDurationDays = days },
                                label = { Text("${days}d", fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryGreen.copy(alpha = 0.18f),
                                    selectedLabelColor = PrimaryGreen
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editDurationDays.toString(),
                        onValueChange = { input ->
                            val parsed = input.filter { it.isDigit() }.toIntOrNull()
                            if (parsed != null && parsed in 1..365) {
                                editDurationDays = parsed
                            }
                        },
                        label = { Text("Custom Expiry in Days") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            focusedLabelColor = PrimaryGreen
                        )
                    )

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "New Expiry: $editCalculatedExpiryDate ($editDurationDays days)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank() && editDesc.isNotBlank()) {
                            val newExpiryMillis = System.currentTimeMillis() + (editDurationDays * 24L * 60 * 60 * 1000L)
                            viewModel.updateAnnouncement(
                                ann.copy(
                                    title = editTitle.trim(),
                                    description = editDesc.trim(),
                                    priority = editPriority,
                                    expiryDays = editDurationDays,
                                    expiryDateStr = editCalculatedExpiryDate,
                                    expiryDateMillis = newExpiryMillis
                                )
                            ) {
                                editingAnnouncement = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAnnouncement = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    deletingAnnouncementId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingAnnouncementId = null },
            title = { Text("Delete Announcement?") },
            text = { Text("Are you sure you want to remove this announcement? It will no longer appear on students' Home screens.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAnnouncement(id) {
                            deletingAnnouncementId = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAnnouncementId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminPaymentVerificationsContent(
    viewModel: MainViewModel
) {
    val verifications by viewModel.allPaymentVerifications.collectAsState()
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    var selectedFilter by remember { mutableStateOf("PENDING") } // ALL, PENDING, CONFIRMED, DECLINED
    var searchQuery by remember { mutableStateOf("") }

    var confirmDialogItem by remember { mutableStateOf<PaymentVerificationRequest?>(null) }
    var declineDialogItem by remember { mutableStateOf<PaymentVerificationRequest?>(null) }
    var selectedDeclinePreset by remember { mutableStateOf("Payment Not Received in Bank / UPI") }
    var customDeclineNotes by remember { mutableStateOf("") }

    var fullScreenScreenshotUri by remember { mutableStateOf<String?>(null) }
    var isProcessingAction by remember { mutableStateOf(false) }
    var actionErrorMsg by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(verifications, selectedFilter, searchQuery) {
        verifications.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> item.status.equals("PENDING", ignoreCase = true)
                "CONFIRMED" -> item.status.equals("CONFIRMED", ignoreCase = true)
                "DECLINED" -> item.status.equals("DECLINED", ignoreCase = true)
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                    item.studentName.lowercase().contains(q) ||
                    item.studentMobile.contains(q) ||
                    item.utrNumber.lowercase().contains(q) ||
                    item.seatNumber.contains(q)
            matchesFilter && matchesSearch
        }
    }

    val pendingCount = remember(verifications) {
        verifications.count { it.status.equals("PENDING", ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Manual UPI Verifications",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Review UTRs & screenshots to confirm seat allotments",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (pendingCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFE65100),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "$pendingCount Pending",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Bar & Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, phone, UTR, or seat...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Filter Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PENDING", "ALL", "CONFIRMED", "DECLINED").forEach { statusKey ->
                        val isSelected = selectedFilter == statusKey
                        val count = remember(verifications, statusKey) {
                            when (statusKey) {
                                "ALL" -> verifications.size
                                else -> verifications.count { it.status.equals(statusKey, ignoreCase = true) }
                            }
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = statusKey },
                            label = {
                                Text(
                                    text = when (statusKey) {
                                        "PENDING" -> "Pending ($count)"
                                        "CONFIRMED" -> "Confirmed ($count)"
                                        "DECLINED" -> "Declined ($count)"
                                        else -> "All ($count)"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (statusKey) {
                                    "PENDING" -> Color(0xFFE65100)
                                    "CONFIRMED" -> PrimaryGreen
                                    "DECLINED" -> Color(0xFFC62828)
                                    else -> PrimaryGreen
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No verification requests found", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("All student payments for this filter are up to date.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (item.status.uppercase()) {
                            "PENDING" -> Color(0xFFFFFDE7)
                            "CONFIRMED" -> Color(0xFFF1F8E9)
                            "DECLINED" -> Color(0xFFFFEBEE)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = when (item.status.uppercase()) {
                            "PENDING" -> Color(0xFFFFB300)
                            "CONFIRMED" -> Color(0xFF81C784)
                            "DECLINED" -> Color(0xFFEF9A9A)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Top row: Student Name & Status Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.studentName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "📞 ${item.studentMobile} • ${item.requestDateStr}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (item.status.uppercase()) {
                                    "PENDING" -> Color(0xFFE65100)
                                    "CONFIRMED" -> PrimaryGreen
                                    "DECLINED" -> Color(0xFFC62828)
                                    else -> Color.Gray
                                },
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = when (item.status.uppercase()) {
                                        "PENDING" -> "UNDER VERIFICATION"
                                        "CONFIRMED" -> "CONFIRMED"
                                        "DECLINED" -> "DECLINED"
                                        else -> item.status
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Details Grid
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Requested Seat", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "Seat ${item.seatNumber}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Shift(s)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = item.shiftTitles,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Plan Duration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${item.durationMonths} Month${if (item.durationMonths > 1) "s" else ""}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 2.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Total Payable Amount", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = "₹${item.amount}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryGreen
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("12-Digit UTR / Ref #", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = item.utrNumber.ifBlank { "Not Provided" },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (item.utrNumber.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(item.utrNumber))
                                                android.widget.Toast.makeText(context, "UTR ${item.utrNumber} copied!", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy UTR", modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Copy UTR", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Screenshot thumbnail section
                        if (item.proofImageUri.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Payment Proof Screenshot:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.05f))
                                    .clickable { fullScreenScreenshotUri = item.proofImageUri },
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = Uri.parse(item.proofImageUri),
                                    contentDescription = "Payment Screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.Black.copy(alpha = 0.65f),
                                    contentColor = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Tap to Enlarge", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }

                        // Decline notes if status is DECLINED
                        if (item.status.equals("DECLINED", ignoreCase = true) && !item.adminNotes.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFCDD2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Decline Reason: ${item.adminNotes}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB71C1C),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Actions for PENDING status
                        if (item.status.equals("PENDING", ignoreCase = true)) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        declineDialogItem = item
                                        selectedDeclinePreset = "Payment Not Received in Bank / UPI"
                                        customDeclineNotes = ""
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Decline", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { confirmDialogItem = item },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Confirm Payment", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirm Payment Dialog
    confirmDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) confirmDialogItem = null },
            title = {
                Text("Confirm Payment & Allot Seat", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Are you sure you want to verify this payment of ₹${item.amount} for Seat ${item.seatNumber}?",
                        fontSize = 13.5.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Student: ${item.studentName} (${item.studentMobile})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("Seat: Seat ${item.seatNumber} • ${item.shiftTitles}", fontSize = 12.sp)
                            Text("UTR: ${item.utrNumber}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Text(
                        text = "This action will atomically confirm the seat, activate their membership plan, record the payment as Paid, and grant immediate library admission.",
                        fontSize = 11.5.sp,
                        color = PrimaryGreen
                    )
                    actionErrorMsg?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingAction = true
                        actionErrorMsg = null
                        viewModel.confirmPaymentVerification(
                            requestId = item.id,
                            onSuccess = {
                                isProcessingAction = false
                                confirmDialogItem = null
                                android.widget.Toast.makeText(context, "Payment confirmed! Seat ${item.seatNumber} allotted to ${item.studentName}.", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                isProcessingAction = false
                                actionErrorMsg = err
                            }
                        )
                    },
                    enabled = !isProcessingAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(if (isProcessingAction) "Confirming..." else "Yes, Confirm & Allot")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmDialogItem = null },
                    enabled = !isProcessingAction
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Decline Payment Dialog
    declineDialogItem?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) declineDialogItem = null },
            title = {
                Text("Decline Payment Verification", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFC62828))
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Declining will immediately release Seat ${item.seatNumber} back to available seats and notify the student with the specified reason.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text("Select Reason for Decline:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    val reasons = listOf(
                        "Payment Not Received in Bank / UPI",
                        "Invalid / Fake 12-Digit UTR",
                        "Amount Mismatch (Paid less than required)",
                        "Screenshot Unclear or Tampered",
                        "Other (Custom Reason)"
                    )

                    reasons.forEach { preset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDeclinePreset = preset }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedDeclinePreset == preset,
                                onClick = { selectedDeclinePreset = preset }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(preset, fontSize = 12.5.sp)
                        }
                    }

                    OutlinedTextField(
                        value = customDeclineNotes,
                        onValueChange = { customDeclineNotes = it },
                        label = { Text("Additional Remarks / Feedback for Student") },
                        placeholder = { Text("e.g. Please re-check your UPI transaction status or pay ₹${item.amount} again.") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    actionErrorMsg?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingAction = true
                        actionErrorMsg = null
                        val finalReason = if (customDeclineNotes.isNotBlank()) {
                            "$selectedDeclinePreset: ${customDeclineNotes.trim()}"
                        } else {
                            selectedDeclinePreset
                        }
                        viewModel.declinePaymentVerification(
                            requestId = item.id,
                            declineReason = finalReason,
                            onSuccess = {
                                isProcessingAction = false
                                declineDialogItem = null
                                android.widget.Toast.makeText(context, "Payment declined. Seat ${item.seatNumber} has been released.", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                isProcessingAction = false
                                actionErrorMsg = err
                            }
                        )
                    },
                    enabled = !isProcessingAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(if (isProcessingAction) "Declining..." else "Decline & Release Seat", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { declineDialogItem = null },
                    enabled = !isProcessingAction
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full-screen Screenshot Viewer Dialog
    fullScreenScreenshotUri?.let { uriStr ->
        AlertDialog(
            onDismissRequest = { fullScreenScreenshotUri = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Screenshot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = { fullScreenScreenshotUri = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = Uri.parse(uriStr),
                        contentDescription = "Full Screenshot",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { fullScreenScreenshotUri = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun AdminPasswordResetRequestsContent(
    viewModel: MainViewModel
) {
    val resetRequests by viewModel.allResetRequests.collectAsState()
    val context = LocalContext.current

    var selectedFilter by remember { mutableStateOf("PENDING") } // ALL, PENDING, APPROVED, REJECTED
    var searchQuery by remember { mutableStateOf("") }

    var approveDialogItem by remember { mutableStateOf<PasswordResetRequest?>(null) }
    var rejectDialogItem by remember { mutableStateOf<PasswordResetRequest?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    var isProcessingAction by remember { mutableStateOf(false) }
    var actionErrorMsg by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(resetRequests, selectedFilter, searchQuery) {
        resetRequests.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> item.status.equals("PENDING", ignoreCase = true)
                "APPROVED" -> item.status.equals("APPROVED", ignoreCase = true)
                "REJECTED" -> item.status.equals("REJECTED", ignoreCase = true)
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                    item.userName.lowercase().contains(q) ||
                    item.mobile.contains(q)
            matchesFilter && matchesSearch
        }
    }

    val pendingCount = remember(resetRequests) {
        resetRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Student Password Requests",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Approve or reject student password change & recovery requests",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (pendingCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF1565C0),
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = "$pendingCount Pending",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search Bar & Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by user name or mobile number...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Filter Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("PENDING", "ALL", "APPROVED", "REJECTED").forEach { statusKey ->
                        val isSelected = selectedFilter == statusKey
                        val count = remember(resetRequests, statusKey) {
                            when (statusKey) {
                                "ALL" -> resetRequests.size
                                else -> resetRequests.count { it.status.equals(statusKey, ignoreCase = true) }
                            }
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = statusKey },
                            label = {
                                Text(
                                    text = when (statusKey) {
                                        "PENDING" -> "Pending ($count)"
                                        "APPROVED" -> "Approved ($count)"
                                        "REJECTED" -> "Rejected ($count)"
                                        else -> "All ($count)"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = when (statusKey) {
                                    "PENDING" -> Color(0xFF1565C0)
                                    "APPROVED" -> PrimaryGreen
                                    "REJECTED" -> Color(0xFFC62828)
                                    else -> PrimaryGreen
                                },
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        if (filteredList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("No password reset requests found", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("No student accounts currently need password reset action.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (item.status.uppercase()) {
                            "PENDING" -> Color(0xFFE3F2FD)
                            "APPROVED" -> Color(0xFFF1F8E9)
                            "REJECTED" -> Color(0xFFFFEBEE)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = when (item.status.uppercase()) {
                            "PENDING" -> Color(0xFF90CAF9)
                            "APPROVED" -> Color(0xFF81C784)
                            "REJECTED" -> Color(0xFFEF9A9A)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.userName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "📱 ${item.mobile}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = "Requested: ${item.requestDateStr}",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                val isChangeReq = item.requestType == "CHANGE"
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isChangeReq) Color(0xFF00695C) else Color(0xFF1565C0),
                                    contentColor = Color.White
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isChangeReq) Icons.Default.SyncLock else Icons.Default.Key,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isChangeReq) "PROFILE PASSWORD CHANGE (Current Verified)" else "FORGOT PASSWORD RECOVERY",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (item.status.uppercase()) {
                                    "PENDING" -> Color(0xFF1565C0)
                                    "APPROVED" -> PrimaryGreen
                                    "REJECTED" -> Color(0xFFC62828)
                                    else -> Color.Gray
                                },
                                contentColor = Color.White
                            ) {
                                Text(
                                    text = when (item.status.uppercase()) {
                                        "PENDING" -> "AWAITING APPROVAL"
                                        "APPROVED" -> "APPROVED"
                                        "REJECTED" -> "REJECTED"
                                        else -> item.status
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Security explanation
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Password Security: The student's chosen password is cryptographically salted & hashed (Argon2 / PBKDF2 / SHA-256). Admin cannot view plain passwords.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // If rejected with remarks
                        if (item.status.equals("REJECTED", ignoreCase = true) && !item.adminNotes.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFCDD2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Rejection Reason: ${item.adminNotes}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFB71C1C),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        // Actions for PENDING
                        if (item.status.equals("PENDING", ignoreCase = true)) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        rejectDialogItem = item
                                        rejectReason = ""
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC62828)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Reject", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = { approveDialogItem = item },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (item.requestType == "CHANGE") "Approve Change" else "Approve Reset", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Approve Dialog
    approveDialogItem?.let { item ->
        val isChange = item.requestType == "CHANGE"
        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) approveDialogItem = null },
            title = {
                Text(if (isChange) "Approve Password Change" else "Approve Password Reset", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isChange) "Approve password change request for ${item.userName} (${item.mobile})?" else "Approve password reset request for ${item.userName} (${item.mobile})?",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isChange) "The student verified their current password upon submitting. Approving will immediately activate their new password." else "Their new requested password will immediately become active, allowing them to log in to the student app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    actionErrorMsg?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingAction = true
                        actionErrorMsg = null
                        viewModel.approvePasswordResetRequest(
                            requestId = item.id,
                            onSuccess = {
                                isProcessingAction = false
                                approveDialogItem = null
                                val actionLabel = if (isChange) "Password change" else "Password reset"
                                android.widget.Toast.makeText(context, "$actionLabel approved for ${item.userName}!", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                isProcessingAction = false
                                actionErrorMsg = err
                            }
                        )
                    },
                    enabled = !isProcessingAction,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(if (isProcessingAction) "Approving..." else if (isChange) "Yes, Approve Change" else "Yes, Approve Reset")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { approveDialogItem = null },
                    enabled = !isProcessingAction
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reject Dialog
    rejectDialogItem?.let { item ->
        val isChange = item.requestType == "CHANGE"
        AlertDialog(
            onDismissRequest = { if (!isProcessingAction) rejectDialogItem = null },
            title = {
                Text(if (isChange) "Reject Password Change" else "Reject Password Reset", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFC62828))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isChange) "Reject password change request for ${item.userName} (${item.mobile})?" else "Reject password reset request for ${item.userName} (${item.mobile})?",
                        fontSize = 13.5.sp
                    )
                    if (isChange) {
                        Text(
                            text = "Their existing password will remain unchanged.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason for Rejection (Optional)") },
                        placeholder = { Text("e.g. Identity could not be verified") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    actionErrorMsg?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isProcessingAction = true
                        actionErrorMsg = null
                        viewModel.rejectPasswordResetRequest(
                            requestId = item.id,
                            adminNotes = rejectReason.ifBlank { null },
                            onSuccess = {
                                isProcessingAction = false
                                rejectDialogItem = null
                                val actionLabel = if (isChange) "Password change" else "Password reset"
                                android.widget.Toast.makeText(context, "$actionLabel rejected for ${item.userName}.", android.widget.Toast.LENGTH_LONG).show()
                            },
                            onError = { err ->
                                isProcessingAction = false
                                actionErrorMsg = err
                            }
                        )
                    },
                    enabled = !isProcessingAction,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text(if (isProcessingAction) "Rejecting..." else "Reject Request", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { rejectDialogItem = null },
                    enabled = !isProcessingAction
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
