package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.ConditionResult
import ai.koog.agents.ext.agent.subgraphWithRetrySimple
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.processing.DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD
import tech.dokus.features.ai.graph.nodes.InputWithDocumentId
import tech.dokus.features.ai.graph.nodes.InputWithTenantContext
import tech.dokus.features.ai.graph.nodes.InputWithUserFeedback
import tech.dokus.features.ai.graph.sub.documentProcessingSubGraph
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.FinancialExtractionResult
import tech.dokus.features.ai.models.confidenceScore
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.foundation.backend.config.AIConfig
import java.util.*

@Serializable
data class AcceptDocumentInput(
    override val documentId: DocumentId,
    override val tenant: Tenant,
    override val associatedPersonNames: List<String> = emptyList(),
    override val userFeedback: String? = null,
    override val maxPagesOverride: Int? = null,
    override val dpiOverride: Int? = null
) : InputWithDocumentId, InputWithTenantContext, InputWithUserFeedback

fun acceptDocumentGraph(
    aiConfig: AIConfig,
    registry: ToolRegistry,
    documentFetcher: DocumentFetcher,
): AIAgentGraphStrategy<AcceptDocumentInput, DocumentAiProcessingResult> {
    return strategy<AcceptDocumentInput, DocumentAiProcessingResult>("accept-document-graph") {
        val confirmThreshold = AUTO_CONFIRM_CONFIDENCE_THRESHOLD

        val processWithRetry by subgraphWithRetrySimple<AcceptDocumentInput, DocumentAiProcessingResult>(
            name = "process-document-with-retry",
            maxRetries = 2,
            strict = false,
            conditionDescription = buildString {
                appendLine("Auto-confirm only if:")
                appendLine("- classification confidence >= $confirmThreshold")
                appendLine("- extraction confidence >= $confirmThreshold")
                appendLine("- validation has no critical issues")
                appendLine()
                appendLine("If uncertain, prefer UNKNOWN and nulls over guessing.")
                appendLine("If feedback is provided, correct those fields.")
            },
            condition = { result ->
                val classificationConfidence = result.classification.confidence
                val extractionConfidence = result.extraction.confidenceScore()
                val meetsConfidence =
                    classificationConfidence >= confirmThreshold && extractionConfidence >= confirmThreshold
                val isValid = result.auditReport.isValid
                val hasSameTenantDualPartyAmbiguity = hasSameTenantDualPartyAmbiguity(result)

                if (meetsConfidence && isValid && !hasSameTenantDualPartyAmbiguity) {
                    ConditionResult.Approve
                } else {
                    ConditionResult.Reject(buildRetryFeedback(result, confirmThreshold))
                }
            }
        ) {
            val process by documentProcessingSubGraph(aiConfig, documentFetcher, registry.tools)
            nodeStart then process then nodeFinish
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
        lines += "Re-check the legal document type only (Invoice/CreditNote/etc); direction is resolved later."
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
