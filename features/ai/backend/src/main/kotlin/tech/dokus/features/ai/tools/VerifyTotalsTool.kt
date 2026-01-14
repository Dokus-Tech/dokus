package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Layer 3 Tool: Math verification for AI agents.
 *
 * Validates that subtotal + VAT equals total amount.
 * Use this BEFORE outputting final JSON to catch math errors.
 */
object VerifyTotalsTool : SimpleTool<VerifyTotalsTool.Args>(
    argsSerializer = Args.serializer(),
    name = "verify_totals",
    description = """
        Validates that subtotal + VAT equals total amount.
        Use this BEFORE outputting final JSON to catch math errors.
        Returns "VALID" if math is correct, or an error message with the expected value.

        Example: subtotal="100.00", vatAmount="21.00", total="121.00" -> VALID
        Example: subtotal="100.00", vatAmount="21.00", total="120.00" -> ERROR: Expected 121.00
    """.trimIndent()
) {
    // Tolerance of ±0.02 EUR for rounding differences
    private val TOLERANCE = BigDecimal("0.02")

    @Serializable
    data class Args(
        @property:LLMDescription("The subtotal amount (net, before VAT) as a decimal string, e.g., '100.00'")
        val subtotal: String,

        @property:LLMDescription("The VAT amount as a decimal string, e.g., '21.00'")
        val vatAmount: String,

        @property:LLMDescription("The total amount (gross, including VAT) as a decimal string, e.g., '121.00'")
        val total: String
    )

    override suspend fun execute(args: Args): String {
        return try {
            val subtotal = parseAmount(args.subtotal)
            val vat = parseAmount(args.vatAmount)
            val total = parseAmount(args.total)

            if (subtotal == null || vat == null || total == null) {
                "ERROR: Could not parse amounts. Use decimal format like '100.00'"
            } else {
                val expected = subtotal + vat
                val diff = (expected - total).abs()

                if (diff <= TOLERANCE) {
                    "VALID: Math verified. ${args.subtotal} + ${args.vatAmount} = ${args.total}"
                } else {
                    "ERROR: Math error. ${args.subtotal} + ${args.vatAmount} should equal ${expected.toPlainString()}, but found ${args.total}. " +
                        "Difference: ${diff.toPlainString()}. Re-read the total amount from the document."
                }
            }
        } catch (e: Exception) {
            "ERROR: ${e.message}"
        }
    }

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
}
