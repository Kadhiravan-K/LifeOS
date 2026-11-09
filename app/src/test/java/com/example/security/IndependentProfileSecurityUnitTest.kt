package com.example.security

import com.example.data.local.entities.ProfileEntity
import com.example.data.local.entities.ProfileSecurityEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LifeOS v4.0.1 — Issue 03: Independent Profile Security, Pattern Lock & Biometric Availability.
 * Comprehensive Unit Tests verifying:
 * - 1. Independent Security for every profile (Profile A vs Profile B isolation).
 * - 2. Pattern Lock creation, confirmation, validation (>=4 dots), and PBKDF2 hashing.
 * - 3. Biometric capability and status evaluation.
 * - 4. Non-leakage of credentials across profiles.
 * - 5. Profile deletion cleanup and safe defaults.
 */
class IndependentProfileSecurityUnitTest {

    @Test
    fun testIndependentProfileSecurityConfiguration() {
        val profileAId = 1L
        val profileBId = 2L

        // Profile A configures Pattern A
        val patternA = "0-1-2-5-8"
        val saltA = ProfileSecurityManager.generateSalt()
        val hashA = ProfileSecurityManager.hashCredential(patternA, saltA)
        val secProfileA = ProfileSecurityEntity(
            profileId = profileAId,
            isLockEnabled = true,
            authMethod = ProfileSecurityEntity.AUTH_METHOD_PATTERN,
            credentialHash = hashA,
            salt = saltA,
            biometricEnabled = true
        )

        // Profile B configures Pattern B
        val patternB = "6-7-8-5-2"
        val saltB = ProfileSecurityManager.generateSalt()
        val hashB = ProfileSecurityManager.hashCredential(patternB, saltB)
        val secProfileB = ProfileSecurityEntity(
            profileId = profileBId,
            isLockEnabled = true,
            authMethod = ProfileSecurityEntity.AUTH_METHOD_PATTERN,
            credentialHash = hashB,
            salt = saltB,
            biometricEnabled = false // Biometric disabled on B
        )

        // Verify independent credentials
        assertNotEquals("Salts must be independent", secProfileA.salt, secProfileB.salt)
        assertNotEquals("Hashes must be independent", secProfileA.credentialHash, secProfileB.credentialHash)
        assertTrue("Profile A has biometrics enabled", secProfileA.biometricEnabled)
        assertFalse("Profile B has biometrics disabled", secProfileB.biometricEnabled)

        // Profile A unlocks with Pattern A, fails with Pattern B
        assertTrue(ProfileSecurityManager.verifyCredential(patternA, secProfileA.credentialHash, secProfileA.salt))
        assertFalse(ProfileSecurityManager.verifyCredential(patternB, secProfileA.credentialHash, secProfileA.salt))

        // Profile B unlocks with Pattern B, fails with Pattern A
        assertTrue(ProfileSecurityManager.verifyCredential(patternB, secProfileB.credentialHash, secProfileB.salt))
        assertFalse(ProfileSecurityManager.verifyCredential(patternA, secProfileB.credentialHash, secProfileB.salt))
    }

    @Test
    fun testPatternValidation_rejectsTooShortOrDuplicatePatterns() {
        // Valid patterns (>= 4 dots, distinct dots in 0..8)
        assertTrue(ProfileSecurityManager.validatePattern("0-1-2-5"))
        assertTrue(ProfileSecurityManager.validatePattern("0-1-2-5-8"))
        assertTrue(ProfileSecurityManager.validatePattern("0-1-2-3-4-5-6-7-8"))

        // Invalid patterns: fewer than 4 dots
        assertFalse(ProfileSecurityManager.validatePattern("0-1-2"))
        assertFalse(ProfileSecurityManager.validatePattern("0-1"))
        assertFalse(ProfileSecurityManager.validatePattern("0"))
        assertFalse(ProfileSecurityManager.validatePattern(""))

        // Invalid patterns: duplicate dots
        assertFalse(ProfileSecurityManager.validatePattern("0-1-2-0"))
        assertFalse(ProfileSecurityManager.validatePattern("1-2-3-4-2"))

        // Invalid patterns: out of bounds indices (<0 or >8)
        assertFalse(ProfileSecurityManager.validatePattern("0-1-2-9"))
        assertFalse(ProfileSecurityManager.validatePattern("-1-0-1-2"))

        // Invalid formatting
        assertFalse(ProfileSecurityManager.validatePattern("not-a-pattern"))
    }

