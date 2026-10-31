package com.example.mcp

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Handles security, bearer token generation, constant-time validation,
 * and profile-scoped access control for the Model Context Protocol bridge (#3).
 */
object McpSecurityManager {

    private val secureRandom = SecureRandom()

    /**
     * Generates a cryptographically strong 32-character hex bearer token.
     */
    fun generateAuthToken(): String {
        val bytes = ByteArray(16)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validates an incoming bearer token using constant-time comparison
     * to eliminate timing side-channel attacks.
     */
    fun validateToken(providedToken: String?, storedToken: String?): Boolean {
        if (providedToken.isNullOrBlank() || storedToken.isNullOrBlank()) {
            return false
        }
        val cleanProvided = providedToken.removePrefix("Bearer ").trim()
        val cleanStored = storedToken.trim()

        val providedBytes = cleanProvided.toByteArray(Charsets.UTF_8)
        val storedBytes = cleanStored.toByteArray(Charsets.UTF_8)

        return MessageDigest.isEqual(providedBytes, storedBytes)
    }

    /**
     * Enforces strict profile isolation: if a specific profile is configured
     * for the MCP bridge, calls requesting a different profile ID are rejected.
     */
    fun validateProfileAccess(requestedProfileId: Long?, allowedProfileId: Long): Boolean {
        if (requestedProfileId == null) return true
        return requestedProfileId == allowedProfileId
    }
}
