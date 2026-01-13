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
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Agent responsible for extracting data from receipt documents using vision models.
 * Step 2c in the two-step document processing pipeline (for receipts).
 *
 * Uses vision-capable LLMs (qwen3-vl) to analyze document images directly,
 * eliminating the need for OCR preprocessing.
 */
class ReceiptExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: AgentPrompt.Extraction
) {
    private val logger = loggerFor()

    /**
     * Extract receipt data from document images using vision model.
     *
     * @param images List of document page images
     * @return ExtractedReceiptData with values, provenance, and extracted text for RAG
     */
    suspend fun extract(images: List<DocumentImage>): ExtractedReceiptData {
        logger.debug("Extracting receipt data with vision (${images.size} pages)")

        if (images.isEmpty()) {
            return ExtractedReceiptData(confidence = 0.0)
        }

        return try {
            // Build vision prompt with image attachments (direct construction for compatibility)
            val systemMessage = Message.System(
                parts = listOf(ContentPart.Text(prompt.systemPrompt)),
                metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
            )

            val userParts = buildList {
                add(ContentPart.Text("Extract receipt data from this ${images.size}-page document:"))
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
                id = "receipt-extractor"
            )

            // Execute prompt and get response
            val response = executor.execute(visionPrompt, model, emptyList()).first()
            parseExtractionResponse(response.content)
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

        return if (startIndex in 0..<endIndex) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            cleaned
        }
    }
}
