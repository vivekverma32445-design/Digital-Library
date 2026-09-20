package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NotificationItem
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var detailNotification by remember { mutableStateOf<NotificationItem?>(null) }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Unread" -> notifications.filter { !it.isRead }
            "Announcements" -> notifications.filter { it.type == "ANNOUNCEMENT" || it.type == "LIBRARY_NOTICE" }
            "Payments" -> notifications.filter {
                it.type == "PAYMENT_SUCCESS" || it.type == "PAYMENT_DUE" ||
                        it.type == "PAYMENT_REMINDER" || it.type == "PAYMENT_HISTORY"
            }
            "Seats" -> notifications.filter {
                it.type == "SEAT_ADMISSION" || it.type == "SEAT_AVAILABILITY"
            }
            "Complaints" -> notifications.filter { it.type == "COMPLAINT_STATUS" }
            else -> notifications
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notifications",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (unreadCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = PrimaryGreen
                            ) {
                                Text(
                                    text = "$unreadCount new",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (notifications.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.markAllNotificationsAsRead() },
                            enabled = unreadCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (unreadCount > 0) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Mark all as read",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (unreadCount > 0) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // Filter chips row
            ScrollableTabRow(
                selectedTabIndex = when (selectedFilter) {
                    "All" -> 0
                    "Unread" -> 1
                    "Announcements" -> 2
                    "Payments" -> 3
                    "Seats" -> 4
                    "Complaints" -> 5
                    else -> 0
                },
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryGreen,
                divider = {}
            ) {
                val filterList = listOf("All", "Unread", "Announcements", "Payments", "Seats", "Complaints")
                filterList.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Tab(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        text = {
                            Text(
                                text = filter,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (filteredNotifications.isEmpty()) {
                // Empty state
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
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F8EE))
                                .border(1.5.dp, PrimaryGreen.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            text = if (selectedFilter == "All") "No new notifications" else "No $selectedFilter notifications",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "You're all caught up!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Announcements from admin, payment receipts, due reminders, and seat updates will automatically appear here.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredNotifications, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                if (!item.isRead) {
                                    viewModel.markNotificationAsRead(item.id)
                                }
                                detailNotification = item.copy(isRead = true)
                            },
                            onDelete = {
                                viewModel.deleteNotification(item.id)
                            }
                        )
                    }
                }
            }
        }

        // Notification Detail Dialog
        detailNotification?.let { item ->
            val (icon, iconBg, iconTint) = getNotificationVisuals(item.type)
            AlertDialog(
                onDismissRequest = { detailNotification = null },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = item.timestamp,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = item.description,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { detailNotification = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, iconBg, iconTint) = getNotificationVisuals(item.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) {
                Color(0xFFF1F8F3) // Subtle soft mint highlight for unread
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (!item.isRead) {
            androidx.compose.foundation.BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f))
        } else {
            androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (!item.isRead) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 14.sp,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    if (!item.isRead) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = item.description,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.timestamp,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = iconBg
                        ) {
                            Text(
                                text = formatTypeLabel(item.type),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconTint,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(Modifier.width(6.dp))

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTypeLabel(type: String): String {
    return when (type) {
        "ANNOUNCEMENT" -> "Announcement"
        "PAYMENT_DUE" -> "Payment Due"
        "PAYMENT_SUCCESS" -> "Paid"
        "PAYMENT_REMINDER" -> "Reminder"
        "PAYMENT_HISTORY" -> "Receipt"
        "SEAT_ADMISSION" -> "Admission"
        "SEAT_AVAILABILITY" -> "Seats"
        "LIBRARY_NOTICE" -> "Notice"
        "COMPLAINT_STATUS" -> "Complaint"
        "ACCOUNT" -> "Account"
        else -> "Update"
    }
}

private fun getNotificationVisuals(type: String): Triple<ImageVector, Color, Color> {
    return when (type) {
        "ANNOUNCEMENT", "LIBRARY_NOTICE" -> Triple(
            Icons.Default.Campaign,
            Color(0xFFFFF3E0), // Soft warm amber
            Color(0xFFE65100)
        )
        "PAYMENT_SUCCESS" -> Triple(
            Icons.Default.CheckCircle,
            Color(0xFFE8F8EE), // Soft green
            PrimaryGreen
        )
        "PAYMENT_DUE", "PAYMENT_REMINDER" -> Triple(
            Icons.Default.PendingActions,
            Color(0xFFFFEBEE), // Soft red/pink
            Color(0xFFC62828)
        )
        "PAYMENT_HISTORY" -> Triple(
            Icons.Default.ReceiptLong,
            Color(0xFFE0F2F1), // Soft teal
            Color(0xFF00796B)
        )
        "SEAT_ADMISSION", "SEAT_AVAILABILITY" -> Triple(
            Icons.Default.EventSeat,
            Color(0xFFE8EAF6), // Soft indigo
            Color(0xFF283593)
        )
        "COMPLAINT_STATUS" -> Triple(
            Icons.Default.Feedback,
            Color(0xFFF3E5F5), // Soft purple
            Color(0xFF7B1FA2)
        )
        "ACCOUNT" -> Triple(
            Icons.Default.AccountCircle,
            Color(0xFFECEFF1),
            Color(0xFF455A64)
        )
        else -> Triple(
            Icons.Default.Notifications,
            Color(0xFFE8F8EE),
            PrimaryGreen
        )
    }
}
