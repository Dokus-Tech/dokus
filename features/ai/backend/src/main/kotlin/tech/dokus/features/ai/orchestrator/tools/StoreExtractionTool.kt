package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import tech.dokus.domain.enums.ContactLinkDecisionType

/**
 * Tool for persisting document extraction to the database.
 *
 * Stores the extracted data, description, and keywords in the document record.
 */
class StoreExtractionTool(
    private val storeFunction: suspend (Payload) -> Boolean
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
        - Link decision (auto-link or suggestion) with evidence

        Use this tool after successful extraction and validation.
    """.trimIndent()
) {
    /**
     * Parsed payload passed to the storage callback.
     */
    data class Payload(
        val documentId: String,
        val tenantId: String,
        val runId: String?,
        val documentType: String?,
        val extraction: JsonElement,
        val description: String,
        val keywords: List<String>,
        val confidence: Double,
        val rawText: String?,
        val contactId: String?,
        val contactCreated: Boolean?,
        val contactConfidence: Float?,
        val contactReason: String?,
        val linkDecisionType: ContactLinkDecisionType?,
        val linkDecisionContactId: String?,
        val linkDecisionReason: String?,
        val linkDecisionConfidence: Float?,
        val linkDecisionEvidence: String?
    )

    @Serializable
    data class Args(
        @property:LLMDescription("The document ID to store extraction for")
        val documentId: String,

        @property:LLMDescription("The tenant ID")
        val tenantId: String,

        @property:LLMDescription("Optional ingestion run ID to update")
        val runId: String? = null,

        @property:LLMDescription("Document type (INVOICE, BILL, RECEIPT, EXPENSE, CREDIT_NOTE, PRO_FORMA)")
        val documentType: String? = null,

        @property:LLMDescription("The extraction JSON data")
        val extraction: String,

        @property:LLMDescription("The human-readable description")
        val description: String,

        @property:LLMDescription("Comma-separated keywords for search")
        val keywords: String,

        @property:LLMDescription("Confidence score (0.0 - 1.0)")
        val confidence: Double,

        @property:LLMDescription("Optional raw text extracted from the document")
        val rawText: String? = null,

        @property:LLMDescription("Optional contact ID to suggest/link")
        val contactId: String? = null,

        @property:LLMDescription("Whether the contact was created during this run")
        val contactCreated: Boolean? = null,

        @property:LLMDescription("Optional contact suggestion confidence (0.0 - 1.0)")
        val contactConfidence: Float? = null,

        @property:LLMDescription("Optional contact suggestion reason")
        val contactReason: String? = null,

        @property:LLMDescription("Link decision type: AUTO_LINK | SUGGEST | NONE")
        val linkDecisionType: ContactLinkDecisionType? = null,

        @property:LLMDescription("Contact ID associated with link decision")
        val linkDecisionContactId: String? = null,

        @property:LLMDescription("Reason for link decision")
        val linkDecisionReason: String? = null,

        @property:LLMDescription("Suggestion confidence (0.0 - 1.0) when decision is SUGGEST")
        val linkDecisionConfidence: Float? = null,

        @property:LLMDescription("JSON evidence payload for link decision")
        val linkDecisionEvidence: String? = null
    )

    private val jsonFormat = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    override suspend fun execute(args: Args): String {
        return try {
            val extractionJson = jsonFormat.parseToJsonElement(args.extraction)
            val keywordList = args.keywords.split(",").map { it.trim() }.filter { it.isNotBlank() }

            val success = storeFunction(
                Payload(
                    documentId = args.documentId,
                    tenantId = args.tenantId,
                    runId = args.runId,
                    documentType = args.documentType,
                    extraction = extractionJson,
                    description = args.description,
                    keywords = keywordList,
                    confidence = args.confidence,
                    rawText = args.rawText,
                    contactId = args.contactId,
                    contactCreated = args.contactCreated,
                    contactConfidence = args.contactConfidence,
                    contactReason = args.contactReason,
                    linkDecisionType = args.linkDecisionType,
                    linkDecisionContactId = args.linkDecisionContactId,
                    linkDecisionReason = args.linkDecisionReason,
                    linkDecisionConfidence = args.linkDecisionConfidence,
                    linkDecisionEvidence = args.linkDecisionEvidence
                )
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
