package ai.dokus.ai.agents

import ai.dokus.ai.models.ExtractedInvoiceData
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Agent responsible for extracting data from invoice documents.
 * Step 2a in the two-step document processing pipeline (for invoices and bills).
 */
class InvoiceExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = LoggerFactory.getLogger(InvoiceExtractionAgent::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val systemPrompt = """
        You are an invoice data extraction specialist.
        Extract structured data from business invoices.
        Always respond with valid JSON matching the requested schema.

        Extract these fields:
        - Vendor: name, VAT number (BE format or international), address
        - Invoice: number, issue date, due date, payment terms
        - Line items: description, quantity, unit price, VAT rate, total
        - Totals: subtotal, VAT breakdown by rate, total amount, currency
        - Payment: bank account (IBAN/BIC), payment reference

        Guidelines:
        - Use null for fields that cannot be found or are unclear
        - Dates should be in ISO format (YYYY-MM-DD)
        - Currency should be 3-letter ISO code (EUR, USD, GBP)
        - VAT rates should include % symbol (e.g., "21%")
        - Amounts should be strings to preserve precision (e.g., "1234.56")
        - For Belgian VAT numbers, format as "BE0123456789"
        - Confidence should reflect how complete and accurate the extraction is (0.0 to 1.0)

        Respond with a JSON object matching this schema:
        {
            "vendorName": "string or null",
            "vendorVatNumber": "string or null",
            "vendorAddress": "string or null",
            "invoiceNumber": "string or null",
            "issueDate": "YYYY-MM-DD or null",
            "dueDate": "YYYY-MM-DD or null",
            "paymentTerms": "string or null",
            "lineItems": [
                {
                    "description": "string",
                    "quantity": number or null,
                    "unitPrice": "string or null",
                    "vatRate": "string or null",
                    "total": "string or null"
                }
            ],
            "currency": "EUR",
            "subtotal": "string or null",
            "vatBreakdown": [
                {
                    "rate": "21%",
                    "base": "string or null",
                    "amount": "string or null"
                }
            ],
            "totalVatAmount": "string or null",
            "totalAmount": "string or null",
            "iban": "string or null",
            "bic": "string or null",
            "paymentReference": "string or null",
            "confidence": 0.0 to 1.0
        }
    """.trimIndent()

    /**
     * Extract invoice data from OCR text.
     */
    suspend fun extract(ocrText: String): ExtractedInvoiceData {
        logger.debug("Extracting invoice data (${ocrText.length} chars)")

        val userPrompt = """
            Extract invoice data from this text:

            $ocrText
        """.trimIndent()

        return try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = model,
                strategy = singleRunStrategy(),
                toolRegistry = ToolRegistry.EMPTY,
                id = "invoice-extractor",
                systemPrompt = systemPrompt
            )

            val response: String = agent.run(userPrompt)
            parseExtractionResponse(response ?: "")
        } catch (e: Exception) {
            logger.error("Failed to extract invoice data", e)
            ExtractedInvoiceData(confidence = 0.0)
        }
    }

    private fun parseExtractionResponse(response: String): ExtractedInvoiceData {
        return try {
            val jsonString = extractJson(response)
            json.decodeFromString<ExtractedInvoiceData>(jsonString)
        } catch (e: Exception) {
            logger.warn("Failed to parse extraction response: ${response.take(500)}", e)
            ExtractedInvoiceData(confidence = 0.0)
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
