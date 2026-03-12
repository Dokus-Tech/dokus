package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.agent.ConditionResult
import ai.koog.agents.ext.agent.subgraphWithRetrySimple
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.processing.DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.features.ai.graph.nodes.DirectionResolutionResolver
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.features.ai.graph.nodes.InputWithUserFeedback
import tech.dokus.features.ai.graph.nodes.documentImagesInjectorNode
import tech.dokus.features.ai.graph.nodes.extractionTenantContextInjectorNode
import tech.dokus.features.ai.graph.nodes.tenantContextInjectorNode
import tech.dokus.features.ai.graph.nodes.userFeedbackInjectorNode
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.graph.sub.ClassifyDocumentInput
import tech.dokus.features.ai.graph.sub.classifyDocumentSubGraph
import tech.dokus.features.ai.graph.sub.financialExtractionSubGraph
import tech.dokus.features.ai.models.DirectionResolution
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.ResolvedExtraction
import tech.dokus.features.ai.models.confidenceScore
import tech.dokus.features.ai.models.toPeppolProcessingResult
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.validation.FinancialExtractionAuditor
import tech.dokus.features.ai.validation.counterpartyInvariantCheck
import tech.dokus.features.ai.validation.mergeAudit
import tech.dokus.features.ai.validation.rawVatInvariantCheck
import tech.dokus.foundation.backend.config.AIConfig
import java.util.*

@Serializable
data class AcceptDocumentInput(
    override val documentId: DocumentId,
    override val tenant: Tenant,
    val sourceChannel: DocumentSource = DocumentSource.Upload,
    val peppolStructuredSnapshotJson: String? = null,
    val peppolSnapshotVersion: Int? = null,
    override val associatedPersonNames: List<String> = emptyList(),
    override val userFeedback: String? = null,
    override val maxPagesOverride: Int? = null,
    override val dpiOverride: Int? = null
) : InputWithDocumentId, InputWithTenantContext, InputWithUserFeedback

