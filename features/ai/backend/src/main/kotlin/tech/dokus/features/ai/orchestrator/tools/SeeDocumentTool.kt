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
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import java.util.Base64

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
    private val tenantContext: AgentPrompt.TenantContext
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
            "Base64-encoded PNG images of document pages, separated by newlines. " +
                "Include at least the first page for classification."
        )
        val images: String
    )

    private val jsonFormat = Json { prettyPrint = true }

    override suspend fun execute(args: Args): String {
        // Parse base64 images
        val imageLines = args.images.trim().lines().filter { it.isNotBlank() }
        if (imageLines.isEmpty()) {
            return "ERROR: No images provided for classification"
        }

        val documentImages = try {
            imageLines.mapIndexed { index, base64 ->
                val bytes = Base64.getDecoder().decode(base64.trim())
                DocumentImage(pageNumber = index + 1, imageBytes = bytes)
            }
        } catch (e: Exception) {
            return "ERROR: Invalid base64 image data: ${e.message}"
        }

        // Run classification
        val agent = DocumentClassificationAgent(executor, model, prompt)
        val result = agent.classify(documentImages, tenantContext)

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
