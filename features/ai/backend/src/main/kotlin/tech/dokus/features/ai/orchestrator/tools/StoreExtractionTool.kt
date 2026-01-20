package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Tool for persisting document extraction to the database.
 *
 * Stores the extracted data, description, and keywords in the document record.
 */
class StoreExtractionTool(
    private val storeFunction: suspend (
        documentId: String,
        tenantId: String,
        extraction: JsonElement,
        description: String,
        keywords: List<String>,
        confidence: Double
    ) -> Boolean
) : SimpleTool<StoreExtractionTool.Args>(
    argsSerializer = Args.serializer(),
    name = "store_extraction",
    description = """
        Persists document extraction data to the database.

        Stores:
        - Extraction JSON with all extracted fields
        - Human-readable description for UI
        - Search keywords
        - Confidence score

        Use this tool after successful extraction and validation.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The document ID to store extraction for")
        val documentId: String,

        @property:LLMDescription("The tenant ID")
        val tenantId: String,

        @property:LLMDescription("The extraction JSON data")
        val extraction: String,

        @property:LLMDescription("The human-readable description")
        val description: String,

        @property:LLMDescription("Comma-separated keywords for search")
        val keywords: String,

        @property:LLMDescription("Confidence score (0.0 - 1.0)")
        val confidence: Double
    )

    private val jsonFormat = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    override suspend fun execute(args: Args): String {
        return try {
            val extractionJson = jsonFormat.parseToJsonElement(args.extraction)
            val keywordList = args.keywords.split(",").map { it.trim() }.filter { it.isNotBlank() }

            val success = storeFunction(
                args.documentId,
                args.tenantId,
                extractionJson,
                args.description,
                keywordList,
                args.confidence
            )

            if (success) {
                "SUCCESS: Stored extraction for document ${args.documentId}"
            } else {
                "ERROR: Failed to store extraction - database operation returned false"
            }
        } catch (e: Exception) {
            "ERROR: Failed to store extraction: ${e.message}"
        }
    }
}
