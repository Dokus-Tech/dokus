package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.agents.DocumentClassificationAgent
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageCache

/**
 * Vision tool for document classification (triage).
 *
 * Uses vision model to analyze document images and classify the document type.
 * This is the first step in document processing - determining what kind of document it is.
 */
class SeeDocumentTool(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: AgentPrompt.DocumentClassification,
    private val tenantContext: AgentPrompt.TenantContext,
    private val imageCache: DocumentImageCache,
    private val traceSink: tech.dokus.features.ai.orchestrator.ToolTraceSink? = null
) : SimpleTool<SeeDocumentTool.Args>(
    argsSerializer = Args.serializer(),
    name = "see_document",
    description = """
        Classifies a document by analyzing its images using vision AI.

        Use this tool first to determine the document type before extraction.
        Returns classification with confidence score and reasoning.

        Document types:
        - INVOICE: Sales invoice issued BY the tenant
        - BILL: Purchase invoice received BY the tenant (from supplier)
        - RECEIPT: POS/cash register receipt
        - EXPENSE: Expense report/reimbursement
        - CREDIT_NOTE: Credit note or refund
        - PRO_FORMA: Pro-forma invoice (quote-like)
        - UNKNOWN: Cannot determine type
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "Image IDs (from get_document_images) or base64 PNGs, separated by newlines. " +
                "Include at least the first page for classification."
        )
        val images: String
    )

    private val jsonFormat = Json { prettyPrint = true }

    override suspend fun execute(args: Args): String {
        val documentImages = try {
            DocumentImageResolver(imageCache).resolve(args.images)
        } catch (e: Exception) {
            traceSink?.record(
                action = "classify_document",
                tool = name,
                durationMs = 0,
                input = null,
                output = null,
                notes = "error=${e.message}"
            )
            return "ERROR: ${e.message}"
        }

        // Run classification
        val agent = DocumentClassificationAgent(executor, model, prompt)
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val result = agent.classify(documentImages, tenantContext)
        traceSink?.record(
            action = "classify_document",
            tool = name,
            durationMs = start.elapsedNow().inWholeMilliseconds,
            input = null,
            output = null,
            notes = "type=${result.documentType}, confidence=${result.confidence}"
        )

        return buildString {
            appendLine("CLASSIFICATION RESULT:")
            appendLine("Type: ${result.documentType}")
            appendLine("Confidence: ${String.format("%.0f%%", result.confidence * 100)}")
            appendLine("Reasoning: ${result.reasoning}")
            appendLine()
            appendLine("JSON:")
            appendLine(jsonFormat.encodeToString(result))
        }
    }
}
