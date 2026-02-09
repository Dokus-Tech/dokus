package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.ConditionResult
import ai.koog.agents.ext.agent.subgraphWithRetrySimple
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.processing.DocumentProcessingConstants
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
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.ExtractDocumentInput
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.confidenceScore
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.validation.FinancialExtractionAuditor
import tech.dokus.foundation.backend.config.AIConfig
import java.util.*

@Serializable
data class AcceptDocumentInput(
    override val documentId: DocumentId,
    override val tenant: Tenant
) : InputWithDocumentId, InputWithTenantContext

fun acceptDocumentGraph(
    aiConfig: AIConfig,
    _registries: List<ToolRegistry>,
    documentFetcher: DocumentFetcher,
): AIAgentGraphStrategy<AcceptDocumentInput, DocumentAiProcessingResult> {
    return strategy<AcceptDocumentInput, DocumentAiProcessingResult>("accept-document-graph") {
        val autoConfirmThreshold = DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD

        val processWithRetry by subgraphWithRetrySimple<AcceptDocumentInput, DocumentAiProcessingResult>(
            name = "process-document-with-retry",
            maxRetries = 1,
            strict = false,
            conditionDescription = buildString {
                appendLine("Auto-confirm only if:")
                appendLine("- classification confidence >= $autoConfirmThreshold")
                appendLine("- extraction confidence >= $autoConfirmThreshold")
                appendLine("- validation has no critical issues")
                appendLine()
                appendLine("If uncertain, prefer UNKNOWN and nulls over guessing.")
                appendLine("If feedback is provided, correct those fields.")
            },
            condition = { result ->
                val classificationConfidence = result.classification.confidence
                val extractionConfidence = result.extraction.confidenceScore()
                val meetsConfidence =
                    classificationConfidence >= autoConfirmThreshold && extractionConfidence >= autoConfirmThreshold
                val isValid = result.auditReport.isValid

                if (meetsConfidence && isValid) {
                    ConditionResult.Approve
                } else {
                    ConditionResult.Reject(
                        buildRetryFeedback(
                            result = result,
                            threshold = autoConfirmThreshold
                        )
                    )
                }
            }
        ) {
            val classificationKey = createStorageKey<ClassificationResult>("classification-result")

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

            val prepareClassifyInput by node<AcceptDocumentInput, ClassifyDocumentInput>("prepare-classify") { input ->
                ClassifyDocumentInput(input.documentId, input.tenant)
            }
            val storeClassification by node<ClassificationResult, ClassificationResult>("store-classification") { result ->
                storage.set(classificationKey, result)
                result
            }
            val prepareExtractionInput by node<ClassificationResult, ExtractDocumentInput>("prepare-extraction") { input ->
                ExtractDocumentInput(input.documentType, input.language)
            }

            val unsupported by node<ExtractDocumentInput, FinancialExtractionResult>("unsupported-doc-type") { input ->
                FinancialExtractionResult.Unsupported(
                    documentType = input.documentType.name,
                    reason = "Unsupported or unknown document type"
                )
            }

            val auditAndWrap by node<FinancialExtractionResult, DocumentAiProcessingResult>("audit-extraction") { extraction ->
                val classification = storage.getValue(classificationKey)
                val auditReport = FinancialExtractionAuditor.audit(extraction)
                DocumentAiProcessingResult(
                    classification = classification,
                    extraction = extraction,
                    auditReport = auditReport
                )
            }

            edge(nodeStart forwardTo injectTenant)
            edge(injectTenant forwardTo injectImages)
            edge(injectImages forwardTo prepareClassifyInput)

            edge(prepareClassifyInput forwardTo classify)
            edge(classify forwardTo storeClassification)
            edge(storeClassification forwardTo prepareExtractionInput)

            edge(
                prepareExtractionInput forwardTo unsupported onCondition {
                    !it.documentType.supported || it.documentType == DocumentType.Unknown
                }
            )
            edge(unsupported forwardTo auditAndWrap)

            edge(prepareExtractionInput forwardTo extractInvoice onCondition { it.documentType == DocumentType.Invoice })
            edge(prepareExtractionInput forwardTo extractBill onCondition { it.documentType == DocumentType.Bill })
            edge(prepareExtractionInput forwardTo extractCreditNote onCondition { it.documentType == DocumentType.CreditNote })
            edge(prepareExtractionInput forwardTo extractQuote onCondition { it.documentType == DocumentType.Quote })
            edge(prepareExtractionInput forwardTo extractProForma onCondition { it.documentType == DocumentType.ProForma })
            edge(prepareExtractionInput forwardTo extractPurchaseOrder onCondition { it.documentType == DocumentType.PurchaseOrder })
            edge(prepareExtractionInput forwardTo extractReceipt onCondition { it.documentType == DocumentType.Receipt })

            edge(extractInvoice forwardTo auditAndWrap)
            edge(extractBill forwardTo auditAndWrap)
            edge(extractCreditNote forwardTo auditAndWrap)
            edge(extractQuote forwardTo auditAndWrap)
            edge(extractProForma forwardTo auditAndWrap)
            edge(extractPurchaseOrder forwardTo auditAndWrap)
            edge(extractReceipt forwardTo auditAndWrap)

            edge(auditAndWrap forwardTo nodeFinish)
        }

        edge(nodeStart forwardTo processWithRetry)
        edge(processWithRetry forwardTo nodeFinish)
    }
}

private fun buildRetryFeedback(
    result: DocumentAiProcessingResult,
    threshold: Double
): String {
    val lines = mutableListOf<String>()

    if (result.extraction is FinancialExtractionResult.Unsupported) {
        lines += "Document type is unsupported or UNKNOWN. Re-check the header and party roles."
    }

    val classificationConfidence = result.classification.confidence
    if (classificationConfidence < threshold) {
        lines += "Classification confidence ${formatConfidence(classificationConfidence)} below $threshold."
        lines += "Re-check whether the tenant is seller (INVOICE) or buyer (BILL)."
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

    return if (lines.isEmpty()) {
        "Please re-check the document and correct any uncertain fields."
    } else {
        lines.joinToString("\n")
    }
}

private fun formatConfidence(value: Double): String =
    String.format(Locale.US, "%.2f", value)
