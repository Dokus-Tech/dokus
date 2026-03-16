package tech.dokus.features.ai.validation

import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.VatNumber

/**
 * Wraps checksum validators (OGM, IBAN) and produces AuditCheck results.
 *
 * This validator provides:
 * - Validation result as AuditCheck
 * - Specific hints for Layer 4 retry prompts
 */
object ChecksumValidator {

    /**
     * Validates a Belgian OGM (Structured Communication) and returns an AuditCheck.
     *
     * @param structuredComm The structured communication to validate (may be null)
     * @return AuditCheck with validation result
     */
    fun auditOgm(structuredComm: StructuredCommunication?): AuditCheck {
        val raw = structuredComm?.value
        if (raw.isNullOrBlank()) {
            return AuditCheck.incomplete(
                type = CheckType.CHECKSUM_OGM,
                field = "structuredComm",
                message = "No structured communication to validate"
            )
        }

        return if (structuredComm.isValid) {
            AuditCheck.passed(
                type = CheckType.CHECKSUM_OGM,
                field = "structuredComm",
                message = "OGM checksum verified: $raw"
            )
        } else {
            AuditCheck.criticalFailure(
                type = CheckType.CHECKSUM_OGM,
                field = "structuredComm",
                message = "OGM checksum failed",
                hint = "Re-read the PAYMENT SECTION of the document and verify the structured communication.",
                expected = "Valid OGM (mod-97 check)",
                actual = raw
            )
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

        return if (iban.isValid) {
            AuditCheck.passed(
                type = CheckType.CHECKSUM_IBAN,
                field = "iban",
                message = "IBAN checksum verified: ${iban.value}"
            )
        } else {
            AuditCheck.warning(
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
     * Validates a VAT number format and returns an AuditCheck.
     *
     * @param vat The VAT number to validate (may be null)
     * @param fieldName The field name for reporting (e.g. "sellerVat", "buyerVat")
     * @return AuditCheck with validation result
     */
    fun auditVatFormat(vat: VatNumber?, fieldName: String): AuditCheck {
        if (vat == null) {
            return AuditCheck.incomplete(
                type = CheckType.COUNTERPARTY_INTEGRITY,
                field = fieldName,
                message = "No $fieldName to validate"
            )
        }
        return if (vat.isValid) {
            AuditCheck.passed(
                type = CheckType.COUNTERPARTY_INTEGRITY,
                field = fieldName,
                message = "$fieldName format verified: ${vat.normalized}"
            )
        } else {
            AuditCheck.warning(
                type = CheckType.COUNTERPARTY_INTEGRITY,
                field = fieldName,
                message = "$fieldName appears malformed: ${vat.value}",
                hint = "The extracted $fieldName '${vat.value}' does not match any known EU VAT format. " +
                    "Re-read the document footer and legal block carefully for the correct VAT number. " +
                    "Belgian VAT = BE + exactly 10 digits. If you cannot find a valid VAT, set it to null.",
                expected = "Valid EU VAT number format",
                actual = vat.value,
            )
        }
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
}
