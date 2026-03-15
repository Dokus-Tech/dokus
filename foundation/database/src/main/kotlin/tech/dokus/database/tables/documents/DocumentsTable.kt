package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.foundation.backend.database.dbEnumeration

private const val ContentHashLength = 64

/**
 * Documents table — canonical document record.
 *
 * A document represents a financial fact (invoice, receipt, credit note, etc.),
 * not a file. File metadata lives on DocumentSourcesTable → DocumentBlobsTable.
 * The file columns here are vestigial and will be removed in a future stage.
 *
 * This table is the merged result of the former DocumentsTable + DocumentDraftsTable.
 * Draft columns (status, type, direction, canonical data, counterparty, purpose, etc.)
 * are nullable — null means "unprocessed" (no draft yet).
 *
 * OWNER: documents service
 * ACCESS: processor service (read-only)
 * CRITICAL: All queries MUST filter by tenant_id for tenant isolation.
 */
object DocumentsTable : UUIDTable("documents") {

    // ============================================
    // Multi-tenancy (CRITICAL)
    // ============================================
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // ============================================
    // 4-Axis Classification (all queryable columns)
    // ============================================

    // Detected document type (null = unprocessed)
    val documentType = dbEnumeration<DocumentType>("document_type").nullable()

    // Document direction (null = unprocessed)
    val direction = dbEnumeration<DocumentDirection>("direction").nullable()

    // Draft review status (null = unprocessed)
    val documentStatus = dbEnumeration<DocumentStatus>("document_status").nullable()

    // ============================================
    // Canonical Document Data
    // ============================================

    // Canonical document data — the single reviewable truth (may include user edits)
    val canonicalData = text("extracted_data").nullable()

    // AI-generated keywords for search (JSON array)
    val aiKeywords = text("ai_keywords").nullable()

    // ============================================
    // Field Provenance & User Locks
    // ============================================

    // Per-field provenance map (JSON: Map<String, FieldProvenance>)
    // Tracks source trust, extraction confidence, and user locks for each field
    val fieldProvenance = text("field_provenance").nullable()

    // ============================================
    // Identity & Dedup
    // ============================================

    // Canonical content fingerprint for deduplication (SHA-256 hex)
    val canonicalContentHash = varchar("content_hash", ContentHashLength).nullable()

    // Canonical identity fingerprint for legal identity matching
    val canonicalIdentityKey = varchar("identity_key_hash", ContentHashLength).nullable()

    // ============================================
    // Purpose / Filing
    // ============================================
    val purposeBase = text("purpose_base").nullable()
    val purposePeriodYear = integer("purpose_period_year").nullable()
    val purposePeriodMonth = integer("purpose_period_month").nullable()
    val purposeRendered = text("purpose_rendered").nullable()
    val purposeSource = dbEnumeration<DocumentPurposeSource>("purpose_source").nullable()
    val purposeLocked = bool("purpose_locked").default(false)
    val purposePeriodMode = dbEnumeration<PurposePeriodMode>("purpose_period_mode")
        .default(PurposePeriodMode.IssueMonth)

    // Stable identity keys for purpose template + RAG retrieval filters
    val counterpartyKey = varchar("counterparty_key", 255).nullable()
    val merchantToken = varchar("merchant_token", 120).nullable()

    // ============================================
    // Counterparty Resolution
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

    // ============================================
    // Draft Versioning & Audit
    // ============================================

    // Version number - incremented each time user edits the draft
    val draftVersion = integer("draft_version").default(0)

    // When the draft was last edited by a user
    val draftEditedAt = datetime("draft_edited_at").nullable()

    // Who last edited the draft
    val draftEditedBy = uuid("draft_edited_by")
        .references(UsersTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable()

    // Which ingestion run produced the first AI draft
    // Note: no FK constraint — circular reference with DocumentIngestionRunsTable
    val aiDraftSourceRunId = uuid("ai_draft_source_run_id").nullable()

    // Last successful ingestion run that updated canonical data
    // Note: no FK constraint — circular reference with DocumentIngestionRunsTable
    val lastSuccessfulRunId = uuid("last_successful_run_id").nullable()

    // ============================================
    // Status
    // ============================================

    // Rejection reason (if documentStatus == Rejected)
    val rejectReason = dbEnumeration<DocumentRejectReason>("reject_reason").nullable()

    // ============================================
    // Sorting
    // ============================================

    // Canonical sorting date: issueDate for invoices/credit notes, receipt date for receipts,
    // periodEnd for bank statements, falls back to uploadedAt on creation.
    // Updated when canonical data is written.
    val sortDate = date("sort_date").nullable()

    // ============================================
    // Timestamps
    // ============================================
    val uploadedAt = datetime("uploaded_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Identity key for matching
        index(false, tenantId, canonicalIdentityKey)
        // For listing documents by status
        index(false, tenantId, documentStatus)
        // For listing documents by type
        index(false, tenantId, documentType)
        // For listing documents by direction
        index(false, tenantId, direction)
        // For contact activity queries
        index(false, tenantId, linkedContactId)
        // For purpose template and similarity retrieval
        index(false, tenantId, counterpartyKey, documentType, documentStatus)
        index(false, tenantId, merchantToken, documentType, documentStatus)
        // For sorting documents by canonical date
        index(false, tenantId, sortDate)
    }
}
