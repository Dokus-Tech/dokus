package tech.dokus.features.ai.orchestrator

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ExampleId
import tech.dokus.features.ai.models.ClassifiedDocumentType

/**
 * Result of document processing by the orchestrator.
 *
 * The orchestrator returns one of three outcomes:
 * - Success: Document was processed and extraction completed
 * - NeedsReview: Document requires human review before proceeding
 * - Failed: Processing failed and cannot be recovered
 */
@Serializable
sealed class OrchestratorResult {

    /**
     * Document was successfully processed.
     *
     * All fields have been extracted, validated, and enriched.
     * The document is ready to be persisted.
     */
    @Serializable
    data class Success(
        /** Classified document type */
        val documentType: ClassifiedDocumentType,

        /** Extracted data as JSON (type depends on documentType) */
        val extraction: JsonElement,

        /** Overall confidence score (0.0 - 1.0) */
        val confidence: Double,

        /** Raw text extracted from the document */
        val rawText: String,

        /** Human-readable description for UI: "Vendor — Item — €Amount" */
        val description: String,

        /** Keywords for search */
        val keywords: List<String>,

        /** Whether all validation checks passed */
        val validationPassed: Boolean,

        /** Number of self-corrections applied during processing */
        val correctionsApplied: Int,

        /** ID of the example used for few-shot learning, if any */
        val exampleUsed: ExampleId? = null,

        /** Resolved contact ID (existing or newly created) */
        val contactId: ContactId? = null,

        /** Whether a new contact was created during this run */
        val contactCreated: Boolean = false,

        /** Audit trail of processing steps */
        val auditTrail: List<ProcessingStep>
    ) : OrchestratorResult()

    /**
     * Document requires human review before proceeding.
     *
     * This happens when:
     * - Document type cannot be determined with confidence
     * - Extraction confidence is below threshold
     * - Validation failures that cannot be auto-corrected
     */
    @Serializable
    data class NeedsReview(
        /** Best-guess document type, if any */
        val documentType: ClassifiedDocumentType? = null,

        /** Partial extraction data, if any */
        val partialExtraction: JsonElement? = null,

        /** Human-readable reason for requiring review */
        val reason: String,

        /** List of specific issues found */
        val issues: List<String>,

        /** Audit trail of processing steps */
        val auditTrail: List<ProcessingStep>
    ) : OrchestratorResult()

    /**
     * Processing failed and cannot be recovered.
     *
     * This happens when:
     * - Document cannot be read or converted
     * - Catastrophic model failure
     * - System errors
     */
    @Serializable
    data class Failed(
        /** Human-readable failure reason */
        val reason: String,

        /** Processing stage where failure occurred */
        val stage: String,

        /** Audit trail up to failure point */
        val auditTrail: List<ProcessingStep>
    ) : OrchestratorResult()
}
