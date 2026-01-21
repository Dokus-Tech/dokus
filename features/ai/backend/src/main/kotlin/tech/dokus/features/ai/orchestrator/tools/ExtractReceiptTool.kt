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
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.prompts.AgentPrompt

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
    private val prompt: AgentPrompt.Extraction,
    private val imageCache: DocumentImageCache,
    private val traceSink: tech.dokus.features.ai.orchestrator.ToolTraceSink? = null
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
            "Image IDs (from get_document_images) or base64 PNGs, separated by newlines."
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
        val documentImages = try {
            DocumentImageResolver(imageCache).resolve(args.images)
        } catch (e: Exception) {
            traceSink?.record(
                action = "extract_receipt",
                tool = name,
                durationMs = 0,
                notes = "error=${e.message}"
            )
            return "ERROR: ${e.message}"
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
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val result = agent.extract(documentImages)
        traceSink?.record(
            action = "extract_receipt",
            tool = name,
            durationMs = start.elapsedNow().inWholeMilliseconds,
            notes = "confidence=${result.confidence}, items=${result.items.size}"
        )

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
