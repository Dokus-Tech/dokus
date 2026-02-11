package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.graph.nodes.DirectionResolutionResolver
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.AuditReport
import tech.dokus.features.ai.validation.CheckType
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.validation.FinancialExtractionAuditor
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.documentProcessingSubGraph(
    aiConfig: AIConfig,
    documentFetcher: DocumentFetcher,
    tools: List<Tool<*, *>>
): AIAgentSubgraphDelegate<AcceptDocumentInput, DocumentAiProcessingResult> {
    return subgraph(name = "document-processing") {
        val tenantKey = createStorageKey<Tenant>("tenant-context")
        val associatedNamesKey = createStorageKey<List<String>>("associated-person-names")
        val classificationKey = createStorageKey<ClassificationResult>("classification-result")

        val storeInputContext by node<AcceptDocumentInput, AcceptDocumentInput>("store-input-context") { input ->
            storage.set(tenantKey, input.tenant)
            storage.set(associatedNamesKey, input.associatedPersonNames)
            input
        }
        val prepare by documentPreparationSubGraph<AcceptDocumentInput>(documentFetcher)
        val classify by classifyDocumentSubGraph(aiConfig, tools)
        val storeClassification by node<ClassificationResult, ClassificationResult>("store-classification") { result ->
            storage.set(classificationKey, result)
            result
        }
        val prepareExtractionInput by node<ClassificationResult, ExtractDocumentInput>("prepare-extraction") { input ->
            ExtractDocumentInput(input.documentType, input.language)
        }
        val extract by financialExtractionSubGraph(aiConfig, tools)
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

        nodeStart then storeInputContext then prepare then classify then storeClassification then prepareExtractionInput then extract then resolveDirection then auditAndWrap then nodeFinish
    }
}

private data class ResolvedExtraction(
    val extraction: FinancialExtractionResult,
    val directionResolution: tech.dokus.features.ai.models.DirectionResolution
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
