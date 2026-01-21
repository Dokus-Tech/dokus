package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import java.util.Base64

/**
 * Vision tool for extracting bill (purchase invoice) data from document images.
 *
 * Uses vision model to extract structured bill data including:
 * - Supplier information (name, VAT, address)
 * - Bill details (number, dates, terms)
 * - Line items (description, quantity, price, VAT)
 * - Totals (subtotal, VAT breakdown, total)
 * - Payment info (IBAN, BIC, reference/OGM)
 */
class ExtractBillTool(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: AgentPrompt.Extraction
) : SimpleTool<ExtractBillTool.Args>(
    argsSerializer = Args.serializer(),
    name = "extract_bill",
    description = """
        Extracts structured data from a BILL (purchase invoice) document using vision AI.

        A BILL is an invoice received BY the tenant from a supplier (purchase).
        Use this tool after classifying the document as BILL.

        Extracts: supplier info, bill number, dates, line items, totals, payment details.
        Returns structured JSON that can be stored in the database.

        Belgian bills may have OGM (gestructureerde mededeling) payment reference.
        Optional: Provide an example extraction from a similar supplier to improve accuracy.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "Base64-encoded PNG images of all document pages, separated by newlines."
        )
        val images: String,

        @property:LLMDescription(
            "Optional: JSON example of a previous extraction from the same supplier. " +
                "This improves accuracy for repeat suppliers."
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
        val agent = ExtractionAgent<ExtractedBillData>(
            executor = executor,
            model = model,
            prompt = prompt,
            userPromptPrefix = buildUserPromptPrefix(args.example),
            promptId = "bill-extractor",
            emptyResult = { ExtractedBillData(confidence = 0.0) }
        )

        // Run extraction
        val result = agent.extract(documentImages)

        return buildString {
            appendLine("EXTRACTION RESULT:")
            appendLine("Supplier: ${result.supplierName ?: "Unknown"}")
            appendLine("Bill #: ${result.invoiceNumber ?: "Unknown"}")
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
            Extract bill data from this document.

            REFERENCE EXAMPLE from same supplier (use this format and look for similar fields):
            $example

            Now extract from this
            """.trimIndent()
        } else {
            "Extract bill data from this"
        }
    }
}
