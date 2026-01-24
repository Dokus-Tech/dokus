package tech.dokus.features.ai.orchestrator

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.ToolCalls
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.enums.ContactLinkDecisionType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ExampleRepository
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.orchestrator.tools.ContactCreatorHandler
import tech.dokus.features.ai.orchestrator.tools.ContactLookupHandler
import tech.dokus.features.ai.orchestrator.tools.CreateContactTool
import tech.dokus.features.ai.orchestrator.tools.IndexingStatusUpdater
import tech.dokus.features.ai.orchestrator.tools.PeppolDataFetcher
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionHandler
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionTool
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
        val baseStoreHandler = storeExtraction
        var storeCalled = false
        var storeSucceeded = false

        val storeWrapper = StoreExtractionHandler { payload ->
            storeCalled = true
            val success = baseStoreHandler(payload)
            storeSucceeded = success
            success
        }

        val toolRegistry = buildToolRegistry(
            tenantId = tenantId,
            tenantContext = tenantContext,
            traceSink = traceCollector,
            storeExtractionOverride = storeWrapper
        )
        val agentConfig = AIAgentConfig.withSystemPrompt(
            prompt = OrchestratorPrompt(tenantContext).value,
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

        val parsed = parseAgentOutputWithRepair(rawResponse, traceCollector)
        val auditTrail = traceCollector.snapshot()

        val resolvedOutput = when (parsed) {
            null -> {
                val fallback = buildFallbackOutputFromTrace(auditTrail)
                if (fallback != null) {
                    val fallbackTool = findLatestExtractionTool(auditTrail)
                    traceCollector.record(
                        action = "orchestrator_output_parse_failed",
                        tool = "document-orchestrator",
                        durationMs = 0,
                        input = null,
                        output = null,
                        notes = "using_extraction_trace_fallback, sourceTool=${fallbackTool ?: "unknown"}"
                    )
                }
                fallback
            }

            else -> {
                val normalizedParsed = parsed.copy(
                    extraction = normalizeExtraction(parsed.extraction)
                )
                val missingDocumentType = normalizedParsed.documentType.isNullOrBlank()
                val missingExtraction = normalizedParsed.extraction == null

                if (missingDocumentType || missingExtraction) {
                    val fallback = buildFallbackOutputFromTrace(auditTrail)
                    if (fallback != null) {
                        traceCollector.record(
                            action = "orchestrator_output_incomplete",
                            tool = "document-orchestrator",
                            durationMs = 0,
                            input = null,
                            output = null,
                            notes = "missingDocumentType=$missingDocumentType, missingExtraction=$missingExtraction"
                        )
                        mergeFallbackOutput(fallback, normalizedParsed)
                    } else {
                        null
                    }
                } else {
                    normalizedParsed
                }
            }
        }

        if (resolvedOutput == null) {
            logger.error("Failed to parse orchestrator response: {}", rawResponse.take(1000))
            return OrchestratorResult.Failed(
                reason = "Failed to parse orchestrator output",
                stage = "orchestrator",
                auditTrail = auditTrail
            )
        }

        val persisted = ensureExtractionPersisted(
            documentId = documentId,
            tenantId = tenantId,
            runId = runId,
            output = resolvedOutput,
            storeCalled = storeCalled,
            storeSucceeded = storeSucceeded,
            traceSink = traceCollector,
            storeHandler = baseStoreHandler
        )
        if (!persisted && resolvedOutput.extraction != null && resolvedOutput.documentType != null) {
            logger.error(
                "Orchestrator produced extraction but persistence failed: documentId={}, runId={}",
                documentId,
                runId ?: "unknown"
            )
            return OrchestratorResult.Failed(
                reason = "Extraction completed but could not be persisted",
                stage = "store_extraction",
                auditTrail = auditTrail
            )
        }

        return toResult(resolvedOutput, auditTrail)
    }

    // =========================================================================
    // Tool Registry
    // =========================================================================

    private fun buildToolRegistry(
        tenantId: TenantId,
        tenantContext: AgentPrompt.TenantContext,
        traceSink: ToolTraceSink? = null,
        storeExtractionOverride: StoreExtractionHandler? = null
    ): ai.koog.agents.core.tools.ToolRegistry {
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
            storeExtraction = storeExtractionOverride ?: storeExtraction,
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

    private fun maxAgentIterations(): Int =
        when (mode) {
            IntelligenceMode.Assisted -> 8
            IntelligenceMode.Autonomous -> 12
            IntelligenceMode.Sovereign -> 32
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

    private suspend fun parseAgentOutputWithRepair(
        output: String,
        traceSink: ToolTraceSink? = null
    ): OrchestratorAgentOutput? {
        val normalized = normalizeJson(output)
        val parsed = parseAgentOutputStrict(normalized)

        if (parsed != null && !containsPlaceholders(normalized)) {
            return parsed
        }

        val lenientParsed = parseAgentOutputLenient(normalized)
        if (lenientParsed != null && !containsPlaceholders(normalized)) {
            return lenientParsed
        }

        traceSink?.record(
            action = "orchestrator_output_invalid",
            tool = "document-orchestrator",
            durationMs = 0,
            input = null,
            output = null,
            notes = "attempting_repair"
        )

        val repaired = repairAgentOutput(output) ?: return null
        val repairedNormalized = normalizeJson(repaired)
        if (containsPlaceholders(repairedNormalized)) {
            return null
        }

        val repairedParsed = parseAgentOutputStrict(repairedNormalized)
        if (repairedParsed != null) {
            return repairedParsed
        }

        return parseAgentOutputLenient(repairedNormalized)
    }

    private fun parseAgentOutputStrict(normalized: String): OrchestratorAgentOutput? {
        return runCatching {
            json.decodeFromString(OrchestratorAgentOutput.serializer(), normalized)
        }.getOrNull()
    }

    private fun parseAgentOutputLenient(normalized: String): OrchestratorAgentOutput? {
        val element = runCatching { json.decodeFromString<JsonElement>(normalized) }.getOrNull()
            ?: return null
        val obj = parseJsonObjectOrNull(element) ?: return null

        val status = obj["status"].asStringOrNull() ?: return null
        val keywords = obj["keywords"].asStringListOrNull()

        return OrchestratorAgentOutput(
            status = status,
            documentType = obj["documentType"].asStringOrNull(),
            extraction = obj["extraction"],
            rawText = obj["rawText"].asStringOrNull(),
            description = obj["description"].asStringOrNull(),
            keywords = keywords,
            confidence = obj["confidence"].asDoubleOrNull(),
            validationPassed = obj["validationPassed"].asBooleanOrNull(),
            correctionsApplied = obj["correctionsApplied"].asIntOrNull(),
            contactId = obj["contactId"].asStringOrNull(),
            contactCreated = obj["contactCreated"].asBooleanOrNull(),
            issues = obj["issues"].asStringListOrNull(),
            reason = obj["reason"].asStringOrNull()
        )
    }

    private fun containsPlaceholders(output: String): Boolean {
        return output.contains("...") || output.contains("…")
    }

    private suspend fun repairAgentOutput(rawResponse: String): String? {
        val repairPrompt = """
            You are a JSON repair agent.
            Your task: return ONLY a valid JSON object that matches this schema:
            {
              "status": "success|needs_review|failed",
              "documentType": "INVOICE|BILL|RECEIPT|EXPENSE|CREDIT_NOTE|PRO_FORMA|UNKNOWN",
              "extraction": { ... } or null,
              "rawText": "string or null",
              "description": "string",
              "keywords": ["..."],
              "confidence": 0.0,
              "validationPassed": true/false,
              "correctionsApplied": 0,
              "contactId": "uuid or null",
              "contactCreated": false,
              "issues": ["..."],
              "reason": "string"
            }
            Rules:
            - Output ONLY JSON, no explanations.
            - Do NOT include placeholders like "..." or "…".
            - If a field is missing, set it to null or an empty value.
        """.trimIndent()

        val truncated = if (rawResponse.length > 12000) {
            rawResponse.take(12000)
        } else {
            rawResponse
        }

        return try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = orchestratorModel,
                strategy = singleRunStrategy(),
                toolRegistry = ai.koog.agents.core.tools.ToolRegistry.EMPTY,
                id = "orchestrator-output-repair",
                systemPrompt = repairPrompt
            )
            try {
                agent.run("Fix this output:\n$truncated")
            } finally {
                runCatching { agent.close() }
            }
        } catch (e: Exception) {
            logger.error("Failed to repair orchestrator output", e)
            null
        }
    }

    private suspend fun ensureExtractionPersisted(
        documentId: DocumentId,
        tenantId: TenantId,
        runId: String?,
        output: OrchestratorAgentOutput,
        storeCalled: Boolean,
        storeSucceeded: Boolean,
        traceSink: ToolTraceSink? = null,
        storeHandler: StoreExtractionHandler
    ): Boolean {
        if (storeCalled && storeSucceeded) {
            return true
        }

        val documentType = output.documentType?.trim()?.uppercase() ?: return false
        val extraction = normalizeExtraction(output.extraction) ?: return false
        val description = output.description ?: ""
        val keywords = output.keywords ?: emptyList()
        val confidence = output.confidence
            ?: extractConfidence(extraction)
            ?: 0.0
        val rawText = output.rawText ?: extractRawText(extraction)

        traceSink?.record(
            action = "fallback_store_extraction",
            tool = "store_extraction",
            durationMs = 0,
            input = null,
            output = null,
            notes = "storeCalled=$storeCalled, storeSucceeded=$storeSucceeded"
        )

        // Use AUTO_LINK when contactId is recovered from fallback (sets linkedContactId → confirm works)
        val hasRecoveredContact = output.contactId != null

        val success = storeHandler(
            StoreExtractionTool.Payload(
                documentId = documentId.toString(),
                tenantId = tenantId.toString(),
                runId = runId,
                documentType = documentType,
                extraction = extraction,
                description = description,
                keywords = keywords,
                confidence = confidence,
                rawText = rawText,
                contactId = output.contactId,
                contactCreated = output.contactCreated,
                contactConfidence = null,
                contactReason = null,
                linkDecisionType = if (hasRecoveredContact) ContactLinkDecisionType.AutoLink else null,
                linkDecisionContactId = output.contactId,
                linkDecisionReason = if (hasRecoveredContact)
                    "Recovered from trace: VAT lookup exact match (fallback)" else null,
                linkDecisionConfidence = if (hasRecoveredContact) 1.0f else null,
                linkDecisionEvidence = null
            )
        )

        traceSink?.record(
            action = "fallback_store_extraction_result",
            tool = "store_extraction",
            durationMs = 0,
            input = null,
            output = null,
            notes = "success=$success"
        )

        return success
    }

    /**
     * Recovers contact ID from trace if orchestrator already performed a lookup.
     * Only returns contact when:
     * 1. store_extraction output contains linkedContactId or contactId (not suggestedContactId), OR
     * 2. lookup_contact returned found=true with matchType=EXACT
     */
    private fun findContactFromTrace(auditTrail: List<ProcessingStep>): String? {
        fun JsonElement?.str(key: String): String? =
            (this as? JsonObject)?.get(key)?.jsonPrimitive?.contentOrNull

        // Priority 1: Explicit linked contact from store_extraction (if it partially succeeded)
        // GUARDRAIL: Only use linkedContactId or contactId - NOT suggestedContactId
        // (suggested is not a confirmed identity decision)
        auditTrail.lastOrNull {
            it.action == "store_extraction" || it.tool == "store_extraction"
        }?.output?.let { output ->
            output.str("linkedContactId") ?: output.str("contactId")
        }?.let { return it }

        // Priority 2: VAT lookup exact match
        // GUARDRAIL: Only return contactId when matchType == "EXACT"
        // (protects against fuzzy matches if contactLookup ever becomes less strict)
        auditTrail.lastOrNull {
            it.action == "lookup_contact" || it.tool == "lookup_contact"
        }?.output?.let { output ->
            val obj = output as? JsonObject ?: return@let
            val found = obj["found"]?.jsonPrimitive?.booleanOrNull ?: false
            val matchType = obj["matchType"]?.jsonPrimitive?.contentOrNull
            if (found && matchType == "EXACT") {
                obj["contactId"]?.jsonPrimitive?.contentOrNull?.let { return it }
            }
        }

        return null
    }

    private fun buildFallbackOutputFromTrace(
        auditTrail: List<ProcessingStep>
    ): OrchestratorAgentOutput? {
        val extractionStep = auditTrail.lastOrNull { step ->
            isExtractionStep(step) && step.output != null
        } ?: return null

        val sourceTool = extractionStepName(extractionStep)
        val documentType = sourceTool?.let { toolName ->
            extractionToolDocumentType[toolName]
        } ?: return null

        val extraction = extractionStep.output ?: return null
        val confidence = extractConfidence(extraction)
        val rawText = extractRawText(extraction)

        // Recover contact from trace if orchestrator already performed lookup
        val contactId = findContactFromTrace(auditTrail)

        return OrchestratorAgentOutput(
            status = "needs_review",
            documentType = documentType,
            extraction = extraction,
            rawText = rawText,
            description = null,
            keywords = emptyList(),
            confidence = confidence,
            validationPassed = false,
            correctionsApplied = 0,
            contactId = contactId,
            contactCreated = null,
            issues = listOf(
                "Orchestrator output parse failed; persisted extraction output",
                "Fallback source tool: ${sourceTool ?: "unknown"}"
            ),
            reason = "Orchestrator output parse failed (fallback source: ${sourceTool ?: "unknown"})"
        )
    }

    private fun mergeFallbackOutput(
        fallback: OrchestratorAgentOutput,
        parsed: OrchestratorAgentOutput
    ): OrchestratorAgentOutput {
        val mergedIssues = buildList {
            addAll(fallback.issues.orEmpty())
            addAll(parsed.issues.orEmpty())
        }.distinct()

        val normalizedFallbackExtraction =
            normalizeExtraction(fallback.extraction) ?: fallback.extraction

        return fallback.copy(
            extraction = normalizedFallbackExtraction,
            description = parsed.description ?: fallback.description,
            keywords = parsed.keywords ?: fallback.keywords,
            confidence = parsed.confidence ?: fallback.confidence,
            rawText = parsed.rawText ?: fallback.rawText,
            contactId = parsed.contactId ?: fallback.contactId,
            contactCreated = parsed.contactCreated ?: fallback.contactCreated,
            issues = if (mergedIssues.isEmpty()) null else mergedIssues
        )
    }

    private fun findLatestExtractionTool(auditTrail: List<ProcessingStep>): String? {
        return auditTrail.lastOrNull { step ->
            isExtractionStep(step) && step.output != null
        }?.let { extractionStepName(it) }
    }

    private fun JsonElement?.asStringOrNull(): String? {
        val primitive = this as? JsonPrimitive ?: return null
        if (!primitive.isString && primitive.content.isBlank()) return null
        return primitive.content
    }

    private fun JsonElement?.asBooleanOrNull(): Boolean? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.booleanOrNull ?: primitive.content.toBooleanStrictOrNull()
        ?: primitive.content.toBoolean()
    }

    private fun JsonElement?.asIntOrNull(): Int? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.intOrNull ?: primitive.content.toIntOrNull()
    }

    private fun JsonElement?.asDoubleOrNull(): Double? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.doubleOrNull ?: primitive.content.toDoubleOrNull()
    }

    private fun JsonElement?.asStringListOrNull(): List<String>? {
        return when (this) {
            is JsonArray -> this.mapNotNull { it.asStringOrNull() }
            is JsonPrimitive -> this.content
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            else -> null
        }
    }

    private val extractionToolNames = setOf(
        "extract_invoice",
        "extract_bill",
        "extract_receipt",
        "extract_expense"
    )

    private val extractionToolDocumentType = mapOf(
        "extract_invoice" to "INVOICE",
        "extract_bill" to "BILL",
        "extract_receipt" to "RECEIPT",
        "extract_expense" to "EXPENSE"
    )

    private fun extractionStepName(step: ProcessingStep): String? {
        return step.tool ?: step.action
    }

    private fun isExtractionStep(step: ProcessingStep): Boolean {
        val name = extractionStepName(step) ?: return false
        return name in extractionToolNames
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
        val obj = parseJsonObjectOrNull(extraction) ?: return ""
        return obj["extractedText"]?.jsonPrimitive?.content ?: ""
    }

    private fun extractConfidence(extraction: JsonElement?): Double? {
        val obj = parseJsonObjectOrNull(extraction) ?: return null
        return obj["confidence"]?.jsonPrimitive?.content?.toDoubleOrNull()
    }

    private fun normalizeExtraction(extraction: JsonElement?): JsonElement? {
        if (extraction == null) return null
        return when (extraction) {
            is JsonObject, is JsonArray -> extraction
            is JsonPrimitive -> {
                if (!extraction.isString) {
                    null
                } else {
                    val parsed =
                        runCatching { json.parseToJsonElement(extraction.content) }.getOrNull()
                    when (parsed) {
                        is JsonObject, is JsonArray -> parsed
                        else -> null
                    }
                }
            }
        }
    }

    private fun parseJsonObjectOrNull(element: JsonElement?): JsonObject? {
        if (element == null) return null
        return when (element) {
            is JsonObject -> element
            is JsonPrimitive -> {
                if (!element.isString) {
                    null
                } else {
                    val parsed =
                        runCatching { json.parseToJsonElement(element.content) }.getOrNull()
                    parsed as? JsonObject
                }
            }

            else -> null
        }
    }
}
