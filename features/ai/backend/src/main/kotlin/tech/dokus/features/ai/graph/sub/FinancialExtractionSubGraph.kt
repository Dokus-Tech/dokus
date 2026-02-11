package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.tools.Tool
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.ai.graph.sub.extraction.financial.extractCreditNoteSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractInvoiceSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractProFormaSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractPurchaseOrderSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractQuoteSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractReceiptSubGraph
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.financialExtractionSubGraph(
    aiConfig: AIConfig,
    tools: List<Tool<*, *>>
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult> {
    return subgraph(name = "financial-extraction") {
        val extractInvoice by extractInvoiceSubGraph(aiConfig, tools)
        val extractCreditNote by extractCreditNoteSubGraph(aiConfig)
        val extractQuote by extractQuoteSubGraph(aiConfig)
        val extractProForma by extractProFormaSubGraph(aiConfig)
        val extractPurchaseOrder by extractPurchaseOrderSubGraph(aiConfig)
        val extractReceipt by extractReceiptSubGraph(aiConfig)

        val unsupported by node<ExtractDocumentInput, FinancialExtractionResult>("unsupported-doc-type") { input ->
            FinancialExtractionResult.Unsupported(
                documentType = input.documentType.name,
                reason = "Unsupported or unknown document type"
            )
        }

        edge(
            nodeStart forwardTo unsupported
                onCondition { !it.documentType.supported || it.documentType == DocumentType.Unknown }
        )
        edge(unsupported forwardTo nodeFinish)

        edge(nodeStart forwardTo extractInvoice onCondition { it.documentType == DocumentType.Invoice })
        // BILL is treated as invoice semantics with inbound direction resolved deterministically later.
        edge(nodeStart forwardTo extractInvoice onCondition { it.documentType == DocumentType.Bill })
        edge(nodeStart forwardTo extractCreditNote onCondition { it.documentType == DocumentType.CreditNote })
        edge(nodeStart forwardTo extractQuote onCondition { it.documentType == DocumentType.Quote })
        edge(nodeStart forwardTo extractProForma onCondition { it.documentType == DocumentType.ProForma })
        edge(nodeStart forwardTo extractPurchaseOrder onCondition { it.documentType == DocumentType.PurchaseOrder })
        edge(nodeStart forwardTo extractReceipt onCondition { it.documentType == DocumentType.Receipt })

        edge(extractInvoice forwardTo nodeFinish)
        edge(extractCreditNote forwardTo nodeFinish)
        edge(extractQuote forwardTo nodeFinish)
        edge(extractProForma forwardTo nodeFinish)
        edge(extractPurchaseOrder forwardTo nodeFinish)
        edge(extractReceipt forwardTo nodeFinish)
    }
}
