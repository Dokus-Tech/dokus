package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.agent.entity.AIAgentStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.ai.graph.sub.extraction.financial.extractCreditNoteSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractInvoiceSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractBankStatementSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractCsvBankStatementSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractProFormaSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractPurchaseOrderSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractQuoteSubGraph
import tech.dokus.features.ai.graph.sub.extraction.financial.extractReceiptSubGraph
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.financialExtractionSubGraph(
    aiConfig: AIConfig,
    csvBytesKey: AIAgentStorageKey<ByteArray>? = null,
): AIAgentSubgraphDelegate<ExtractDocumentInput, FinancialExtractionResult> {
    return subgraph(name = "financial-extraction") {
        val extractInvoice by extractInvoiceSubGraph(aiConfig)
        val extractCreditNote by extractCreditNoteSubGraph(aiConfig)
        val extractQuote by extractQuoteSubGraph(aiConfig)
        val extractProForma by extractProFormaSubGraph(aiConfig)
        val extractPurchaseOrder by extractPurchaseOrderSubGraph(aiConfig)
        val extractReceipt by extractReceiptSubGraph(aiConfig)
        val extractBankStatement by extractBankStatementSubGraph(aiConfig)

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
        edge(nodeStart forwardTo extractCreditNote onCondition { it.documentType == DocumentType.CreditNote })
        edge(nodeStart forwardTo extractQuote onCondition { it.documentType == DocumentType.Quote })
        edge(nodeStart forwardTo extractProForma onCondition { it.documentType == DocumentType.ProForma })
        edge(nodeStart forwardTo extractPurchaseOrder onCondition { it.documentType == DocumentType.PurchaseOrder })
        edge(nodeStart forwardTo extractReceipt onCondition { it.documentType == DocumentType.Receipt })

        if (csvBytesKey != null) {
            val extractCsvBankStatement by extractCsvBankStatementSubGraph(aiConfig, csvBytesKey)

            edge(nodeStart forwardTo extractCsvBankStatement onCondition {
                it.documentType == DocumentType.BankStatement && it.contentType == "text/csv"
            })
            edge(nodeStart forwardTo extractBankStatement onCondition {
                it.documentType == DocumentType.BankStatement && it.contentType != "text/csv"
            })
            edge(extractCsvBankStatement forwardTo nodeFinish)
        } else {
            edge(nodeStart forwardTo extractBankStatement onCondition {
                it.documentType == DocumentType.BankStatement
            })
        }

        edge(extractInvoice forwardTo nodeFinish)
        edge(extractCreditNote forwardTo nodeFinish)
        edge(extractQuote forwardTo nodeFinish)
        edge(extractProForma forwardTo nodeFinish)
        edge(extractPurchaseOrder forwardTo nodeFinish)
        edge(extractReceipt forwardTo nodeFinish)
        edge(extractBankStatement forwardTo nodeFinish)
    }
}