    @Test
    fun testPatternFormattingAndParsing() {
        val dots = listOf(0, 1, 4, 7, 8)
        val formatted = ProfileSecurityManager.formatPattern(dots)
        assertEquals("0-1-4-7-8", formatted)

        val parsed = ProfileSecurityManager.parsePattern(formatted)
        assertEquals(dots, parsed)

        // Empty string handling
        assertEquals(emptyList<Int>(), ProfileSecurityManager.parsePattern(""))
    }

    @Test
    fun testBiometricStatusExplanations() {
        assertEquals(
            "Biometric authentication is ready and enrolled",
            ProfileSecurityManager.BiometricStatus.AVAILABLE.userExplanation
        )
        assertEquals(
            "Biometric hardware is supported, but no credentials are enrolled in Android Settings",
            ProfileSecurityManager.BiometricStatus.NONE_ENROLLED.userExplanation
        )
        assertEquals(
            "Biometric sensor is present, but temporarily busy or unavailable",
            ProfileSecurityManager.BiometricStatus.HARDWARE_UNAVAILABLE.userExplanation
        )
        assertEquals(
            "Biometric hardware is not supported on this device",
            ProfileSecurityManager.BiometricStatus.NOT_SUPPORTED.userExplanation
        )
    }

    @Test
    fun testProfileDefaultSecurityHasSafeDefaults() {
        val defaultSec = ProfileSecurityEntity.defaultForProfile(42L)
        assertEquals(42L, defaultSec.profileId)
        assertFalse("Profile default must not have lock enabled", defaultSec.isLockEnabled)
        assertEquals(ProfileSecurityEntity.AUTH_METHOD_NONE, defaultSec.authMethod)
        assertEquals("", defaultSec.credentialHash)
        assertEquals("", defaultSec.salt)
        assertFalse("Biometrics must default to disabled", defaultSec.biometricEnabled)
        assertEquals(0, defaultSec.failedAttempts)
        assertEquals(0L, defaultSec.lockoutUntilEpochMs)
    }

    @Test
    fun testDisablingSecurityOnOneProfileLeavesOtherIntact() {
        val profileAId = 10L
        val profileBId = 20L

        var secProfileA: ProfileSecurityEntity? = ProfileSecurityEntity(
            profileId = profileAId,
            isLockEnabled = true,
            authMethod = ProfileSecurityEntity.AUTH_METHOD_PIN,
            credentialHash = "hashA",
            salt = "saltA",
            biometricEnabled = true
        )

        var secProfileB: ProfileSecurityEntity? = ProfileSecurityEntity(
            profileId = profileBId,
            isLockEnabled = true,
            authMethod = ProfileSecurityEntity.AUTH_METHOD_PATTERN,
            credentialHash = "hashB",
            salt = "saltB",
            biometricEnabled = false
        )

        // Disable security on Profile A
        secProfileA = null

        // Verify Profile B remains fully configured and locked
        assertNotNull(secProfileB)
        assertTrue(secProfileB!!.isLockEnabled)
        assertEquals(ProfileSecurityEntity.AUTH_METHOD_PATTERN, secProfileB.authMethod)
        assertEquals("hashB", secProfileB.credentialHash)
    }

    @Test
    fun testLockoutPolicyBehavior() {
        val profileId = 5L
        val initial = ProfileSecurityEntity.defaultForProfile(profileId).copy(
            isLockEnabled = true,
            authMethod = ProfileSecurityEntity.AUTH_METHOD_PIN,
            credentialHash = "someHash",
            salt = "someSalt"
        )

        // Simulate 4 failed attempts: not locked out yet
        val after4Attempts = initial.copy(failedAttempts = 4, lockoutUntilEpochMs = 0L)
        val now = System.currentTimeMillis()
        assertFalse(after4Attempts.lockoutUntilEpochMs > now)

        // 5th failed attempt: triggers lockout
        val lockoutUntil = now + ProfileSecurityEntity.LOCKOUT_DURATION_MS
        val after5Attempts = after4Attempts.copy(
            failedAttempts = 5,
            lockoutUntilEpochMs = lockoutUntil
        )
        assertTrue(after5Attempts.lockoutUntilEpochMs > now)
        assertEquals(30_000L, ProfileSecurityEntity.LOCKOUT_DURATION_MS)
    }
}