fun acceptDocumentGraph(
    aiConfig: AIConfig,
    documentFetcher: DocumentFetcher,
): AIAgentGraphStrategy<AcceptDocumentInput, DocumentAiProcessingResult> {
    return strategy<AcceptDocumentInput, DocumentAiProcessingResult>("accept-document-parent") {
        val confirmThreshold = AUTO_CONFIRM_CONFIDENCE_THRESHOLD

        val visionRoute by subgraph<AcceptDocumentInput, DocumentAiProcessingResult>("vision-extraction") {
            val tenantKey = createStorageKey<Tenant>("tenant-context")
            val associatedNamesKey = createStorageKey<List<String>>("associated-person-names")
            val classificationKey = createStorageKey<ClassificationResult>("classification-result")

            // Captured before classification, used by cleanForExtraction to strip classification Q&A
            var preClassifyMessageCount = 0

            // --- Classification phase (runs once) ---

            val storeContext by node<AcceptDocumentInput, AcceptDocumentInput>("store-input-context") { input ->
                storage.set(tenantKey, input.tenant)
                storage.set(associatedNamesKey, input.associatedPersonNames)
                input
            }
            val injectTenant by tenantContextInjectorNode<AcceptDocumentInput>()
            val injectImages by documentImagesInjectorNode<AcceptDocumentInput>(documentFetcher)
            val injectFeedback by userFeedbackInjectorNode<AcceptDocumentInput>()
            val prepareClassify by node<AcceptDocumentInput, ClassifyDocumentInput>("prepare-classify") { input ->
                llm.readSession { preClassifyMessageCount = prompt.messages.size }
                ClassifyDocumentInput(input.documentId, input.tenant)
            }
            val classify by classifyDocumentSubGraph(aiConfig)
            val storeClassification by node<ClassificationResult, ClassificationResult>("store-classification") { result ->
                storage.set(classificationKey, result)
                result
            }
            val cleanForExtraction by node<ClassificationResult, ClassificationResult>("clean-for-extraction") { result ->
                llm.writeSession {
                    rewritePrompt { prompt -> prompt.copy(messages = prompt.messages.take(preClassifyMessageCount)) }
                }
                result
            }

            // --- Skip unsupported types (no extraction, no LLM tokens) ---

            val skipUnsupported by node<ClassificationResult, DocumentAiProcessingResult>("skip-unsupported") { result ->
                DocumentAiProcessingResult(
                    classification = result,
                    extraction = FinancialExtractionResult.Unsupported,
                    directionResolution = DirectionResolution(),
                    auditReport = AuditReport.EMPTY
                )
            }

            // --- Extraction phase (retries up to 2x) ---

            val extractionWithRetry by subgraphWithRetrySimple<ClassificationResult, DocumentAiProcessingResult>(
                name = "extraction-with-retry",
                maxRetries = 2,
                strict = false,
                conditionDescription = buildString {
                    appendLine("Auto-confirm only if:")
                    appendLine("- extraction confidence >= $confirmThreshold")
                    appendLine("- validation has no critical issues")
                    appendLine()
                    appendLine("If uncertain, prefer UNKNOWN and nulls over guessing.")
                    appendLine("If feedback is provided, correct those fields.")
                },
                condition = { result ->
                    val extractionConfidence = result.extraction.confidenceScore()
                    val isValid = result.auditReport.isValid
                    val hasDualPartyIssue = hasSameTenantDualPartyAmbiguity(result)

                    if (extractionConfidence >= confirmThreshold && isValid && !hasDualPartyIssue) {
                        ConditionResult.Approve
                    } else {
                        ConditionResult.Reject(buildExtractionRetryFeedback(result, confirmThreshold))
                    }
                }
            ) {
                val injectExtractionContext by extractionTenantContextInjectorNode<ClassificationResult>(
                    tenantKey, associatedNamesKey
                )
                val prepareExtraction by node<ClassificationResult, ExtractDocumentInput>("prepare-extraction") { input ->
                    ExtractDocumentInput(input.documentType, input.language)
                }
                val extract by financialExtractionSubGraph(aiConfig)
                val resolveDirection by node<FinancialExtractionResult, ResolvedExtraction>("resolve-direction") { extraction ->
                    val tenant = storage.getValue(tenantKey)
                    val associatedNames = storage.getValue(associatedNamesKey)
                    val tenantVat = tenant.vatNumber.normalized.takeIf { it.isNotBlank() }
                    val directionResolution = DirectionResolutionResolver.resolve(extraction, tenant, associatedNames)
                    val counterpartyVat = DirectionResolutionResolver
                        .resolvedCounterpartyVat(extraction, directionResolution.direction, tenantVat)
                    ResolvedExtraction(
                        extraction = extraction,
                        directionResolution = directionResolution.copy(counterpartyVat = counterpartyVat)
                    )
                }
                val auditAndWrap by node<ResolvedExtraction, DocumentAiProcessingResult>("audit-extraction") { resolved ->
                    val classification = storage.getValue(classificationKey)
                    val tenant = storage.getValue(tenantKey)
                    val tenantVat = tenant.vatNumber.normalized.takeIf { it.isNotBlank() }
                    val baseAudit = FinancialExtractionAuditor.audit(resolved.extraction)
                    val invariantCheck = counterpartyInvariantCheck(
                        tenantVat = tenantVat,
                        counterpartyVat = resolved.directionResolution.counterpartyVat
                    )
                    val rawVatCheck = rawVatInvariantCheck(
                        tenantVat = tenantVat,
                        rawMerchantOrSellerVat = (resolved.extraction as? FinancialExtractionResult.Receipt)
                            ?.data?.merchantVat?.normalized
                    )
                    val auditReport = mergeAudit(mergeAudit(baseAudit, invariantCheck), rawVatCheck)
                    DocumentAiProcessingResult(
                        classification = classification,
                        extraction = resolved.extraction,
                        directionResolution = resolved.directionResolution,
                        auditReport = auditReport
                    )
                }

                nodeStart then injectExtractionContext then prepareExtraction then extract then
                    resolveDirection then auditAndWrap then nodeFinish
            }

            nodeStart then storeContext then injectTenant then injectImages then injectFeedback then
                prepareClassify then classify then storeClassification then cleanForExtraction

            edge(cleanForExtraction forwardTo extractionWithRetry onCondition { it.documentType.supported })
            edge(cleanForExtraction forwardTo skipUnsupported onCondition { !it.documentType.supported })
            edge(extractionWithRetry forwardTo nodeFinish)
            edge(skipUnsupported forwardTo nodeFinish)
        }

        // --- PEPPOL route (deterministic, no LLM) ---

        val peppolStructuredExtractionSubGraph by subgraph(
            name = "peppol-structured-extraction-subgraph"
        ) {
            val processStructured by node<AcceptDocumentInput, DocumentAiProcessingResult>("parse-peppol-structured-snapshot") { input ->
                val snapshot = requireNotNull(input.peppolStructuredSnapshotJson) {
                    "PEPPOL structured snapshot is required for PEPPOL extraction route"
                }
                val draftData = try {
                    tech.dokus.domain.utils.json.decodeFromString<tech.dokus.domain.model.DocumentDraftData>(snapshot)
                } catch (e: SerializationException) {
                    throw IllegalStateException(
                        "Failed to deserialize PEPPOL structured snapshot (version=${input.peppolSnapshotVersion}); " +
                            "document ${input.documentId} will need vision extraction",
                        e
                    )
                }
                draftData.toPeppolProcessingResult(input.peppolSnapshotVersion)
            }
            nodeStart then processStructured then nodeFinish
        }

        edge(
            nodeStart forwardTo peppolStructuredExtractionSubGraph
                onCondition { input ->
                    input.sourceChannel == DocumentSource.Peppol &&
                        !input.peppolStructuredSnapshotJson.isNullOrBlank()
                }
        )
        edge(
            nodeStart forwardTo visionRoute
                onCondition { input ->
                    input.sourceChannel != DocumentSource.Peppol ||
                        input.peppolStructuredSnapshotJson.isNullOrBlank()
                }
        )
        edge(visionRoute forwardTo nodeFinish)
        edge(peppolStructuredExtractionSubGraph forwardTo nodeFinish)
    }
}

