package tech.dokus.features.ai.retry

import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.CheckType

/**
 * Builds specific, actionable feedback prompts from audit failures.
 *
 * The key to effective self-correction is providing the model with:
 * 1. WHAT went wrong (the specific error)
 * 2. WHERE to look (which section of the document)
 * 3. WHAT to check (common OCR mistakes, expected format, etc.)
 *
 * Generic "try again" prompts don't work - specific hints dramatically
 * improve correction rates.
 */
object FeedbackPromptBuilder {

    /**
     * Build a complete feedback prompt from an audit report.
     *
     * @param auditReport The audit report with failures
     * @param attempt Current retry attempt number (for context)
     * @param maxRetries Maximum retries allowed (for urgency context)
     * @return Formatted feedback prompt for the retry
     */
    fun buildFeedbackPrompt(
        auditReport: AuditReport,
        attempt: Int,
        maxRetries: Int
    ): String = buildString {
        appendLine("═".repeat(70))
        appendLine("⚠️ CORRECTION REQUIRED (Attempt $attempt of $maxRetries)")
        appendLine("═".repeat(70))
        appendLine()

        val failures = auditReport.criticalFailures + auditReport.warnings

        if (failures.isEmpty()) {
            appendLine("No specific failures to address.")
            return@buildString
        }

        appendLine("Your previous extraction failed validation. Please carefully address")
        appendLine("the following ${failures.size} issue(s):")
        appendLine()

        failures.forEachIndexed { index, check ->
            appendLine("─".repeat(70))
            appendLine("Issue ${index + 1}: ${check.type.displayName}")
            appendLine("─".repeat(70))
            appendLine()
            append(buildCheckFeedback(check))
            appendLine()
        }

        appendLine("═".repeat(70))
        appendLine()
        appendLine("IMPORTANT: Focus ONLY on the fields mentioned above. Re-read those")
        appendLine("specific sections of the document and correct the errors.")
        appendLine()
        if (attempt == maxRetries) {
            appendLine("⚠️ This is your FINAL attempt. Take extra care to verify each field.")
        }
    }

    /**
     * Build feedback for a single check failure.
     */
    fun buildCheckFeedback(check: AuditCheck): String {
        return when (check.type) {
            CheckType.MATH -> buildMathFeedback(check)
            CheckType.CHECKSUM_OGM -> buildOgmFeedback(check)
            CheckType.CHECKSUM_IBAN -> buildIbanFeedback(check)
            CheckType.VAT_RATE -> buildVatRateFeedback(check)
            CheckType.COMPANY_EXISTS -> buildCompanyExistsFeedback(check)
            CheckType.COMPANY_NAME -> buildCompanyNameFeedback(check)
        }
    }

    private fun buildMathFeedback(check: AuditCheck): String = buildString {
        appendLine("❌ MATH ERROR in '${check.field}'")
        appendLine()
        appendLine("Problem: ${check.message}")
        appendLine()
        appendLine("SPECIFIC ACTION: Re-read the TOTALS section of the document.")
        appendLine()

        if (check.expected != null && check.actual != null) {
            appendLine("Expected value: ${check.expected}")
            appendLine("Your extraction: ${check.actual}")
            appendLine()
        }

        appendLine("Common causes of math errors:")
        appendLine("  • Misread digits (1↔7, 0↔6, 5↔S)")
        appendLine("  • Decimal point in wrong position (121.00 vs 1210.0)")
        appendLine("  • Missed a negative sign")
        appendLine("  • Confused net/gross amounts")
        appendLine()
        appendLine("Please re-extract subtotal, VAT amount, and total, paying close")
        appendLine("attention to each digit.")

        check.hint?.let {
            appendLine()
            appendLine("Hint: $it")
        }
    }

    private fun buildOgmFeedback(check: AuditCheck): String = buildString {
        appendLine("❌ OGM CHECKSUM FAILED in '${check.field}'")
        appendLine()
        appendLine("Problem: ${check.message}")
        appendLine()
        appendLine("SPECIFIC ACTION: Re-read the PAYMENT SECTION of the document.")
        appendLine("Look for the structured communication (+++XXX/XXXX/XXXXX+++ format).")
        appendLine()

        if (check.expected != null && check.actual != null) {
            appendLine("Expected check digit: ${check.expected}")
            appendLine("Found check digit: ${check.actual}")
            appendLine()
        }

        appendLine("Common OCR mistakes in payment references:")
        appendLine("  • 0 ↔ O (zero vs letter O)")
        appendLine("  • 1 ↔ I ↔ l (one vs letter I vs lowercase L)")
        appendLine("  • 8 ↔ B (eight vs letter B)")
        appendLine("  • 5 ↔ S (five vs letter S)")
        appendLine("  • 6 ↔ G (six vs letter G)")
        appendLine()
        appendLine("Please re-extract the payment reference, checking each character")
        appendLine("carefully against the document.")

        check.hint?.let {
            appendLine()
            appendLine("Hint: $it")
        }
    }

