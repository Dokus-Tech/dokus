package tech.dokus.features.ai.validation

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import kotlin.math.abs

/**
 * Belgian VAT Rate Validator - Legally Aware
 *
 * Standard Belgian VAT rates:
 * - 0%  : Exempt (newspapers, certain medical)
 * - 6%  : Reduced (food, water, books, pharmaceuticals)
 * - 12% : Intermediate (social housing, restaurant food - EXPANDED March 2026)
 * - 21% : Standard (most goods/services)
 *
 * IMPORTANT: March 1, 2026 VAT Reform
 * - 12% rate EXPANDED to include:
 *   - Horeca (restaurants, cafes, bars) - currently 12% for food, 21% for drinks
 *   - Hotels and accommodation
 *   - Restaurant beverages (previously 21%)
 *
 * Reference: Belgian Federal Public Service Finance
 */
object BelgianVatRateValidator {

    /**
     * March 2026 VAT reform effective date.
     * After this date, 12% VAT is valid for all Horeca including beverages.
     */
    val MARCH_2026_REFORM = LocalDate(2026, 3, 1)

    /**
     * Standard Belgian VAT rates in basis points.
     * 100 basis points = 1%
     */
    private val STANDARD_RATES_BP = listOf(0, 600, 1200, 2100) // 0%, 6%, 12%, 21%

    /**
     * Tolerance for rate matching: 0.5% (50 basis points).
     * Allows for minor rounding differences in extracted amounts.
     */
    private const val RATE_TOLERANCE_BP = 50

    /**
     * Categories eligible for 12% rate (especially relevant for March 2026 reform).
     */
    private val HORECA_CATEGORIES = setOf(
        "RESTAURANT", "CAFE", "BAR", "HOTEL", "ACCOMMODATION",
        "FOOD_SERVICE", "CATERING", "HOSPITALITY", "LODGING",
        "BED_AND_BREAKFAST", "HOSTEL", "MOTEL"
    )

    /**
     * Categories eligible for 6% reduced rate.
     */
    private val REDUCED_RATE_CATEGORIES = setOf(
        "FOOD",
        "WATER",
        "BOOKS",
        "NEWSPAPERS",
        "PHARMACEUTICALS",
        "MEDICAL",
        "RENOVATION",
        "SOCIAL_HOUSING"
    )

