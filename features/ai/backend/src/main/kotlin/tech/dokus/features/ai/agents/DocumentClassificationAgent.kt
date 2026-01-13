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
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.DocumentClassification
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.features.ai.utils.normalizeJson
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
    private val model: LLModel,
    private val prompt: AgentPrompt.DocumentClassification
) {
    private val logger = loggerFor()

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
                parts = listOf(ContentPart.Text(prompt.systemPrompt)),
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
        return parseSafe<DocumentClassification>(normalizeJson(response)).getOrElse {
            logger.warn("Failed to parse classification response: $response", it)
            // Fallback: try to extract document type from text
            fallbackParse(response)
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
