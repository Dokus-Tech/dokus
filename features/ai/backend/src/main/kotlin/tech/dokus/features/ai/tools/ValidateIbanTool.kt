package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.validators.ValidateIbanUseCase

/**
 * Layer 3 Tool: IBAN (International Bank Account Number) validation.
 *
 * Validates IBANs using the Mod-97 checksum algorithm.
 * Belgian IBANs must be exactly 16 characters.
 */
object ValidateIbanTool : SimpleTool<ValidateIbanTool.Args>(
    argsSerializer = Args.serializer(),
    name = "validate_iban",
    description = """
        Validates an IBAN (International Bank Account Number) using Mod-97 checksum.

        Belgian IBANs are exactly 16 characters: BExx xxxx xxxx xxxx
        Other country IBANs vary from 15-34 characters.

        Use this to verify bank account numbers have correct check digits.
        Common OCR errors: 0↔O, 1↔I (one vs letter I).

        Returns "VALID" with normalized format, or "INVALID" with error details.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "The IBAN to validate. Can include spaces or dashes. " +
                "Example: 'BE68 5390 0754 7034' or 'BE68539007547034'"
        )
        val iban: String
    )

    override suspend fun execute(args: Args): String {
        val cleaned = args.iban.replace(" ", "").replace("-", "").uppercase()

        if (cleaned.isBlank()) {
            return "INVALID: Empty IBAN provided."
        }

        // Basic format check
        if (!cleaned.matches(Regex("^[A-Z]{2}\\d{2}[A-Z0-9]+$"))) {
            return "INVALID: IBAN format incorrect. Must start with 2 letters (country code), " +
                "followed by 2 digits (check digits), then alphanumeric characters. " +
                "Got: '${args.iban}'. Check for OCR errors: 0↔O, 1↔I."
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
                "Got: '${args.iban}'"
        }
    }

    private fun formatIban(iban: String): String {
        return iban.chunked(4).joinToString(" ")
    }
}
