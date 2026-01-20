package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ExampleRepository

/**
 * Tool for finding similar documents (examples) for few-shot learning.
 *
 * Looks up previously processed documents from the same vendor to use
 * as reference examples during extraction.
 */
class FindSimilarDocumentTool(
    private val exampleRepository: ExampleRepository
) : SimpleTool<FindSimilarDocumentTool.Args>(
    argsSerializer = Args.serializer(),
    name = "find_similar_document",
    description = """
        Finds a similar document example for few-shot learning.

        Searches for previously processed documents from the same vendor
        (by VAT number or name) to use as extraction reference.

        Lookup priority:
        1. Exact match on vendor VAT number (most reliable)
        2. Fuzzy match on vendor name (fallback)

        Returns the example extraction JSON if found.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The tenant ID")
        val tenantId: String,

        @property:LLMDescription("Vendor VAT number (primary lookup key)")
        val vendorVat: String? = null,

        @property:LLMDescription("Vendor name (fallback lookup key)")
        val vendorName: String? = null
    )

    private val jsonFormat = Json { prettyPrint = true }

    override suspend fun execute(args: Args): String {
        if (args.vendorVat.isNullOrBlank() && args.vendorName.isNullOrBlank()) {
            return "ERROR: Either vendorVat or vendorName must be provided"
        }

        val tenantId = TenantId.parse(args.tenantId)

        return try {
            // Try VAT lookup first (most reliable)
            val example = if (!args.vendorVat.isNullOrBlank()) {
                exampleRepository.findByVendorVat(tenantId, args.vendorVat)
            } else {
                null
            }

            // Fall back to name lookup
            val foundExample = example ?: if (!args.vendorName.isNullOrBlank()) {
                exampleRepository.findByVendorName(tenantId, args.vendorName)
            } else {
                null
            }

            if (foundExample != null) {
                // Increment usage counter
                exampleRepository.incrementUsage(foundExample.id)

                buildString {
                    appendLine("SUCCESS: Found example ${foundExample.id}")
                    appendLine("Vendor: ${foundExample.vendorName}")
                    if (foundExample.vendorVat != null) {
                        appendLine("VAT: ${foundExample.vendorVat}")
                    }
                    appendLine("Type: ${foundExample.documentType}")
                    appendLine("Confidence: ${String.format("%.0f%%", foundExample.confidence * 100)}")
                    appendLine("Times used: ${foundExample.timesUsed + 1}")
                    appendLine()
                    appendLine("Extraction example:")
                    appendLine(jsonFormat.encodeToString(foundExample.extraction))
                }
            } else {
                "NOT_FOUND: No example found for vendor " +
                    (args.vendorVat ?: args.vendorName ?: "unknown")
            }
        } catch (e: Exception) {
            "ERROR: Failed to find similar document: ${e.message}"
        }
    }
}