    /**
     * Verifies that the implied VAT rate matches a standard Belgian rate.
     *
     * @param subtotal Net amount before VAT
     * @param vatAmount VAT amount
     * @param documentDate Date of the document (for reform awareness)
     * @param category Expense category (optional, for Horeca handling)
     * @return AuditCheck with validation result
     */
    fun verify(
        subtotal: Money?,
        vatAmount: Money?,
        documentDate: LocalDate?,
        category: String? = null
    ): AuditCheck {
        // Cannot verify if amounts are missing
        if (subtotal == null || vatAmount == null) {
            return AuditCheck.incomplete(
                type = CheckType.VAT_RATE,
                field = "vatRate",
                message = "Cannot verify VAT rate (missing subtotal or VAT amount)"
            )
        }

        // Cannot calculate rate if subtotal is zero
        if (subtotal.isZero) {
            return AuditCheck.incomplete(
                type = CheckType.VAT_RATE,
                field = "vatRate",
                message = "Cannot verify VAT rate (subtotal is zero)"
            )
        }

        // Calculate implied rate in basis points
        // Formula: (vatAmount / subtotal) * 10000
        val impliedRateBp = ((vatAmount.minor * 10000) / subtotal.minor).toInt()

        // Find the closest standard rate
        val closestRate = STANDARD_RATES_BP.minByOrNull { abs(it - impliedRateBp) } ?: 2100
        val deviation = abs(impliedRateBp - closestRate)

        // Check if within tolerance
        return if (deviation <= RATE_TOLERANCE_BP) {
            validateMatchedRate(closestRate, documentDate, category, impliedRateBp)
        } else {
            // Rate doesn't match any standard Belgian rate
            AuditCheck.warning(
                type = CheckType.VAT_RATE,
                field = "vatRate",
                message = "Unusual VAT rate: ~${formatRate(
                    impliedRateBp
                )} (nearest standard: ${formatRate(closestRate)})",
                hint = buildUnusualRateHint(impliedRateBp, closestRate),
                expected = formatRate(closestRate),
                actual = "~${formatRate(impliedRateBp)}"
            )
        }
    }

    /**
     * Validates a matched rate with special handling for 12% (Horeca/March 2026).
     */
    private fun validateMatchedRate(
        rateBp: Int,
        documentDate: LocalDate?,
        category: String?,
        impliedRateBp: Int
    ): AuditCheck {
        // Special handling for 12% rate (most complex due to March 2026 reform)
        if (rateBp == 1200 && category != null) {
            return validate12PercentRate(documentDate, category, impliedRateBp)
        }

        // Standard rates validation
        val rateDescription = when (rateBp) {
            0 -> "0% (exempt)"
            600 -> "6% (reduced rate)"
            1200 -> "12% (intermediate rate)"
            2100 -> "21% (standard rate)"
            else -> "${formatRate(rateBp)}"
        }

        return AuditCheck.passed(
            type = CheckType.VAT_RATE,
            field = "vatRate",
            message = "VAT rate verified: $rateDescription"
        )
    }

    /**
     * Validates 12% rate with awareness of March 2026 Horeca reform.
     */
    private fun validate12PercentRate(
        documentDate: LocalDate?,
        category: String,
        impliedRateBp: Int
    ): AuditCheck {
        val isHoreca = HORECA_CATEGORIES.any {
            category.uppercase().contains(it)
        }

        val isPostReform = documentDate != null && documentDate >= MARCH_2026_REFORM

        return when {
            isHoreca && isPostReform -> {
                // Post-reform: 12% is fully valid for all Horeca (food AND drinks)
                AuditCheck.passed(
                    type = CheckType.VAT_RATE,
                    field = "vatRate",
                    message = "VAT rate 12% verified (post-March 2026 Horeca rate - includes beverages)"
                )
            }
            isHoreca && !isPostReform -> {
                // Pre-reform: 12% only for food, drinks should be 21%
                // This is still valid but worth noting
                AuditCheck.passed(
                    type = CheckType.VAT_RATE,
                    field = "vatRate",
                    message = "VAT rate 12% verified (pre-March 2026: applies to food only, beverages at 21%)"
                )
            }
            else -> {
                // Non-Horeca 12%: social housing, certain construction work
                AuditCheck.passed(
                    type = CheckType.VAT_RATE,
                    field = "vatRate",
                    message = "VAT rate 12% verified (intermediate rate)"
                )
            }
        }
    }

    /**
     * Builds a helpful hint for unusual VAT rates.
     */
    private fun buildUnusualRateHint(impliedBp: Int, nearestBp: Int): String = buildString {
        append("The implied VAT rate (~${formatRate(impliedBp)}) ")
        append("doesn't match any standard Belgian rate. ")
        append("Belgian rates are: 0%, 6%, 12%, 21%. ")

        if (impliedBp > 2100) {
            append("Rate seems too high - check if amounts are correct. ")
        } else if (impliedBp < 0) {
            append("Negative rate detected - check for credit note or refund. ")
        } else {
            append("Nearest standard rate is ${formatRate(nearestBp)}. ")
        }

        append("Please verify: (1) This is a Belgian document, ")
        append("(2) Subtotal and VAT amounts are correct.")
    }

    /**
     * Formats a basis point rate as percentage string.
     */
    private fun formatRate(basisPoints: Int): String {
        val percent = basisPoints / 100
        val decimal = basisPoints % 100
        return if (decimal == 0) {
            "$percent%"
        } else {
            "$percent.${decimal.toString().padStart(2, '0')}%"
        }
    }

    /**
     * Checks if a category suggests Horeca (for 12% rate validation).
     */
    fun isHorecaCategory(category: String?): Boolean {
        if (category == null) return false
        return HORECA_CATEGORIES.any { category.uppercase().contains(it) }
    }

    /**
     * Checks if a date is after the March 2026 reform.
     */
    fun isPostReform(date: LocalDate?): Boolean {
        return date != null && date >= MARCH_2026_REFORM
    }

    /**
     * Returns all valid Belgian VAT rates in basis points.
     */
    fun getStandardRates(): List<Int> = STANDARD_RATES_BP
}
