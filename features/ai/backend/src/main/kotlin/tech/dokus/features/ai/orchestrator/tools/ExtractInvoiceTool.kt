package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import tech.dokus.features.ai.services.DocumentImageCache
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.prompts.AgentPrompt

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
    private val prompt: AgentPrompt.Extraction,
    private val imageCache: DocumentImageCache,
    private val traceSink: tech.dokus.features.ai.orchestrator.ToolTraceSink? = null
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
            "Image IDs (from get_document_images) or base64 PNGs, separated by newlines."
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
        val documentImages = try {
            DocumentImageResolver(imageCache).resolve(args.images)
        } catch (e: Exception) {
            traceSink?.record(
                action = "extract_invoice",
                tool = name,
                durationMs = 0,
                input = null,
                output = null,
                notes = "error=${e.message}"
            )
            return "ERROR: ${e.message}"
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
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val result = agent.extract(documentImages)
        val outputJson = jsonFormat.decodeFromString<JsonElement>(jsonFormat.encodeToString(result))
        traceSink?.record(
            action = "extract_invoice",
            tool = name,
            durationMs = start.elapsedNow().inWholeMilliseconds,
            input = null,
            output = outputJson,
            notes = "confidence=${result.confidence}, lineItems=${result.lineItems.size}"
        )

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
