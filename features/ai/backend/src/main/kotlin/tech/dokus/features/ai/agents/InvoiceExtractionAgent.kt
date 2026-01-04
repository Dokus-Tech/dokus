package tech.dokus.features.ai.agents

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.datetime.Clock
import tech.dokus.domain.utils.parseSafe
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.features.ai.utils.normalizeJson
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Agent responsible for extracting data from invoice documents using vision models.
 * Step 2a in the two-step document processing pipeline (for invoices and bills).
 *
 * Uses vision-capable LLMs (qwen3-vl) to analyze document images directly,
 * eliminating the need for OCR preprocessing.
 *
 * This agent extracts structured invoice data AND provenance information,
 * linking each extracted field back to its source page in the document.
 */
class InvoiceExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = loggerFor()

    private val systemPrompt = """
        You are an invoice data extraction specialist with vision capabilities.
        Analyze the invoice image(s) and extract structured data.
        Always respond with ONLY valid JSON (no markdown, no explanation).

        Extract these fields:
        - Vendor: name, VAT number (BE format or international), address
        - Invoice: number, issue date, due date, payment terms
        - Line items: description, quantity, unit price, VAT rate, total
        - Totals: subtotal, VAT breakdown by rate, total amount, currency
        - Payment: bank account (IBAN/BIC), payment reference

        For each field, include provenance:
        - pageNumber: Which page (1-indexed) the value appears on
        - sourceText: The exact text you read from the document
        - fieldConfidence: Confidence in this field (0.0 to 1.0)

        Guidelines:
        - Use null for fields not visible or unclear
        - Dates: ISO format (YYYY-MM-DD)
        - Currency: 3-letter ISO code (EUR, USD, GBP)
        - VAT rates: Include % symbol (e.g., "21%")
        - Amounts: Strings to preserve precision (e.g., "1234.56")
        - Belgian VAT: Format as "BE0123456789"

        ALSO provide extractedText: A clean transcription of all visible text for indexing.

        JSON Schema:
        {
            "vendorName": "string or null",
            "vendorVatNumber": "string or null",
            "vendorAddress": "string or null",
            "invoiceNumber": "string or null",
            "issueDate": "YYYY-MM-DD or null",
            "dueDate": "YYYY-MM-DD or null",
            "paymentTerms": "string or null",
            "lineItems": [{"description": "...", "quantity": 1, "unitPrice": "...", "vatRate": "21%", "total": "..."}],
            "currency": "EUR",
            "subtotal": "string or null",
            "vatBreakdown": [{"rate": "21%", "base": "...", "amount": "..."}],
            "totalVatAmount": "string or null",
            "totalAmount": "string or null",
            "iban": "string or null",
            "bic": "string or null",
            "paymentReference": "string or null",
            "confidence": 0.85,
            "extractedText": "Full text transcription of the document for indexing",
            "provenance": {
                "vendorName": {"pageNumber": 1, "sourceText": "...", "fieldConfidence": 0.9},
                "invoiceNumber": {"pageNumber": 1, "sourceText": "...", "fieldConfidence": 0.95}
            }
        }
    """.trimIndent()

    /**
     * Extract invoice data from document images using vision model.
     *
     * @param images List of document page images
     * @return ExtractedInvoiceData with values, provenance, and extracted text for RAG
     */
    suspend fun extract(images: List<DocumentImage>): ExtractedInvoiceData {
        logger.debug("Extracting invoice data with vision (${images.size} pages)")

        if (images.isEmpty()) {
            return ExtractedInvoiceData(confidence = 0.0)
        }

        return try {
            // Build vision prompt with image attachments (direct construction for compatibility)
            val systemMessage = Message.System(
                parts = listOf(ContentPart.Text(systemPrompt)),
                metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
            )

            val userParts = buildList {
                add(ContentPart.Text("Extract invoice data from this ${images.size}-page document:"))
                images.forEach { docImage ->
                    add(
                        ContentPart.Image(
                            content = AttachmentContent.Binary.Bytes(docImage.imageBytes),
                            format = "png",
                            mimeType = docImage.mimeType
                        )
                    )
                }
            }
            val userMessage = Message.User(
                parts = userParts,
                metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
            )

            val visionPrompt = Prompt(
                messages = listOf(systemMessage, userMessage),
                id = "invoice-extractor"
            )

            // Execute prompt and get response
            val response = executor.execute(visionPrompt, model, emptyList()).first()
            parseExtractionResponse(response.content)
        } catch (e: Exception) {
            logger.error("Failed to extract invoice data", e)
            ExtractedInvoiceData(confidence = 0.0)
        }
    }

    private fun parseExtractionResponse(response: String): ExtractedInvoiceData {
        return parseSafe<ExtractedInvoiceData>(normalizeJson(response)).getOrElse {
            logger.warn("Failed to parse extraction response: ${response.take(500)}", it)
            ExtractedInvoiceData(confidence = 0.0)
        }
    }
}
