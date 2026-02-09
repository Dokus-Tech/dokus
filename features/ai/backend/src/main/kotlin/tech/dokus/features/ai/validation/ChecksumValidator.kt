package tech.dokus.features.ai.validation

import tech.dokus.domain.ids.Iban
import tech.dokus.domain.validators.OgmValidationResult
import tech.dokus.domain.validators.ValidateIbanUseCase
import tech.dokus.domain.validators.ValidateOgmUseCase

/**
 * Wraps checksum validators (OGM, IBAN) and produces AuditCheck results.
 *
 * This validator provides:
 * - Validation result as AuditCheck
 * - Specific hints for Layer 4 retry prompts
 * - OCR correction awareness (for OGM)
 */
object ChecksumValidator {

    /**
     * Validates a Belgian OGM (Structured Communication) and returns an AuditCheck.
     *
     * @param structuredComm The structured communication to validate (may be null)
     * @return AuditCheck with validation result and retry hints if failed
     */
    fun auditOgm(structuredComm: String?): AuditCheck {
        if (structuredComm.isNullOrBlank()) {
            return AuditCheck.incomplete(
                type = CheckType.CHECKSUM_OGM,
                field = "structuredComm",
                message = "No structured communication to validate"
            )
        }

        // Check if it looks like an OGM at all
        if (!ValidateOgmUseCase.looksLikeOgm(structuredComm)) {
            // Not an OGM format - this is INFO, not a failure
            return AuditCheck.incomplete(
                type = CheckType.CHECKSUM_OGM,
                field = "structuredComm",
                message = "Structured communication is not in OGM format"
            )
        }

        return when (val result = ValidateOgmUseCase.validate(structuredComm)) {
            is OgmValidationResult.Valid -> {
                AuditCheck.passed(
                    type = CheckType.CHECKSUM_OGM,
                    field = "structuredComm",
                    message = "OGM checksum verified: ${result.normalized}"
                )
            }

            is OgmValidationResult.CorrectedValid -> {
                // Valid after OCR correction - pass but note the correction
                AuditCheck(
                    type = CheckType.CHECKSUM_OGM,
                    field = "structuredComm",
                    passed = true,
                    severity = Severity.INFO,
                    message = "OGM validated after OCR correction: ${result.normalized}",
                    hint = result.corrections,
                    expected = result.normalized,
                    actual = result.original
                )
            }

            is OgmValidationResult.InvalidFormat -> {
                AuditCheck.warning(
                    type = CheckType.CHECKSUM_OGM,
                    field = "structuredComm",
                    message = "Invalid OGM format: ${result.message}",
                    hint = buildOgmFormatHint(structuredComm, result.hint),
                    expected = "+++XXX/XXXX/XXXXX+++",
                    actual = structuredComm
                )
            }

            is OgmValidationResult.InvalidChecksum -> {
                AuditCheck.criticalFailure(
                    type = CheckType.CHECKSUM_OGM,
                    field = "structuredComm",
                    message = "OGM checksum failed: expected ${result.expected}, got ${result.actual}",
                    hint = buildOgmChecksumHint(structuredComm, result.expected, result.actual),
                    expected = "Check digit: ${result.expected.toString().padStart(2, '0')}",
                    actual = "Check digit: ${result.actual.toString().padStart(2, '0')}"
                )
            }
        }
    }

    /**
     * Validates an IBAN and returns an AuditCheck.
     *
     * @param iban The IBAN string to validate (may be null)
     * @return AuditCheck with validation result and retry hints if failed
     */
    fun auditIban(iban: Iban?): AuditCheck {
        val raw = iban?.value
        if (raw.isNullOrBlank()) {
            return AuditCheck.incomplete(
                type = CheckType.CHECKSUM_IBAN,
                field = "iban",
                message = "No IBAN to validate"
            )
        }

        val isValid = ValidateIbanUseCase(iban)

        return if (isValid) {
            val normalized = normalizeIban(raw)
            AuditCheck.passed(
                type = CheckType.CHECKSUM_IBAN,
                field = "iban",
                message = "IBAN checksum verified: $normalized"
            )
        } else {
            AuditCheck.criticalFailure(
                type = CheckType.CHECKSUM_IBAN,
                field = "iban",
                message = "IBAN checksum failed",
                hint = buildIbanHint(raw),
                expected = "Valid IBAN (mod-97 check)",
                actual = raw
            )
        }
    }

    /**
     * Builds a helpful hint for OGM format errors.
     */
    private fun buildOgmFormatHint(input: String, validatorHint: String): String = buildString {
        append("Re-read the PAYMENT SECTION of the document. ")
        append(validatorHint)
        append(" ")

        // Check for specific format issues
        val digitCount = input.count { it.isDigit() }
        if (digitCount < 12) {
            append("Found only $digitCount digits, need exactly 12. ")
        } else if (digitCount > 12) {
            append("Found $digitCount digits, should be exactly 12. ")
        }

        if (!input.contains("/")) {
            append("Missing slash separators (format: XXX/XXXX/XXXXX). ")
        }
    }

    /**
     * Builds a specific hint for OGM checksum failures.
     */
    private fun buildOgmChecksumHint(input: String, expected: Int, actual: Int): String = buildString {
        append("Re-read the payment reference number carefully. ")
        append("The Mod-97 checksum failed. ")
        append("Expected check digits: ${expected.toString().padStart(2, '0')}, ")
        append("found: ${actual.toString().padStart(2, '0')}. ")
        appendLine()
        appendLine()
        append("Common OCR character substitutions to check:")
        appendLine()
        append("  - 0 (zero) ↔ O (letter O)")
        appendLine()
        append("  - 1 (one) ↔ I (letter I) ↔ l (lowercase L)")
        appendLine()
        append("  - 8 ↔ B")
        appendLine()
        append("  - 5 ↔ S")
        appendLine()
        append("  - 6 ↔ G")
        appendLine()
        appendLine()
        append("Look at each digit in the payment section and verify it matches what you extracted.")
    }

    /**
     * Builds a helpful hint for IBAN validation failures.
     */
    private fun buildIbanHint(iban: String): String = buildString {
        val cleaned = iban.replace(" ", "").replace("-", "").uppercase()

        append("Re-read the BANK DETAILS section of the document. ")
        append("The IBAN checksum verification failed. ")
        appendLine()

        // Check for specific issues
        if (cleaned.length < 15) {
            append("IBAN too short (${cleaned.length} characters, minimum 15). ")
        } else if (cleaned.length > 34) {
            append("IBAN too long (${cleaned.length} characters, maximum 34). ")
        }

        if (cleaned.startsWith("BE") && cleaned.length != 16) {
            append("Belgian IBANs must be exactly 16 characters (found ${cleaned.length}). ")
        }

        appendLine()
        append("Common OCR substitutions to check:")
        appendLine()
        append("  - 0 (zero) ↔ O (letter O)")
        appendLine()
        append("  - 1 (one) ↔ I (letter I)")
        appendLine()
        append("  - Missing or extra characters")
        appendLine()
        appendLine()

        if (cleaned.startsWith("BE")) {
            append("Belgian IBAN format: BE + 2 check digits + 12 digits")
            appendLine()
            append("Example: BE68 5390 0754 7034")
        } else {
            append("Verify country code and all characters are correct.")
        }
    }

    /**
     * Normalizes an IBAN to standard format (uppercase, no spaces).
     */
    private fun normalizeIban(iban: String): String {
        val cleaned = iban.replace(" ", "").replace("-", "").uppercase()
        // Format with spaces every 4 characters for readability
        return cleaned.chunked(4).joinToString(" ")
    }
}
