package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentTypeCategory
import tech.dokus.domain.enums.category
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.features.ai.graph.nodes.documentImagesInjectorNode
import tech.dokus.features.ai.graph.nodes.tenantContextInjectorNode
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.graph.sub.ClassifyDocumentInput
import tech.dokus.features.ai.graph.sub.classifyDocumentSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractInvoiceSubGraph
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.foundation.backend.config.AIConfig

@Serializable
data class AcceptDocumentInput(
    override val documentId: DocumentId,
    override val tenant: Tenant
) : InputWithDocumentId, InputWithTenantContext

fun acceptDocumentGraph(
    aiConfig: AIConfig,
    registries: List<ToolRegistry>,
    documentFetcher: DocumentFetcher,
): AIAgentGraphStrategy<AcceptDocumentInput, FinancialDocumentDto> {
    return strategy<AcceptDocumentInput, FinancialDocumentDto>("accept-document-graph") {
        val godRegistry = ToolRegistry { tools(registries.flatMap { it.tools }) }

        val classify by classifyDocumentSubGraph(aiConfig)
        val injectImages by documentImagesInjectorNode<AcceptDocumentInput>(documentFetcher)
        val injectTenant by tenantContextInjectorNode<AcceptDocumentInput>()

        val extractInvoiceSubGraph by extractInvoiceSubGraph(aiConfig)

        // Transform AcceptDocumentInput → ClassifyDocumentInput
        val prepareClassifyInput by node<AcceptDocumentInput, ClassifyDocumentInput>("prepare-classify") { input ->
            ClassifyDocumentInput(input.documentId, input.tenant)
        }
        val prepareExtractionInput by node<ClassificationResult, ExtractDocumentInput> { input ->
            ExtractDocumentInput(input.documentType, input.language)
        }

        // Context setup
        edge(nodeStart forwardTo injectTenant)
        edge(injectTenant forwardTo injectImages)
        edge(injectImages forwardTo prepareClassifyInput)

        // Classification
        edge(prepareClassifyInput forwardTo classify)

        // Extraction
        edge(classify forwardTo prepareExtractionInput)
        edge(prepareExtractionInput forwardTo extractInvoiceSubGraph onCondition { it.documentType == DocumentType.Invoice })

        edge(extractFinancialDocument forwardTo nodeFinish)
    }
}