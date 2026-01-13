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
import tech.dokus.features.ai.prompts.AgentPrompt
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
    private val model: LLModel,
    private val prompt: AgentPrompt.Extraction
) {
    private val logger = loggerFor()

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
                parts = listOf(ContentPart.Text(prompt.systemPrompt)),
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
