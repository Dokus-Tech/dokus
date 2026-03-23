@file:Suppress(
    "ReturnCount" // Parsing functions require multiple early returns for validation
)

package tech.dokus.domain

import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.exceptions.DokusException
import kotlin.jvm.JvmInline

// Money conversion constants
private const val CentsPerUnit = 100
private const val CentsPerUnitDouble = 100.0

// Decimal parsing constants
private const val DecimalPlaces = 2
private const val SingleDigitMultiplier = 10
private const val NoDecimalDigits = 0
private const val OneDecimalDigit = 1
private const val TwoDecimalDigits = 2

// Percentage/VAT basis points constants
private const val BasisPointsPerPercent = 100
private const val BasisPointsPerFull = 10000
private const val BasisPointsPerFullDouble = 10000.0

/**
 * Money represented in minor units (cents for EUR/USD/GBP) with mandatory currency.
 *
 * Currency is never optional, never defaulted. Every monetary value carries its currency.
 *
 * Examples:
 * - Money.eur(12345)           → €123.45
 * - Money.of(1, Currency.Usd)  → $0.01
 * - Money.eur(-5000)           → €-50.00
 *
 * Cross-currency arithmetic is illegal — operators require matching currencies.
 * Formatting belongs to the presentation layer, not domain.
 */
