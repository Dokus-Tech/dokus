package tech.dokus.features.ai.agents

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.prompts.ExtractionPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.features.ai.utils.normalizeJson
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.reflect.KClass

/**
 * Generic interface for document extraction agents.
 *
 * All extraction agents (Invoice, Bill, Receipt) use the same vision-based
 * extraction pattern. This interface unifies them with a type-safe API.
 */
interface ExtractionAgent<T : Any> {
    /**
     * Extract structured data from document images using vision model.
     *
     * @param images List of document page images
     * @return Extracted data of type T
     */
    suspend fun extract(images: List<DocumentImage>): T

    companion object {
        /**
         * Create an extraction agent for the specified type.
         *
         * Usage:
         * ```kotlin
         * val invoiceAgent = ExtractionAgent<ExtractedInvoiceData>(
         *     executor = executor,
         *     model = model,
         *     prompt = AgentPrompt.Extraction.Invoice,
         *     userPromptPrefix = "Extract invoice data from this",
         *     promptId = "invoice-extractor",
         *     emptyResult = { ExtractedInvoiceData(confidence = 0.0) }
         * )
         * ```
         */
        inline operator fun <reified T : Any> invoke(
            executor: PromptExecutor,
            model: LLModel,
            prompt: ExtractionPrompt,
            userPromptPrefix: String,
            promptId: String,
            noinline emptyResult: () -> T
        ): ExtractionAgent<T> = ExtractionAgentImpl(
            executor = executor,
            model = model,
            prompt = prompt,
            userPromptPrefix = userPromptPrefix,
            promptId = promptId,
            emptyResult = emptyResult,
            resultClass = T::class
        )
    }
}

/**
 * Implementation of the generic extraction agent.
 *
 * Uses vision-capable LLMs (qwen3-vl) to analyze document images directly,
 * eliminating the need for OCR preprocessing.
 */
@PublishedApi
internal class ExtractionAgentImpl<T : Any>(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: ExtractionPrompt,
    private val userPromptPrefix: String,
    private val promptId: String,
    private val emptyResult: () -> T,
    private val resultClass: KClass<T>
) : ExtractionAgent<T> {
    private val logger = loggerFor()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun extract(images: List<DocumentImage>): T {
        logger.debug("Extracting ${resultClass.simpleName} with vision (${images.size} pages)")

        if (images.isEmpty()) {
            return emptyResult()
        }

        return try {
            // Build vision prompt with image attachments
            val systemMessage = Message.System(
                parts = listOf(ContentPart.Text(prompt.systemPrompt.value)),
                metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
            )

            val userParts = buildList {
                add(ContentPart.Text("$userPromptPrefix ${images.size}-page document:"))
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
                id = promptId
            )

            // Execute prompt and get response
            val response = executor.execute(visionPrompt, model, emptyList()).first()
            parseResponse(response.content)
        } catch (e: Exception) {
            logger.error("Failed to extract ${resultClass.simpleName}", e)
            emptyResult()
        }
    }

    private fun parseResponse(response: String): T {
        return try {
            val normalized = normalizeJson(response)
            json.decodeFromString(serializer(), normalized)
        } catch (e: Exception) {
            logger.warn("Failed to parse extraction response: ${response.take(500)}", e)
            emptyResult()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun serializer(): kotlinx.serialization.KSerializer<T> {
        return kotlinx.serialization.serializer(resultClass.java) as kotlinx.serialization.KSerializer<T>
    }
}
