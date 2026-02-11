package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.validation.FinancialExtractionAuditor
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.documentProcessingSubGraph(
    aiConfig: AIConfig,
    documentFetcher: DocumentFetcher,
    tools: List<Tool<*, *>>
): AIAgentSubgraphDelegate<AcceptDocumentInput, DocumentAiProcessingResult> {
    return subgraph(name = "document-processing") {
        val classificationKey = createStorageKey<ClassificationResult>("classification-result")

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
        val auditAndWrap by node<FinancialExtractionResult, DocumentAiProcessingResult>("audit-extraction") { extraction ->
            val classification = storage.getValue(classificationKey)
            val auditReport = FinancialExtractionAuditor.audit(extraction)
            DocumentAiProcessingResult(
                classification = classification,
                extraction = extraction,
                auditReport = auditReport
            )
        }

        nodeStart then prepare then classify then storeClassification then prepareExtractionInput then extract then auditAndWrap then nodeFinish
    }
}
