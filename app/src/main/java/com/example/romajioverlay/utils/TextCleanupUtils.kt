package com.example.romajioverlay.utils

object TextCleanupUtils {
    // Regex to detect Japanese text (Hiragana, Katakana, Kanji)
    private val japaneseRegex = Regex("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]")

    /**
     * Cleans up the content description text from Facebook Messenger to extract
     * only the actual message content.
     */
    fun cleanMessengerText(text: String): String {
        // Find the first and last Japanese characters
        val firstMatch = japaneseRegex.find(text) ?: return text
        val lastMatch = japaneseRegex.findAll(text).lastOrNull() ?: return text

        val firstJpIndex = firstMatch.range.first
        val lastJpIndex = lastMatch.range.first

        // 1. Clean the prefix (sender name, e.g., "Traci, ")
        var startIndex = 0
        val prefixSearch = text.substring(0, firstJpIndex)
        val lastComma = prefixSearch.lastIndexOf(", ")
        if (lastComma != -1) {
            startIndex = lastComma + 2
        } else {
            val lastSpace = prefixSearch.lastIndexOf(' ')
            if (lastSpace != -1) {
                startIndex = lastSpace + 1
            }
        }

        // 2. Clean the suffix (accessibility actions, e.g., ", double tap to...")
        var endIndex = text.length
        val suffixSearch = text.substring(lastJpIndex + 1)
        val firstComma = suffixSearch.indexOf(", ")
        if (firstComma != -1) {
            endIndex = lastJpIndex + 1 + firstComma
        }

        // Extract and trim the cleaned message
        if (startIndex < endIndex) {
            return text.substring(startIndex, endIndex).trim()
        }
        return text
    }
}
