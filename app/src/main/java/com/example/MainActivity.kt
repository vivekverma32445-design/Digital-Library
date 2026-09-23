package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.screens.*
import com.example.ui.theme.DigitalLibraryTheme
import com.example.ui.theme.PrimaryGreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDark by viewModel.isDarkTheme.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val studentTab by viewModel.studentTab.collectAsState()
            val selectedReceipt by viewModel.selectedReceiptForView.collectAsState()
            val membership by viewModel.activeMembership.collectAsState()

            val context = LocalContext.current
            var lastBackPressTime by remember { mutableLongStateOf(0L) }

            BackHandler(enabled = true) {
                if (selectedReceipt != null) {
                    viewModel.closeReceiptDialog()
                } else {
                    val handled = viewModel.navigateBack()
                    if (!handled) {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPressTime < 2000L) {
                            (context as? Activity)?.finish()
                        } else {
                            lastBackPressTime = now
                            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            DigitalLibraryTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(350)) +
                                    scaleIn(initialScale = 0.97f, animationSpec = tween(350)))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(200))
                                )
                        },
                        label = "MainAppScreenNavigationAnimation",
                        modifier = Modifier.fillMaxSize()
                    ) { targetScreen ->
                        when (targetScreen) {
                        "splash" -> {
                            SplashScreen(
                                onTimeout = {
                                    viewModel.checkPersistedSession(
                                        onLoggedIn = { /* ViewModel sets currentScreen directly */ },
                                        onNotLoggedIn = { viewModel.navigateTo("welcome") }
                                    )
                                }
                            )
                        }
                        "welcome" -> {
                            WelcomeScreen(
                                onContinueAsStudent = { viewModel.navigateTo("login") },
                                onContinueAsAdmin = { viewModel.navigateTo("admin_login") },
                                onExploreTour = { viewModel.navigateTo("onboarding") }
                            )
                        }
                        "onboarding" -> {
                            OnboardingScreen(
                                onFinished = { viewModel.navigateTo("welcome") }
                            )
                        }
                        "login" -> {
                            LoginScreen(
                                viewModel = viewModel,
                                onBackToWelcome = { viewModel.navigateTo("welcome") },
                                onNavigateToRegister = { viewModel.navigateTo("register") },
                                onNavigateToForgotPassword = { viewModel.navigateTo("forgot_password") }
                            )
                        }
                        "admin_login" -> {
                            AdminLoginScreen(
                                viewModel = viewModel,
                                onBackToWelcome = { viewModel.navigateTo("welcome") }
                            )
                        }
                        "register" -> {
                            RegisterScreen(
                                viewModel = viewModel,
                                onNavigateToLogin = { viewModel.navigateTo("login") }
                            )
                        }
                        "forgot_password" -> {
                            ForgotPasswordScreen(
                                viewModel = viewModel,
                                onNavigateToLogin = { viewModel.navigateTo("login") }
                            )
                        }
                        "qr_scan" -> {
                            QrScannerScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo("student_home") },
                                onScanned = { viewModel.navigateTo("entry_success") }
                            )
                        }
                        "entry_success" -> {
                            EntrySuccessScreen(
                                viewModel = viewModel,
                                onDone = { viewModel.navigateTo("student_home") }
                            )
                        }
                        "membership_selection" -> {
                            MembershipSelectionScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo("student_home") },
                                onProceedToPay = { viewModel.navigateTo("payment_screen") }
                            )
                        }
                        "payment_screen" -> {
                            PaymentScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo("student_home") },
                                onPaymentSuccess = { viewModel.navigateTo("payment_success") }
                            )
                        }
                        "payment_success" -> {
                            PaymentSuccessScreen(
                                viewModel = viewModel,
                                onViewReceipt = {
                                    viewModel.lastCompletedPayment.value?.let { p ->
                                        viewModel.viewReceipt(p)
                                    }
                                },
                                onGoToDashboard = {
                                    viewModel.setStudentTab("home")
                                    viewModel.navigateTo("student_home")
                                }
                            )
                        }
                        "admin_dashboard" -> {
                            AdminDashboardScreen(
                                viewModel = viewModel,
                                onBackToStudent = { viewModel.switchToAdminMode(false) }
                            )
                        }
                        "complaints" -> {
                            RaiseComplaintScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo("student_home") }
                            )
                        }
                        "notifications" -> {
                            NotificationsScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo("student_home") }
                            )
                        }
                        else -> {
                            // Student Container with Bottom Navigation
                            Scaffold(
                                contentWindowInsets = WindowInsets.statusBars,
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        tonalElevation = 6.dp
                                    ) {
                                        NavigationBarItem(
                                            selected = studentTab == "home",
                                            onClick = { viewModel.setStudentTab("home") },
                                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                            label = { Text("Home", fontSize = 10.5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = PrimaryGreen,
                                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = studentTab == "seats",
                                            onClick = { viewModel.setStudentTab("seats") },
                                            icon = { Icon(Icons.Default.EventSeat, contentDescription = "Seats") },
                                            label = { Text("Seats", fontSize = 10.5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = PrimaryGreen,
                                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = studentTab == "attendance",
                                            onClick = { viewModel.setStudentTab("attendance") },
                                            icon = { Icon(Icons.Default.FactCheck, contentDescription = "Attendance") },
                                            label = { Text("Attendance", fontSize = 10.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = PrimaryGreen,
                                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = studentTab == "payments",
                                            onClick = { viewModel.setStudentTab("payments") },
                                            icon = { Icon(Icons.Default.Payment, contentDescription = "Payments") },
                                            label = { Text("Payments", fontSize = 10.5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = PrimaryGreen,
                                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                        NavigationBarItem(
                                            selected = studentTab == "profile",
                                            onClick = { viewModel.setStudentTab("profile") },
                                            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                            label = { Text("Profile", fontSize = 10.5.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = PrimaryGreen,
                                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(modifier = Modifier.padding(innerPadding)) {
                                    when (studentTab) {
                                        "home" -> StudentHomeScreen(
                                            viewModel = viewModel,
                                            onNavigateToNotifications = { viewModel.navigateTo("notifications") },
                                            onNavigateToSeatSelection = { viewModel.setStudentTab("seats") },
                                            onNavigateToQrScan = { viewModel.navigateTo("qr_scan") },
                                            onNavigateToAttendance = { viewModel.setStudentTab("attendance") },
                                            onNavigateToPayments = { viewModel.setStudentTab("payments") },
                                            onNavigateToComplaints = { viewModel.navigateTo("complaints") }
                                        )
                                        "seats" -> SeatSelectionScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.setStudentTab("home") },
                                            onContinue = { viewModel.navigateTo("membership_selection") }
                                        )
                                        "attendance" -> AttendanceScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.setStudentTab("home") }
                                        )
                                        "payments" -> PaymentHistoryScreen(
                                            viewModel = viewModel,
                                            onBack = { viewModel.setStudentTab("home") }
                                        )
                                        "profile" -> ProfileScreen(
                                            viewModel = viewModel,
                                            onNavigateToAttendance = { viewModel.setStudentTab("attendance") },
                                            onNavigateToPayments = { viewModel.setStudentTab("payments") },
                                            onNavigateToComplaints = { viewModel.navigateTo("complaints") },
                                            onNavigateToSeats = { viewModel.setStudentTab("seats") },
                                            onNavigateToNotifications = { viewModel.navigateTo("notifications") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    }

                    // Global Digital Receipt Modal Dialog
                    selectedReceipt?.let { payment ->
                        DigitalReceiptDialog(
                            payment = payment,
                            seatNumber = membership?.seatNumber ?: "01",
                            onDismiss = { viewModel.closeReceiptDialog() }
                        )
                    }
                }
            }
        }
    }
}
