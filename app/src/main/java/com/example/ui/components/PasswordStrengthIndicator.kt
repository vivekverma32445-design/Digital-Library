package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.example.util.PasswordSecurity

@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier,
    title: String = "Password Strength Requirements"
) {
    val check = PasswordSecurity.checkPasswordCriteria(password)
    val progress = (check.passedCount / 5f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val strengthColor by animateColorAsState(
        targetValue = when {
            check.isStrong -> PrimaryGreen
            check.passedCount >= 3 -> Color(0xFFFFA000) // Amber / Moderate
            else -> Color(0xFFE53935) // Red / Weak
        },
        label = "color"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, strengthColor.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (password.isNotEmpty()) {
                    Text(
                        text = check.strengthLevel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = strengthColor
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Progress Bar Meter
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = strengthColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            Spacer(Modifier.height(8.dp))

            // 5 Criteria
            CriteriaRow(isMet = check.hasMinLength, text = "Minimum 8 characters")
            CriteriaRow(isMet = check.hasUpper, text = "At least one uppercase letter (A-Z)")
            CriteriaRow(isMet = check.hasLower, text = "At least one lowercase letter (a-z)")
            CriteriaRow(isMet = check.hasDigit, text = "At least one number (0-9)")
            CriteriaRow(isMet = check.hasSpecial, text = "At least one special character (!@#\$%^&*...)")

            if (password.isNotEmpty() && !check.isStrong) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "⚠️ Password is weak. All 5 criteria must be satisfied to proceed.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
private fun CriteriaRow(isMet: Boolean, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isMet) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = if (isMet) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isMet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
