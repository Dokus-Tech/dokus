@file:Suppress(
    "ReturnCount" // Parsing functions require multiple early returns for validation
)

package tech.dokus.domain

import kotlinx.serialization.Serializable
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
 * Money represented in minor units (cents for EUR/USD/GBP).
 *
 * Examples:
 * - €123.45 = Money(12345)
 * - $0.01 = Money(1)
 * - €-50.00 = Money(-5000)
 *
 * Using minor units (Long) avoids all floating-point precision issues.
 */
@Serializable
@JvmInline
value class Money(val minor: Long) : Comparable<Money> {

    /**
     * Display string with 2 decimal places.
     * Examples: "123.45", "-50.00", "0.00"
     */
    fun toDisplayString(): String {
        val isNegative = minor < 0
        val absMinor = if (minor < 0) -minor else minor
        val whole = absMinor / CentsPerUnit
        val cents = absMinor % CentsPerUnit
        val sign = if (isNegative) "-" else ""
        return "$sign$whole.${cents.toString().padStart(DecimalPlaces, '0')}"
    }

    override fun toString(): String = toDisplayString()

    override fun compareTo(other: Money): Int = minor.compareTo(other.minor)

    operator fun plus(other: Money): Money = Money(minor + other.minor)
    operator fun minus(other: Money): Money = Money(minor - other.minor)
    operator fun times(quantity: Int): Money = Money(minor * quantity)
    operator fun times(quantity: Long): Money = Money(minor * quantity)
    operator fun unaryMinus(): Money = Money(-minor)

    /**
     * Check if this amount is zero.
     */
    val isZero: Boolean get() = minor == 0L

    /**
     * Check if this amount is negative.
     */
    val isNegative: Boolean get() = minor < 0

    /**
     * Check if this amount is positive (> 0).
     */
    val isPositive: Boolean get() = minor > 0

    /**
     * Convert to Double value in major units.
     * Example: Money(12345).toDouble() = 123.45
     */
    fun toDouble(): Double = minor / CentsPerUnitDouble

    companion object {
        val ZERO = Money(0L)

        /**
         * Parse a display string like "123.45" or "-50.00" into Money.
         * Returns null if the string is not a valid money format.
         *
         * Handles:
         * - Currency symbols (€$£) - stripped
         * - Whitespace - stripped
         * - Comma as decimal separator - converted to dot
         * - Optional negative sign
         */
        @Suppress("CyclomaticComplexMethod") // Currency parsing inherently requires multiple format checks
        fun parse(value: String): Money? {
            val cleaned = value
                .replace("€", "")
                .replace("$", "")
                .replace("£", "")
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
                        // No decimal part: "123" -> 12300
                        val whole = parts[0].toLongOrNull() ?: return null
                        val minor = whole * CentsPerUnit
                        Money(if (isNegative) -minor else minor)
                    }
                    2 -> {
                        // Has decimal part: "123.45" -> 12345
                        val whole = parts[0].toLongOrNull() ?: return null
                        val decimalPart = parts[1]
                        if (decimalPart.length > DecimalPlaces) return null // Too many decimals

                        val cents = when (decimalPart.length) {
                            NoDecimalDigits -> 0L
                            OneDecimalDigit -> (decimalPart.toLongOrNull() ?: return null) * SingleDigitMultiplier
                            TwoDecimalDigits -> decimalPart.toLongOrNull() ?: return null
                            else -> return null
                        }
                        val minor = whole * CentsPerUnit + cents
                        Money(if (isNegative) -minor else minor)
                    }
                    else -> null // Multiple decimal points
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        /**
         * Create Money from a Double value.
         * Uses rounding to handle floating-point imprecision.
         *
         * @param value The amount in major units (e.g., 123.45)
         */
        fun fromDouble(value: Double): Money {
            val minor = kotlin.math.round(value * CentsPerUnit).toLong()
            return Money(minor)
        }

        /**
         * Create Money from an Int value (whole units, no cents).
         *
         * @param value The amount in major units (e.g., 123 becomes €123.00)
         */
        fun fromInt(value: Int): Money = Money(value.toLong() * CentsPerUnit)

        /**
         * Parse a string with validation, throwing if invalid.
         */
        fun parseOrThrow(value: String): Money {
            return parse(value) ?: throw DokusException.Validation.InvalidMoney
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
        return Money(vatMinor)
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
        return Money(result)
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
