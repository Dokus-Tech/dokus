package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Document drafts table - editable extraction state (one per document).
 *
 * OWNER: documents service
 * CRITICAL: All queries MUST filter by tenant_id for multi-tenant security.
 *
 * This table stores the editable draft state for a document:
 * - Draft status (NeedsReview -> Confirmed/Rejected)
 * - AI draft data (immutable, from first successful run)
 * - Extracted data (editable current version)
 * - User edits and versioning for audit
 * - Version number for optimistic locking
 * - Contact suggestion from AI matching
 *
 * Note: Confirmation creates Invoice/Expense with documentId FK.
 * The draft's documentStatus changes to Confirmed, but the linkage is in the
 * financial entity tables (single source of truth).
 */
object DocumentDraftsTable : Table("document_drafts") {

    // Primary key: one draft per document
    val documentId = uuid("document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.CASCADE)

    // Multi-tenancy (denormalized for query performance)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // Draft review status
    val documentStatus = dbEnumeration<DocumentStatus>("document_status").default(DocumentStatus.NeedsReview)

    // Detected document type
    val documentType = dbEnumeration<DocumentType>("document_type").default(DocumentType.Unknown)

    // ============================================
    // Extraction Data Fields
    // ============================================

    // Original AI extraction output - IMMUTABLE once set (from first successful run)
    // Preserves the AI's initial extraction for audit purposes
    val aiDraftData = text("ai_draft_data").nullable()

    // AI-generated keywords for search (JSON array)
    val aiKeywords = text("ai_keywords").nullable()

    // Canonical purpose/description fields for UI and downstream enrichment.
    val purposeBase = text("purpose_base").nullable()
    val purposePeriodYear = integer("purpose_period_year").nullable()
    val purposePeriodMonth = integer("purpose_period_month").nullable()
    val purposeRendered = text("purpose_rendered").nullable()
    val purposeSource = dbEnumeration<DocumentPurposeSource>("purpose_source").nullable()
    val purposeLocked = bool("purpose_locked").default(false)
    val purposePeriodMode = dbEnumeration<PurposePeriodMode>("purpose_period_mode")
        .default(PurposePeriodMode.IssueMonth)

    // Stable identity keys for purpose template + RAG retrieval filters.
    val counterpartyKey = varchar("counterparty_key", 255).nullable()
    val merchantToken = varchar("merchant_token", 120).nullable()

    // Which ingestion run produced the ai_draft_data
    val aiDraftSourceRunId = uuid("ai_draft_source_run_id")
        .references(DocumentIngestionRunsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // Current extraction data (may include user edits)
    val extractedData = text("extracted_data").nullable()

    // JSON linking extracted fields to their source locations in the document
    // Format: { "vendorName": { "page": 1, "bbox": [x1,y1,x2,y2], "text": "..." }, ... }
    val provenanceData = text("provenance_data").nullable()

    // ============================================
    // Versioning & Audit Fields
    // ============================================

    // Version number - incremented each time user edits the draft
    val draftVersion = integer("draft_version").default(0)

    // When the draft was last edited by a user
    val draftEditedAt = datetime("draft_edited_at").nullable()

    // Who last edited the draft
    val draftEditedBy = uuid("draft_edited_by")
        .references(UsersTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // ============================================
    // Contact Suggestion Fields
    // ============================================

    // Contact suggestions (JSON array of SuggestedContact)
    val contactSuggestions = text("contact_suggestions").nullable()

    // Counterparty snapshot used for matching (JSON)
    val counterpartySnapshot = text("counterparty_snapshot").nullable()

    // User-linked contact (explicit selection)
    val linkedContactId = uuid("linked_contact_id")
        .references(ContactsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()
    val linkedContactSource = dbEnumeration<ContactLinkSource>("linked_contact_source").nullable()

    // Evidence JSON for contact decision (MatchEvidence)
    val matchEvidence = text("match_evidence").nullable()

    // Whether a new contact should be created from the counterparty snapshot
    val pendingCreation = bool("pending_creation").default(false)

    // Rejection reason (if documentStatus == Rejected)
    val rejectReason = dbEnumeration<DocumentRejectReason>("reject_reason").nullable()

    // ============================================
    // Run Reference
    // ============================================

    // Last successful ingestion run that updated extracted_data
    val lastSuccessfulRunId = uuid("last_successful_run_id")
        .references(DocumentIngestionRunsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // ============================================
    // Timestamps
    // ============================================

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(documentId)

    init {
        // For listing documents by status
        index(false, tenantId, documentStatus)

        // For listing documents by type
        index(false, tenantId, documentType)

        // For contact activity queries
        index(false, tenantId, linkedContactId)

        // For purpose template and similarity retrieval.
        index(false, tenantId, counterpartyKey, documentType, documentStatus)
        index(false, tenantId, merchantToken, documentType, documentStatus)
    }
}
