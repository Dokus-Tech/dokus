package tech.dokus.backend.worker.models

import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.features.ai.judgment.JudgmentOutcome

/**
 * Result from document processing (from either pipeline).
 */
internal data class ProcessingResult(
    val extractedData: ExtractedDocumentData,
    val documentType: DocumentType,
    val meetsThreshold: Boolean,
    val rawText: String,
    val confidence: Double,
    val judgmentInfo: JudgmentInfo? = null
)

/**
 * Judgment information from 5-Layer pipeline.
 */
internal data class JudgmentInfo(
    val outcome: JudgmentOutcome,
    val wasCorrected: Boolean,
    val issues: List<String>
)
