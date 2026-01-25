package tech.dokus.features.ai.orchestrator

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.ToolCalls
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ExampleRepository
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.orchestrator.tools.ContactCreatorHandler
import tech.dokus.features.ai.orchestrator.tools.ContactLookupHandler
import tech.dokus.features.ai.orchestrator.tools.CreateContactTool
import tech.dokus.features.ai.orchestrator.tools.IndexingStatusUpdater
import tech.dokus.features.ai.orchestrator.tools.PeppolDataFetcher
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionHandler
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.prompts.OrchestratorPrompt
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentImageCache
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
 * - Store extraction results
 *
 * Philosophy: If the agent fails to produce valid output, fail cleanly.
 * No recovery, no fallback, no repair agent.
 */
@Suppress("LongParameterList")
class DocumentOrchestrator(
    private val executor: PromptExecutor,
    private val orchestratorModel: LLModel,
    private val visionModel: LLModel,
    private val mode: IntelligenceMode,
    private val exampleRepository: ExampleRepository,
    private val documentImageService: DocumentImageService,
    private val imageCache: DocumentImageCache,
    private val chunkingService: ChunkingService,
    private val embeddingService: EmbeddingService,
    private val chunkRepository: ChunkRepository,
    private val cbeApiClient: CbeApiClient?,
    private val indexingUpdater: IndexingStatusUpdater? = null,
    private val documentFetcher: DocumentFetcher,
    private val peppolDataFetcher: PeppolDataFetcher = PeppolDataFetcher { null },
    private val contactLookup: ContactLookupHandler = ContactLookupHandler { _, _ -> null },
    private val contactCreator: ContactCreatorHandler = ContactCreatorHandler { _, _, _, _ ->
        CreateContactTool.CreateResult(success = false, contactId = null, error = "disabled")
    },
    private val storeExtraction: StoreExtractionHandler = StoreExtractionHandler { false }
) {
    private val logger = loggerFor()
    private val json = Json { ignoreUnknownKeys = true }

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
        logger.info(
            "Starting orchestration run: documentId={}, tenantId={}, runId={}",
            documentId,
            tenantId,
            runId ?: "unknown"
        )

        val traceCollector = ProcessingTraceCollector()

        val toolRegistry = buildToolRegistry(
            tenantId = tenantId,
            tenantContext = tenantContext,
            traceSink = traceCollector
        )
        val agentConfig = AIAgentConfig.withSystemPrompt(
            prompt = OrchestratorPrompt(tenantContext).value,
            llm = orchestratorModel,
            id = "document-orchestrator",
            maxAgentIterations = mode.maxIterations,
        )

        val agent = AIAgent(
            promptExecutor = executor,
            agentConfig = agentConfig,
            strategy = singleRunStrategy(ToolCalls.SEQUENTIAL),
            toolRegistry = toolRegistry,
            id = "document-orchestrator"
        )

        val userPrompt = buildUserPrompt(documentId, tenantId, tenantContext, runId, maxPages, dpi)
        val p = prompt("") {
            user(userPrompt)
        }
        val (rawResponse, duration) = try {
            measureTimedValue { agent.run(userPrompt) }
        } catch (e: Exception) {
            logger.error("Orchestrator run failed", e)
            traceCollector.record(
                action = "orchestrator_run_failed",
                tool = "document-orchestrator",
                durationMs = 0,
                input = null,
                output = null,
                notes = e.message
            )
            return OrchestratorResult.Failed(
                reason = e.message ?: "Orchestrator execution failed",
                stage = "orchestrator",
                auditTrail = traceCollector.snapshot()
            )
        } finally {
            runCatching { agent.close() }
        }

        traceCollector.record(
            action = "orchestrator_run_completed",
            tool = "document-orchestrator",
            durationMs = duration.inWholeMilliseconds,
            input = null,
            output = null,
            notes = null
        )

        val auditTrail = traceCollector.snapshot()

        val output = parseOutput(rawResponse)
        if (output == null) {
            logger.error("Failed to parse orchestrator response: {}", rawResponse.take(1000))
            return OrchestratorResult.Failed(
                reason = "Failed to parse orchestrator output",
                stage = "orchestrator",
                auditTrail = auditTrail
            )
        }

        return toResult(output, auditTrail)
    }

    // =========================================================================
    // Tool Registry
    // =========================================================================

    private fun buildToolRegistry(
        tenantId: TenantId,
        tenantContext: AgentPrompt.TenantContext,
        traceSink: ToolTraceSink
    ): ToolRegistry {
        val config = OrchestratorToolRegistry.Config(
            executor = executor,
            visionModel = visionModel,
            documentImageService = documentImageService,
            imageCache = imageCache,
            chunkingService = chunkingService,
            embeddingService = embeddingService,
            exampleRepository = exampleRepository,
            chunkRepository = chunkRepository,
            cbeApiClient = cbeApiClient,
            tenantContext = tenantContext,
            indexingUpdater = indexingUpdater,
            traceSink = traceSink,
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

    private fun buildUserPrompt(
        documentId: DocumentId,
        tenantId: TenantId,
        tenantContext: AgentPrompt.TenantContext,
        runId: String?,
        maxPages: Int?,
        dpi: Int?
    ): String = """
        Task: Process document
        documentId: $documentId
        tenantId: $tenantId
        runId: ${runId ?: "unknown"}
        tenantVatNumber: ${tenantContext.vatNumber}
        tenantCompanyName: ${tenantContext.companyName}
        source: UPLOAD
        maxPages: ${maxPages ?: "default"}
        dpi: ${dpi ?: "default"}
    """.trimIndent()

    // =========================================================================
    // Output Parsing
    // =========================================================================

    private fun parseOutput(raw: String): OrchestratorAgentOutput? {
        val normalized = normalizeJson(raw)
        return runCatching {
            json.decodeFromString(OrchestratorAgentOutput.serializer(), normalized)
        }
            .onFailure { logger.warn("Failed to parse orchestrator output: ${it.message}") }
            .getOrNull()
    }

    // =========================================================================
    // Result Mapping
    // =========================================================================

    private fun toResult(
        output: OrchestratorAgentOutput,
        auditTrail: List<ProcessingStep>
    ): OrchestratorResult {
        val status = output.status.lowercase().trim()
        val documentType = output.documentType
        val extraction = output.extraction
        val rawText = output.rawText
        val confidence = output.confidence
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
}
