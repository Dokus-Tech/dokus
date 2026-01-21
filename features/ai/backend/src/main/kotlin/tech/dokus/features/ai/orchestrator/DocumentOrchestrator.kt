package tech.dokus.features.ai.orchestrator

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.ToolCalls
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.enums.IndexingStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ExampleRepository
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.ExtractedDocumentData
import tech.dokus.features.ai.orchestrator.tools.CreateContactTool
import tech.dokus.features.ai.orchestrator.tools.GetDocumentImagesTool
import tech.dokus.features.ai.orchestrator.tools.LookupContactTool
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionTool
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.services.EmbeddingService
import tech.dokus.features.ai.utils.normalizeJson
import tech.dokus.foundation.backend.config.IntelligenceMode
import tech.dokus.foundation.backend.lookup.CbeApiClient
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.time.measureTimedValue

/**
 * Tool-calling orchestrator for document processing.
 *
 * The orchestrator model is the single decision maker. It calls tools to:
 * - Render documents to images
 * - Classify document type
 * - Extract structured data with vision tools
 * - Validate and correct extraction
 * - Generate description and keywords
 *
 * Storage and RAG indexing remain in the worker for now.
 */
@Suppress("LongParameterList")
class DocumentOrchestrator(
    private val executor: PromptExecutor,
    private val orchestratorModel: LLModel,
    private val visionModel: LLModel,
    private val mode: IntelligenceMode,
    private val exampleRepository: ExampleRepository,
    private val documentImageService: DocumentImageService,
    private val chunkingService: ChunkingService,
    private val embeddingService: EmbeddingService,
    private val chunkRepository: ChunkRepository,
    private val cbeApiClient: CbeApiClient?,
    private val indexingUpdater: (suspend (runId: String, status: IndexingStatus, chunksCount: Int?, errorMessage: String?) -> Boolean)? = null,
    private val documentFetcher: suspend (documentId: String, tenantId: String) -> GetDocumentImagesTool.DocumentData?,
    private val peppolDataFetcher: suspend (documentId: String) -> ExtractedDocumentData? = { null },
    private val contactLookup: suspend (tenantId: String, vatNumber: String) -> LookupContactTool.ContactInfo? =
        { _, _ -> null },
    private val contactCreator: suspend (
        tenantId: String,
        name: String,
        vatNumber: String?,
        address: String?
    ) -> CreateContactTool.CreateResult =
        { _, _, _, _ -> CreateContactTool.CreateResult(success = false, contactId = null, error = "disabled") },
    private val storeExtraction: suspend (StoreExtractionTool.Payload) -> Boolean =
        { false }
) {
    private val logger = loggerFor()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        /** Confidence threshold for auto-confirm */
        const val AUTO_CONFIRM_THRESHOLD = 0.85

        /** Maximum self-correction attempts */
        const val MAX_CORRECTIONS = 3
    }

    @Serializable
    private data class OrchestratorAgentOutput(
        val status: String,
        val documentType: String? = null,
        val extraction: JsonElement? = null,
        val rawText: String? = null,
        val description: String? = null,
        val keywords: List<String>? = null,
        val confidence: Double? = null,
        val validationPassed: Boolean? = null,
        val correctionsApplied: Int? = null,
        val contactId: String? = null,
        val contactCreated: Boolean? = null,
        val issues: List<String>? = null,
        val reason: String? = null
    )

    /**
     * Process an uploaded document (full pipeline).
     *
     * @param documentId The document ID
     * @param tenantId The tenant ID
     * @param tenantContext Tenant context for classification
     * @return Processing result (Success, NeedsReview, or Failed)
     */
    suspend fun process(
        documentId: DocumentId,
        tenantId: TenantId,
        tenantContext: AgentPrompt.TenantContext,
        runId: String? = null,
        maxPages: Int? = null,
        dpi: Int? = null
    ): OrchestratorResult {
        val toolRegistry = buildToolRegistry(tenantId, tenantContext)
        val agentConfig = AIAgentConfig.withSystemPrompt(
            prompt = orchestratorSystemPrompt(),
            llm = orchestratorModel,
            id = "document-orchestrator",
            maxAgentIterations = maxAgentIterations()
        )

        val agent = AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            strategy = singleRunStrategy(ToolCalls.SEQUENTIAL),
            toolRegistry = toolRegistry,
            id = "document-orchestrator"
        )

        val userPrompt = buildUserPrompt(documentId, tenantId, tenantContext, runId, maxPages, dpi)
        val (rawResponse, duration) = try {
            measureTimedValue { agent.run(userPrompt) }
        } catch (e: Exception) {
            logger.error("Orchestrator run failed", e)
            return OrchestratorResult.Failed(
                reason = e.message ?: "Orchestrator execution failed",
                stage = "orchestrator",
                auditTrail = emptyList()
            )
        } finally {
            runCatching { agent.close() }
        }
        val auditTrail = listOf(
            ProcessingStep.create(
                step = 1,
                action = "Tool-calling orchestrator run",
                tool = null,
                durationMs = duration.inWholeMilliseconds
            )
        )

        val parsed = parseAgentOutput(rawResponse)
        if (parsed == null) {
            logger.error("Failed to parse orchestrator response: {}", rawResponse)
            return OrchestratorResult.Failed(
                reason = "Failed to parse orchestrator output",
                stage = "orchestrator",
                auditTrail = auditTrail
            )
        }

        return toResult(parsed, auditTrail)
    }

    // =========================================================================
    // Tool Registry
    // =========================================================================

    private fun buildToolRegistry(
        tenantId: TenantId,
        tenantContext: AgentPrompt.TenantContext
    ): ai.koog.agents.core.tools.ToolRegistry {
        val config = OrchestratorToolRegistry.Config(
            executor = executor,
            visionModel = visionModel,
            documentImageService = documentImageService,
            chunkingService = chunkingService,
            embeddingService = embeddingService,
            exampleRepository = exampleRepository,
            chunkRepository = chunkRepository,
            cbeApiClient = cbeApiClient,
            tenantContext = tenantContext,
            indexingUpdater = indexingUpdater,
            documentFetcher = { documentId ->
                documentFetcher(documentId, tenantId.toString())
            },
            peppolDataFetcher = peppolDataFetcher,
            storeExtraction = storeExtraction,
            contactLookup = contactLookup,
            contactCreator = contactCreator
        )

        return OrchestratorToolRegistry.create(config)
    }

    // =========================================================================
    // Prompting
    // =========================================================================

    private fun orchestratorSystemPrompt(): String = """
        You are the Dokus document processing orchestrator.
        You must solve the task by calling tools. Do not guess.

        Core rules:
        - Always use tools for document understanding, extraction, and validation.
        - If a core tool fails (images, classification, extraction, validation, store_extraction), return status="failed".
        - Non-critical tools (store_chunks, index_as_example) may fail without failing the run; include the error in "issues".
        - If classification is UNKNOWN or confidence < 0.5, return status="needs_review".
        - If overall confidence is below ${AUTO_CONFIRM_THRESHOLD}, return status="needs_review".
        - Max corrections: $MAX_CORRECTIONS attempts.
        - Output ONLY valid JSON (no markdown).

        Tool usage notes:
        - get_document_images returns lines "Page N: <base64>". Use the base64 values.
        - If maxPages or dpi are provided in the task, pass them to get_document_images.
        - see_document and extract_* tools return a JSON section. Use that JSON.
        - find_similar_document may return an extraction example. Use it to re-run extraction once.
        - verify_totals / validate_iban / validate_ogm / lookup_company are validation tools.
        - generate_description / generate_keywords should be used after extraction.
        - After success (or needs_review with extraction), call store_extraction with runId, documentType,
          extraction, description, keywords, confidence, rawText, and any contactId you resolved.
          If you created a contact, set contactCreated=true in store_extraction.
        - For RAG indexing, call prepare_rag_chunks -> embed_text for each chunk -> store_chunks with runId.
        - If you can resolve a contact, use lookup_contact then create_contact if missing, and pass contactId to store_extraction.

        Final output JSON schema:
        {
          "status": "success|needs_review|failed",
          "documentType": "INVOICE|BILL|RECEIPT|EXPENSE|CREDIT_NOTE|PRO_FORMA|UNKNOWN",
          "extraction": { ... },
          "rawText": "string",
          "description": "string",
          "keywords": ["..."],
          "confidence": 0.0,
          "validationPassed": true,
          "correctionsApplied": 0,
          "contactId": "uuid or null",
          "contactCreated": false,
          "issues": ["..."],
          "reason": "string"
        }

        If status="needs_review" and you have any extraction data, include it in "extraction".
    """.trimIndent()

    private fun buildUserPrompt(
        documentId: DocumentId,
        tenantId: TenantId,
        tenantContext: AgentPrompt.TenantContext,
        runId: String?,
        maxPages: Int?,
        dpi: Int?
    ): String = """
        Task: Process document
        documentId: ${documentId}
        tenantId: ${tenantId}
        runId: ${runId ?: "unknown"}
        tenantVatNumber: ${tenantContext.vatNumber ?: "unknown"}
        tenantCompanyName: ${tenantContext.companyName ?: "unknown"}
        source: UPLOAD
        maxPages: ${maxPages ?: "default"}
        dpi: ${dpi ?: "default"}
    """.trimIndent()

    private fun maxAgentIterations(): Int =
        when (mode) {
            IntelligenceMode.Assisted -> 8
            IntelligenceMode.Autonomous -> 12
            IntelligenceMode.Sovereign -> 16
        }

    // =========================================================================
    // Output Parsing
    // =========================================================================

    private fun parseAgentOutput(output: String): OrchestratorAgentOutput? {
        val normalized = normalizeJson(output)
        return runCatching {
            json.decodeFromString(OrchestratorAgentOutput.serializer(), normalized)
        }.getOrNull()
    }

    private fun toResult(
        output: OrchestratorAgentOutput,
        auditTrail: List<ProcessingStep>
    ): OrchestratorResult {
        val status = output.status.lowercase().trim()
        val documentType = parseDocumentType(output.documentType)
        val extraction = normalizeExtraction(output.extraction)
        val rawText = output.rawText ?: extractRawText(extraction)
        val confidence = output.confidence
            ?: extractConfidence(extraction)
            ?: 0.0
        val description = output.description ?: ""
        val keywords = output.keywords ?: emptyList()
        val validationPassed = output.validationPassed ?: output.issues.isNullOrEmpty()
        val correctionsApplied = output.correctionsApplied ?: 0
        val contactId = output.contactId?.let { runCatching { ContactId.parse(it) }.getOrNull() }
        val contactCreated = output.contactCreated ?: false
        val issues = output.issues ?: emptyList()
        val reason = output.reason ?: "Needs review"

        return when (status) {
            "success" -> {
                if (documentType == null || extraction == null) {
                    OrchestratorResult.Failed(
                        reason = "Missing documentType or extraction in orchestrator output",
                        stage = "orchestrator",
                        auditTrail = auditTrail
                    )
                } else {
                    OrchestratorResult.Success(
                        documentType = documentType,
                        extraction = extraction,
                        confidence = confidence,
                        rawText = rawText,
                        description = description,
                        keywords = keywords,
                        validationPassed = validationPassed,
                        correctionsApplied = correctionsApplied,
                        exampleUsed = null,
                        contactId = contactId,
                        contactCreated = contactCreated,
                        auditTrail = auditTrail
                    )
                }
            }

            "needs_review" -> {
                OrchestratorResult.NeedsReview(
                    documentType = documentType,
                    partialExtraction = extraction,
                    reason = reason,
                    issues = issues,
                    auditTrail = auditTrail
                )
            }

            "failed" -> {
                OrchestratorResult.Failed(
                    reason = reason,
                    stage = "orchestrator",
                    auditTrail = auditTrail
                )
            }

            else -> {
                OrchestratorResult.Failed(
                    reason = "Unknown orchestrator status: ${output.status}",
                    stage = "orchestrator",
                    auditTrail = auditTrail
                )
            }
        }
    }

    private fun parseDocumentType(type: String?): ClassifiedDocumentType? {
        val normalized = type?.trim()?.uppercase() ?: return null
        return runCatching { ClassifiedDocumentType.valueOf(normalized) }.getOrNull()
    }

    private fun extractRawText(extraction: JsonElement?): String {
        val obj = extraction?.jsonObject ?: return ""
        return obj["extractedText"]?.jsonPrimitive?.content ?: ""
    }

    private fun extractConfidence(extraction: JsonElement?): Double? {
        val obj = extraction?.jsonObject ?: return null
        return obj["confidence"]?.jsonPrimitive?.content?.toDoubleOrNull()
    }

    private fun normalizeExtraction(extraction: JsonElement?): JsonElement? {
        if (extraction == null) return null
        if (extraction is JsonPrimitive && extraction.isString) {
            return runCatching { json.parseToJsonElement(extraction.content) }.getOrNull() ?: extraction
        }
        return extraction
    }
}
