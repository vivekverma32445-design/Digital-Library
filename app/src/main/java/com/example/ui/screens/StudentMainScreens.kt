package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.util.AppImageViewer
import com.example.util.ImageUtils
import com.example.data.model.SeatAvailabilityState
import com.example.data.model.SeatVisualItem
import com.example.ui.MainViewModel
import com.example.ui.components.CameraQrScanner
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.DynamicUpiQrCanvas
import com.example.ui.theme.*

@Composable
fun StudentHomeScreen(
    viewModel: MainViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSeatSelection: () -> Unit,
    onNavigateToQrScan: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToComplaints: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val membership by viewModel.activeMembership.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val dismissedAnnouncements by viewModel.dismissedAnnouncements.collectAsState()
    var localDismissedInSession by remember { mutableStateOf<Set<String>>(emptySet()) }
    val unreadPopupAnnouncement = remember(announcements, dismissedAnnouncements, localDismissedInSession) {
        announcements.firstOrNull { ann ->
            !dismissedAnnouncements.contains(ann.id) &&
            !localDismissedInSession.contains(ann.id) &&
            !viewModel.isAnnouncementDismissed(ann.id)
        }
    }
    val studentAttendance by viewModel.studentAttendance.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val unreadNotifCount by viewModel.unreadNotificationCount.collectAsState()

    val daysRemaining = membership?.getDaysRemaining() ?: 0
    val isExpired = membership?.isExpired == true || (membership != null && daysRemaining <= 0)
    val hasValidMembership = membership != null && !isExpired
    var showNoMembershipDialog by remember { mutableStateOf(false) }

    // 10-Day Daily Membership Expiry Reminder
    var showDailyExpiryReminderDialog by remember { mutableStateOf(false) }
    var dismissedReminderInSession by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser?.id, membership, daysRemaining) {
        val studentId = currentUser?.id
        if (studentId != null && hasValidMembership && daysRemaining in 1..10 && !dismissedReminderInSession) {
            if (viewModel.shouldShowDailyExpiryReminder(studentId, daysRemaining)) {
                showDailyExpiryReminderDialog = true
            }
        }
    }

    val studentVerificationRequests by viewModel.studentPaymentVerificationRequests.collectAsState()
    val pendingVerification = remember(studentVerificationRequests) {
        studentVerificationRequests.firstOrNull { it.status.equals("PENDING", ignoreCase = true) }
    }
    val latestDeclinedVerification = remember(studentVerificationRequests) {
        studentVerificationRequests.firstOrNull { it.status.equals("DECLINED", ignoreCase = true) }
    }

    LaunchedEffect(Unit) {
        viewModel.checkMembershipExpiries()
    }

    val todayRecord by viewModel.todayAttendance.collectAsState()
    val isInsideNow = todayRecord != null && (todayRecord!!.isInside || todayRecord!!.exitTime == null)
    val isCompletedToday = todayRecord != null && !isInsideNow && (todayRecord!!.status.equals("COMPLETED", ignoreCase = true) || todayRecord!!.exitTime != null)
    val context = LocalContext.current
    var showCameraPermissionRationaleDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!hasValidMembership) {
                showNoMembershipDialog = true
            } else {
                onNavigateToQrScan()
            }
        } else {
            val activity = context as? Activity
            val shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            if (!shouldShowRationale) {
                showCameraPermissionRationaleDialog = true
            }
        }
    }

    val displayDuration = when {
        isInsideNow -> "Running"
        isCompletedToday -> todayRecord?.duration ?: "0m"
        else -> "0m"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${viewModel.getDynamicGreeting()} 👋",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Ready to study today?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Bell / Notification Icon with Unread Badge
                IconButton(
                    onClick = onNavigateToNotifications,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifCount > 0) {
                                Badge(
                                    containerColor = PrimaryGreen,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadNotifCount > 9) "9+" else unreadNotifCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.toggleDarkTheme() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val isDark by viewModel.isDarkTheme.collectAsState()
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = PrimaryGreen
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Membership Expiry & Reminder Banner (5-day countdown & expired alert)
        val isExpiringSoon = membership != null && !isExpired && daysRemaining in 1..5

        if (isExpired) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Membership Expired ⚠️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            text = "Your seat (${membership?.seatNumber}) has expired. Renew your plan to regain access and mark attendance.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToSeatSelection,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Renew", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else if (isExpiringSoon) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⏳ Expiring in $daysRemaining Day${if (daysRemaining > 1) "s" else ""}!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFE65100)
                        )
                        Text(
                            text = "Valid till ${membership?.expiryDate}. Renew now to retain Seat ${membership?.seatNumber}.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onNavigateToSeatSelection,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Renew", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Pending Seat Verification Banner
        if (pendingVerification != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFFB300))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Booking Under Verification",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFE65100)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE65100),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "PENDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Seat ${pendingVerification.seatNumber} • ${pendingVerification.shiftTitles}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Amount: ₹${pendingVerification.amount} • UTR: ${pendingVerification.utrNumber.ifBlank { "Attached" }}",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFECB3).copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Your seat is safely held and locked. Other students cannot book this seat while pending. The Admin will verify your UPI payment receipt and activate your membership shortly.",
                            fontSize = 11.sp,
                            color = Color(0xFF5D4037),
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onNavigateToPayments,
                        modifier = Modifier.align(Alignment.End),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("View Payment Status", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (membership == null && latestDeclinedVerification != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Booking Request Declined",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFC62828),
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "DECLINED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Seat ${latestDeclinedVerification.seatNumber} • Reason: ${latestDeclinedVerification.adminNotes ?: "Payment verification failed"}",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(
                        onClick = onNavigateToSeatSelection,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Seat & Re-Submit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Membership Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when {
                        membership != null && !isExpired -> {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0F471A), Color(0xFF1B5E20), Color(0xFF2E7D32))
                            )
                        }
                        pendingVerification != null -> {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFF9800))
                            )
                        }
                        else -> {
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFD32F2F), Color(0xFFC62828), Color(0xFFB71C1C))
                            )
                        }
                    }
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CardMembership,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Membership",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            membership != null && !isExpired -> Color(0xFF00C853)
                            pendingVerification != null -> Color(0xFFFFD54F)
                            else -> Color(0xFFFFCDD2)
                        }
                    ) {
                        Text(
                            text = when {
                                membership != null && !isExpired -> "ACTIVE"
                                pendingVerification != null -> "PENDING"
                                isExpired -> "EXPIRED"
                                else -> "INACTIVE"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            color = when {
                                membership != null && !isExpired -> Color.Black
                                pendingVerification != null -> Color(0xFFE65100)
                                else -> Color(0xFFC62828)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                val mem = membership
                if (mem != null && !isExpired) {
                    Text(
                        text = "Seat ${mem.seatNumber} | ${mem.shiftTitles}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "${mem.durationMonths} Month Membership Validity",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    // Daily Countdown Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "⏳ $daysRemaining Days Remaining Until Expiry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Expires on ${mem.expiryDate} • Auto-expires on ${mem.expiryDate}",
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                } else if (pendingVerification != null) {
                    Text(
                        text = "Seat ${pendingVerification.seatNumber} Reserved",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Payment proof submitted • Awaiting Admin confirmation (${pendingVerification.shiftTitles})",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "UTR: ${pendingVerification.utrNumber.ifBlank { "Submitted" }} • ₹${pendingVerification.amount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = if (isExpired) "Membership Expired" else "No Active Membership",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = "Please complete admission to get a seat.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Button(
                        onClick = onNavigateToSeatSelection,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Get Admission", color = Color(0xFFC62828), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Today's Attendance Card
        val attendanceStatus = when {
            isInsideNow -> "INSIDE NOW"
            isCompletedToday -> "CHECKED OUT"
            else -> "NOT CHECKED IN"
        }
        val attendanceStatusColor = when {
            isInsideNow -> PrimaryGreen
            isCompletedToday -> Color(0xFF1565C0)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        val attendanceStatusBg = when {
            isInsideNow -> Color(0xFFE8F8EE)
            isCompletedToday -> Color(0xFFE3F2FD)
            else -> MaterialTheme.colorScheme.surfaceVariant
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFFFF3E0),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "$currentStreak Days",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = attendanceStatusBg
                        ) {
                            Text(
                                text = attendanceStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = attendanceStatusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AttendanceMetricItem(
                        label = "Entry",
                        value = if (todayRecord != null) todayRecord!!.entryTime else "--"
                    )
                    AttendanceMetricItem(
                        label = "Exit",
                        value = when {
                            isInsideNow -> "--"
                            isCompletedToday -> todayRecord?.exitTime ?: "--"
                            else -> "--"
                        }
                    )
                    AttendanceMetricItem(
                        label = "Duration",
                        value = displayDuration
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Entrance QR CTA: First check membership, then camera permission, then navigate to dedicated scanner
        Button(
            onClick = {
                if (!hasValidMembership) {
                    showNoMembershipDialog = true
                    return@Button
                }
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    onNavigateToQrScan()
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isInsideNow) Color(0xFFC62828) else PrimaryGreen
            )
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (isInsideNow) "Scan Entrance QR to Exit" else "Scan Entrance QR to Enter",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Active Membership Requirement Reminder Dialog
        if (showNoMembershipDialog) {
            AlertDialog(
                onDismissRequest = { showNoMembershipDialog = false },
                icon = {
                    Icon(
                        Icons.Default.CardMembership,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Active Membership Required 🎟️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Aapke paas active membership nahi hai. Pehle aap library membership lein, tabhi aap attendance laga sakte hain. Kripya apni seat & shift select karke membership activate karein.",
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNoMembershipDialog = false
                            onNavigateToSeatSelection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Get Membership Now", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoMembershipDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Camera Permission Permanently Denied Rationale Dialog
        if (showCameraPermissionRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showCameraPermissionRationaleDialog = false },
                title = { Text("Camera Permission Required", fontWeight = FontWeight.Bold) },
                text = { Text("Camera permission is required to scan the library QR.") },
                confirmButton = {
                    Button(
                        onClick = {
                            showCameraPermissionRationaleDialog = false
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Open Settings", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCameraPermissionRationaleDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Announcement Modal Popup for New / Unread Notice
        unreadPopupAnnouncement?.let { ann ->
            AlertDialog(
                onDismissRequest = {
                    localDismissedInSession = localDismissedInSession + ann.id
                    viewModel.dismissAnnouncement(ann.id)
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
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
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (ann.priority) {
                                "Urgent" -> Color(0xFFFFEBEE)
                                "Important" -> Color(0xFFFFF3E0)
                                else -> Color(0xFFE8F8EE)
                            }
                        ) {
                            Text(
                                text = "${ann.priority.uppercase()} NOTICE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (ann.priority) {
                                    "Urgent" -> Color(0xFFC62828)
                                    "Important" -> Color(0xFFE65100)
                                    else -> PrimaryGreen
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = ann.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = ann.description,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Posted: ${ann.dateStr}",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ann.expiryDateStr != null) {
                                Text(
                                    text = "Expires: ${ann.expiryDateStr}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            localDismissedInSession = localDismissedInSession + ann.id
                            viewModel.dismissAnnouncement(ann.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Remove / Continue to App", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            )
        }

        // Daily Membership Expiry Reminder Dialog (10 days before expiry)
        if (showDailyExpiryReminderDialog && membership != null && currentUser != null) {
            AlertDialog(
                onDismissRequest = {
                    dismissedReminderInSession = true
                    showDailyExpiryReminderDialog = false
                    viewModel.dismissDailyExpiryReminder(currentUser!!.id)
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = "MEMBERSHIP EXPIRY REMINDER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Aapki Membership Expire Hone Wali Hai",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Namaste ${currentUser!!.fullName},",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "Aapki library seat (${membership!!.seatNumber}) ki membership agle $daysRemaining dino me (${membership!!.expiryDate}) expire hone wali hai.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF8E1),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Valid Till: ${membership!!.expiryDate} ($daysRemaining days left)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                        Text(
                            text = "Kripya samay par renew karein taaki aapki seat confirm rahe aur library me padhai uninterrupted chalti rahe.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dismissedReminderInSession = true
                            showDailyExpiryReminderDialog = false
                            viewModel.dismissDailyExpiryReminder(currentUser!!.id)
                            onNavigateToSeatSelection()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Renew Membership Now", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            dismissedReminderInSession = true
                            showDailyExpiryReminderDialog = false
                            viewModel.dismissDailyExpiryReminder(currentUser!!.id)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Remind Me Tomorrow", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        Spacer(Modifier.height(22.dp))

        // Quick Actions (Seats, Payment, Attendance, Complaints)
        Text(
            text = "Quick Actions",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(
                title = "Seats",
                icon = Icons.Default.EventSeat,
                onClick = onNavigateToSeatSelection
            )
            QuickActionButton(
                title = "Payment",
                icon = Icons.Default.Payment,
                onClick = onNavigateToPayments
            )
            QuickActionButton(
                title = "Attendance",
                icon = Icons.Default.FactCheck,
                onClick = onNavigateToAttendance
            )
            QuickActionButton(
                title = "Complaint",
                icon = Icons.Default.Feedback,
                onClick = onNavigateToComplaints
            )
        }

        Spacer(Modifier.height(20.dp))

        // Latest Announcement Header & Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Latest Announcement",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (announcements.isNotEmpty()) {
                TextButton(onClick = onNavigateToNotifications) {
                    Text(
                        text = "View All (${announcements.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val latestAnnouncement = announcements.firstOrNull()
        if (latestAnnouncement != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onNavigateToNotifications),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f)),
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
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF3E0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = latestAnnouncement.title,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (latestAnnouncement.priority == "Important" || latestAnnouncement.priority == "Urgent") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = latestAnnouncement.priority,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = latestAnnouncement.description,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = latestAnnouncement.dateStr,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Details in Notifications",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryGreen
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Clean Placeholder State when no announcements exist (Requirement 4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Stay Alert for Announcements",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Admin announcements will appear here.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(30.dp))
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
private fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F8EE))
                .border(1.dp, PrimaryGreen.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = PrimaryGreen,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SeatSelectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val selectedShift by viewModel.selectedShiftForMap.collectAsState()
    val selectedSeat by viewModel.selectedSeatNumber.collectAsState()
    val seats by viewModel.seats.collectAsState()
    val allocations by viewModel.repository.getAllocationsForShift(selectedShift).collectAsState(emptyList())
    val currentUser by viewModel.currentUser.collectAsState()
    val activeMembership by viewModel.activeMembership.collectAsState()

    // Check if current user has a confirmed seat in THIS specific shift
    val myAllocInThisShift = allocations.find { it.studentId == currentUser?.id && it.status == "CONFIRMED" }
    val hasBookedSeatInThisShift = myAllocInThisShift != null
    val mySeatInThisShift = myAllocInThisShift?.seatNumber

    // If user has a seat in this shift, select it automatically.
    // If not, ensure an available seat is selected for the new shift so student can easily book a different seat.
    LaunchedEffect(selectedShift, allocations, currentUser, seats) {
        val myAlloc = allocations.find { it.studentId == currentUser?.id && it.status == "CONFIRMED" }
        if (myAlloc != null) {
            viewModel.selectedSeatNumber.value = myAlloc.seatNumber
        } else {
            val occupiedSet = allocations.map { it.seatNumber }.toSet()
            val current = viewModel.selectedSeatNumber.value
            val currentSeatObj = seats.find { it.seatNumber == current }
            val isCurrentValid = current.isNotBlank() && currentSeatObj != null && !currentSeatObj.isMaintenance && !occupiedSet.contains(current)
            if (!isCurrentValid) {
                val firstAvail = seats.firstOrNull { s -> !s.isMaintenance && !occupiedSet.contains(s.seatNumber) }
                if (firstAvail != null) {
                    viewModel.selectedSeatNumber.value = firstAvail.seatNumber
                }
            }
        }
    }

    // Filter states
    var statusFilter by remember { mutableStateOf("ALL") } // "ALL", "AVAILABLE", "OCCUPIED", "RESERVED"
    var seatSearchQuery by remember { mutableStateOf("") }
    var unavailableNoticeMessage by remember { mutableStateOf<String?>(null) }

    // Shift options with exact timings for all 4 shifts (Morning, Afternoon, Evening, Night)
    val shiftOptions = listOf(
        Triple(1, "Shift 1 (Morning)", "6:00 AM - 12:00 PM"),
        Triple(2, "Shift 2 (Afternoon)", "12:00 PM - 6:00 PM"),
        Triple(3, "Shift 3 (Evening)", "6:00 PM - 12:00 AM"),
        Triple(4, "Shift 4 (Night)", "12:00 AM - 6:00 AM")
    )

    // Reserved seats for admission quota/special reservation
    val reservedSeatsSet = remember { setOf<String>() } // Clear demo data
    val occupiedSeatsSet = remember(allocations) { allocations.map { it.seatNumber }.toSet() }

    // Count available seats in current shift
    val availableCount = remember(seats, occupiedSeatsSet, reservedSeatsSet) {
        seats.count { !it.isMaintenance && !occupiedSeatsSet.contains(it.seatNumber) && !reservedSeatsSet.contains(it.seatNumber) }
    }

    // Filter seats based on status and search query
    val filteredSeats = remember(seats, statusFilter, seatSearchQuery, occupiedSeatsSet, reservedSeatsSet, selectedSeat) {
        seats.filter { seat ->
            val isOccupied = occupiedSeatsSet.contains(seat.seatNumber)
            val isReserved = reservedSeatsSet.contains(seat.seatNumber)
            val isMaintenance = seat.isMaintenance
            val isAvailable = !isOccupied && !isReserved && !isMaintenance

            val matchesStatus = when (statusFilter) {
                "AVAILABLE" -> isAvailable
                "OCCUPIED" -> isOccupied
                "RESERVED" -> isReserved
                else -> true
            }

            val matchesSearch = if (seatSearchQuery.isBlank()) true
            else seat.seatNumber.contains(seatSearchQuery.trim(), ignoreCase = true)

            matchesStatus && matchesSearch
        }
    }

    // Check if currently selected seat is valid/available
    val isCurrentSeatAvailable = remember(selectedSeat, occupiedSeatsSet, reservedSeatsSet, seats, hasBookedSeatInThisShift, mySeatInThisShift) {
        if (hasBookedSeatInThisShift && selectedSeat == mySeatInThisShift) {
            true
        } else {
            val seatObj = seats.find { it.seatNumber == selectedSeat }
            seatObj != null && !seatObj.isMaintenance && !occupiedSeatsSet.contains(selectedSeat) && !reservedSeatsSet.contains(selectedSeat)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // Pinned Bottom Bar with Clear CTA: "Apply for Admission" / "Extend Seat"
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (hasBookedSeatInThisShift) "Your Booked Seat (Shift $selectedShift)" else "Selected Seat (Shift $selectedShift)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Seat $selectedSeat",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasBookedSeatInThisShift || isCurrentSeatAvailable) PrimaryGreen else Color(0xFFC62828)
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (hasBookedSeatInThisShift || isCurrentSeatAvailable) Color(0xFFE8F8EE) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = if (hasBookedSeatInThisShift) "Active Seat" else if (isCurrentSeatAvailable) "Available" else "Unavailable",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasBookedSeatInThisShift || isCurrentSeatAvailable) PrimaryGreen else Color(0xFFC62828),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (hasBookedSeatInThisShift && activeMembership != null) {
                            Text(
                                text = "Current validity: till ${activeMembership?.expiryDate}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.selectSeatAndShiftForAdmission(selectedSeat, selectedShift)
                            onContinue()
                        },
                        enabled = if (hasBookedSeatInThisShift) true else isCurrentSeatAvailable,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            disabledContainerColor = PrimaryGreen.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Text(
                            text = if (hasBookedSeatInThisShift) "Extend Seat $mySeatInThisShift" else "Apply for Admission",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Header
            item(span = { GridItemSpan(4) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Available Seats",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Choose your preferred seat and shift based on availability.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Helpful Admission / Active Seat Policy Card
            item(span = { GridItemSpan(4) }) {
                if (hasBookedSeatInThisShift) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F8EE)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Your Booked Seat: Seat $mySeatInThisShift (Shift $selectedShift)",
                                    fontSize = 12.5.sp,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Shift $selectedShift me aapke paas Seat $mySeatInThisShift confirmed hai. Yahan se aap apni Seat $mySeatInThisShift ko aage extend kar sakte hain. Dusri shift ke liye aap doosra shift select karke nayi seat book kar sakte hain.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FAF5)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "You can take admission from here by selecting an available seat and your preferred shift.",
                                fontSize = 12.sp,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Shift Selector (All 4 Shifts clearly visible)
            item(span = { GridItemSpan(4) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select Shift ($availableCount seats open)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    val chunkedShifts = shiftOptions.chunked(2)
                    chunkedShifts.forEach { rowShifts ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowShifts.forEach { (shiftId, shiftName, shiftTiming) ->
                                val isSelected = shiftId == selectedShift
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            viewModel.selectedShiftForMap.value = shiftId
                                            unavailableNoticeMessage = null
                                        }
                                        .padding(vertical = 8.dp, horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = shiftName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = shiftTiming,
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item(span = { GridItemSpan(4) }) {
                OutlinedTextField(
                    value = seatSearchQuery,
                    onValueChange = { seatSearchQuery = it },
                    placeholder = { Text("Search seat (e.g. 05)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            // Filter Chips (Scrollable row so no text wraps or overflows)
            item(span = { GridItemSpan(4) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All Seats", "AVAILABLE" to "Available", "OCCUPIED" to "Occupied", "RESERVED" to "Reserved").forEach { (code, title) ->
                        val isSelected = statusFilter == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { statusFilter = code },
                            label = { Text(title, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryGreen.copy(alpha = 0.15f),
                                selectedLabelColor = PrimaryGreen
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                enabled = true,
                                selected = isSelected
                            ),
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
            }

            // Legend Row
            item(span = { GridItemSpan(4) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendIndicator(color = SeatAvailableBorder, label = "Available")
                    LegendIndicator(color = SeatOccupiedBorder, label = "Occupied")
                    LegendIndicator(color = SeatReservedBorder, label = "Reserved")
                    LegendIndicator(color = PrimaryGreen, label = "Selected")
                    LegendIndicator(color = SeatMaintenanceBorder, label = "Maint.")
                }
            }

            // Inline alert when user taps unavailable seat
            unavailableNoticeMessage?.let { notice ->
                item(span = { GridItemSpan(4) }) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFEBEE),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFEF5350).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = notice,
                                fontSize = 11.sp,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Seat Grid Items
            items(filteredSeats) { seat ->
                val isMySeatInThisShift = hasBookedSeatInThisShift && seat.seatNumber == mySeatInThisShift
                val isOccupied = !isMySeatInThisShift && occupiedSeatsSet.contains(seat.seatNumber)
                val isReserved = reservedSeatsSet.contains(seat.seatNumber)
                val isSelected = seat.seatNumber == selectedSeat || isMySeatInThisShift
                val isMaintenance = seat.isMaintenance

                val state = when {
                    isMaintenance -> SeatAvailabilityState.MAINTENANCE
                    isSelected -> SeatAvailabilityState.SELECTED
                    isOccupied -> SeatAvailabilityState.OCCUPIED
                    isReserved -> SeatAvailabilityState.RESERVED
                    else -> SeatAvailabilityState.AVAILABLE
                }

                SeatGridBox(
                    seatNumber = seat.seatNumber,
                    state = state,
                    onClick = {
                        if (hasBookedSeatInThisShift) {
                            if (seat.seatNumber != mySeatInThisShift) {
                                unavailableNoticeMessage = "Shift $selectedShift me aapke paas pehle se Seat $mySeatInThisShift booked hai. Iss shift me aap doosri seat book nahi kar sakte (sirf apni Seat $mySeatInThisShift ko extend kar sakte hain). Nayi seat lene ke liye doosri shift (jaise Shift 2, 3 ya 4) select karein."
                            } else {
                                viewModel.selectedSeatNumber.value = mySeatInThisShift
                                unavailableNoticeMessage = null
                            }
                            return@SeatGridBox
                        }

                        when (state) {
                            SeatAvailabilityState.AVAILABLE, SeatAvailabilityState.SELECTED -> {
                                viewModel.selectedSeatNumber.value = seat.seatNumber
                                unavailableNoticeMessage = null
                            }
                            SeatAvailabilityState.OCCUPIED -> {
                                unavailableNoticeMessage = "Seat ${seat.seatNumber} is occupied for Shift $selectedShift. Please choose an available green seat."
                            }
                            SeatAvailabilityState.RESERVED -> {
                                unavailableNoticeMessage = "Seat ${seat.seatNumber} is reserved. Please pick an available seat to take admission."
                            }
                            SeatAvailabilityState.MAINTENANCE -> {
                                unavailableNoticeMessage = "Seat ${seat.seatNumber} is under maintenance."
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LegendIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SeatGridBox(
    seatNumber: String,
    state: SeatAvailabilityState,
    onClick: () -> Unit
) {
    val (bgColor, borderColor, textColor, statusText) = when (state) {
        SeatAvailabilityState.AVAILABLE -> Quadruple(SeatAvailableBg, SeatAvailableBorder, SeatAvailableText, "Available")
        SeatAvailabilityState.OCCUPIED -> Quadruple(SeatOccupiedBg, SeatOccupiedBorder, SeatOccupiedText, "Occupied")
        SeatAvailabilityState.RESERVED -> Quadruple(SeatReservedBg, SeatReservedBorder, SeatReservedText, "Reserved")
        SeatAvailabilityState.SELECTED -> Quadruple(SeatSelectedBg, SeatSelectedBorder, SeatSelectedText, "Selected")
        SeatAvailabilityState.MAINTENANCE -> Quadruple(SeatMaintenanceBg, SeatMaintenanceBorder, SeatMaintenanceText, "Maint.")
    }

    Box(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = seatNumber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = statusText,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Medium,
                color = textColor.copy(alpha = 0.85f)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun MembershipSelectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProceedToPay: () -> Unit
) {
    val shifts by viewModel.shifts.collectAsState()
    val selectedShifts by viewModel.selectedShiftsForMembership.collectAsState()
    val selectedSeat by viewModel.selectedSeatNumber.collectAsState()
    val selectedDuration by viewModel.selectedDurationMonths.collectAsState()
    val activeMembership by viewModel.activeMembership.collectAsState()
    val activeMem = activeMembership
    val hasActiveMembership = activeMem != null && !activeMem.isExpired
    val isExtension = hasActiveMembership && activeMem?.seatNumber == selectedSeat

    val totalFee = remember(shifts, selectedShifts, selectedDuration) {
        viewModel.calculateTotalFee(
            customShifts = shifts,
            customSelectedShiftIds = selectedShifts,
            customDuration = selectedDuration
        )
    }
    val durationOptions = listOf(
        1 to "1 Month (30 Days)",
        2 to "2 Months (60 Days)",
        3 to "3 Months (90 Days)",
        6 to "6 Months (180 Days)",
        12 to "12 Months (1 Year)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isExtension) "Extend Seat $selectedSeat Membership" else "Choose Your Membership",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isExtension)
                            "Renewing validity for Seat $selectedSeat (current expiry: ${activeMembership?.expiryDate})"
                        else
                            "Select shifts & duration for Seat $selectedSeat",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isExtension) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F8EE),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Aap apni Seat $selectedSeat ko extend kar rahe hain. Selected validity aapki current expiry date (${activeMembership?.expiryDate}) ke aage jud jayegi.",
                            fontSize = 11.5.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Duration Selector Section
            Text(
                text = "Select Membership Plan Duration",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Automatic deactivation & 5-day daily renewal reminders",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            // Duration Horizontal Grid / Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationOptions.take(3).forEach { (months, label) ->
                    val isSelected = selectedDuration == months
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setMembershipDuration(months) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$months Month${if (months > 1) "s" else ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${months * 30} Days",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationOptions.drop(3).forEach { (months, label) ->
                    val isSelected = selectedDuration == months
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 1.5.dp else 1.dp,
                            if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setMembershipDuration(months) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (months == 12) "1 Year Plan" else "$months Months",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${months * 30} Days Validity",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Shift Selector Section
            Text(
                text = "Select Daily Shifts",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // 4 Shift Checkbox cards
            shifts.forEach { shift ->
                val isChecked = selectedShifts.contains(shift.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            if (isChecked) 1.5.dp else 1.dp,
                            if (isChecked) PrimaryGreen else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { viewModel.toggleShiftForMembership(shift.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) Color(0xFFF1F8F3) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { viewModel.toggleShiftForMembership(shift.id) },
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = shift.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = shift.timeRange,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "₹${shift.monthlyFee}/mo",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Bottom Calculation & Proceed Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selected Shifts", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${selectedShifts.size} Shift(s)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Duration & Validity", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "$selectedDuration Month${if (selectedDuration > 1) "s" else ""} (${selectedDuration * 30} Days)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Amount", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        val monthlyRate = if (selectedShifts.isNotEmpty()) {
                            selectedShifts.sumOf { sId -> shifts.find { it.id == sId }?.monthlyFee ?: 500 }
                        } else 500
                        Text(
                            text = "(${selectedShifts.size} shift${if (selectedShifts.size > 1) "s" else ""} • ₹$monthlyRate/mo × $selectedDuration mo)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "₹$totalFee",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = onProceedToPay,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text(
                        text = if (isExtension) "Proceed to Pay ₹$totalFee (Extend Validity)" else "Proceed to Pay ₹$totalFee",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val shifts by viewModel.shifts.collectAsState()
    val selectedShifts by viewModel.selectedShiftsForMembership.collectAsState()
    val selectedSeat by viewModel.selectedSeatNumber.collectAsState()
    val selectedDuration by viewModel.selectedDurationMonths.collectAsState()
    val activeMembership by viewModel.activeMembership.collectAsState()
    val activeMem = activeMembership
    val hasActiveMembership = activeMem != null && !activeMem.isExpired
    val isExtension = hasActiveMembership && activeMem?.seatNumber == selectedSeat

    val totalAmount = remember(shifts, selectedShifts, selectedDuration) {
        viewModel.calculateTotalFee(
            customShifts = shifts,
            customSelectedShiftIds = selectedShifts,
            customDuration = selectedDuration
        )
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showEditUpiDialog by remember { mutableStateOf(false) }
    var customUpiInput by remember { mutableStateOf("") }

    val paymentConfig by viewModel.paymentConfig.collectAsState()
    val manualProofUri by viewModel.manualPaymentProofUri.collectAsState()
    val manualUtr by viewModel.manualPaymentUtr.collectAsState()
    val manualRemarks by viewModel.manualPaymentRemarks.collectAsState()
    val manualPaymentMsg by viewModel.manualPaymentStatusMessage.collectAsState()

    val adminUpiIdFromVm by viewModel.adminUpiId.collectAsState()
    val currentUpiId = paymentConfig.upiId.ifBlank { adminUpiIdFromVm }
    val payeeName = paymentConfig.payeeName.ifBlank { "Maa Durga Digital Library" }
    val upiNote = if (isExtension) "Extension_Seat_${selectedSeat}" else "Admission_Seat_${selectedSeat}"
    val upiPayload = remember(currentUpiId, payeeName, totalAmount, upiNote) {
        "upi://pay?pa=$currentUpiId&pn=${Uri.encode(payeeName)}&am=$totalAmount.00&cu=INR&tn=${Uri.encode(upiNote)}"
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri) ?: uri.toString()
            viewModel.setManualPaymentProofUri(base64)
        }
    }

    val selectedShiftTitles = shifts
        .filter { selectedShifts.contains(it.id) }
        .joinToString(" + ") { it.title }
        .ifEmpty { "Shift ${selectedShifts.joinToString()}" }

    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val response = data?.getStringExtra("response") ?: ""
        val isSuccess = response.contains("success", ignoreCase = true) ||
                response.contains("status=success", ignoreCase = true) ||
                result.resultCode == Activity.RESULT_OK

        if (isSuccess) {
            val txnId = if (response.isNotBlank()) {
                val match = Regex("(?i)txnId=([^&]+)").find(response)
                match?.groups?.get(1)?.value ?: ""
            } else ""
            if (txnId.isNotBlank()) {
                viewModel.setManualPaymentUtr(txnId)
            }
            Toast.makeText(context, "Payment initiated in UPI app. Please attach screenshot and submit proof below.", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(
                context,
                "After paying in your UPI app, please attach the screenshot and enter UTR below to reserve your seat.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun launchUpiPayment(specificPkg: String? = null) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiPayload))
            if (specificPkg != null) {
                intent.setPackage(specificPkg)
            }
            val chooser = if (specificPkg != null) intent else Intent.createChooser(intent, "Pay ₹$totalAmount using UPI")
            upiLauncher.launch(chooser)
        } catch (e: ActivityNotFoundException) {
            if (specificPkg != null) {
                try {
                    val fallback = Intent.createChooser(Intent(Intent.ACTION_VIEW, Uri.parse(upiPayload)), "Pay ₹$totalAmount with")
                    upiLauncher.launch(fallback)
                } catch (ex: Exception) {
                    Toast.makeText(context, "No UPI app found on device. Please scan the QR code to pay.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "No UPI app found on device. Please scan the QR code to pay.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Payment & Admission",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Seat $selectedSeat • $selectedShiftTitles",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Order Summary Card
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
                        Text("Selected Seat", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "Seat $selectedSeat",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${selectedDuration * 30} Days Validity (${selectedDuration} Month${if (selectedDuration > 1) "s" else ""})",
                            color = PrimaryGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Selected Shifts", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = selectedShiftTitles,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (isExtension) "Total Extension Fee" else "Total Admission Fee", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "₹$totalAmount",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Digital QR Code Card (Pure, Clean ZXing QR without any logo)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DIGITAL UPI PAYMENT QR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = payeeName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Scan with any UPI app to pay",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(14.dp))

                // Real working QR code generated with exact dynamic amount
                DynamicUpiQrCanvas(
                    payload = upiPayload,
                    modifier = Modifier.widthIn(max = 260.dp).fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // Highlighted Fee Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFE8F8EE),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payable Amount: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "₹$totalAmount",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryGreen
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Scan with Google Pay, PhonePe, Paytm or any UPI scanner",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))

                // UPI ID Row with Copy button & Handle Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            clipboardManager.setText(AnnotatedString(currentUpiId))
                            Toast.makeText(context, "UPI ID copied: $currentUpiId", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "UPI ID: $currentUpiId",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy UPI ID",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // UPI Handle Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("UPI Handle: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    listOf("9569556006@ybl" to "@ybl", "9569556006@ibl" to "@ibl", "9569556006@axl" to "@axl").forEach { (fullId, label) ->
                        val isSelected = currentUpiId == fullId
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clickable {
                                    viewModel.updateAdminUpiId(fullId)
                                    Toast.makeText(context, "Selected UPI ID: $fullId", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            customUpiInput = currentUpiId
                            showEditUpiDialog = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Custom UPI ID",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Pay via Installed Apps Section
        Text(
            text = "OR PAY DIRECTLY VIA UPI APPS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )

        Spacer(Modifier.height(10.dp))

        // Quick UPI Apps row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UpiAppClickBadge(
                name = "Google Pay",
                color = Color(0xFF1A73E8),
                modifier = Modifier.weight(1f),
                onClick = { launchUpiPayment("com.google.android.apps.nbu.paisa.user") }
            )
            UpiAppClickBadge(
                name = "PhonePe",
                color = Color(0xFF5F259F),
                modifier = Modifier.weight(1f),
                onClick = { launchUpiPayment("com.phonepe.app") }
            )
            UpiAppClickBadge(
                name = "Paytm",
                color = Color(0xFF002E6E),
                modifier = Modifier.weight(1f),
                onClick = { launchUpiPayment("net.one97.paytm") }
            )
        }

        Spacer(Modifier.height(16.dp))

        if (errorMsg != null || manualPaymentMsg != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMsg ?: manualPaymentMsg ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            errorMsg = null
                            viewModel.clearManualPaymentStatusMessage()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // 1. Direct UPI Apps Button: "Pay via UPI Apps"
        Button(
            onClick = { launchUpiPayment(null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Pay via UPI App (₹$totalAmount)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(16.dp))

        // 2. Section: Manual UPI Payment Verification & Proof Upload
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Submit Payment Proof",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Upload your UPI transaction screenshot and 12-digit UTR number. Your seat will immediately be reserved in Pending status while Admin verifies the payment.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(Modifier.height(14.dp))

                // Screenshot Preview or Picker Button
                if (manualProofUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, PrimaryGreen, RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F8E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        AppImageViewer(
                            model = manualProofUri,
                            contentDescription = "Payment Screenshot",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        IconButton(
                            onClick = { viewModel.setManualPaymentProofUri(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                        Spacer(Modifier.width(6.dp))
                        Text("Change Screenshot", fontSize = 12.sp, color = PrimaryGreen)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen)
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Attach Payment Screenshot *", color = PrimaryGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // UTR / Transaction ID Field
                OutlinedTextField(
                    value = manualUtr,
                    onValueChange = { if (it.length <= 16) viewModel.setManualPaymentUtr(it.filter { ch -> ch.isLetterOrDigit() }) },
                    label = { Text("12-Digit UTR / Transaction Reference ID *") },
                    placeholder = { Text("e.g. 423589123456") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Remarks Field
                OutlinedTextField(
                    value = manualRemarks,
                    onValueChange = { viewModel.setManualPaymentRemarks(it) },
                    label = { Text("Remarks / Payer Name (Optional)") },
                    placeholder = { Text("e.g. Paid from Rahul GPay") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(14.dp))

                // Submit Proof Button
                Button(
                    onClick = {
                        if (manualProofUri.isNullOrBlank()) {
                            errorMsg = "Please attach a screenshot of your UPI payment receipt."
                            return@Button
                        }
                        if (manualUtr.isBlank() || manualUtr.length < 6) {
                            errorMsg = "Please enter a valid 12-digit UTR / Reference ID."
                            return@Button
                        }
                        isProcessing = true
                        errorMsg = null
                        viewModel.submitManualPaymentProof(
                            onSuccess = {
                                isProcessing = false
                                Toast.makeText(context, "Payment proof submitted! Seat $selectedSeat is now reserved in Pending status.", Toast.LENGTH_LONG).show()
                                onPaymentSuccess()
                            },
                            onError = { err ->
                                isProcessing = false
                                errorMsg = err
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Submit Proof & Reserve Seat (₹$totalAmount)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
    }

    // Custom UPI ID Dialog
    if (showEditUpiDialog) {
        AlertDialog(
            onDismissRequest = { showEditUpiDialog = false },
            title = { Text("Enter Your PhonePe / UPI ID", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text(
                        text = "Enter the exact UPI ID from your PhonePe app (Profile -> My QR Code / UPI IDs). E.g. 9569556006@ybl or username@ybl:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customUpiInput,
                        onValueChange = { customUpiInput = it },
                        label = { Text("UPI ID / VPA") },
                        placeholder = { Text("9569556006@ybl") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customUpiInput.isNotBlank()) {
                            viewModel.updateAdminUpiId(customUpiInput.trim())
                            Toast.makeText(context, "QR Code updated with UPI ID: ${customUpiInput.trim()}", Toast.LENGTH_SHORT).show()
                        }
                        showEditUpiDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("Update QR Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUpiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UpiAppClickBadge(
    name: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = name,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PaymentSuccessScreen(
    viewModel: MainViewModel,
    onViewReceipt: () -> Unit,
    onGoToDashboard: () -> Unit
) {
    val membership by viewModel.lastCompletedMembership.collectAsState()
    val payment by viewModel.lastCompletedPayment.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(10.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Big green checkmark circle
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F8EE))
                    .border(2.dp, EmeraldAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Payment Successful",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            val currentSeat = membership?.seatNumber ?: viewModel.selectedSeatNumber.value
            val currentAmount = payment?.amount ?: viewModel.calculateTotalFee()

            Text(
                text = "₹$currentAmount",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Text(
                text = "Seat $currentSeat Allotted Successfully",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmeraldAccent
            )

            Spacer(Modifier.height(28.dp))

            // Card details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Allotted Seat", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(
                            text = "Seat $currentSeat",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Shifts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(
                            text = membership?.shiftTitles ?: "Shift 1 + 2",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Valid Until", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(
                            text = membership?.expiryDate ?: "30 Sep 2026",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = PrimaryGreen
                        )
                    }
                }
            }
        }

        // Action buttons
        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onViewReceipt,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Receipt", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onGoToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Go to Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun QrScannerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onScanned: () -> Unit
) {
    val scanError by viewModel.attendanceScanErrorMessage.collectAsState()
    var isProcessing by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scanError) {
        if (scanError != null) {
            showErrorDialog = true
            isProcessing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraQrScanner(
            onQrScanned = { payload ->
                isProcessing = true
                viewModel.processAttendanceScan(
                    qrPayload = payload,
                    onSuccess = {
                        isProcessing = false
                        onScanned()
                    },
                    onError = {
                        isProcessing = false
                        showErrorDialog = true
                    }
                )
            },
            onBack = onBack,
            isProcessing = isProcessing,
            errorMessage = scanError,
            onClearError = {
                viewModel.clearAttendanceError()
                showErrorDialog = false
            }
        )

        // Error / Validation Notice Dialog
        if (showErrorDialog && scanError != null) {
            val err = scanError ?: ""
            val isCycleCompleted = err.contains("CYCLE_COMPLETED", ignoreCase = true)
            val isAlreadyInside = err.contains("ALREADY_CHECKED_IN", ignoreCase = true)
            val isInvalidGate = err.contains("INVALID_GATE_QR", ignoreCase = true) || err.contains("INVALID_QR", ignoreCase = true)
            val isExpiryErr = err.contains("MEMBERSHIP_EXPIRED", ignoreCase = true) || err.contains("expired", ignoreCase = true)
            val isNoMembership = err.contains("NO_MEMBERSHIP", ignoreCase = true) || err.contains("NO_ACTIVE_MEMBERSHIP", ignoreCase = true)

            AlertDialog(
                onDismissRequest = {
                    showErrorDialog = false
                    viewModel.clearAttendanceError()
                },
                title = {
                    Text(
                        text = when {
                            isCycleCompleted -> "Attendance Cycle Completed"
                            isAlreadyInside -> "Already Checked In"
                            isInvalidGate -> "Invalid QR Code"
                            isExpiryErr -> "Membership Expired ⚠️"
                            isNoMembership -> "No Active Membership"
                            else -> "Attendance Notice"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (isExpiryErr || isInvalidGate) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = when {
                            isCycleCompleted -> "You have already completed today's attendance. You can scan again after 24 hours."
                            isAlreadyInside -> "You're already checked in."
                            isInvalidGate -> "The scanned QR code is invalid. Please point your camera at the official entrance QR code installed at the library gate."
                            isExpiryErr -> "Your library membership has expired. As per library rules, attendance cannot be recorded. Please renew your membership to continue."
                            isNoMembership -> "No active membership found for your account. Please choose a seat and complete admission first."
                            else -> err.replace("^[A-Z_]+:\\s*".toRegex(), "")
                        },
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showErrorDialog = false
                            viewModel.clearAttendanceError()
                            if (isExpiryErr || isNoMembership) {
                                onBack()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExpiryErr || isInvalidGate) Color(0xFFC62828) else PrimaryGreen
                        )
                    ) {
                        Text(if (isExpiryErr || isNoMembership) "Renew / Get Admission" else "Scan Again", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showErrorDialog = false
                        viewModel.clearAttendanceError()
                        onBack()
                    }) {
                        Text("Exit Scanner")
                    }
                }
            )
        }
    }
}

@Composable
fun EntrySuccessScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit
) {
    val scanResult by viewModel.lastScanResult.collectAsState()
    val legacyResult by viewModel.lastAttendanceResult.collectAsState()

    val isEntry = scanResult?.action?.equals("entry", ignoreCase = true) ?: legacyResult?.first ?: true
    val record = scanResult?.record ?: legacyResult?.second

    val accentColor = if (isEntry) PrimaryGreen else Color(0xFF1565C0)
    val bgColor = if (isEntry) Color(0xFFE8F8EE) else Color(0xFFE3F2FD)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .border(2.5.dp, accentColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isEntry) Icons.Default.Check else Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = bgColor,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = if (isEntry) "STATUS: INSIDE NOW" else "STATUS: CHECKED OUT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // User-specified heading: "Entry Successful ✓" / "Exit Successful ✓"
            Text(
                text = if (isEntry) "Entry Successful ✓" else "Exit Successful ✓",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            // User-specified subtitle: "Welcome to Maa Durga Digital Library" / "Thank you for studying today."
            Text(
                text = if (isEntry) "Welcome to Maa Durga Digital Library" else "Thank you for studying today.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            if (isEntry) {
                // User-specified: "Entry Time: [server time]"
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Entry Time",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = record?.entryTime ?: "Just Now",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Text(
                            text = "Seat ${record?.seatNumber ?: "--"} • ${record?.shiftTitle ?: "General Shift"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else {
                // User-specified: Entry: [time], Exit: [time], Duration: [calculated duration]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Entry", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record?.entryTime ?: "—", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Exit", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record?.exitTime ?: "—", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Duration", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(record?.duration ?: "0h 0m", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Seat ${record?.seatNumber ?: "--"} • ${record?.shiftTitle ?: "General Shift"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = if (isEntry) "Have a focused and productive study session!" else "See you next time at Maa Durga Digital Library.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Text("Back to Home", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
