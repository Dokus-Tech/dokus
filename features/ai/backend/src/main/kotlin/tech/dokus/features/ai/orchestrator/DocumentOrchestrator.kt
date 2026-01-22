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
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.enums.ContactLinkPolicy
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
import tech.dokus.features.ai.prompts.Prompt
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
    private val linkingPolicy: ContactLinkPolicy = ContactLinkPolicy.VatOnly,
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
            prompt = orchestratorSystemPrompt().value,
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
        if (parsed == null) {
            val fallback = buildFallbackOutputFromTrace(traceCollector.snapshot())
            if (fallback != null) {
                val fallbackTool = findLatestExtractionTool(traceCollector.snapshot())
                traceCollector.record(
                    action = "orchestrator_output_parse_failed",
                    tool = "document-orchestrator",
                    durationMs = 0,
                    input = null,
                    output = null,
                    notes = "using_extraction_trace_fallback, sourceTool=${fallbackTool ?: "unknown"}"
                )
                val persisted = ensureExtractionPersisted(
                    documentId = documentId,
                    tenantId = tenantId,
                    runId = runId,
                    output = fallback,
                    storeCalled = storeCalled,
                    storeSucceeded = storeSucceeded,
                    traceSink = traceCollector,
                    storeHandler = baseStoreHandler
                )
                if (!persisted) {
                    return OrchestratorResult.Failed(
                        reason = "Extraction completed but could not be persisted",
                        stage = "store_extraction",
                        auditTrail = traceCollector.snapshot()
                    )
                }

                return toResult(fallback, traceCollector.snapshot())
            }

            logger.error("Failed to parse orchestrator response: {}", rawResponse.take(1000))
            return OrchestratorResult.Failed(
                reason = "Failed to parse orchestrator output",
                stage = "orchestrator",
                auditTrail = traceCollector.snapshot()
            )
        }

        val persisted = ensureExtractionPersisted(
            documentId = documentId,
            tenantId = tenantId,
            runId = runId,
            output = parsed,
            storeCalled = storeCalled,
            storeSucceeded = storeSucceeded,
            traceSink = traceCollector,
            storeHandler = baseStoreHandler
        )
        if (!persisted && parsed.extraction != null && parsed.documentType != null) {
            logger.error(
                "Orchestrator produced extraction but persistence failed: documentId={}, runId={}",
                documentId,
                runId ?: "unknown"
            )
            return OrchestratorResult.Failed(
                reason = "Extraction completed but could not be persisted",
                stage = "store_extraction",
                auditTrail = traceCollector.snapshot()
            )
        }

        return toResult(parsed, traceCollector.snapshot())
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

    // =========================================================================
    // Prompting
    // =========================================================================

    private fun orchestratorSystemPrompt(): Prompt = Prompt(
        """
        You are the Dokus document processing orchestrator.
        You must solve the task by calling tools. Do not guess.

        Core rules:
        - Always use tools for document understanding, extraction, and validation.
        - If a core tool fails (images, classification, extraction, validation, store_extraction), return status="failed".
        - Non-critical tools (store_chunks, index_as_example) may fail without failing the run; include the error in "issues".
        - If classification is UNKNOWN or confidence < 0.5, return status="needs_review".
        - If overall confidence is below ${AUTO_CONFIRM_THRESHOLD}, return status="needs_review".
        - Max corrections: $MAX_CORRECTIONS attempts.
        - Output ONLY valid JSON (no markdown, no <think> tags).
        - Never include placeholders like "..." or "…". Always return concrete JSON values.

        Tool usage notes:
        - get_document_images returns lines "Page N: <image_id>". Use the IDs as-is.
        - If maxPages or dpi are provided in the task, pass them to get_document_images.
        - see_document and extract_* tools return a JSON section. Use that JSON.
        - find_similar_document may return an extraction example. Use it to re-run extraction once.
        - verify_totals / validate_iban / validate_ogm / lookup_company are validation tools.
        - generate_description / generate_keywords should be used after extraction.
        - After success (or needs_review with extraction), call store_extraction with runId, documentType,
          extraction, description, keywords, confidence, rawText, and a LinkDecision payload.
        - You MUST call store_extraction whenever you have any extraction data, even if status="needs_review".
        ${linkPolicyPrompt()}
        - Provide linkDecision fields:
          linkDecisionType = AUTO_LINK | SUGGEST | NONE
          linkDecisionContactId (if applicable)
          linkDecisionReason (short, human-readable)
          linkDecisionConfidence (only for SUGGEST)
          linkDecisionEvidence (JSON string with evidence fields):
            vatExtracted, vatValid, vatMatched, cbeExists, ibanMatched, nameSimilarity, addressMatched, ambiguityCount
        - If you created a contact, set contactCreated=true in store_extraction.
        - For RAG indexing, call prepare_rag_chunks -> embed_text for each chunk -> store_chunks with runId.
        - If you can resolve a contact, use lookup_contact then create_contact if missing, and include VAT evidence.

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
    """
    )

    private fun linkPolicyPrompt(): Prompt {
        return when (linkingPolicy) {
            ContactLinkPolicy.VatOnly -> Prompt("""
        - LinkDecision policy (VAT-only):
          AUTO_LINK only when VAT is valid AND exact VAT match (no ambiguity).
          If VAT missing/invalid, NEVER auto-link; use SUGGEST or NONE.
            """)

            ContactLinkPolicy.VatOrStrongSignals -> Prompt("""
        - LinkDecision policy (VAT or strong signals):
          AUTO_LINK when VAT is valid AND exact VAT match (no ambiguity),
          OR when strong multi-signal evidence is present:
            nameSimilarity >= 0.93, ibanMatched=true, addressMatched=true, ambiguityCount=1.
          If VAT missing/invalid and strong signals are not met, NEVER auto-link; use SUGGEST or NONE.
            """)
        }
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
        val extraction = output.extraction ?: return false
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
                linkDecisionType = null,
                linkDecisionContactId = null,
                linkDecisionReason = null,
                linkDecisionConfidence = null,
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

    private fun buildFallbackOutputFromTrace(
        auditTrail: List<ProcessingStep>
    ): OrchestratorAgentOutput? {
        val extractionStep = auditTrail.lastOrNull { step ->
            step.tool in extractionToolNames && step.output != null
        } ?: return null

        val sourceTool = extractionStep.tool
        val documentType = sourceTool?.let { toolName ->
            extractionToolDocumentType[toolName]
        } ?: return null

        val extraction = extractionStep.output ?: return null
        val confidence = extractConfidence(extraction)
        val rawText = extractRawText(extraction)

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
            contactId = null,
            contactCreated = null,
            issues = listOf(
                "Orchestrator output parse failed; persisted extraction output",
                "Fallback source tool: ${sourceTool ?: "unknown"}"
            ),
            reason = "Orchestrator output parse failed (fallback source: ${sourceTool ?: "unknown"})"
        )
    }

    private fun findLatestExtractionTool(auditTrail: List<ProcessingStep>): String? {
        return auditTrail.lastOrNull { step ->
            step.tool in extractionToolNames && step.output != null
        }?.tool
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
                    val parsed = runCatching { json.parseToJsonElement(extraction.content) }.getOrNull()
                    when (parsed) {
                        is JsonObject, is JsonArray -> parsed
                        else -> null
                    }
                }
            }
            else -> null
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
                    val parsed = runCatching { json.parseToJsonElement(element.content) }.getOrNull()
                    parsed as? JsonObject
                }
            }
            else -> null
        }
    }
}
