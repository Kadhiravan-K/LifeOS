package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object ProfileSecurityManager {

    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 32

    const val MIN_PATTERN_DOTS = 4
    const val MAX_PATTERN_DOTS = 9

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hashCredential(raw: String, saltHex: String): String {
        val saltBytes = hexToByteArray(saltHex)
        val spec = PBEKeySpec(raw.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun verifyCredential(raw: String, storedHash: String, saltHex: String): Boolean {
        if (storedHash.isBlank() || saltHex.isBlank() || raw.isBlank()) return false
        return try {
            val computedHash = hashCredential(raw, saltHex)
            MessageDigest.isEqual(
                computedHash.toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8)
            )
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Validates that a pattern string represents a valid, non-trivial pattern:
     * - Formatted with numbers separated by hyphens (e.g. "0-1-2-5-8")
     * - Contains between 4 and 9 dots
     * - Every dot index is between 0 and 8 inclusive
     * - No dot is repeated
     */
    fun validatePattern(patternString: String): Boolean {
        if (patternString.isBlank()) return false
        val dots = parsePattern(patternString)
        return validatePatternDots(dots)
    }

    fun validatePatternDots(dots: List<Int>): Boolean {
        if (dots.size < MIN_PATTERN_DOTS || dots.size > MAX_PATTERN_DOTS) return false
        if (dots.any { it !in 0 until MAX_PATTERN_DOTS }) return false
        return dots.distinct().size == dots.size
    }

    fun formatPattern(dots: List<Int>): String {
        return dots.joinToString("-")
    }

    fun parsePattern(patternString: String): List<Int> {
        if (patternString.isBlank()) return emptyList()
        return try {
            patternString.split("-").mapNotNull { it.trim().toIntOrNull() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val clean = if (hex.length % 2 != 0) "0$hex" else hex
        return clean.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    enum class BiometricStatus(val userExplanation: String) {
        AVAILABLE("Biometric authentication is ready and enrolled"),
        NONE_ENROLLED("Biometric hardware is supported, but no credentials are enrolled in Android Settings"),
        HARDWARE_UNAVAILABLE("Biometric sensor is present, but temporarily busy or unavailable"),
        NOT_SUPPORTED("Biometric hardware is not supported on this device")
    }

    fun checkBiometricStatus(context: Context): BiometricStatus {
        return try {
            val biometricManager = BiometricManager.from(context)
            when (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
            )) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
                BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
                BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> BiometricStatus.NOT_SUPPORTED
                else -> BiometricStatus.NOT_SUPPORTED
            }
        } catch (_: Exception) {
            BiometricStatus.NOT_SUPPORTED
        }
    }

    fun isBiometricHardwarePresent(context: Context): Boolean {
        val status = checkBiometricStatus(context)
        return status == BiometricStatus.AVAILABLE || status == BiometricStatus.NONE_ENROLLED || status == BiometricStatus.HARDWARE_UNAVAILABLE
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Unlock Profile",
        subtitle: String = "Authenticate using your biometric credential",
        negativeButtonText: String = "Use Fallback Credential",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                    onCancel()
                } else {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }
}
