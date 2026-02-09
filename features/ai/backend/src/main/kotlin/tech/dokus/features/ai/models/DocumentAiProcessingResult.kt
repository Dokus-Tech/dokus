package tech.dokus.features.ai.models

import kotlinx.serialization.Serializable
import tech.dokus.features.ai.graph.sub.ClassificationResult
import tech.dokus.features.ai.validation.AuditReport

@Serializable
data class DocumentAiProcessingResult(
    val classification: ClassificationResult,
    val extraction: FinancialExtractionResult,
    val auditReport: AuditReport
)
