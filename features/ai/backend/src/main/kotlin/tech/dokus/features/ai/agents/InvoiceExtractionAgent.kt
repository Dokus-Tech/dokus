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
import tech.dokus.features.ai.prompts.AgentPrompt
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
    private val model: LLModel,
    private val prompt: AgentPrompt.Extraction
) {
    private val logger = loggerFor()

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
                parts = listOf(ContentPart.Text(prompt.systemPrompt)),
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
