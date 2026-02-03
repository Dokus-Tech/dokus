package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Tenant
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.features.ai.graph.nodes.documentImagesInjectorNode
import tech.dokus.features.ai.graph.nodes.tenantContextInjectorNode
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.graph.sub.ClassifyDocumentInput
import tech.dokus.features.ai.graph.sub.classifyDocumentSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractBillSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractCreditNoteSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractInvoiceSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractProFormaSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractPurchaseOrderSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractQuoteSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractReceiptSubGraph
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.ExtractionResult
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
): AIAgentGraphStrategy<AcceptDocumentInput, ExtractionResult> {
    return strategy<AcceptDocumentInput, ExtractionResult>("accept-document-graph") {
        val godRegistry = ToolRegistry { tools(registries.flatMap { it.tools }) }

        val classify by classifyDocumentSubGraph(aiConfig)
        val injectImages by documentImagesInjectorNode<AcceptDocumentInput>(documentFetcher)
        val injectTenant by tenantContextInjectorNode<AcceptDocumentInput>()

        val extractInvoice by extractInvoiceSubGraph(aiConfig)
        val extractBill by extractBillSubGraph(aiConfig)
        val extractCreditNote by extractCreditNoteSubGraph(aiConfig)
        val extractQuote by extractQuoteSubGraph(aiConfig)
        val extractProForma by extractProFormaSubGraph(aiConfig)
        val extractPurchaseOrder by extractPurchaseOrderSubGraph(aiConfig)
        val extractReceipt by extractReceiptSubGraph(aiConfig)

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

        // Handle unsupported case
        val unsupported by node<ExtractDocumentInput, ExtractionResult>("unsupported-doc-type") { input ->
            // TODO: create a lightweight DTO with warning that the type is not yet supported.
            error("Unsupported document type: ${input.documentType}")
        }
        edge(prepareExtractionInput forwardTo unsupported onCondition { !it.documentType.supported })
        edge(unsupported forwardTo nodeFinish)

        // Extraction
        edge(classify forwardTo prepareExtractionInput)
        edge(prepareExtractionInput forwardTo extractInvoice onCondition { it.documentType == DocumentType.Invoice })
        edge(prepareExtractionInput forwardTo extractBill onCondition { it.documentType == DocumentType.Bill })
        edge(prepareExtractionInput forwardTo extractCreditNote onCondition { it.documentType == DocumentType.CreditNote })
        edge(prepareExtractionInput forwardTo extractQuote onCondition { it.documentType == DocumentType.Quote })
        edge(prepareExtractionInput forwardTo extractProForma onCondition { it.documentType == DocumentType.ProForma })
        edge(prepareExtractionInput forwardTo extractPurchaseOrder onCondition { it.documentType == DocumentType.PurchaseOrder })
        edge(prepareExtractionInput forwardTo extractReceipt onCondition { it.documentType == DocumentType.Receipt })
    }
}