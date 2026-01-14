package tech.dokus.features.ai.utils

import java.math.BigDecimal

/**
 * Safe amount parsing for essential field validation.
 *
 * Normalizes input by:
 * - Removing currency symbols (EUR, USD, GBP, etc.)
 * - Removing whitespace and non-breaking spaces (NBSP)
 * - Handling European format (1.234,56) vs US format (1,234.56)
 *
 * Then attempts actual decimal parsing - not just regex.
 */
object AmountParser {

    // Currency symbols to strip
    private val CURRENCY_SYMBOLS = setOf('€', '$', '£', '¥', '₹', '₽', '₿')

    // Whitespace including NBSP (\u00A0) and thin space (\u2009)
    private val WHITESPACE = Regex("[\\s\\u00A0\\u2009]+")

    /**
     * Check if string is a parseable monetary amount.
     *
     * Accepts: "123.45", "€ 123,45", "$ 1,234.56", "-50.00", "1 234,56"
     * Rejects: "N/A", "", "unknown", "TBD", "abc"
     */
    fun isParseable(value: String?): Boolean {
        return tryParse(value) != null
    }

    /**
     * Try to parse string as a BigDecimal amount.
     * Returns null if parsing fails.
     */
    fun tryParse(value: String?): BigDecimal? {
        if (value.isNullOrBlank()) return null

        val normalized = normalize(value)
        if (normalized.isEmpty()) return null

        return try {
            BigDecimal(normalized)
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Normalize amount string to standard decimal format.
     *
     * Handles:
     * - "€ 1.234,56" → "1234.56" (European)
     * - "$ 1,234.56" → "1234.56" (US)
     * - "1 234,56" → "1234.56" (Space as thousands separator)
     * - "-50.00" → "-50.00" (Negative)
     */
    private fun normalize(value: String): String {
        var s = value

        // 1. Remove currency symbols
        s = s.filter { it !in CURRENCY_SYMBOLS }

        // 2. Remove all whitespace (including NBSP)
        s = s.replace(WHITESPACE, "")

        // 3. Handle empty after stripping
        if (s.isEmpty() || s == "-") return ""

        // 4. Detect format and normalize decimal separator
        // European: 1.234,56 (dot = thousands, comma = decimal)
        // US: 1,234.56 (comma = thousands, dot = decimal)
        val lastDot = s.lastIndexOf('.')
        val lastComma = s.lastIndexOf(',')

        s = when {
            // Only dots: US format or no thousands (123.45)
            lastComma < 0 -> s

            // Only commas: European format (123,45) or thousands only
            lastDot < 0 -> {
                // If comma is in decimal position (last 3 chars), treat as decimal
                if (s.length - lastComma <= 3) {
                    s.replace(',', '.')
                } else {
                    // Comma is thousands separator
                    s.replace(",", "")
                }
            }

            // Both present: determine which is decimal separator
            lastComma > lastDot -> {
                // European: 1.234,56 → comma is decimal
                s.replace(".", "").replace(',', '.')
            }

            else -> {
                // US: 1,234.56 → dot is decimal
                s.replace(",", "")
            }
        }

        return s
    }
}
