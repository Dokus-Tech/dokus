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
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.prompts.AgentPrompt

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
    private val prompt: AgentPrompt.Extraction,
    private val imageCache: DocumentImageCache,
    private val traceSink: tech.dokus.features.ai.orchestrator.ToolTraceSink? = null
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
            "Image IDs (from get_document_images) or base64 PNGs, separated by newlines."
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
        val documentImages = try {
            DocumentImageResolver(imageCache).resolve(args.images)
        } catch (e: Exception) {
            traceSink?.record(
                action = "extract_bill",
                tool = name,
                durationMs = 0,
                input = null,
                output = null,
                notes = "error=${e.message}"
            )
            return "ERROR: ${e.message}"
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
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val result = agent.extract(documentImages)
        traceSink?.record(
            action = "extract_bill",
            tool = name,
            durationMs = start.elapsedNow().inWholeMilliseconds,
            input = null,
            output = null,
            notes = "confidence=${result.confidence}, lineItems=${result.lineItems.size}"
        )

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
