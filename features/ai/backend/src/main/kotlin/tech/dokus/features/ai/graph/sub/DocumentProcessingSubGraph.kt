package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.graph.nodes.DirectionResolutionResolver
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.ResolvedExtraction
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.validation.FinancialExtractionAuditor
import tech.dokus.features.ai.validation.counterpartyInvariantCheck
import tech.dokus.features.ai.validation.mergeAudit
import tech.dokus.features.ai.validation.rawVatInvariantCheck
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.documentProcessingSubGraph(
    aiConfig: AIConfig,
    documentFetcher: DocumentFetcher,
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
        // Validation and lookup helpers run deterministically after extraction. Exposing them
        // here causes the vision model to loop on repeated tool calls and resend the full
        // multimodal prompt on every round-trip.
        val classify by classifyDocumentSubGraph(aiConfig)
        val storeClassification by node<ClassificationResult, ClassificationResult>("store-classification") { result ->
            storage.set(classificationKey, result)
            result
        }
        val prepareExtractionInput by node<ClassificationResult, ExtractDocumentInput>("prepare-extraction") { input ->
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

        nodeStart then storeInputContext then prepare then classify then storeClassification then prepareExtractionInput then extract then resolveDirection then auditAndWrap then nodeFinish
    }
}
