package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import java.util.Base64

/**
 * Vision tool for extracting receipt data from document images.
 *
 * Uses vision model to extract structured receipt data including:
 * - Store information (name, address, VAT)
 * - Receipt details (number, date, time)
 * - Line items (description, quantity, price)
 * - Totals (subtotal, VAT, total)
 * - Payment method
 */
class ExtractReceiptTool(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: AgentPrompt.Extraction
) : SimpleTool<ExtractReceiptTool.Args>(
    argsSerializer = Args.serializer(),
    name = "extract_receipt",
    description = """
        Extracts structured data from a RECEIPT document using vision AI.

        A RECEIPT is a POS/cash register receipt from a purchase.
        Use this tool after classifying the document as RECEIPT.

        Receipts typically have:
        - Merchant/store name and address
        - Date and time of purchase
        - List of items with prices
        - Payment method (cash, card)
        - Often lower resolution or thermal paper quality

        Returns structured JSON that can be stored in the database.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "Base64-encoded PNG images of the receipt, separated by newlines."
        )
        val images: String,

        @property:LLMDescription(
            "Optional: JSON example of a previous extraction from the same merchant. " +
                "This improves accuracy for repeat merchants."
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
        val agent = ExtractionAgent<ExtractedReceiptData>(
            executor = executor,
            model = model,
            prompt = prompt,
            userPromptPrefix = buildUserPromptPrefix(args.example),
            promptId = "receipt-extractor",
            emptyResult = { ExtractedReceiptData(confidence = 0.0) }
        )

        // Run extraction
        val result = agent.extract(documentImages)

        return buildString {
            appendLine("EXTRACTION RESULT:")
            appendLine("Merchant: ${result.merchantName ?: "Unknown"}")
            appendLine("Date: ${result.transactionDate ?: "Unknown"}")
            appendLine("Total: ${result.totalAmount ?: "Unknown"} ${result.currency ?: ""}")
            appendLine("Confidence: ${String.format("%.0f%%", (result.confidence ?: 0.0) * 100)}")
            appendLine()
            appendLine("JSON:")
            appendLine(jsonFormat.encodeToString(result))
        }
    }

    private fun buildUserPromptPrefix(example: String?): String {
        return if (example != null) {
            """
            Extract receipt data from this document.

            REFERENCE EXAMPLE from same merchant (use this format and look for similar fields):
            $example

            Now extract from this
            """.trimIndent()
        } else {
            "Extract receipt data from this"
        }
    }
}