    private fun buildIbanFeedback(check: AuditCheck): String = buildString {
        appendLine("❌ IBAN CHECKSUM FAILED in '${check.field}'")
        appendLine()
        appendLine("Problem: ${check.message}")
        appendLine()
        appendLine("SPECIFIC ACTION: Re-read the BANK DETAILS section of the document.")
        appendLine()
        appendLine("Belgian IBAN format: BE + 2 check digits + 12 digits = 16 characters")
        appendLine("Example: BE68 5390 0754 7034")
        appendLine()

        if (check.actual != null) {
            appendLine("Your extraction: ${check.actual}")
            val length = check.actual.replace(" ", "").length
            if (length != 16 && check.actual.startsWith("BE")) {
                appendLine("Length: $length characters (should be 16 for Belgian IBAN)")
            }
            appendLine()
        }

        appendLine("Common OCR mistakes in IBANs:")
        appendLine("  • 0 ↔ O (zero vs letter O)")
        appendLine("  • 1 ↔ I (one vs letter I)")
        appendLine("  • Missing or extra characters")
        appendLine()
        appendLine("Please re-extract the IBAN, counting all characters.")

        check.hint?.let {
            appendLine()
            appendLine("Hint: $it")
        }
    }

    private fun buildVatRateFeedback(check: AuditCheck): String = buildString {
        appendLine("⚠️ UNUSUAL VAT RATE in '${check.field}'")
        appendLine()
        appendLine("Problem: ${check.message}")
        appendLine()
        appendLine("SPECIFIC ACTION: Verify the amounts in the TOTALS section.")
        appendLine()
        appendLine("Belgian standard VAT rates are: 0%, 6%, 12%, 21%")
        appendLine()

        if (check.expected != null && check.actual != null) {
            appendLine("Expected rate: ${check.expected}")
            appendLine("Implied rate from extraction: ${check.actual}")
            appendLine()
        }

        appendLine("This could indicate:")
        appendLine("  • Misread amounts (subtotal, VAT, or total)")
        appendLine("  • Foreign invoice (non-Belgian VAT)")
        appendLine("  • Multiple VAT rates on one invoice (needs itemized extraction)")
        appendLine()
        appendLine("Please re-check:")
        appendLine("  1. Subtotal (excl. VAT)")
        appendLine("  2. VAT amount")
        appendLine("  3. Total (incl. VAT)")

        check.hint?.let {
            appendLine()
            appendLine("Hint: $it")
        }
    }

    private fun buildCompanyExistsFeedback(check: AuditCheck): String = buildString {
        appendLine("⚠️ COMPANY NOT FOUND in '${check.field}'")
        appendLine()
        appendLine("Problem: ${check.message}")
        appendLine()
        appendLine("SPECIFIC ACTION: Re-read the VAT NUMBER on the document.")
        appendLine()

        if (check.actual != null) {
            appendLine("Your extraction: ${check.actual}")
            appendLine()
        }

        appendLine("The VAT number you extracted was not found in the Belgian registry (KBO/CBE).")
        appendLine()
        appendLine("Common issues:")
        appendLine("  • Missing 'BE' prefix")
        appendLine("  • Incorrect digits (verify against document)")
        appendLine("  • Foreign company (non-Belgian VAT number)")
        appendLine("  • Misread characters (0↔O, 1↔I)")
        appendLine()
        appendLine("Belgian VAT format: BE + 10 digits (e.g., BE0123456789)")
        appendLine()
        appendLine("Please re-extract the VAT number from the document header or footer.")

        check.hint?.let {
            appendLine()
            appendLine("Hint: $it")
        }
    }

    private fun buildCompanyNameFeedback(check: AuditCheck): String = buildString {
        appendLine("⚠️ COMPANY NAME MISMATCH in '${check.field}'")
        appendLine()
        appendLine("Problem: ${check.message}")
        appendLine()

        if (check.expected != null && check.actual != null) {
            appendLine("Official name (from registry): ${check.expected}")
            appendLine("Your extraction: ${check.actual}")
            appendLine()
        }

        appendLine("The extracted company name doesn't match the official registry name.")
        appendLine()
        appendLine("Possible reasons:")
        appendLine("  • Commercial name vs legal name (both may be correct)")
        appendLine("  • OCR errors in the name")
        appendLine("  • Abbreviated name on document")
        appendLine()
        appendLine("Consider using the official name from the registry for consistency.")

        check.hint?.let {
            appendLine()
            appendLine("Hint: $it")
        }
    }

    /**
     * Build a summary of what fields need correction.
     */
    fun buildCorrectionSummary(failures: List<AuditCheck>): String = buildString {
        val fieldsByType = failures.groupBy { it.type }

        appendLine("Fields requiring correction:")
        fieldsByType.forEach { (type, checks) ->
            appendLine("  • ${type.displayName}: ${checks.map { it.field }.distinct().joinToString(", ")}")
        }
    }

    /**
     * Human-readable display name for check types.
     */
    private val CheckType.displayName: String
        get() = when (this) {
            CheckType.MATH -> "Mathematical Verification"
            CheckType.CHECKSUM_OGM -> "OGM Payment Reference"
            CheckType.CHECKSUM_IBAN -> "IBAN Bank Account"
            CheckType.VAT_RATE -> "VAT Rate"
            CheckType.COMPANY_EXISTS -> "Company Registry"
            CheckType.COMPANY_NAME -> "Company Name"
        }
}
