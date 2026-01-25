package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tech.dokus.domain.model.ExtractedDocumentData

fun interface PeppolDataFetcher {
    suspend operator fun invoke(documentId: String): ExtractedDocumentData?
}

/**
 * Tool for fetching pre-parsed PEPPOL UBL data.
 *
 * PEPPOL documents arrive pre-parsed from the Recommand provider.
 * This tool retrieves the already-extracted data for enrichment.
 */
class GetPeppolDataTool(
    private val peppolDataFetcher: PeppolDataFetcher
) : SimpleTool<GetPeppolDataTool.Args>(
    argsSerializer = Args.serializer(),
    name = "get_peppol_data",
    description = """
        Retrieves pre-parsed PEPPOL UBL document data.

        PEPPOL documents arrive with extraction already done by the Recommand provider.
        Use this tool to get the structured data for a PEPPOL document.

        Returns the full extraction data including vendor, amounts, line items, etc.
        If the document is not a PEPPOL document, returns an error.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The document ID to fetch PEPPOL data for")
        val documentId: String
    )

    private val jsonFormat = Json { prettyPrint = true }

    override suspend fun execute(args: Args): String {
        val peppolData = peppolDataFetcher(args.documentId)
            ?: return "ERROR: No PEPPOL data found for document ${args.documentId}. " +
                "This document may not be from PEPPOL source."

        return try {
            buildString {
                appendLine("SUCCESS: Retrieved PEPPOL data")
                appendLine()
                appendLine("Extraction data:")
                appendLine(jsonFormat.encodeToString(peppolData))
            }
        } catch (e: Exception) {
            "ERROR: Failed to retrieve PEPPOL data: ${e.message}"
        }
    }
}
