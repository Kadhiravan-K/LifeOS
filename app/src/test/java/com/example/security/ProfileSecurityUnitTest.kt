package com.example.security

import com.example.data.local.entities.ProfileSecurityEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSecurityUnitTest {

    @Test
    fun testPinHashingAndVerification() {
        val pin = "1234"
        val salt = ProfileSecurityManager.generateSalt()
        val hash = ProfileSecurityManager.hashCredential(pin, salt)

        assertTrue(ProfileSecurityManager.verifyCredential(pin, hash, salt))
        assertFalse(ProfileSecurityManager.verifyCredential("9999", hash, salt))
        assertFalse(ProfileSecurityManager.verifyCredential("", hash, salt))
    }

    @Test
    fun testPasswordHashingAndVerification() {
        val password = "SecretMasterPassword!2026"
        val salt = ProfileSecurityManager.generateSalt()
        val hash = ProfileSecurityManager.hashCredential(password, salt)

        assertTrue(ProfileSecurityManager.verifyCredential(password, hash, salt))
        assertFalse(ProfileSecurityManager.verifyCredential("WrongPassword", hash, salt))
    }

    @Test
    fun testPatternHashingAndVerification() {
        val pattern = "0-1-2-5-8"
        val salt = ProfileSecurityManager.generateSalt()
        val hash = ProfileSecurityManager.hashCredential(pattern, salt)

        assertTrue(ProfileSecurityManager.verifyCredential(pattern, hash, salt))
        assertFalse(ProfileSecurityManager.verifyCredential("0-1-2-4-8", hash, salt))
    }

    @Test
    fun testSaltUniquenessAndProfileIsolation() {
        val salt1 = ProfileSecurityManager.generateSalt()
        val salt2 = ProfileSecurityManager.generateSalt()
        assertNotEquals(salt1, salt2)

        val pin = "4321"
        val hashProfile1 = ProfileSecurityManager.hashCredential(pin, salt1)
        val hashProfile2 = ProfileSecurityManager.hashCredential(pin, salt2)

        // Same PIN with different salts produces completely different hashes
        assertNotEquals(hashProfile1, hashProfile2)

        // Verifying with Profile 2's salt against Profile 1's hash fails
        assertFalse(ProfileSecurityManager.verifyCredential(pin, hashProfile1, salt2))
    }

    @Test
    fun testLockoutPolicyConstants() {
        assertEquals(5, ProfileSecurityEntity.MAX_FAILED_ATTEMPTS)
        assertEquals(30_000L, ProfileSecurityEntity.LOCKOUT_DURATION_MS)
        assertEquals(300, ProfileSecurityEntity.DEFAULT_AUTO_LOCK_SECONDS)
    }

    @Test
    fun testProfileSecurityEntityDefaultConstruction() {
        val entity = ProfileSecurityEntity.defaultForProfile(2L)
        assertEquals(2L, entity.profileId)
        assertFalse(entity.isLockEnabled)
        assertEquals(ProfileSecurityEntity.AUTH_METHOD_NONE, entity.authMethod)
        assertEquals(0, entity.failedAttempts)
        assertEquals(0L, entity.lockoutUntilEpochMs)
    }
}
