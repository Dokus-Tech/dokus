package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.validators.OgmValidationResult
import tech.dokus.domain.validators.ValidateIbanUseCase
import tech.dokus.domain.validators.ValidateOgmUseCase
import tech.dokus.foundation.backend.lookup.CbeApiClient
import java.math.BigDecimal

class LegalEntitiesTools(
    private val cbeApiClient: CbeApiClient
) : ToolSet {

    @Tool
    @LLMDescription(
        """
        Looks up a company in the Belgian CBE (Crossroads Bank for Enterprises) registry.

        Use this to verify:
        - A VAT number exists in the official Belgian registry
        - The extracted company name matches the official legal name
        - The company is still active

        Input: Belgian VAT number (e.g., 'BE0123456789' or '0123.456.789')
        Returns: Official company name, address, and status, or "NOT FOUND".
    """
    )
    suspend fun lookupCompany(
        @LLMDescription("The Belgian VAT number to look up. Can include 'BE' prefix, spaces, or dots. Examples: 'BE0123456789', '0123.456.789', 'BE 0123 456 789'")
        vatNumber: String
    ): EntityLookup? {
        // Create and normalize the VAT number
        val vatNumber = VatNumber(vatNumber).also {
            // Check if input looks valid enough to process
            if (it.normalized.isBlank()) return null
            if (!it.isBelgian) return null
        }

        // Look up in CBE
        return cbeApiClient.searchByVat(vatNumber).getOrNull()
    }

    @Tool
    @LLMDescription(
        """
        Validates an IBAN (International Bank Account Number) using Mod-97 checksum.

        Belgian IBANs are exactly 16 characters: BExx xxxx xxxx xxxx
        Other country IBANs vary from 15-34 characters.

        Use this to verify bank account numbers have correct check digits.
        Common OCR errors: 0↔O, 1↔I (one vs letter I).

        Returns "VALID" with normalized format, or "INVALID" with error details.
    """
    )
    fun validateIban(
        @LLMDescription(
            "The IBAN to validate. Can include spaces or dashes. " +
                    "Example: 'BE68 5390 0754 7034' or 'BE68539007547034'"
        )
        iban: String
    ): String {
        val cleaned = iban.replace(" ", "").replace("-", "").uppercase()

        if (cleaned.isBlank()) {
            return "INVALID: Empty IBAN provided."
        }

        // Basic format check
        if (!cleaned.matches(Regex("^[A-Z]{2}\\d{2}[A-Z0-9]+$"))) {
            return "INVALID: IBAN format incorrect. Must start with 2 letters (country code), " +
                    "followed by 2 digits (check digits), then alphanumeric characters. " +
                    "Got: '${iban}'. Check for OCR errors: 0↔O, 1↔I."
        }

        // Check Belgian IBAN length
        if (cleaned.startsWith("BE") && cleaned.length != 16) {
            return "INVALID: Belgian IBAN must be exactly 16 characters. " +
                    "Got ${cleaned.length} characters. Re-read the bank details section."
        }

        // Validate checksum
        val isValid = ValidateIbanUseCase(Iban(cleaned))

        return if (isValid) {
            val formatted = formatIban(cleaned)
            "VALID: IBAN checksum verified. Normalized: $formatted"
        } else {
            "INVALID: IBAN checksum failed (Mod-97 validation). " +
                    "Re-read the IBAN carefully. Common OCR errors: 0↔O, 1↔I. " +
                    "Got: '${iban}'"
        }
    }

    @Tool
    @LLMDescription(
        """
        Validates a Belgian Structured Communication (OGM/Gestructureerde Mededeling).
        Format: +++123/4567/89012+++ or ***123/4567/89012***

        The last 2 digits are a checksum: first 10 digits mod 97 (if 0, use 97).

        Use this to verify payment references have correct check digits.
        Also detects and corrects common OCR errors: 0↔O, 1↔I, 8↔B, 5↔S, 6↔G.

        Returns "VALID" with normalized format, or "INVALID" with error details.
        """
    )
    fun validateOgm(
        @LLMDescription(
            "The OGM payment reference to validate. " +
                    "Expected format: +++123/4567/89012+++ or ***123/4567/89012***"
        )
        ogm: String
    ): String {
        return when (val result = ValidateOgmUseCase.validate(ogm)) {
            is OgmValidationResult.Valid -> {
                "VALID: OGM checksum verified. Normalized: ${result.normalized}"
            }

            is OgmValidationResult.CorrectedValid -> {
                "VALID (with OCR correction): Original '${result.original}' was corrected to '${result.normalized}'. " +
                        "Applied corrections: ${result.corrections}"
            }

            is OgmValidationResult.InvalidFormat -> {
                "INVALID: Not a valid OGM format. Expected: +++XXX/XXXX/XXXXX+++ or ***XXX/XXXX/XXXXX***. " +
                        "Got: '${ogm}'. Re-read the payment section of the document."
            }

            is OgmValidationResult.InvalidChecksum -> {
                "INVALID: OGM checksum failed. Expected check digit: ${result.expected}, found: ${result.actual}. " +
                        "Common OCR errors to check: 0↔O, 1↔I, 8↔B, 5↔S, 6↔G. " +
                        "Re-read the payment reference carefully, especially ambiguous characters."
            }
        }
    }

    @Tool("verify_totals")
    @LLMDescription(
        """
        Validates that subtotal + VAT equals total amount.
        Use this BEFORE outputting final JSON to catch math errors.

        Returns "VALID" if math is correct, or an error message with the expected value.
        """
    )
    fun verifyTotals(
        @LLMDescription("The subtotal amount (net, before VAT) as a decimal string, e.g., '100.00'")
        subtotal: String,
        @LLMDescription("The VAT amount as a decimal string, e.g., '21.00'")
        vatAmount: String,
        @LLMDescription("The total amount (gross, including VAT) as a decimal string, e.g., '121.00'")
        total: String
    ): String {
        return try {
            val subtotal = parseAmount(subtotal)
            val vat = parseAmount(vatAmount)
            val total = parseAmount(total)

            // Validates amounts; returns success or error message
            if (subtotal == null || vat == null || total == null) {
                "ERROR: Could not parse amounts. Use decimal format like '100.00'"
            } else {
                val expected = subtotal + vat
                val diff = (expected - total).abs()

                if (diff <= TOLERANCE) {
                    "VALID: Math verified. $subtotal + $vatAmount = $total"
                } else {
                    "ERROR: Math error. $subtotal + $vatAmount should equal ${expected.toPlainString()}, but found ${total}. " +
                            "Difference: ${diff.toPlainString()}. Re-read the total amount from the document."
                }
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }
}

private fun formatIban(iban: String): String {
    return iban.chunked(4).joinToString(" ")
}

private val TOLERANCE = BigDecimal("0.02")
private fun parseAmount(value: String): BigDecimal? {
    val cleaned = value
        .trim()
        .replace(" ", "")
        .replace(",", ".") // Handle European decimal separator
        .replace("€", "")
        .replace("EUR", "")
    return try {
        BigDecimal(cleaned)
    } catch (_: NumberFormatException) {
        null
    }
}
