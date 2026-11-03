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
        if (storedHash.isBlank() || saltHex.isBlank()) return false
        return try {
            val computedHash = hashCredential(raw, saltHex)
            MessageDigest.isEqual(
                computedHash.toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            false
        }
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val clean = if (hex.length % 2 != 0) "0$hex" else hex
        return clean.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    enum class BiometricStatus {
        AVAILABLE,
        NONE_ENROLLED,
        NOT_SUPPORTED
    }

    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.NOT_SUPPORTED
        }
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
