package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.domain.validators.OgmValidationResult
import tech.dokus.domain.validators.ValidateOgmUseCase

/**
 * Layer 3 Tool: OGM (Belgian Structured Communication) validation.
 *
 * Validates Belgian payment references with Mod-97 checksum.
 * Also attempts OCR correction for common character substitutions.
 */
object ValidateOgmTool : SimpleTool<ValidateOgmTool.Args>(
    argsSerializer = Args.serializer(),
    name = "validate_ogm",
    description = """
        Validates a Belgian Structured Communication (OGM/Gestructureerde Mededeling).
        Format: +++123/4567/89012+++ or ***123/4567/89012***

        The last 2 digits are a checksum: first 10 digits mod 97 (if 0, use 97).

        Use this to verify payment references have correct check digits.
        Also detects and corrects common OCR errors: 0↔O, 1↔I, 8↔B, 5↔S, 6↔G.

        Returns "VALID" with normalized format, or "INVALID" with error details.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "The OGM payment reference to validate. " +
                "Expected format: +++123/4567/89012+++ or ***123/4567/89012***"
        )
        val ogm: String
    )

    override suspend fun execute(args: Args): String {
        return when (val result = ValidateOgmUseCase.validate(args.ogm)) {
            is OgmValidationResult.Valid -> {
                "VALID: OGM checksum verified. Normalized: ${result.normalized}"
            }

            is OgmValidationResult.CorrectedValid -> {
                "VALID (with OCR correction): Original '${result.original}' was corrected to '${result.normalized}'. " +
                    "Applied corrections: ${result.corrections}"
            }

            is OgmValidationResult.InvalidFormat -> {
                "INVALID: Not a valid OGM format. Expected: +++XXX/XXXX/XXXXX+++ or ***XXX/XXXX/XXXXX***. " +
                    "Got: '${args.ogm}'. Re-read the payment section of the document."
            }

            is OgmValidationResult.InvalidChecksum -> {
                "INVALID: OGM checksum failed. Expected check digit: ${result.expected}, found: ${result.actual}. " +
                    "Common OCR errors to check: 0↔O, 1↔I, 8↔B, 5↔S, 6↔G. " +
                    "Re-read the payment reference carefully, especially ambiguous characters."
            }
        }
    }
}
