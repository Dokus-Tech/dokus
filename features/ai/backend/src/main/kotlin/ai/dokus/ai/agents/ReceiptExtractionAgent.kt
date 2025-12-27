package ai.dokus.ai.agents

import ai.dokus.ai.models.ExtractedReceiptData
import tech.dokus.foundation.ktor.utils.loggerFor
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.json.Json

/**
 * Agent responsible for extracting data from receipt documents.
 * Step 2b in the two-step document processing pipeline (for receipts).
 */
class ReceiptExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = loggerFor()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val systemPrompt = """
        You are a receipt data extraction specialist.
        Extract structured data from store receipts and purchase proofs.
        Always respond with valid JSON matching the requested schema.

        Extract these fields:
        - Merchant: name, address, VAT number (if present)
        - Transaction: date, time, receipt number
        - Items: description, quantity, price (group similar items)
        - Totals: subtotal, VAT amount, total, payment method
        - Category suggestion based on merchant/items

        Guidelines:
        - Use null for fields that cannot be found or are unclear
        - Dates should be in ISO format (YYYY-MM-DD)
        - Times should be in HH:mm format (24-hour)
        - Currency should be 3-letter ISO code (EUR, USD, GBP)
        - Amounts should be strings to preserve precision (e.g., "12.50")
        - Payment method: "Cash", "Card", "Contactless", "Mobile", etc.
        - For card payments, extract last 4 digits if visible (e.g., "1234")
        - Suggest category from: "Office Supplies", "Travel", "Meals", "Transportation",
          "Software", "Hardware", "Utilities", "Professional Services", "Other"
        - Confidence should reflect how complete and accurate the extraction is (0.0 to 1.0)

        Respond with a JSON object matching this schema:
        {
            "merchantName": "string or null",
            "merchantAddress": "string or null",
            "merchantVatNumber": "string or null",
            "receiptNumber": "string or null",
            "transactionDate": "YYYY-MM-DD or null",
            "transactionTime": "HH:mm or null",
            "items": [
                {
                    "description": "string",
                    "quantity": number or null,
                    "price": "string or null"
                }
            ],
            "currency": "EUR",
            "subtotal": "string or null",
            "vatAmount": "string or null",
            "totalAmount": "string or null",
            "paymentMethod": "Cash | Card | etc. or null",
            "cardLastFour": "1234 or null",
            "suggestedCategory": "string or null",
            "confidence": 0.0 to 1.0
        }
    """.trimIndent()

    /**
     * Extract receipt data from OCR text.
     */
    suspend fun extract(ocrText: String): ExtractedReceiptData {
        logger.debug("Extracting receipt data (${ocrText.length} chars)")

        val userPrompt = """
            Extract receipt data from this text:

            $ocrText
        """.trimIndent()

        return try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = model,
                strategy = singleRunStrategy(),
                toolRegistry = ToolRegistry.EMPTY,
                id = "receipt-extractor",
                systemPrompt = systemPrompt
            )

            val response: String = agent.run(userPrompt)
            parseExtractionResponse(response ?: "")
        } catch (e: Exception) {
            logger.error("Failed to extract receipt data", e)
            ExtractedReceiptData(confidence = 0.0)
        }
    }

    private fun parseExtractionResponse(response: String): ExtractedReceiptData {
        return try {
            val jsonString = extractJson(response)
            json.decodeFromString<ExtractedReceiptData>(jsonString)
        } catch (e: Exception) {
            logger.warn("Failed to parse extraction response: ${response.take(500)}", e)
            ExtractedReceiptData(confidence = 0.0)
        }
    }

    private fun extractJson(response: String): String {
        val cleaned = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')

        return if (startIndex >= 0 && endIndex > startIndex) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            cleaned
        }
    }
}
