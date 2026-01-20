package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import java.util.Base64

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
    private val prompt: AgentPrompt.Extraction
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
            "Base64-encoded PNG images of the expense document, separated by newlines."
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
        // Parse base64 images
        val imageLines = args.images.trim().lines().filter { it.isNotBlank() }
        if (imageLines.isEmpty()) {
            return "ERROR: No images provided for extraction"
        }

        val documentImages = try {
            imageLines.mapIndexed { index, base64 ->
                val bytes = Base64.getDecoder().decode(base64.trim())
                DocumentImage(pageNumber = index + 1, imageBytes = bytes)
            }
        } catch (e: Exception) {
            return "ERROR: Invalid base64 image data: ${e.message}"
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
