package tech.dokus.features.ai.prompts

/**
 * Simplified prompts optimized for 4B models in light mode (qwen3:4b).
 *
 * These prompts use:
 * - Shorter system instructions
 * - Few-shot examples (2-3 instead of zero-shot)
 * - Simpler JSON output schemas
 * - No provenance tracking
 *
 * The few-shot examples are designed around common Belgian invoice formats
 * from KBC Leasing, Belfius, Proximus, and similar providers.
 *
 * TODO: Add real Belgian invoice examples once collected.
 */
object LightModePrompts {

    /**
     * Simplified classification prompt for 4B models.
     *
     * Uses few-shot examples to improve accuracy on smaller models.
     */
    val CLASSIFICATION_SYSTEM_PROMPT = """
        You classify documents. Output JSON only.

        Types:
        - INVOICE: Payment request with invoice number, VAT, line items
        - RECEIPT: Store purchase proof
        - BILL: Utility/service charge
        - UNKNOWN: Cannot determine

        Output: {"documentType": "TYPE", "confidence": 0.0-1.0}
    """.trimIndent()

    /**
     * Few-shot examples for classification.
     *
     * TODO: Replace with real Belgian invoice examples.
     */
    val CLASSIFICATION_EXAMPLES = listOf(
        // Example 1: Invoice
        FewShotExample(
            input = "FACTUUR Nr. 2024-001\nBTW: BE0123456789\nBedrag: €1.500,00",
            output = """{"documentType": "INVOICE", "confidence": 0.95}"""
        ),
        // Example 2: Receipt
        FewShotExample(
            input = "CARREFOUR\nTicket 12345\nTOTAAL: €25,50\nBetaald met Bancontact",
            output = """{"documentType": "RECEIPT", "confidence": 0.90}"""
        ),
        // Example 3: Bill
        FewShotExample(
            input = "PROXIMUS\nFactuur voor periode 01/01 - 31/01\nKlantnummer: 123456",
            output = """{"documentType": "BILL", "confidence": 0.92}"""
        )
    )

    /**
     * Simplified invoice extraction prompt for 4B models.
     *
     * TODO: Implement simplified extraction prompts with few-shot examples.
     */
    val INVOICE_EXTRACTION_SYSTEM_PROMPT = """
        Extract invoice data. Output JSON only.

        Extract: vendor name, VAT number (BE format), invoice number,
        date, total amount, currency.

        TODO: Add full schema and few-shot examples.
    """.trimIndent()

    /**
     * Simplified bill extraction prompt for 4B models.
     *
     * TODO: Implement simplified extraction prompts with few-shot examples.
     */
    val BILL_EXTRACTION_SYSTEM_PROMPT = """
        Extract bill/supplier invoice data. Output JSON only.

        Extract: supplier name, bill number, date, total amount, category.

        TODO: Add full schema and few-shot examples.
    """.trimIndent()

    /**
     * Simplified receipt extraction prompt for 4B models.
     *
     * TODO: Implement simplified extraction prompts with few-shot examples.
     */
    val RECEIPT_EXTRACTION_SYSTEM_PROMPT = """
        Extract receipt data. Output JSON only.

        Extract: merchant name, date, total amount, payment method.

        TODO: Add full schema and few-shot examples.
    """.trimIndent()

    /**
     * Simplified categorization prompt for 4B models.
     *
     * TODO: Implement with Belgian expense categories.
     */
    val CATEGORIZATION_SYSTEM_PROMPT = """
        Categorize expense for Belgian IT freelancer.

        Categories: OFFICE_SUPPLIES, HARDWARE, SOFTWARE, TRAVEL, MEALS,
        PROFESSIONAL_SERVICES, TELECOM, INSURANCE, TRAINING, OTHER

        Output: {"category": "CATEGORY", "confidence": 0.0-1.0}

        TODO: Add few-shot examples.
    """.trimIndent()
}

/**
 * A few-shot example for prompting.
 */
data class FewShotExample(
    val input: String,
    val output: String
)
