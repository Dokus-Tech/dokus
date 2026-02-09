package tech.dokus.features.ai.models

import tech.dokus.domain.enums.ProcessingOutcome
import tech.dokus.domain.processing.DocumentProcessingConstants

fun DocumentAiProcessingResult.toProcessingOutcome(
    threshold: Double = DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD
): ProcessingOutcome {
    val classificationConfidence = classification.confidence
    val extractionConfidence = extraction.confidenceScore()
    val meetsConfidence = classificationConfidence >= threshold && extractionConfidence >= threshold
    val isValid = auditReport.isValid
    return if (meetsConfidence && isValid) {
        ProcessingOutcome.AutoConfirmEligible
    } else {
        ProcessingOutcome.ManualReviewRequired
    }
}
