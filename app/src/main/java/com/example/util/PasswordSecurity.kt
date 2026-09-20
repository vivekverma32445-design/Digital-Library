package com.example.util

import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordSecurity {
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private val secureRandom = SecureRandom()

    /**
     * Hashes a password using PBKDF2 with HMAC-SHA256 and a random 16-byte cryptographic salt.
     * Output format: "pbkdf2:12000:<saltHex>:<hashHex>"
     */
    fun hashPassword(password: String): String {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        val hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        return "pbkdf2:$ITERATIONS:${bytesToHex(salt)}:${bytesToHex(hash)}"
    }

    /**
     * Securely verifies a plain password against a stored PBKDF2 hash.
     * Uses constant-time equality check to prevent timing attacks.
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        if (!storedHash.startsWith("pbkdf2:")) {
            // Fallback for legacy hashes during migration
            return storedHash == password
        }

        val parts = storedHash.split(":")
        if (parts.size != 4) return false

        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = hexToBytes(parts[2]) ?: return false
        val expectedHash = hexToBytes(parts[3]) ?: return false

        val computedHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.size * 8)
        return constantTimeEquals(expectedHash, computedHash)
    }

    data class PasswordChecklistResult(
        val hasMinLength: Boolean,
        val hasUpper: Boolean,
        val hasLower: Boolean,
        val hasDigit: Boolean,
        val hasSpecial: Boolean
    ) {
        val passedCount: Int get() = listOf(hasMinLength, hasUpper, hasLower, hasDigit, hasSpecial).count { it }
        val isStrong: Boolean get() = hasMinLength && hasUpper && hasLower && hasDigit && hasSpecial
        val strengthLevel: String get() = when {
            passedCount <= 2 -> "Weak"
            passedCount in 3..4 -> "Moderate"
            else -> "Strong"
        }
    }

    fun checkPasswordCriteria(password: String): PasswordChecklistResult {
        val specialCharRegex = "[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]".toRegex()
        return PasswordChecklistResult(
            hasMinLength = password.length >= 8,
            hasUpper = password.any { it.isUpperCase() },
            hasLower = password.any { it.isLowerCase() },
            hasDigit = password.any { it.isDigit() },
            hasSpecial = specialCharRegex.containsMatchIn(password)
        )
    }

    /**
     * Validates password strength according to enterprise security standards:
     * - Minimum 8 characters
     * - At least one uppercase letter (A-Z)
     * - At least one lowercase letter (a-z)
     * - At least one digit (0-9)
     * - At least one special symbol (@, #, $, %, etc.)
     */
    fun validatePasswordStrength(password: String): Pair<Boolean, String?> {
        val check = checkPasswordCriteria(password)
        if (!check.hasMinLength) {
            return false to "Password must be at least 8 characters long."
        }
        if (!check.hasUpper) {
            return false to "Password must contain at least one uppercase letter (A-Z)."
        }
        if (!check.hasLower) {
            return false to "Password must contain at least one lowercase letter (a-z)."
        }
        if (!check.hasDigit) {
            return false to "Password must contain at least one number (0-9)."
        }
        if (!check.hasSpecial) {
            return false to "Password must contain at least one special character (!@#\$%^&*...)."
        }
        return true to null
    }

    /**
     * Generates a 6-digit numeric OTP using SecureRandom.
     */
    fun generateOtp(): String {
        val number = secureRandom.nextInt(900000) + 100000
        return number.toString()
    }

    /**
     * Generates a cryptographically strong session token.
     */
    fun generateSessionToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray, iterations: Int, keyLength: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, keyLength)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        return factory.generateSecret(spec).encoded
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef".toCharArray()
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xff
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        val bytes = ByteArray(hex.length / 2)
        for (i in bytes.indices) {
            val index = i * 2
            val v = hex.substring(index, index + 2).toIntOrNull(16) ?: return null
            bytes[i] = v.toByte()
        }
        return bytes
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
