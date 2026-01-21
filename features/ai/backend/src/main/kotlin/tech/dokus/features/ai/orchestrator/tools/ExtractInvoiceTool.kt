package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import java.util.Base64

/**
 * Vision tool for extracting invoice data from document images.
 *
 * Uses vision model to extract structured invoice data including:
 * - Vendor information (name, VAT, address)
 * - Invoice details (number, dates, terms)
 * - Line items (description, quantity, price, VAT)
 * - Totals (subtotal, VAT breakdown, total)
 * - Payment info (IBAN, BIC, reference)
 */
class ExtractInvoiceTool(
    private val executor: PromptExecutor,
    private val model: LLModel,
    private val prompt: AgentPrompt.Extraction
) : SimpleTool<ExtractInvoiceTool.Args>(
    argsSerializer = Args.serializer(),
    name = "extract_invoice",
    description = """
        Extracts structured data from an INVOICE document using vision AI.

        An INVOICE is a sales document issued BY the tenant to their customer.
        Use this tool after classifying the document as INVOICE.

        Extracts: vendor info, invoice number, dates, line items, totals, payment details.
        Returns structured JSON that can be stored in the database.

        Optional: Provide an example extraction from a similar vendor to improve accuracy.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription(
            "Base64-encoded PNG images of all document pages, separated by newlines."
        )
        val images: String,

        @property:LLMDescription(
            "Optional: JSON example of a previous extraction from the same vendor. " +
                "This improves accuracy for repeat vendors."
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
        val agent = ExtractionAgent<ExtractedInvoiceData>(
            executor = executor,
            model = model,
            prompt = prompt,
            userPromptPrefix = buildUserPromptPrefix(args.example),
            promptId = "invoice-extractor",
            emptyResult = { ExtractedInvoiceData(confidence = 0.0) }
        )

        // Run extraction
        val result = agent.extract(documentImages)

        return buildString {
            appendLine("EXTRACTION RESULT:")
            appendLine("Vendor: ${result.vendorName ?: "Unknown"}")
            appendLine("Invoice #: ${result.invoiceNumber ?: "Unknown"}")
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
            Extract invoice data from this document.

            REFERENCE EXAMPLE from same vendor (use this format and look for similar fields):
            $example

            Now extract from this
            """.trimIndent()
        } else {
            "Extract invoice data from this"
        }
    }
}