@Serializable
data class Money(
    val minor: Long,
    val currency: Currency,
) : Comparable<Money> {

    /**
     * Numeric-only display string with 2 decimal places.
     * No currency symbol — formatting belongs to presentation.
     * Examples: "123.45", "-50.00", "0.00"
     */
    fun formatAmount(): String {
        val isNegative = minor < 0
        val absMinor = if (minor < 0) -minor else minor
        val whole = absMinor / CentsPerUnit
        val cents = absMinor % CentsPerUnit
        val sign = if (isNegative) "-" else ""
        return "$sign$whole.${cents.toString().padStart(DecimalPlaces, '0')}"
    }

    /** @deprecated Use [formatAmount] instead. Will be removed. */
    @Deprecated("Use formatAmount() instead", ReplaceWith("formatAmount()"))
    fun toDisplayString(): String = formatAmount()

    override fun toString(): String = "${currency.dbValue} ${formatAmount()}"

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "Cannot compare ${currency.dbValue} with ${other.currency.dbValue}" }
        return minor.compareTo(other.minor)
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add ${currency.dbValue} and ${other.currency.dbValue}" }
        return Money(minor + other.minor, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Cannot subtract ${currency.dbValue} and ${other.currency.dbValue}" }
        return Money(minor - other.minor, currency)
    }

    operator fun times(quantity: Int): Money = Money(minor * quantity, currency)
    operator fun times(quantity: Long): Money = Money(minor * quantity, currency)
    operator fun unaryMinus(): Money = Money(-minor, currency)

    val isZero: Boolean get() = minor == 0L
    val isNegative: Boolean get() = minor < 0
    val isPositive: Boolean get() = minor > 0

    /**
     * Convert to Double value in major units.
     * Example: Money.eur(12345).toDouble() = 123.45
     */
    fun toDouble(): Double = minor / CentsPerUnitDouble

    companion object {
        /** Convenience factory for EUR amounts. */
        fun eur(minor: Long) = Money(minor, Currency.Eur)

        /** Factory with explicit currency. */
        fun of(minor: Long, currency: Currency) = Money(minor, currency)

        /** Zero amount in the given currency. */
        fun zero(currency: Currency) = Money(0L, currency)

        /**
         * Parse a human-entered money string into [Money].
         * Currency must be provided explicitly — symbols in the input are stripped.
         */
        fun from(value: String?, currency: Currency): Money? {
            val normalized = normalizeMoneyInput(value) ?: return null
            return parseMinor(normalized)?.let { Money(it, currency) }
        }

        /**
         * Parse a display string like "123.45" or "-50.00" into Money.
         * Currency symbols in the input are stripped — the explicit [currency] is used.
         */
        fun parse(value: String, currency: Currency): Money? =
            parseMinor(value)?.let { Money(it, currency) }

        /**
         * Create Money from a Double value in major units.
         */
        fun fromDouble(value: Double, currency: Currency): Money {
            val minor = kotlin.math.round(value * CentsPerUnit).toLong()
            return Money(minor, currency)
        }

        /**
         * Create Money from an Int value (whole units, no cents).
         */
        fun fromInt(value: Int, currency: Currency): Money =
            Money(value.toLong() * CentsPerUnit, currency)

        /**
         * Parse a string with validation, throwing if invalid.
         */
        fun parseOrThrow(value: String, currency: Currency): Money =
            parse(value, currency) ?: throw DokusException.Validation.InvalidMoney

        /**
         * Parse the numeric part of a money string, returning minor units.
         * Strips currency symbols — the caller provides the currency.
         */
        @Suppress("CyclomaticComplexMethod") // Currency parsing inherently requires multiple format checks
        private fun parseMinor(value: String): Long? {
            val cleaned = value
                .replace("€", "")
                .replace("$", "")
                .replace("£", "")
                .replace("¥", "")
                .replace("zł", "")
                .replace("Kč", "")
                .replace("Ft", "")
                .replace("лв", "")
                .replace(Regex("^(CHF|SEK|NOK|DKK|RON|BGN|RSD|CA\\$|A\\$)"), "")
                .replace(" ", "")
                .replace(",", ".")
                .trim()

            if (cleaned.isEmpty()) return null

            return try {
                val isNegative = cleaned.startsWith("-")
                val absValue = if (isNegative) cleaned.substring(1) else cleaned

                val parts = absValue.split(".")
                when (parts.size) {
                    1 -> {
                        val whole = parts[0].toLongOrNull() ?: return null
                        val minor = whole * CentsPerUnit
                        if (isNegative) -minor else minor
                    }
                    2 -> {
                        val whole = parts[0].toLongOrNull() ?: return null
                        val decimalPart = parts[1]
                        if (decimalPart.length > DecimalPlaces) return null

                        val cents = when (decimalPart.length) {
                            NoDecimalDigits -> 0L
                            OneDecimalDigit -> (decimalPart.toLongOrNull() ?: return null) * SingleDigitMultiplier
                            TwoDecimalDigits -> decimalPart.toLongOrNull() ?: return null
                            else -> return null
                        }
                        val minor = whole * CentsPerUnit + cents
                        if (isNegative) -minor else minor
                    }
                    else -> null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        private fun normalizeMoneyInput(value: String?): String? {
            if (value.isNullOrBlank()) return null

            val trimmed = value.trim()
            if (trimmed.isEmpty()) return null

            val sign = if (trimmed.startsWith("-")) "-" else ""
            val unsigned = trimmed
                .removePrefix("-")
                .replace("€", "")
                .replace("$", "")
                .replace("£", "")
                .replace(" ", "")

            if (unsigned.isEmpty()) return null

            val lastDot = unsigned.lastIndexOf('.')
            val lastComma = unsigned.lastIndexOf(',')
            val lastSep = maxOf(lastDot, lastComma)
            val sepCount = unsigned.count { it == '.' || it == ',' }

            if (lastSep < 0) {
                return sign + unsigned.replace(".", "").replace(",", "")
            }

            val integerPart = unsigned.substring(0, lastSep)
                .replace(".", "")
                .replace(",", "")
            val fractionPart = unsigned.substring(lastSep + 1)
                .replace(".", "")
                .replace(",", "")

            if (fractionPart.isEmpty()) {
                return sign + integerPart
            }

            if (fractionPart.length <= DecimalPlaces) {
                return "$sign$integerPart.$fractionPart"
            }

            if (sepCount == 1 && fractionPart.length > DecimalPlaces) {
                return sign + (integerPart + fractionPart)
            }

            return sign + (integerPart + fractionPart)
        }
    }
}

/**
 * VAT rate represented in basis points.
 *
 * Examples:
 * - 21.00% = VatRate(2100)
 * - 6.00% = VatRate(600)
 * - 0% = VatRate(0)
 *
 * Basis points allow 2 decimal places of precision (0.01% = 1 bp).
 * Maximum representable: 327.67% (Int.MAX_VALUE / 100)
 */
@Serializable
@JvmInline
value class VatRate(val basisPoints: Int) : Comparable<VatRate> {

    /**
     * Display string with 2 decimal places.
     * Examples: "21.00", "6.00", "0.00"
     */
    fun toDisplayString(): String {
        val whole = basisPoints / BasisPointsPerPercent
        val fraction = basisPoints % BasisPointsPerPercent
        return "$whole.${fraction.toString().padStart(DecimalPlaces, '0')}"
    }

    override fun toString(): String = toDisplayString()

    override fun compareTo(other: VatRate): Int = basisPoints.compareTo(other.basisPoints)

    /**
     * Convert to decimal multiplier for calculations.
     * 21.00% -> 0.21
     */
    fun toMultiplier(): Double = basisPoints / BasisPointsPerFullDouble

    /**
     * Apply this VAT rate to a money amount.
     * Returns the VAT amount (not the total including VAT).
     */
    fun applyTo(amount: Money): Money {
        val vatMinor = (amount.minor * basisPoints) / BasisPointsPerFull
        return Money(vatMinor, amount.currency)
    }

    /**
     * Check if this is a valid VAT rate (0-100%).
     */
    val isValid: Boolean get() = basisPoints in 0..BasisPointsPerFull

    /**
     * Convert to percentage as Double.
     * Example: VatRate(2100).toPercentDouble() = 21.0
     */
    fun toPercentDouble(): Double = basisPoints / BasisPointsPerPercent.toDouble()

    companion object {
        val ZERO = VatRate(0)
        val STANDARD_BE = VatRate(2100) // Belgium standard rate 21%
        val REDUCED_BE = VatRate(600) // Belgium reduced rate 6%

        /**
         * Parse a nullable percent string into [VatRate].
         * Examples: "21", "21.00", "21%", "6,00".
         */
        fun from(value: String?): VatRate? {
            if (value.isNullOrBlank()) return null
            return parse(value)
        }

        /**
         * Parse a display string like "21.00" or "21" into VatRate.
         */
        @Suppress("CyclomaticComplexMethod") // Parsing requires multiple format checks
        fun parse(value: String): VatRate? {
            val cleaned = value.replace("%", "").replace(",", ".").trim()
            if (cleaned.isEmpty()) return null

            return try {
                val parts = cleaned.split(".")
                when (parts.size) {
                    1 -> {
                        val whole = parts[0].toIntOrNull() ?: return null
                        VatRate(whole * BasisPointsPerPercent)
                    }
                    2 -> {
                        val whole = parts[0].toIntOrNull() ?: return null
                        val decimalPart = parts[1]
                        if (decimalPart.length > DecimalPlaces) return null

                        val fraction = when (decimalPart.length) {
                            NoDecimalDigits -> 0
                            OneDecimalDigit -> (decimalPart.toIntOrNull() ?: return null) * SingleDigitMultiplier
                            TwoDecimalDigits -> decimalPart.toIntOrNull() ?: return null
                            else -> return null
                        }
                        VatRate(whole * BasisPointsPerPercent + fraction)
                    }
                    else -> null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        /**
         * Create from a decimal multiplier (0.21 -> 21.00%).
         */
        fun fromMultiplier(multiplier: Double): VatRate {
            val bp = kotlin.math.round(multiplier * BasisPointsPerFull).toInt()
            return VatRate(bp)
        }

        /**
         * Parse with validation, throwing if invalid.
         */
        fun parseOrThrow(value: String): VatRate {
            return parse(value) ?: throw DokusException.Validation.InvalidVatRate
        }
    }
}

/**
 * Percentage represented in basis points.
 *
 * Examples:
 * - 100.00% = Percentage(10000)
 * - 50.00% = Percentage(5000)
 * - 33.33% = Percentage(3333)
 *
 * Basis points allow 2 decimal places of precision (0.01% = 1 bp).
 */
@Serializable
@JvmInline
value class Percentage(val basisPoints: Int) : Comparable<Percentage> {

    /**
     * Display string with 2 decimal places.
     * Examples: "100.00", "50.00", "33.33"
     */
    fun toDisplayString(): String {
        val whole = basisPoints / BasisPointsPerPercent
        val fraction = basisPoints % BasisPointsPerPercent
        return "$whole.${fraction.toString().padStart(DecimalPlaces, '0')}"
    }

    override fun toString(): String = toDisplayString()

    override fun compareTo(other: Percentage): Int = basisPoints.compareTo(other.basisPoints)

    /**
     * Convert to decimal multiplier for calculations.
     * 50.00% -> 0.50
     */
    fun toMultiplier(): Double = basisPoints / BasisPointsPerFullDouble

    /**
     * Apply this percentage to a money amount.
     */
    fun applyTo(amount: Money): Money {
        val result = (amount.minor * basisPoints) / BasisPointsPerFull
        return Money(result, amount.currency)
    }

    /**
     * Check if this is a valid percentage (0-100%).
     */
    val isValid: Boolean get() = basisPoints in 0..BasisPointsPerFull

    companion object {
        val ZERO = Percentage(0)
        val FULL = Percentage(10000) // 100%
        val HALF = Percentage(5000) // 50%

        /**
         * Parse a display string like "50.00" or "50" into Percentage.
         */
        @Suppress("CyclomaticComplexMethod") // Parsing requires multiple format checks
        fun parse(value: String): Percentage? {
            val cleaned = value.replace("%", "").replace(",", ".").trim()
            if (cleaned.isEmpty()) return null

            return try {
                val parts = cleaned.split(".")
                when (parts.size) {
                    1 -> {
                        val whole = parts[0].toIntOrNull() ?: return null
                        Percentage(whole * BasisPointsPerPercent)
                    }
                    2 -> {
                        val whole = parts[0].toIntOrNull() ?: return null
                        val decimalPart = parts[1]
                        if (decimalPart.length > DecimalPlaces) return null

                        val fraction = when (decimalPart.length) {
                            NoDecimalDigits -> 0
                            OneDecimalDigit -> (decimalPart.toIntOrNull() ?: return null) * SingleDigitMultiplier
                            TwoDecimalDigits -> decimalPart.toIntOrNull() ?: return null
                            else -> return null
                        }
                        Percentage(whole * BasisPointsPerPercent + fraction)
                    }
                    else -> null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        /**
         * Create from a decimal multiplier (0.50 -> 50.00%).
         */
        fun fromMultiplier(multiplier: Double): Percentage {
            val bp = kotlin.math.round(multiplier * BasisPointsPerFull).toInt()
            return Percentage(bp)
        }

        /**
         * Parse with validation, throwing if invalid.
         */
        fun parseOrThrow(value: String): Percentage {
            return parse(value) ?: throw DokusException.Validation.InvalidPercentage
        }
    }
}

/**
 * Quantity for line items, supporting decimal quantities.
 *
 * Examples:
 * - 1 unit = Quantity(1.0)
 * - 2.5 hours = Quantity(2.5)
 * - 0.25 kg = Quantity(0.25)
 *
 * Uses Double for simplicity. For high-precision use cases,
 * consider a scaled Long representation.
 */
@Serializable
@JvmInline
value class Quantity(val value: Double) : Comparable<Quantity> {

    override fun toString(): String {
        // Format without unnecessary trailing zeros
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }

    override fun compareTo(other: Quantity): Int = value.compareTo(other.value)

    operator fun plus(other: Quantity): Quantity = Quantity(value + other.value)
    operator fun minus(other: Quantity): Quantity = Quantity(value - other.value)
    operator fun times(factor: Double): Quantity = Quantity(value * factor)

    /**
     * Check if quantity is positive (> 0).
     */
    val isPositive: Boolean get() = value > 0.0

    /**
     * Check if quantity is a whole number.
     */
    val isWhole: Boolean get() = value == value.toLong().toDouble()

    companion object {
        val ONE = Quantity(1.0)
        val ZERO = Quantity(0.0)

        /**
         * Parse a string into Quantity.
         */
        fun parse(value: String): Quantity? {
            val cleaned = value.replace(",", ".").trim()
            if (cleaned.isEmpty()) return null

            return try {
                val d = cleaned.toDoubleOrNull() ?: return null
                if (d <= 0) return null // Quantity must be positive
                Quantity(d)
            } catch (e: NumberFormatException) {
                null
            }
        }

        fun fromDouble(value: Double): Quantity = Quantity(value)
        fun fromInt(value: Int): Quantity = Quantity(value.toDouble())

        /**
         * Parse with validation, throwing if invalid.
         */
        fun parseOrThrow(value: String): Quantity {
            return parse(value) ?: throw DokusException.Validation.InvalidQuantity
        }
    }
}
