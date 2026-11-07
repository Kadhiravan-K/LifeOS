package com.example.utils

/**
 * Utility for matching the hidden Lifetime Premium Morse code easter egg sequence.
 * Morse pattern: "-.- .- - .... .. .-. .- ...- .- -." ("KADHIRAVAN")
 * where '.' = single click / tap, '-' = long press.
 */
object MorseCodeSecretMatcher {
    // Exact user-specified sequence (tokens: K, A, D, H, I, R, A, V, A, N)
    const val USER_MORSE_PATTERN = "-.- .- - .... .. .-. .- ...- .- -."
    const val FLATTENED_USER_SEQUENCE = "-.-.--....-...-.-...-.-."
    const val FLATTENED_STANDARD_SEQUENCE = "-.-.--..--....-...-.-...-.-."

    /**
     * Checks if the accumulated morse string ends with the secret pattern.
     * Tolerant to spaces, pauses, or single-dash/double-dot variations for 'D'.
     */
    fun isSecretMatch(accumulatedInput: String): Boolean {
        val sanitized = accumulatedInput.filter { it == '.' || it == '-' }
        return sanitized.endsWith(FLATTENED_USER_SEQUENCE) || sanitized.endsWith(FLATTENED_STANDARD_SEQUENCE)
    }
}
