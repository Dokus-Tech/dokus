package tech.dokus.features.ai.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.config.installKoogEventLogging
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.graph.nodes.DirectionResolutionResolver
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.features.ai.graph.nodes.InputWithUserFeedback
import tech.dokus.features.ai.graph.nodes.documentImagesInjectorNode
import tech.dokus.features.ai.graph.nodes.tenantContextInjectorNode
import tech.dokus.features.ai.graph.nodes.userFeedbackInjectorNode
import tech.dokus.features.ai.graph.purposeEnrichmentGraph
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.graph.sub.classifyDocumentSubGraph
import tech.dokus.features.ai.graph.sub.documentPreparationSubGraph
import tech.dokus.features.ai.graph.sub.financialExtractionSubGraph
import tech.dokus.features.ai.models.DirectionResolution
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.PurposeEnrichmentInput
import tech.dokus.features.ai.models.PurposeEnrichmentResult
import tech.dokus.features.ai.models.toPeppolProcessingResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.CheckType
import tech.dokus.features.ai.validation.FinancialExtractionAuditor
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.utils.loggerFor

class DocumentProcessingAgent(
    private val executor: PromptExecutor,
    private val aiConfig: AIConfig,
    private val documentFetcher: DocumentFetcher
) {
    private val logger = loggerFor()

    @OptIn(ExperimentalAgentsApi::class)
    suspend fun process(input: AcceptDocumentInput): DocumentAiProcessingResult {
        parsePeppolStructured(input)?.let { return it }

        val classification = try {
            runClassification(input)
        } catch (exception: Exception) {
            logger.warn("Document classification failed: documentId=${input.documentId}", exception)
            return classificationFailureResult(input, exception)
        }

        return try {
            runExtraction(DocumentExtractionInput.from(input, classification))
        } catch (exception: Exception) {
            logger.warn(
                "Document extraction failed: documentId=${input.documentId}, documentType=${classification.documentType}",
                exception
            )
            extractionFailureResult(input, classification, exception)
        }
    }

    @OptIn(ExperimentalAgentsApi::class)
    suspend fun enrichPurpose(input: PurposeEnrichmentInput): PurposeEnrichmentResult {
        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = ToolRegistry.EMPTY,
            strategy = purposeEnrichmentGraph(),
            agentConfig = AIAgentConfig(
                prompt = prompt("koog-purpose-enrichment") {
                    system("You render canonical document purpose labels.")
                },
                model = aiConfig.mode.asVisionModel,
                maxAgentIterations = aiConfig.mode.maxIterations,
            ),
            installFeatures = {
                installKoogEventLogging(
                    agentName = "purpose-enrichment",
                    enabled = aiConfig.koogEventLoggingEnabled
                )
            }
        )

        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }

    @OptIn(ExperimentalAgentsApi::class)
    private suspend fun runClassification(input: AcceptDocumentInput): ClassificationResult {
        val agent = createVisionAgent(
            agentName = "document-classification",
            promptId = "koog-document-classification",
            systemPrompt = "You classify document type and language from provided document pages.",
            strategy = classificationStrategy()
        )

        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }

    @OptIn(ExperimentalAgentsApi::class)
    private suspend fun runExtraction(input: DocumentExtractionInput): DocumentAiProcessingResult {
        val agent = createVisionAgent(
            agentName = "document-extraction",
            promptId = "koog-document-extraction",
            systemPrompt = "You extract structured financial data from provided document pages.",
            strategy = extractionStrategy()
        )

        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }

    private fun parsePeppolStructured(input: AcceptDocumentInput): DocumentAiProcessingResult? {
        if (input.sourceChannel != DocumentSource.Peppol || input.peppolStructuredSnapshotJson.isNullOrBlank()) {
            return null
        }

        return try {
            val draftData = json.decodeFromString<DocumentDraftData>(input.peppolStructuredSnapshotJson)
            draftData.toPeppolProcessingResult(input.peppolSnapshotVersion)
        } catch (exception: SerializationException) {
            logger.warn(
                "Failed to deserialize PEPPOL structured snapshot for documentId=${input.documentId}, falling back to vision extraction",
                exception
            )
            null
        }
    }

    @OptIn(ExperimentalAgentsApi::class)
    private inline fun <reified Input, reified Output> createVisionAgent(
        agentName: String,
        promptId: String,
        systemPrompt: String,
        strategy: AIAgentGraphStrategy<Input, Output>,
    ): AIAgent<Input, Output> {
        return AIAgent(
            promptExecutor = executor,
            toolRegistry = ToolRegistry.EMPTY,
            strategy = strategy,
            agentConfig = AIAgentConfig(
                prompt = prompt(promptId) { system(systemPrompt) },
                model = aiConfig.mode.asVisionModel,
                maxAgentIterations = aiConfig.mode.maxIterations,
            ),
            installFeatures = {
                installKoogEventLogging(
                    agentName = agentName,
                    enabled = aiConfig.koogEventLoggingEnabled
                )
            }
        )
    }

    private fun classificationStrategy(): AIAgentGraphStrategy<AcceptDocumentInput, ClassificationResult> {
        return strategy("document-classification") {
            val prepare by documentPreparationSubGraph<AcceptDocumentInput>(documentFetcher)
            val classify by classifyDocumentSubGraph(aiConfig)

            nodeStart then prepare then classify then nodeFinish
        }
    }

    private fun extractionStrategy(): AIAgentGraphStrategy<DocumentExtractionInput, DocumentAiProcessingResult> {
        return strategy("document-extraction") {
            val tenantKey = createStorageKey<Tenant>("tenant-context")
            val associatedNamesKey = createStorageKey<List<String>>("associated-person-names")
            val classificationKey = createStorageKey<ClassificationResult>("classification-result")

            val storeInputContext by node<DocumentExtractionInput, DocumentExtractionInput>("store-input-context") { input ->
                storage.set(tenantKey, input.tenant)
                storage.set(associatedNamesKey, input.associatedPersonNames)
                storage.set(classificationKey, input.classification)
                input
            }
            val injectTenant by tenantContextInjectorNode<DocumentExtractionInput>()
            val injectImages by documentImagesInjectorNode<DocumentExtractionInput>(documentFetcher)
            val injectUserFeedback by userFeedbackInjectorNode<DocumentExtractionInput>()
            val prepareExtractionInput by node<DocumentExtractionInput, ExtractDocumentInput>("prepare-extraction") { input ->
                ExtractDocumentInput(input.classification.documentType, input.classification.language)
            }
            val extract by financialExtractionSubGraph(aiConfig)
            val resolveDirection by node<FinancialExtractionResult, ResolvedExtraction>("resolve-direction") { extraction ->
                val tenant = storage.getValue(tenantKey)
                val associatedNames = storage.getValue(associatedNamesKey)
                val directionResolution = DirectionResolutionResolver.resolve(extraction, tenant, associatedNames)
                val counterpartyVat = DirectionResolutionResolver
                    .resolvedCounterpartyVat(extraction, directionResolution.direction)
                ResolvedExtraction(
                    extraction = extraction,
                    directionResolution = directionResolution.copy(counterpartyVat = counterpartyVat)
                )
            }
            val auditAndWrap by node<ResolvedExtraction, DocumentAiProcessingResult>("audit-extraction") { resolved ->
                val classification = storage.getValue(classificationKey)
                val tenant = storage.getValue(tenantKey)
                val baseAudit = FinancialExtractionAuditor.audit(resolved.extraction)
                val invariantCheck = counterpartyInvariantCheck(
                    tenantVat = tenant.vatNumber.normalized.takeIf { it.isNotBlank() },
                    counterpartyVat = resolved.directionResolution.counterpartyVat
                )
                val auditReport = mergeAudit(baseAudit, invariantCheck)
                DocumentAiProcessingResult(
                    classification = classification,
                    extraction = resolved.extraction,
                    directionResolution = resolved.directionResolution,
                    auditReport = auditReport
                )
            }

            nodeStart then storeInputContext then injectTenant then injectImages then injectUserFeedback then prepareExtractionInput then extract then resolveDirection then auditAndWrap then nodeFinish
        }
    }

    private fun classificationFailureResult(
        input: AcceptDocumentInput,
        exception: Exception,
    ): DocumentAiProcessingResult {
        return DocumentAiProcessingResult(
            classification = ClassificationResult(
                documentType = DocumentType.Unknown,
                confidence = 0.0,
                language = "unknown",
                reasoning = "Classification failed before a valid finish tool result was produced."
            ),
            extraction = FinancialExtractionResult.Unsupported(
                documentType = DocumentType.Unknown.name,
                reason = failureReason(stage = "classification", exception = exception)
            ),
            directionResolution = DirectionResolution(
                tenantVat = input.tenant.vatNumber.normalized.takeIf { it.isNotBlank() },
                reasoning = "Direction was not resolved because classification failed."
            ),
            auditReport = contractFailureAudit(stage = "classification", exception = exception)
        )
    }

    private fun extractionFailureResult(
        input: AcceptDocumentInput,
        classification: ClassificationResult,
        exception: Exception,
    ): DocumentAiProcessingResult {
        return DocumentAiProcessingResult(
            classification = classification,
            extraction = FinancialExtractionResult.Unsupported(
                documentType = classification.documentType.name,
                reason = failureReason(stage = "extraction", exception = exception)
            ),
            directionResolution = DirectionResolution(
                tenantVat = input.tenant.vatNumber.normalized.takeIf { it.isNotBlank() },
                reasoning = "Direction was not resolved because extraction failed."
            ),
            auditReport = contractFailureAudit(stage = "extraction", exception = exception)
        )
    }

    private fun contractFailureAudit(stage: String, exception: Exception): AuditReport {
        return AuditReport.fromChecks(
            listOf(
                AuditCheck.criticalFailure(
                    type = CheckType.AI_CONTRACT,
                    field = "finishTool",
                    message = "AI $stage failed to return a valid native finish tool call.",
                    hint = "Retry the run. Treat assistant text, raw JSON, repeated wrong tools, and invalid finish payloads as hard failures.",
                    expected = "native finish tool call",
                    actual = failureReason(stage, exception)
                )
            )
        )
    }

    private fun failureReason(stage: String, exception: Exception): String {
        val message = exception.message?.takeIf { it.isNotBlank() } ?: "no error message"
        return "$stage failed with ${exception::class.simpleName}: $message"
    }
}

