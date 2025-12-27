package ai.dokus.foundation.database.tables.cashflow

import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.database.tables.auth.UsersTable
import ai.dokus.foundation.database.tables.contacts.ContactsTable
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.EntityType
import tech.dokus.domain.enums.ProcessingStatus
import tech.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Document processing table - tracks AI extraction status and results.
 *
 * OWNER: cashflow service
 * ACCESS: processor service (read-write for processing updates)
 * CRITICAL: All queries MUST filter by tenant_id for multi-tenant security.
 *
 * This table links to DocumentsTable and stores:
 * - Processing status (pending → queued → processing → processed/failed → confirmed/rejected)
 * - Extracted data as JSON
 * - Raw text from document for future AI use
 * - Confidence scores
 * - AI provider used
 * - Confirmation status when user reviews
 *
 * Draft Versioning (Audit Trail):
 * - aiDraftData: Original AI extraction output (immutable once set)
 * - extractedData: Current version (may include user edits)
 * - userCorrections: JSON tracking what the user changed from AI draft
 * - provenanceData: JSON linking extracted fields to source locations in document
 * - draftVersion: Incremented on each user edit
 * - draftEditedAt/draftEditedBy: Tracks who last edited and when
 */
object DocumentProcessingTable : UUIDTable("document_processing") {

    // Reference to the document being processed
    val documentId = uuid("document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)
        .uniqueIndex()

    // Multi-tenancy (denormalized for query performance)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // Processing state
    val status = dbEnumeration<ProcessingStatus>("status")
        .default(ProcessingStatus.Pending)

    // Detected document type
    val documentType = dbEnumeration<DocumentType>("document_type")
        .nullable()

    // Extraction results as JSON (ExtractedDocumentData serialized)
    // This is the "current" version that may include user edits
    val extractedData = text("extracted_data").nullable()

    // ============================================
    // Draft Versioning Fields (Audit Trail)
    // ============================================

    // Original AI extraction output - IMMUTABLE once set
    // Preserves the AI's initial extraction for audit purposes
    val aiDraftData = text("ai_draft_data").nullable()

    // JSON tracking user corrections/edits from the AI draft
    // Format: [{ "field": "vendorName", "aiValue": "Acme Inc", "userValue": "ACME Corporation", "editedAt": "..." }, ...]
    val userCorrections = text("user_corrections").nullable()

    // JSON linking extracted fields to their source locations in the document
    // Format: { "vendorName": { "page": 1, "bbox": [x1,y1,x2,y2], "text": "..." }, ... }
    val provenanceData = text("provenance_data").nullable()

    // Version number - incremented each time user edits the draft
    val draftVersion = integer("draft_version").default(0)

    // When the draft was last edited by a user
    val draftEditedAt = datetime("draft_edited_at").nullable()

    // Who last edited the draft
    val draftEditedBy = uuid("draft_edited_by")
        .references(UsersTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // ============================================
    // End Draft Versioning Fields
    // ============================================

    // Full OCR/extracted text - stored separately for size
    val rawText = text("raw_text").nullable()

    // Confidence score (0.0000 - 1.0000)
    val confidence = decimal("confidence", 5, 4).nullable()

    // Per-field confidence scores as JSON
    val fieldConfidences = text("field_confidences").nullable()

    // Processing metadata
    val processingAttempts = integer("processing_attempts").default(0)
    val lastProcessedAt = datetime("last_processed_at").nullable()
    val processingStartedAt = datetime("processing_started_at").nullable()
    val errorMessage = text("error_message").nullable()

    // AI provider used (koog_local, openai, anthropic)
    val aiProvider = varchar("ai_provider", 50).nullable()

    // Confirmation - when user reviews and confirms extraction
    val confirmedAt = datetime("confirmed_at").nullable()
    val confirmedEntityType = dbEnumeration<EntityType>("confirmed_entity_type").nullable()
    val confirmedEntityId = uuid("confirmed_entity_id").nullable()

    // Contact suggestion from AI matching (populated after extraction)
    val suggestedContactId = uuid("suggested_contact_id")
        .references(ContactsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val contactSuggestionConfidence = float("contact_suggestion_confidence").nullable()
    val contactSuggestionReason = varchar("contact_suggestion_reason", 255).nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // CRITICAL: Index tenant_id + status for main query pattern
        index(false, tenantId, status)

        // For background job: find pending/failed documents to process
        index(false, status, processingAttempts)

        // For cleanup: find old unconfirmed documents
        index(false, tenantId, confirmedAt)

        // For contact activity queries: find documents suggested for a contact
        index(false, tenantId, suggestedContactId)
    }
}