private fun buildExtractionRetryFeedback(
    result: DocumentAiProcessingResult,
    threshold: Double
): String {
    val lines = mutableListOf<String>()

    if (result.extraction is FinancialExtractionResult.Unsupported) {
        lines += "Document type is unsupported or UNKNOWN. Re-check the header and party roles."
    }

    val extractionConfidence = result.extraction.confidenceScore()
    if (extractionConfidence < threshold) {
        lines += "Extraction confidence ${formatConfidence(extractionConfidence)} below $threshold."
        lines += "Re-read totals, dates, IBAN, and references. Use null for unseen fields."
    }

    val critical = result.auditReport.criticalFailures
    if (critical.isNotEmpty()) {
        lines += "Critical validation issues:"
        critical.take(5).forEach { check ->
            lines += "- ${check.hint ?: check.message}"
        }
    }

    val warnings = result.auditReport.warnings
    if (warnings.isNotEmpty()) {
        lines += "Warnings:"
        warnings.take(5).forEach { check ->
            lines += "- ${check.hint ?: check.message}"
        }
    }

    if (hasSameTenantDualPartyAmbiguity(result)) {
        lines += "Seller and buyer both match tenant VAT."
        lines += "Re-extract parties using issuer block for seller and billed-to/client block for buyer."
        lines += "If one side cannot be proven, set that party to null instead of duplicating values."
    }

    return if (lines.isEmpty()) {
        "Please re-check the document and correct any uncertain fields."
    } else {
        lines.joinToString("\n")
    }
}

private fun hasSameTenantDualPartyAmbiguity(result: DocumentAiProcessingResult): Boolean {
    val tenantVat = result.directionResolution.tenantVat ?: return false

    return when (val extraction = result.extraction) {
        is FinancialExtractionResult.Invoice -> {
            val sellerVat = extraction.data.sellerVat?.normalized
            val buyerVat = extraction.data.buyerVat?.normalized
            sellerVat != null && buyerVat != null && sellerVat == tenantVat && buyerVat == tenantVat
        }
        is FinancialExtractionResult.CreditNote -> {
            val sellerVat = extraction.data.sellerVat?.normalized
            val buyerVat = extraction.data.buyerVat?.normalized
            sellerVat != null && buyerVat != null && sellerVat == tenantVat && buyerVat == tenantVat
        }
        else -> false
    }
}

private fun formatConfidence(value: Double): String = String.format(Locale.US, "%.2f", value)
