package tech.dokus.features.ai.agents

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.DocumentClassification
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Agent responsible for classifying document types using vision models.
 * Step 1 in the two-step document processing pipeline.
 *
 * Uses vision-capable LLMs (qwen3-vl) to analyze document images directly,
 * eliminating the need for OCR preprocessing.
 */
class DocumentClassificationAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = loggerFor()
    private val systemPrompt = """
        You are a document classification specialist with vision capabilities.
        Analyze the document image(s) and determine the document type.

        Document types:
        - INVOICE: A formal request for payment from a supplier/vendor with invoice number, line items, VAT
        - RECEIPT: A proof of payment/purchase from a store, usually simpler format without detailed line items
        - BILL: A utility or service bill (electricity, phone, internet, subscription services)
        - UNKNOWN: Cannot determine the type

        Key visual indicators:
        - INVOICE: Formal letterhead, invoice number, payment terms, due date, detailed line items, often B2B
        - RECEIPT: Point-of-sale format, receipt number, store name/logo, immediate payment confirmation
        - BILL: Utility company branding, account numbers, service periods, recurring charges

        Respond with ONLY a JSON object (no markdown, no explanation):
        {"documentType": "INVOICE", "confidence": 0.85, "reasoning": "Brief explanation"}
    """.trimIndent()

    /**
     * Classify the document type from document images using vision model.
     *
     * @param images List of document page images (usually just the first page for classification)
     * @return DocumentClassification with type, confidence, and reasoning
     */
    suspend fun classify(images: List<DocumentImage>): DocumentClassification {
        logger.debug("Classifying document (${images.size} pages)")

        if (images.isEmpty()) {
            return DocumentClassification(
                documentType = ClassifiedDocumentType.UNKNOWN,
                confidence = 0.0,
                reasoning = "No images provided for classification"
            )
        }

        return try {
            // Build vision prompt with image attachments (direct construction for compatibility)
            val systemMessage = Message.System(
                parts = listOf(ContentPart.Text(systemPrompt)),
                metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
            )

            val userParts = buildList {
                add(ContentPart.Text("Classify this ${images.size}-page document:"))
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
                id = "document-classifier"
            )

            // Execute prompt and get response
            val response = executor.execute(visionPrompt, model, emptyList()).first()
            parseClassificationResponse(response.content)
        } catch (e: Exception) {
            logger.error("Failed to classify document", e)
            DocumentClassification(
                documentType = ClassifiedDocumentType.UNKNOWN,
                confidence = 0.0,
                reasoning = "Classification failed: ${e.message}"
            )
        }
    }

    private fun parseClassificationResponse(response: String): DocumentClassification {
        return try {
            // Extract JSON from response (may be wrapped in markdown code blocks)
            val jsonString = extractJson(response)
            json.decodeFromString<DocumentClassification>(jsonString)
        } catch (e: Exception) {
            logger.warn("Failed to parse classification response: $response", e)
            // Fallback: try to extract document type from text
            fallbackParse(response)
        }
    }

    private fun extractJson(response: String): String {
        // Remove markdown code blocks if present
        val cleaned = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        // Find JSON object in the response
        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')

        return if (startIndex >= 0 && endIndex > startIndex) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            cleaned
        }
    }

    private fun fallbackParse(response: String): DocumentClassification {
        val upperResponse = response.uppercase()
        val documentType = when {
            "INVOICE" in upperResponse -> ClassifiedDocumentType.INVOICE
            "RECEIPT" in upperResponse -> ClassifiedDocumentType.RECEIPT
            "BILL" in upperResponse -> ClassifiedDocumentType.BILL
            else -> ClassifiedDocumentType.UNKNOWN
        }

        return DocumentClassification(
            documentType = documentType,
            confidence = 0.5, // Lower confidence for fallback
            reasoning = "Fallback classification based on keyword detection"
        )
    }
}
