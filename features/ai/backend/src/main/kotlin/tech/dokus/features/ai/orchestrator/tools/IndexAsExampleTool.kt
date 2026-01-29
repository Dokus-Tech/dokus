package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ExampleId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentExample
import tech.dokus.domain.repository.ExampleRepository

/**
 * Tool for indexing a successful extraction as an example for future few-shot learning.
 *
 * Stores high-confidence extractions by vendor VAT/name so they can be used
 * as examples when processing future documents from the same vendor.
 */
class IndexAsExampleTool(
    private val exampleRepository: ExampleRepository
) : SimpleTool<IndexAsExampleTool.Args>(
    argsSerializer = Args.serializer(),
    name = "index_as_example",
    description = """
        Indexes a successful extraction as an example for future few-shot learning.

        When processing documents from the same vendor in the future, this example
        will be used to guide extraction, improving accuracy.

        Only index extractions with high confidence (≥0.85).
        Requires either vendor VAT number or vendor name.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The tenant ID")
        val tenantId: String,

        @property:LLMDescription("Vendor VAT number (primary key for lookup)")
        val vendorVat: String? = null,

        @property:LLMDescription("Vendor name (fallback key for lookup)")
        val vendorName: String,

        @property:LLMDescription("Document type (INVOICE, BILL, RECEIPT, EXPENSE)")
        val documentType: String,

        @property:LLMDescription("The extraction JSON to use as example")
        val extraction: String,

        @property:LLMDescription("Confidence score of this extraction (0.0 - 1.0)")
        val confidence: Double
    )

    private val jsonFormat = Json { ignoreUnknownKeys = true }

    override suspend fun execute(args: Args): String {
        // Validate confidence threshold
        if (args.confidence < 0.85) {
            return "SKIPPED: Confidence ${args.confidence} is below threshold (0.85). " +
                "Only high-confidence extractions are indexed as examples."
        }

        // Validate we have at least vendor name
        if (args.vendorName.isBlank()) {
            return "ERROR: Vendor name is required for indexing as example"
        }

        return try {
            val extractionJson = jsonFormat.parseToJsonElement(args.extraction)
            val documentType = parseDocumentType(args.documentType)
            val now = Clock.System.now()

            val example = DocumentExample(
                id = ExampleId.generate(),
                tenantId = TenantId.parse(args.tenantId),
                vendorVat = args.vendorVat?.takeIf { it.isNotBlank() },
                vendorName = args.vendorName,
                documentType = documentType,
                extraction = extractionJson,
                confidence = args.confidence,
                timesUsed = 0,
                createdAt = now,
                updatedAt = now
            )

            val savedExample = exampleRepository.save(example)

            buildString {
                appendLine("SUCCESS: Indexed example ${savedExample.id}")
                appendLine("Vendor: ${savedExample.vendorName}")
                if (savedExample.vendorVat != null) {
                    appendLine("VAT: ${savedExample.vendorVat}")
                }
                appendLine("Type: ${savedExample.documentType}")
                appendLine("Confidence: ${String.format("%.0f%%", savedExample.confidence * 100)}")
            }
        } catch (e: Exception) {
            "ERROR: Failed to index example: ${e.message}"
        }
    }

    private fun parseDocumentType(value: String): DocumentType = when (value.uppercase()) {
        "INVOICE" -> DocumentType.Invoice
        "BILL" -> DocumentType.Bill
        "RECEIPT" -> DocumentType.Receipt
        "CREDIT_NOTE", "CREDITNOTE" -> DocumentType.CreditNote
        "PRO_FORMA", "PROFORMA" -> DocumentType.ProForma
        else -> DocumentType.Unknown
    }
}
