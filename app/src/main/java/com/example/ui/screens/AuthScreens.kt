package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.components.PasswordStrengthIndicator
import com.example.ui.theme.*
import com.example.util.PasswordSecurity
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1600)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Image
            Image(
                painter = painterResource(id = R.drawable.ic_digital_library_logo),
                contentDescription = "Digital Library Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(2.dp, EmeraldAccent, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Digital Library",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Maa Durga Digital Library",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryGreen,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Your Space. Your Focus.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = PrimaryGreen,
                strokeWidth = 2.5.dp
            )
        }
    }
}

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }

    val steps = listOf(
        Triple(
            "Your Dedicated Study Space",
            "Find your perfect seat and study without distractions.",
            R.drawable.img_study_space
        ),
        Triple(
            "Choose Your Seat",
            "Real-time seat availability across 4 shifts.",
            R.drawable.img_seat_hall
        ),
        Triple(
            "Everything in One App",
            "Attendance, Payments, Membership, and Complaints managed seamlessly.",
            R.drawable.ic_digital_library_logo
        )
    )

    val current = steps[currentStep]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (currentStep < 2) {
                TextButton(onClick = onFinished) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Spacer(Modifier.height(40.dp))
            }
        }

        // Center Content with Illustration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = current.third),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = current.first,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = current.second,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Step Indicator Dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { idx ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (idx == currentStep) 24.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (idx == currentStep) PrimaryGreen else Color.LightGray)
                    )
                }
            }
        }

        // Bottom CTA Button
        Button(
            onClick = {
                if (currentStep < 2) {
                    currentStep++
                } else {
                    onFinished()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
        ) {
            Text(
                text = if (currentStep < 2) "Next" else "Get Started",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun WelcomeScreen(
    onContinueAsStudent: () -> Unit,
    onContinueAsAdmin: () -> Unit,
    onExploreTour: () -> Unit = {}
) {
    var showHelpDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SoftCream,
                        WarmCream,
                        Color(0xFFF0EBE0)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Clean circular digital-library logo at top
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, SubtleGoldBorder, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_digital_library_logo),
                    contentDescription = "Maa Durga Digital Library Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(14.dp))

            // Academic Sanctuary Tag
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SubtleGoldLight,
                border = BorderStroke(1.dp, SubtleGoldBorder.copy(alpha = 0.7f))
            ) {
                Text(
                    text = "✦  ESTD. 2024 • MODERN STUDY SANCTUARY  ✦",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubtleGold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // Large bold typography
            Text(
                text = "Welcome to",
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = CharcoalMuted
            )

            Text(
                text = "Maa Durga Digital Library",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = DeepForestGreen,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(6.dp))

            // Tagline
            Text(
                text = "Read More  •  Learn More  •  Grow More",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SubtleGold,
                letterSpacing = 0.6.sp
            )

            Spacer(Modifier.height(26.dp))

            // Section Heading & Subtitle
            Text(
                text = "Choose Your Access",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CharcoalHeading
            )

            Text(
                text = "Select the portal that matches your role to continue",
                fontSize = 13.sp,
                color = CharcoalMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // 1. STUDENT CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContinueAsStudent() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.2.dp, Color(0xFFD6E2D8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(DeepForestGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "Student Portal Icon",
                                tint = SoftCream,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Student",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForestGreen
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SeatAvailableBg
                                ) {
                                    Text(
                                        text = "LEARNER / MEMBER",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenDeep,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Self-study seats, shifts & digital library access",
                                fontSize = 12.sp,
                                color = CharcoalMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF0F4F1), thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    BulletHighlightItem("Check Seat Availability (36 Seats, 4 Shifts)", PrimaryGreen)
                    BulletHighlightItem("Pay Monthly / Shift Fees via UPI QR", PrimaryGreen)
                    BulletHighlightItem("View Notices, Holidays & Updates", PrimaryGreen)
                    BulletHighlightItem("Submit Help & Maintenance Complaints", PrimaryGreen)
                    BulletHighlightItem("Access Digital Library Services & Silent Hall", PrimaryGreen)

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = onContinueAsStudent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepForestGreen)
                    ) {
                        Text(
                            text = "Continue as Student",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // 2. ADMINISTRATOR CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContinueAsAdmin() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.2.dp, SubtleGoldBorder.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0C2917)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Administrator Portal Icon",
                                tint = SubtleGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Administrator",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalHeading
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SubtleGoldLight
                                ) {
                                    Text(
                                        text = "LIBRARY ADMIN",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SubtleGold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Seat allocation, attendance logs & desk financials",
                                fontSize = 12.sp,
                                color = CharcoalMuted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF6F2E9), thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    BulletHighlightItem("Manage Students & KYC Memberships", SubtleGold)
                    BulletHighlightItem("Seat Allocation & Real-Time Availability", SubtleGold)
                    BulletHighlightItem("Shift & Batch Management (Morning to Night)", SubtleGold)
                    BulletHighlightItem("Facility & Complaint Tracking", SubtleGold)
                    BulletHighlightItem("Collect Fees & View Daily Financials", SubtleGold)
                    BulletHighlightItem("Library Announcements & Shift Reports", SubtleGold)

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SubtleGoldLight.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SubtleGold,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Organization credentials required • No public sign up",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = CharcoalHeading
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = onContinueAsAdmin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenHeader)
                    ) {
                        Text(
                            text = "Continue as Administrator",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Trust Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Secure  •  Simple  •  Study Better",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CharcoalMuted
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Maa Durga Digital Library • Study Smarter",
                fontSize = 11.sp,
                color = CharcoalMuted.copy(alpha = 0.8f)
            )

            Spacer(Modifier.height(16.dp))

            // Help & Tour Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showHelpDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Contact Library Desk",
                        fontSize = 12.sp,
                        color = DeepForestGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(text = "•", color = CharcoalMuted.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 4.dp))

                TextButton(onClick = onExploreTour) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = SubtleGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Explore Tour",
                        fontSize = 12.sp,
                        color = SubtleGold,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        // Library Desk Info Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalLibrary,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Library Desk Help",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Maa Durga Digital Library",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DeepForestGreen
                        )
                        Text(
                            text = "📍 Location: Near Central Plaza, Main Market Road\n" +
                                   "📞 Helpdesk: +91 9569556006\n" +
                                   "⏰ Timings: 06:00 AM – 10:00 PM (All 7 Days)\n" +
                                   "📶 Facilities: 36 Reserved AC Seats, High-Speed WiFi, RO Drinking Water, CCTV Security, Silent Study Zones.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showHelpDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun BulletHighlightItem(
    text: String,
    iconTint: Color = PrimaryGreen,
    textColor: Color = CharcoalHeading
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(11.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

private fun sanitizePhoneInput(input: String): String {
    val digits = input.filter { it.isDigit() }
    return when {
        digits.length > 10 && digits.startsWith("91") -> digits.removePrefix("91").take(10)
        digits.length > 10 && digits.startsWith("0") -> digits.removePrefix("0").take(10)
        else -> digits.take(10)
    }
}

@Composable
fun AdminLoginScreen(
    viewModel: MainViewModel,
    onBackToWelcome: () -> Unit
) {
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Admin Master Key Forgot Password state
    var showForgotAdminMasterKeyDialog by remember { mutableStateOf(false) }
    var forgotMobileInput by remember { mutableStateOf("") }
    var forgotMasterKeyInput by remember { mutableStateOf("") }
    var newAdminPassInput by remember { mutableStateOf("") }
    var confirmAdminPassInput by remember { mutableStateOf("") }
    var isNewPassVisible by remember { mutableStateOf(false) }
    var isConfirmPassVisible by remember { mutableStateOf(false) }
    var forgotDialogError by remember { mutableStateOf<String?>(null) }
    var isSubmittingAdminReset by remember { mutableStateOf(false) }

    // Admin Registration State
    var showAdminRegisterDialog by remember { mutableStateOf(false) }
    var regFullName by remember { mutableStateOf("") }
    var regMobile by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regMasterKey by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var regPassVisible by remember { mutableStateOf(false) }
    var regConfirmVisible by remember { mutableStateOf(false) }
    var regError by remember { mutableStateOf<String?>(null) }
    var isRegisteringAdmin by remember { mutableStateOf(false) }

    if (showAdminRegisterDialog) {
        val regCriteria = remember(regPassword) { PasswordSecurity.checkPasswordCriteria(regPassword) }
        val isPasswordsMatch = regPassword == regConfirmPassword && regConfirmPassword.isNotEmpty()

        AlertDialog(
            onDismissRequest = { if (!isRegisteringAdmin) showAdminRegisterDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = ForestGreenHeader)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Register Administrator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Register a new authorized administrator account. Requires the library Master Security Key.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = regFullName,
                        onValueChange = { regFullName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Rahul Sharma") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ForestGreenHeader) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regMobile,
                        onValueChange = { if (it.length <= 10) regMobile = it },
                        label = { Text("Admin Mobile (10 digits)") },
                        placeholder = { Text("9876543210") },
                        prefix = { Text("+91  ", fontWeight = FontWeight.Bold, color = ForestGreenHeader) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenHeader) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regEmail,
                        onValueChange = { regEmail = it },
                        label = { Text("Email Address (Optional)") },
                        placeholder = { Text("admin@maadurga.org") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ForestGreenHeader) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regMasterKey,
                        onValueChange = { regMasterKey = it },
                        label = { Text("Master Security Key") },
                        placeholder = { Text("MDDL@ADMIN2026") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = SubtleGold) },
                        supportingText = { Text("Authorized library admin key (MDDL@ADMIN2026 or ADMIN9569)", fontSize = 10.5.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = { regPassword = it },
                        label = { Text("Create Strong Password") },
                        placeholder = { Text("Enter strong password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenHeader) },
                        visualTransformation = if (regPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { regPassVisible = !regPassVisible }) {
                                Icon(
                                    imageVector = if (regPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live Password Strength Checklist
                    PasswordStrengthIndicator(
                        password = regPassword,
                        title = "Required Password Standards"
                    )

                    OutlinedTextField(
                        value = regConfirmPassword,
                        onValueChange = { regConfirmPassword = it },
                        label = { Text("Confirm Password") },
                        placeholder = { Text("Re-enter password") },
                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = ForestGreenHeader) },
                        visualTransformation = if (regConfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { regConfirmVisible = !regConfirmVisible }) {
                                Icon(
                                    imageVector = if (regConfirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (regConfirmPassword.isNotEmpty() && !isPasswordsMatch) {
                        Text(
                            text = "❌ Passwords do not match.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (regError != null) {
                        Text(
                            text = regError!!,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                val canSubmit = regCriteria.isStrong && isPasswordsMatch && regMobile.length == 10 && regMasterKey.isNotBlank() && regFullName.isNotBlank()

                Button(
                    onClick = {
                        if (!canSubmit) {
                            if (!regCriteria.isStrong) {
                                regError = "Password does not satisfy all required complexity rules."
                            } else if (!isPasswordsMatch) {
                                regError = "Passwords do not match."
                            } else {
                                regError = "Please fill in all mandatory fields correctly."
                            }
                            return@Button
                        }
                        isRegisteringAdmin = true
                        regError = null
                        viewModel.registerAdmin(
                            fullName = regFullName,
                            mobile = regMobile,
                            email = regEmail,
                            masterKey = regMasterKey,
                            password = regPassword,
                            onSuccess = {
                                isRegisteringAdmin = false
                                mobile = regMobile
                                password = regPassword
                                showAdminRegisterDialog = false
                                errorMessage = "Admin account registered successfully! You can now log in."
                            },
                            onError = { err ->
                                isRegisteringAdmin = false
                                regError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenHeader),
                    enabled = canSubmit && !isRegisteringAdmin
                ) {
                    if (isRegisteringAdmin) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Register Administrator", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAdminRegisterDialog = false },
                    enabled = !isRegisteringAdmin
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showForgotAdminMasterKeyDialog) {
        val forgotCriteria = remember(newAdminPassInput) { PasswordSecurity.checkPasswordCriteria(newAdminPassInput) }
        val isPassMatch = newAdminPassInput == confirmAdminPassInput && confirmAdminPassInput.isNotEmpty()

        AlertDialog(
            onDismissRequest = { if (!isSubmittingAdminReset) showForgotAdminMasterKeyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = ForestGreenHeader)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Admin Password Reset",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reset administrator credentials using the institutional Master Security Key.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = forgotMobileInput,
                        onValueChange = { if (it.length <= 10) forgotMobileInput = it },
                        label = { Text("Admin Mobile (10 digits)") },
                        placeholder = { Text("9876543210") },
                        prefix = { Text("+91  ", fontWeight = FontWeight.Bold, color = ForestGreenHeader) },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenHeader) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = forgotMasterKeyInput,
                        onValueChange = { forgotMasterKeyInput = it },
                        label = { Text("Master Security Key") },
                        placeholder = { Text("Enter Master Key") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = ForestGreenHeader) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newAdminPassInput,
                        onValueChange = { newAdminPassInput = it },
                        label = { Text("New Admin Password") },
                        placeholder = { Text("Enter new password") },
                        visualTransformation = if (isNewPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isNewPassVisible = !isNewPassVisible }) {
                                Icon(
                                    imageVector = if (isNewPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmAdminPassInput,
                        onValueChange = { confirmAdminPassInput = it },
                        label = { Text("Confirm New Password") },
                        placeholder = { Text("Re-enter new password") },
                        visualTransformation = if (isConfirmPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isConfirmPassVisible = !isConfirmPassVisible }) {
                                Icon(
                                    imageVector = if (isConfirmPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    PasswordStrengthIndicator(
                        password = newAdminPassInput,
                        title = "Admin Password Rules"
                    )

                    if (forgotDialogError != null) {
                        Text(
                            text = forgotDialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                val canSubmit = forgotCriteria.isStrong && isPassMatch && forgotMobileInput.length == 10 && forgotMasterKeyInput.isNotBlank()
                Button(
                    onClick = {
                        if (!canSubmit) {
                            if (!forgotCriteria.isStrong) {
                                forgotDialogError = "Password must fulfill all complexity requirements."
                            } else if (!isPassMatch) {
                                forgotDialogError = "Passwords do not match."
                            } else {
                                forgotDialogError = "Please fill in all mandatory fields correctly."
                            }
                            return@Button
                        }
                        isSubmittingAdminReset = true
                        forgotDialogError = null
                        viewModel.resetAdminPasswordWithMasterKey(
                            mobile = forgotMobileInput.trim(),
                            masterKey = forgotMasterKeyInput.trim(),
                            newPass = newAdminPassInput,
                            confirmPass = confirmAdminPassInput,
                            onSuccess = {
                                isSubmittingAdminReset = false
                                mobile = forgotMobileInput
                                password = newAdminPassInput
                                showForgotAdminMasterKeyDialog = false
                                errorMessage = "Admin password updated successfully! You can now log in."
                            },
                            onError = { err ->
                                isSubmittingAdminReset = false
                                forgotDialogError = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenHeader),
                    enabled = canSubmit && !isSubmittingAdminReset
                ) {
                    if (isSubmittingAdminReset) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Reset Admin Password", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotAdminMasterKeyDialog = false },
                    enabled = !isSubmittingAdminReset
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToWelcome) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Welcome",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Back to Welcome",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))

        // Admin Shield Crest
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F2E1B))
                .border(2.dp, SubtleGoldBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin Shield",
                tint = SubtleGold,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Administrator Portal",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Maa Durga Digital Library • Operations Desk",
            fontSize = 13.sp,
            color = PrimaryGreen,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Restricted Access Callout Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = SubtleGoldLight,
            border = BorderStroke(1.dp, SubtleGoldBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SubtleGold,
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Authorized Personnel Only: Enter your 10-digit registered administrator mobile and password to access the desk.",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = CharcoalHeading
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Admin Mobile / ID
        Text(
            text = "Administrator Mobile / ID",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = mobile,
            onValueChange = { mobile = sanitizePhoneInput(it) },
            placeholder = { Text("Enter 10-digit admin mobile") },
            prefix = { Text("+91  ", fontWeight = FontWeight.Bold, color = ForestGreenHeader) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ForestGreenHeader) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Password / Key
        Text(
            text = "Admin Security Password",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Enter admin password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ForestGreenHeader) },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                forgotMobileInput = mobile
                forgotMasterKeyInput = ""
                newAdminPassInput = ""
                confirmAdminPassInput = ""
                forgotDialogError = null
                showForgotAdminMasterKeyDialog = true
            }) {
                Text(
                    text = "Forgot Password?",
                    color = PrimaryGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))

        // Admin Login CTA Button
        Button(
            onClick = {
                if (mobile.isBlank() || password.isBlank()) {
                    errorMessage = "Please enter administrator mobile and password"
                    return@Button
                }
                isSubmitting = true
                errorMessage = null
                viewModel.login(
                    mobile = mobile,
                    pass = password,
                    onSuccess = { isSubmitting = false },
                    onError = {
                        isSubmitting = false
                        errorMessage = it
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenHeader),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Secure Admin Login", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Multi-Admin Info Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Devices,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Multi-Admin & Cross-Device Sync: Any authorized admin can manage the library desk from any Android device with real-time synchronization.",
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Security Assurance Notice
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Encrypted Admin Gateway • Maa Durga Digital Library",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onBackToWelcome: () -> Unit = {},
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Back Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackToWelcome) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Portals",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Back to Portals",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(10.dp))

        // Top Brand Icon
        Image(
            painter = painterResource(id = R.drawable.ic_digital_library_logo),
            contentDescription = null,
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .border(2.dp, SubtleGoldBorder, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Student Portal Login",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Maa Durga Digital Library • Self-Study Center",
            fontSize = 13.sp,
            color = PrimaryGreen,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(20.dp))

        // Mobile Field
        Text(
            text = "Mobile Number",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = mobile,
            onValueChange = { if (it.length <= 10) mobile = it },
            placeholder = { Text("Enter mobile number") },
            prefix = { Text("+91  ", fontWeight = FontWeight.Bold, color = PrimaryGreen) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryGreen) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Password Field
        Text(
            text = "Password",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Enter password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryGreen) },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Forgot Password link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onNavigateToForgotPassword) {
                Text("Forgot Password?", fontSize = 13.sp, color = PrimaryGreen, fontWeight = FontWeight.Medium)
            }
        }

        // Error message
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Login Button
        Button(
            onClick = {
                val cleanMobile = mobile.trim()
                val cleanPassword = password.trim()
                if (cleanMobile.isBlank() || cleanPassword.isBlank()) {
                    errorMessage = "Please enter mobile number and password"
                    return@Button
                }
                isSubmitting = true
                errorMessage = null
                viewModel.login(
                    mobile = cleanMobile,
                    pass = cleanPassword,
                    onSuccess = { isSubmitting = false },
                    onError = {
                        isSubmitting = false
                        errorMessage = it
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Text("Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Don't have an account? Create Account
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Don't have an account? ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Create Account",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToLogin) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "Create Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Join Maa Durga Digital Library",
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Full Name
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            placeholder = { Text("Enter your full name") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryGreen) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Mobile Number
        OutlinedTextField(
            value = mobile,
            onValueChange = { if (it.length <= 10) mobile = it },
            label = { Text("Mobile Number") },
            placeholder = { Text("Enter mobile number") },
            prefix = { Text("+91  ", fontWeight = FontWeight.Bold, color = PrimaryGreen) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryGreen) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Gmail / Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Gmail / Email") },
            placeholder = { Text("Enter your email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryGreen) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        // Gender Dropdown
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                placeholder = { Text("Select Gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                listOf("Male", "Female", "Other").forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g) },
                        onClick = {
                            gender = g
                            genderExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("Create a strong password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryGreen) },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Live Password Strength Checklist
        PasswordStrengthIndicator(
            password = password,
            title = "Required Password Standards"
        )

        Spacer(Modifier.height(14.dp))

        // Confirm Password
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            placeholder = { Text("Re-enter your password") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryGreen) },
            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                    Icon(
                        imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val cleanName = fullName.trim()
                val cleanMobile = mobile.trim()
                val cleanEmail = email.trim()
                val cleanGender = gender.trim()
                val cleanPass = password.trim()
                val cleanConfirmPass = confirmPassword.trim()

                if (cleanName.isBlank() || cleanMobile.length < 10 || cleanEmail.isBlank() || cleanGender.isBlank() || cleanPass.isBlank() || cleanConfirmPass.isBlank()) {
                    errorMessage = "All fields including gender are mandatory and mobile must be 10 digits"
                    return@Button
                }
                if (cleanPass != cleanConfirmPass) {
                    errorMessage = "Passwords do not match"
                    return@Button
                }
                val (isPassValid, passError) = PasswordSecurity.validatePasswordStrength(cleanPass)
                if (!isPassValid) {
                    errorMessage = passError ?: "Password must fulfill all strong password criteria."
                    return@Button
                }
                isSubmitting = true
                errorMessage = null
                viewModel.register(
                    fullName = cleanName,
                    mobile = cleanMobile,
                    email = cleanEmail,
                    gender = cleanGender,
                    pass = cleanPass,
                    onSuccess = { isSubmitting = false },
                    onError = {
                        isSubmitting = false
                        errorMessage = it
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            enabled = !isSubmitting
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
            } else {
                Text("Register", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Already have an account? ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Login",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGreen,
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit
) {
    var mobile by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isNewPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmittedSuccess by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val criteria = remember(newPassword) { PasswordSecurity.checkPasswordCriteria(newPassword) }
    val isPasswordMatching = newPassword.isNotEmpty() && newPassword == confirmPassword

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToLogin) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Header Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(if (isSubmittedSuccess) Color(0xFFE8F8EE) else PrimaryGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSubmittedSuccess) Icons.Default.CheckCircle else Icons.Default.LockReset,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (isSubmittedSuccess) "Request Submitted" else "Reset Password",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = if (isSubmittedSuccess)
                "Your request is pending administrator verification."
            else
                "Submit a password reset request for Admin approval.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp)
        )

        Spacer(Modifier.height(24.dp))

        if (isSubmittedSuccess) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F8EE)),
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Your password reset request has been submitted. Please wait until the Admin approves it. You can log in after your request is approved.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = ForestGreenHeader,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White
                    ) {
                        Text(
                            text = "Registered Mobile: +91 $mobile",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CharcoalHeading,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Return to Login", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            // Form Fields
            OutlinedTextField(
                value = mobile,
                onValueChange = {
                    if (it.length <= 10) {
                        mobile = it
                        errorMessage = null
                    }
                },
                label = { Text("Registered Mobile Number") },
                placeholder = { Text("Enter 10-digit number") },
                prefix = { Text("+91  ", fontWeight = FontWeight.Bold, color = PrimaryGreen) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryGreen) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = {
                    newPassword = it
                    errorMessage = null
                },
                label = { Text("New Password") },
                placeholder = { Text("Enter new password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryGreen) },
                visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                        Icon(
                            imageVector = if (isNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Confirm New Password") },
                placeholder = { Text("Re-enter new password") },
                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null, tint = PrimaryGreen) },
                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                        Icon(
                            imageVector = if (isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (confirmPassword.isNotEmpty() && !isPasswordMatching) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Passwords do not match",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            // Password rules / strength
            PasswordStrengthIndicator(
                password = newPassword,
                title = "Password Security Requirements"
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            val canSubmit = mobile.trim().length == 10 && criteria.isStrong && isPasswordMatching && !isSubmitting

            Button(
                onClick = {
                    val cleanMobile = mobile.trim()
                    if (cleanMobile.length != 10) {
                        errorMessage = "Please enter a valid 10-digit registered mobile number."
                        return@Button
                    }
                    if (!criteria.isStrong) {
                        errorMessage = "Password does not meet all security complexity requirements."
                        return@Button
                    }
                    if (!isPasswordMatching) {
                        errorMessage = "New password and confirm password do not match."
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null

                    viewModel.submitPasswordResetRequest(
                        mobile = cleanMobile,
                        newPass = newPassword,
                        confirmPass = confirmPassword,
                        onSuccess = {
                            isSubmitting = false
                            isSubmittedSuccess = true
                        },
                        onError = { err ->
                            isSubmitting = false
                            errorMessage = err
                        }
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text(
                        text = "Submit Reset Request",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Back to Login", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