@Serializable
private data class DocumentExtractionInput(
    override val documentId: DocumentId,
    override val tenant: Tenant,
    override val associatedPersonNames: List<String> = emptyList(),
    override val userFeedback: String? = null,
    override val maxPagesOverride: Int? = null,
    override val dpiOverride: Int? = null,
    val classification: ClassificationResult,
) : InputWithDocumentId, InputWithTenantContext, InputWithUserFeedback {
    companion object {
        fun from(
            input: AcceptDocumentInput,
            classification: ClassificationResult,
        ): DocumentExtractionInput {
            return DocumentExtractionInput(
                documentId = input.documentId,
                tenant = input.tenant,
                associatedPersonNames = input.associatedPersonNames,
                userFeedback = input.userFeedback,
                maxPagesOverride = input.maxPagesOverride,
                dpiOverride = input.dpiOverride,
                classification = classification,
            )
        }
    }
}

private data class ResolvedExtraction(
    val extraction: FinancialExtractionResult,
    val directionResolution: DirectionResolution
)

private fun counterpartyInvariantCheck(tenantVat: String?, counterpartyVat: String?): AuditCheck? {
    if (tenantVat == null || counterpartyVat == null) return null
    if (tenantVat != counterpartyVat) return null
    return AuditCheck.criticalFailure(
        type = CheckType.COUNTERPARTY_INTEGRITY,
        field = "counterpartyVat",
        message = "Counterparty VAT equals tenant VAT ($tenantVat)",
        hint = "Verify seller/buyer extraction and direction; counterparty must be a non-tenant entity",
        expected = "counterparty VAT != tenant VAT",
        actual = "$counterpartyVat == $tenantVat"
    )
}

private fun mergeAudit(base: AuditReport, invariantCheck: AuditCheck?): AuditReport {
    if (invariantCheck == null) return base
    val checks = if (base.checks.isEmpty()) listOf(invariantCheck) else base.checks + invariantCheck
    return AuditReport.fromChecks(checks)
}
