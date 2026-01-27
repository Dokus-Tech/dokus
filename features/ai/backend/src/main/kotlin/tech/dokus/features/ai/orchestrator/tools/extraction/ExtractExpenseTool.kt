package tech.dokus.features.ai.orchestrator.tools.extraction

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.orchestrator.ToolTraceSink
import tech.dokus.features.ai.orchestrator.tools.DocumentImageResolver
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.prompts.ExtractionPrompt
import tech.dokus.features.ai.services.DocumentImageCache
import tech.dokus.features.ai.services.DocumentImageService
import kotlin.time.TimeSource

/**
 * Vision tool for extracting expense report data from document images.
 *
 * Uses vision model to extract structured expense data including:
 * - Employee information
 * - Expense category
 * - Date and description
 * - Amount and currency
 * - Supporting receipts/invoices
 */
class ExtractExpenseTool(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: ExtractionPrompt,
    private val tenantContext: AgentPrompt.TenantContext,
    private val imageCache: DocumentImageCache,
    private val traceSink: ToolTraceSink
) : Tool<ExtractExpenseTool.Args, String>(
    argsSerializer = Args.serializer(),
    resultSerializer = String.serializer(),
    name = "extract_expense",
    description = """
        Extracts structured data from an EXPENSE document using vision AI.

        An EXPENSE is a reimbursement request or expense report.
        Use this tool after classifying the document as EXPENSE.

        Expenses typically include:
        - Employee/claimant info
        - Expense category (travel, meals, equipment)
        - Date and description
        - Amount to be reimbursed
        - Attached receipts or invoices

        Returns structured JSON that can be stored in the database.
    """.trimIndent(),
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Image IDs (from get_document_images) or base64 PNGs, separated by newlines.")
        val images: String,

        @property:LLMDescription(
            "Optional: JSON example of a previous similar expense. " +
                    "This improves accuracy for similar expense types."
        )
        val example: String? = null
    )

    private val jsonFormat = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @LLMDescription("")
    override suspend fun execute(args: Args): String {
        val documentImages = try {
            DocumentImageResolver(imageCache).resolve(args.images)
        } catch (e: Exception) {
            traceSink.record(
                action = "extract_expense",
                tool = name,
                durationMs = 0,
                input = null,
                output = null,
                notes = "error=${e.message}"
            )
        }

        // Create extraction agent
        val agent = ExtractionAgent<ExtractedExpenseData>(
            executor = executor,
            model = model,
            prompt = prompt,
            userPromptPrefix = buildUserPromptPrefix(args.example),
            promptId = "expense-extractor",
            emptyResult = { ExtractedExpenseData(confidence = 0.0) }
        )

        // Run extraction
        val start = TimeSource.Monotonic.markNow()
        val result = agent.extract(documentImages as List<DocumentImageService.DocumentImage>, tenantContext)
        val outputJson = jsonFormat.decodeFromString<JsonElement>(jsonFormat.encodeToString(result))
        traceSink.record(
            action = "extract_expense",
            tool = name,
            durationMs = start.elapsedNow().inWholeMilliseconds,
            input = null,
            output = outputJson,
            notes = "confidence=${result.confidence}"
        )

        return buildString {
            appendLine("EXTRACTION RESULT:")
            appendLine("Category: ${result.category ?: "Unknown"}")
            appendLine("Date: ${result.date ?: "Unknown"}")
            appendLine("Amount: ${result.totalAmount ?: "Unknown"} ${result.currency ?: ""}")
            appendLine("Confidence: ${String.format("%.0f%%", result.confidence * 100)}")
            appendLine()
            appendLine("JSON:")
            appendLine(jsonFormat.encodeToString(result))
        }
    }

    private fun buildUserPromptPrefix(example: String?): String {
        return if (example != null) {
            """
            Extract expense data from this document.

            REFERENCE EXAMPLE (use this format):
            $example

            Now extract from this
            """.trimIndent()
        } else {
            "Extract expense data from this"
        }
    }
}
