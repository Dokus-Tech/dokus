package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.services.DocumentImageCache
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.prompts.AgentPrompt

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
    private val prompt: AgentPrompt.Extraction,
    private val imageCache: DocumentImageCache
) : SimpleTool<ExtractExpenseTool.Args>(
    argsSerializer = Args.serializer(),
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
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "Image IDs (from get_document_images) or base64 PNGs, separated by newlines."
        )
        val images: String,

        @property:LLMDescription(
            "Optional: JSON example of a previous similar expense. " +
                "This improves accuracy for similar expense types."
        )
        val example: String? = null
    )

    private val jsonFormat = Json { prettyPrint = true; ignoreUnknownKeys = true }

    override suspend fun execute(args: Args): String {
        val documentImages = try {
            DocumentImageResolver(imageCache).resolve(args.images)
        } catch (e: Exception) {
            return "ERROR: ${e.message}"
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
        val result = agent.extract(documentImages)

        return buildString {
            appendLine("EXTRACTION RESULT:")
            appendLine("Category: ${result.category ?: "Unknown"}")
            appendLine("Date: ${result.date ?: "Unknown"}")
            appendLine("Amount: ${result.totalAmount ?: "Unknown"} ${result.currency ?: ""}")
            appendLine("Confidence: ${String.format("%.0f%%", (result.confidence ?: 0.0) * 100)}")
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
