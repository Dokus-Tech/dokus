package tech.dokus.backend.worker.models

import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.ExtractedDocumentData

/**
 * Result from document processing via the orchestrator.
 */
internal data class ProcessingResult(
    val extractedData: ExtractedDocumentData?,
    val documentType: DocumentType,
    val meetsThreshold: Boolean,
    val rawText: String,
    val confidence: Double,
    val judgmentInfo: JudgmentInfo? = null,
    /** Contact ID resolved by orchestrator (existing or newly created) */
    val contactId: String? = null,
    /** Whether a new contact was created during this processing run */
    val contactCreated: Boolean = false
)

/**
 * Judgment/validation information from orchestrator.
 */
internal data class JudgmentInfo(
    val outcome: String,
    val wasCorrected: Boolean,
    val issues: List<String>
)
