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
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.features.ai.utils.normalizeJson
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Agent responsible for extracting data from bill (supplier invoice) documents using vision models.
 * Step 2b in the two-step document processing pipeline (for bills).
 *
 * Bills are incoming invoices from suppliers, as opposed to outgoing invoices to clients.
 *
 * Uses vision-capable LLMs (qwen3-vl) to analyze document images directly,
 * eliminating the need for OCR preprocessing.
 */
class BillExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = loggerFor()

    private val systemPrompt = """
        You are a bill/supplier invoice extraction specialist with vision capabilities.
        Analyze the bill image(s) and extract structured data.
        Always respond with ONLY valid JSON (no markdown, no explanation).

        A "bill" is an invoice you RECEIVE from a supplier (you owe them money).

        Extract these fields:
        - Supplier: name, VAT number, address
        - Bill: invoice number, issue date, due date
        - Amount: total amount, VAT amount, VAT rate, currency
        - Line items (if visible): description, quantity, unit price, VAT rate, total
        - Category: suggested expense category
        - Payment: bank account (IBAN), payment terms

        For each field, include provenance:
        - pageNumber: Which page (1-indexed) the value appears on
        - sourceText: The exact text you read from the document
        - fieldConfidence: Confidence in this field (0.0 to 1.0)

        Expense categories for Belgian freelancers:
        OFFICE_SUPPLIES, HARDWARE, SOFTWARE, TRAVEL, TRANSPORTATION,
        MEALS, PROFESSIONAL_SERVICES, UTILITIES, TRAINING, MARKETING,
        INSURANCE, RENT, OTHER

        ALSO provide extractedText: A clean transcription of all visible text for indexing.

        JSON Schema:
        {
            "supplierName": "string or null",
            "supplierVatNumber": "string or null",
            "supplierAddress": "string or null",
            "invoiceNumber": "string or null",
            "issueDate": "YYYY-MM-DD or null",
            "dueDate": "YYYY-MM-DD or null",
            "currency": "EUR",
            "amount": "string or null",
            "vatAmount": "string or null",
            "vatRate": "21%",
            "totalAmount": "string or null",
            "lineItems": [{"description": "...", "quantity": 1, "unitPrice": "...", "vatRate": "21%", "total": "..."}],
            "category": "EXPENSE_CATEGORY or null",
            "description": "brief description or null",
            "paymentTerms": "string or null",
            "bankAccount": "IBAN or null",
            "notes": "string or null",
            "confidence": 0.85,
            "extractedText": "Full text transcription for indexing",
            "provenance": {
                "supplierName": {"pageNumber": 1, "sourceText": "...", "fieldConfidence": 0.9},
                "invoiceNumber": {"pageNumber": 1, "sourceText": "...", "fieldConfidence": 0.95}
            }
        }
    """.trimIndent()

    /**
     * Extract bill data from document images using vision model.
     *
     * @param images List of document page images
     * @return ExtractedBillData with values, provenance, and extracted text for RAG
     */
    suspend fun extract(images: List<DocumentImage>): ExtractedBillData {
        logger.debug("Extracting bill data with vision (${images.size} pages)")

        if (images.isEmpty()) {
            return ExtractedBillData(confidence = 0.0)
        }

        return try {
            // Build vision prompt with image attachments (direct construction for compatibility)
            val systemMessage = Message.System(
                parts = listOf(ContentPart.Text(systemPrompt)),
                metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
            )

            val userParts = buildList {
                add(ContentPart.Text("Extract bill/supplier invoice data from this ${images.size}-page document:"))
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
                id = "bill-extractor"
            )

            // Execute prompt and get response
            val response = executor.execute(visionPrompt, model, emptyList()).first()
            parseExtractionResponse(response.content)
        } catch (e: Exception) {
            logger.error("Failed to extract bill data", e)
            ExtractedBillData(confidence = 0.0)
        }
    }

    private fun parseExtractionResponse(response: String): ExtractedBillData {
        return parseSafe<ExtractedBillData>(normalizeJson(response)).getOrElse {
            logger.warn("Failed to parse extraction response: ${response.take(500)}", it)
            ExtractedBillData(confidence = 0.0)
        }
    }
}
