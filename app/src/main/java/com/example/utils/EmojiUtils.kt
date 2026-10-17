package com.example.utils

import java.text.BreakIterator
import java.util.Locale

object EmojiUtils {

    /**
     * Extracts the first single Unicode emoji grapheme cluster from the given input.
     * Correctly handles compound emojis, skin-tone modifiers, variation selectors,
     * and zero-width joiner (ZWJ) sequences (e.g. 👨‍👩‍👧‍👦, 🏃🏽‍♀️, 🛠️, 🚀).
     * Returns null if no valid emoji is present.
     */
    fun extractFirstEmoji(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null

        val boundary = BreakIterator.getCharacterInstance(Locale.ROOT)
        boundary.setText(trimmed)

        var start = boundary.first()
        var end = boundary.next()

        while (end != BreakIterator.DONE) {
            val cluster = trimmed.substring(start, end)
            if (isEmojiCluster(cluster)) {
                return cluster
            }
            start = end
            end = boundary.next()
        }
        return null
    }

    /**
     * Checks if the entire input string represents exactly ONE emoji grapheme cluster.
     */
    fun isSingleEmoji(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false

        val boundary = BreakIterator.getCharacterInstance(Locale.ROOT)
        boundary.setText(trimmed)

        val start = boundary.first()
        val end = boundary.next()
        if (end == BreakIterator.DONE || end != trimmed.length) {
            // Either no cluster or more than one cluster
            return false
        }

        val cluster = trimmed.substring(start, end)
        return isEmojiCluster(cluster)
    }

    /**
     * Checks if a grapheme cluster contains an emoji code point.
     */
    fun isEmojiCluster(cluster: String): Boolean {
        if (cluster.isEmpty()) return false
        var index = 0
        var hasEmoji = false
        while (index < cluster.length) {
            val codePoint = cluster.codePointAt(index)
            if (isEmojiCodePoint(codePoint)) {
                hasEmoji = true
                break
            }
            index += Character.charCount(codePoint)
        }
        return hasEmoji
    }

    private fun isEmojiCodePoint(cp: Int): Boolean {
        return (cp in 0x1F600..0x1F64F) || // Emoticons
                (cp in 0x1F300..0x1F5FF) || // Misc Symbols and Pictographs
                (cp in 0x1F680..0x1F6FF) || // Transport and Map
                (cp in 0x1F700..0x1F77F) || // Alchemical Symbols
                (cp in 0x1F780..0x1F7FF) || // Geometric Shapes Extended
                (cp in 0x1F800..0x1F8FF) || // Supplemental Arrows-C
                (cp in 0x1F900..0x1F9FF) || // Supplemental Symbols and Pictographs
                (cp in 0x1FA00..0x1FA6F) || // Chess Symbols
                (cp in 0x1FA70..0x1FAFF) || // Symbols and Pictographs Extended-A
                (cp in 0x2600..0x26FF) ||   // Misc symbols (e.g. ⚡, ☕, ☀️)
                (cp in 0x2700..0x27BF) ||   // Dingbats (e.g. ✨, ✈️, ✂️)
                (cp in 0x1F1E6..0x1F1FF) || // Regional indicator symbol letters (flags)
                (cp in 0x1F3FB..0x1F3FF)    // Skin tone modifiers
    }
}
