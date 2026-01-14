package tech.dokus.features.ai.retry

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.features.ai.utils.normalizeJson
import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.AuditStatus
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.reflect.KClass

/**
 * Layer 4: Self-Correction Loop
 *
 * Unlike generic "retry" systems, this agent receives SPECIFIC feedback
 * from the Auditor about what went wrong and where to look.
 *
 * Key principle: Each retry prompt includes the exact error and a hint
 * about what to re-read (e.g., "the payment section", "the total line").
 *
 * The agent:
 * 1. Takes an extraction that failed validation
 * 2. Builds a feedback prompt with specific hints
 * 3. Re-runs extraction with the feedback context
 * 4. Re-validates the new extraction
 * 5. Repeats until success or max retries
 *
 * @param T The type of extracted data (e.g., ExtractedInvoiceData)
 */
class FeedbackDrivenRetryAgent<T : Any>(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val basePrompt: AgentPrompt.Extraction,
    private val promptId: String,
    private val serializer: KSerializer<T>,
    private val resultClass: KClass<T>,
    private val config: RetryConfig = RetryConfig.DEFAULT,
    private val validator: suspend (T) -> AuditReport
) {
    private val logger = loggerFor()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Attempt to correct an extraction that failed validation.
     *
     * @param images Original document images
     * @param initialExtraction The extraction that failed validation
     * @param initialAuditReport The audit report with failures
     * @return RetryResult indicating success or failure
     */
    suspend fun attemptCorrection(
        images: List<DocumentImage>,
        initialExtraction: T,
        initialAuditReport: AuditReport
    ): RetryResult<T> {
        // Check if retry is needed
        if (initialAuditReport.overallStatus == AuditStatus.PASSED) {
            logger.debug("Audit passed, no retry needed")
            return RetryResult.NoRetryNeeded
        }

        // Determine which failures to retry on
        val failuresToRetry = if (config.retryOnWarnings) {
            initialAuditReport.criticalFailures + initialAuditReport.warnings
        } else {
            initialAuditReport.criticalFailures
        }

        if (failuresToRetry.isEmpty()) {
            logger.debug("No critical failures, no retry needed")
            return RetryResult.NoRetryNeeded
        }

        logger.info(
            "Starting self-correction for ${failuresToRetry.size} failures " +
                "(max ${config.maxRetries} retries)"
        )

        var currentExtraction = initialExtraction
        var currentAuditReport = initialAuditReport
        var attempt = 0

        while (attempt < config.maxRetries) {
            attempt++
            logger.info("Retry attempt $attempt of ${config.maxRetries}")

            // Build feedback prompt with specific hints
            val feedbackPrompt = FeedbackPromptBuilder.buildFeedbackPrompt(
                auditReport = currentAuditReport,
                attempt = attempt,
                maxRetries = config.maxRetries
            )

            // Execute extraction with feedback
            val newExtraction = extractWithFeedback(images, feedbackPrompt)

            if (newExtraction == null) {
                logger.warn("Retry $attempt produced no result")
                continue
            }

            // Re-validate
            val newAuditReport = validator(newExtraction)
            logger.info(
                "Retry $attempt: ${newAuditReport.passedCount} passed, " +
                    "${newAuditReport.failedCount} failed"
            )

            // Check if we've succeeded
            if (newAuditReport.overallStatus == AuditStatus.PASSED ||
                (newAuditReport.criticalFailures.isEmpty() && !config.retryOnWarnings)
            ) {
                val correctedFields = findCorrectedFields(
                    initialAuditReport.criticalFailures + initialAuditReport.warnings,
                    newAuditReport.criticalFailures + newAuditReport.warnings
                )
                logger.info("Retry $attempt succeeded - corrected fields: $correctedFields")

                return RetryResult.CorrectedOnRetry(
                    data = newExtraction,
                    attempt = attempt,
                    correctedFields = correctedFields,
                    originalFailures = failuresToRetry
                )
            }

            // Update for next iteration
            currentExtraction = newExtraction
            currentAuditReport = newAuditReport
        }

        // Max retries exhausted
        logger.warn(
            "Self-correction failed after $attempt attempts. " +
                "Remaining failures: ${currentAuditReport.criticalFailures.size} critical, " +
                "${currentAuditReport.warnings.size} warnings"
        )

        return RetryResult.StillFailing(
            data = currentExtraction,
            attempts = attempt,
            remainingFailures = currentAuditReport.criticalFailures + currentAuditReport.warnings
        )
    }

    /**
     * Execute extraction with feedback context injected into the prompt.
     */
    private suspend fun extractWithFeedback(
        images: List<DocumentImage>,
        feedbackPrompt: String
    ): T? {
        return try {
            // Build system prompt with feedback injected
            val enhancedSystemPrompt = """
                ${basePrompt.systemPrompt}

                $feedbackPrompt
            """.trimIndent()

            val systemMessage = Message.System(
                parts = listOf(ContentPart.Text(enhancedSystemPrompt)),
                metaInfo = RequestMetaInfo(timestamp = Clock.System.now())
            )

            val userParts = buildList {
                add(
                    ContentPart.Text(
                        "Re-extract the data from this ${images.size}-page document, " +
                            "carefully addressing the issues mentioned above:"
                    )
                )
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

            val retryPrompt = Prompt(
                messages = listOf(systemMessage, userMessage),
                id = "$promptId-retry"
            )

            val response = executor.execute(retryPrompt, model, emptyList()).first()
            parseResponse(response.content)
        } catch (e: Exception) {
            logger.error("Failed to execute retry extraction", e)
            null
        }
    }

    private fun parseResponse(response: String): T? {
        return try {
            val normalized = normalizeJson(response)
            json.decodeFromString(serializer, normalized)
        } catch (e: Exception) {
            logger.warn("Failed to parse retry response: ${response.take(500)}", e)
            null
        }
    }

    /**
     * Find which fields were corrected between original and new audit.
     */
    private fun findCorrectedFields(
        originalFailures: List<AuditCheck>,
        newFailures: List<AuditCheck>
    ): List<String> {
        val originalFields = originalFailures.map { it.field }.toSet()
        val newFailedFields = newFailures.map { it.field }.toSet()
        return (originalFields - newFailedFields).toList()
    }

    companion object {
        /**
         * Create a retry agent for the specified type.
         */
        inline fun <reified T : Any> create(
            executor: PromptExecutor,
            model: LLModel,
            basePrompt: AgentPrompt.Extraction,
            promptId: String,
            serializer: KSerializer<T>,
            config: RetryConfig = RetryConfig.DEFAULT,
            noinline validator: suspend (T) -> AuditReport
        ): FeedbackDrivenRetryAgent<T> = FeedbackDrivenRetryAgent(
            executor = executor,
            model = model,
            basePrompt = basePrompt,
            promptId = promptId,
            serializer = serializer,
            resultClass = T::class,
            config = config,
            validator = validator
        )
    }
}
