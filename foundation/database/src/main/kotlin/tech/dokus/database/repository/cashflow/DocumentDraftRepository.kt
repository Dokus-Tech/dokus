package tech.dokus.database.repository.cashflow
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.model.contact.SuggestedContact
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.repository.DocumentStatusChecker
import tech.dokus.domain.utils.json
import kotlin.time.Clock

/**
 * Data class for draft summary.
 */
data class DraftSummary(
    val documentId: DocumentId,
    val tenantId: TenantId,
    val documentStatus: DocumentStatus,
    val documentType: DocumentType?,
    val extractedData: DocumentDraftData?,
    val aiDraftData: DocumentDraftData?,
    val aiDescription: String? = null,
    val aiKeywords: List<String> = emptyList(),
    val aiDraftSourceRunId: IngestionRunId?,
    val draftVersion: Int,
    val draftEditedAt: LocalDateTime?,
    val draftEditedBy: UserId?,
    val contactSuggestions: List<SuggestedContact> = emptyList(),
    val counterpartySnapshot: CounterpartySnapshot? = null,
    val matchEvidence: MatchEvidence? = null,
    val linkedContactId: ContactId?,
    val linkedContactSource: ContactLinkSource? = null,
    val counterpartyIntent: CounterpartyIntent,
    val rejectReason: DocumentRejectReason?,
    val lastSuccessfulRunId: IngestionRunId?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Repository for document draft operations.
 * CRITICAL: All queries filter by tenantId for security.
 *
 * Implements DocumentStatusChecker for chat confirmation checks.
 */
class DocumentDraftRepository : DocumentStatusChecker {

    /**
     * Create or update a draft from an ingestion run result.
     *
     * Rules:
     * - ai_draft_data + ai_draft_source_run_id: Set ONLY if null (from first successful run, immutable)
     * - extracted_data: Set only if force=true OR draftVersion == 0 (not user-edited)
     *
     * CRITICAL: Must provide tenantId.
     */
    suspend fun createOrUpdateFromIngestion(
        documentId: DocumentId,
        tenantId: TenantId,
        runId: IngestionRunId,
        extractedData: DocumentDraftData,
        documentType: DocumentType,
        aiDescription: String? = null,
        aiKeywords: List<String> = emptyList(),
        force: Boolean = false
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = documentId.value
        val tenantIdUuid = tenantId.value
        val runIdUuid = runId.value
        val keywordsJson = aiKeywords.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) }

        // Check if draft exists and get current state
        val existing = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq docIdUuid) and
                    (DocumentDraftsTable.tenantId eq tenantIdUuid)
            }
            .singleOrNull()

        if (existing == null) {
            // Create new draft
            DocumentDraftsTable.insert {
                it[DocumentDraftsTable.documentId] = docIdUuid
                it[DocumentDraftsTable.tenantId] = tenantIdUuid
                it[documentStatus] = DocumentStatus.NeedsReview
                it[DocumentDraftsTable.documentType] = documentType
                it[aiDraftData] = json.encodeToString(extractedData)
                it[DocumentDraftsTable.aiDescription] = aiDescription?.takeIf { value -> value.isNotBlank() }
                it[DocumentDraftsTable.aiKeywords] = keywordsJson
                it[aiDraftSourceRunId] = runIdUuid
                it[DocumentDraftsTable.extractedData] = json.encodeToString(extractedData)
                it[lastSuccessfulRunId] = runIdUuid
                it[createdAt] = now
                it[updatedAt] = now
            }
            true
        } else {
            // Update existing draft
            val currentVersion = existing[DocumentDraftsTable.draftVersion]
            val hasAiDraft = existing[DocumentDraftsTable.aiDraftData] != null
            val shouldUpdateExtracted = force || currentVersion == 0

            DocumentDraftsTable.update({
                (DocumentDraftsTable.documentId eq docIdUuid) and
                    (DocumentDraftsTable.tenantId eq tenantIdUuid)
            }) {
                // ai_draft_data: set only if null (immutable)
                if (!hasAiDraft) {
                    it[aiDraftData] = json.encodeToString(extractedData)
                    it[DocumentDraftsTable.aiDescription] = aiDescription?.takeIf { value -> value.isNotBlank() }
                    it[DocumentDraftsTable.aiKeywords] = keywordsJson
                    it[aiDraftSourceRunId] = runIdUuid
                }

                // extracted_data: update only if force or not user-edited
                if (shouldUpdateExtracted) {
                    it[DocumentDraftsTable.extractedData] = json.encodeToString(extractedData)
                    it[DocumentDraftsTable.documentType] = documentType
                    it[documentStatus] = DocumentStatus.NeedsReview
                    if (!aiDescription.isNullOrBlank()) {
                        it[DocumentDraftsTable.aiDescription] = aiDescription
                    }
                    if (keywordsJson != null) {
                        it[DocumentDraftsTable.aiKeywords] = keywordsJson
                    }
                }

                it[lastSuccessfulRunId] = runIdUuid
                it[updatedAt] = now
            } > 0
        }
    }

    /**
     * Get a draft by document ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByDocumentId(
        documentId: DocumentId,
        tenantId: TenantId
    ): DraftSummary? = newSuspendedTransaction {
        DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq documentId.value) and
                    (DocumentDraftsTable.tenantId eq tenantId.value)
            }
            .map { it.toDraftSummary() }
            .singleOrNull()
    }

    /**
     * Update a draft with user corrections.
     * Increments version, tracks corrections, updates edited timestamp.
     * CRITICAL: Must filter by tenantId.
     *
     * @return The new draft version number, or null if not found
     */
    suspend fun updateDraft(
        documentId: DocumentId,
        tenantId: TenantId,
        userId: UserId,
        updatedData: DocumentDraftData
    ): Int? = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = documentId.value
        val tenantIdUuid = tenantId.value

        // Get current draft to check version
        val current = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq docIdUuid) and
                    (DocumentDraftsTable.tenantId eq tenantIdUuid)
            }
            .singleOrNull() ?: return@newSuspendedTransaction null

        val currentVersion = current[DocumentDraftsTable.draftVersion]
        val newVersion = currentVersion + 1

        val currentStatus = current[DocumentDraftsTable.documentStatus]
        val nextStatus = if (currentStatus == DocumentStatus.Confirmed) {
            // If a confirmed draft is edited, require review again.
            DocumentStatus.NeedsReview
        } else {
            currentStatus
        }

        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq docIdUuid) and
                (DocumentDraftsTable.tenantId eq tenantIdUuid)
        }) {
            it[documentType] = updatedData.toDocumentType()
            it[extractedData] = json.encodeToString(updatedData)
            it[draftVersion] = newVersion
            it[draftEditedAt] = now
            it[draftEditedBy] = userId.value
            if (nextStatus != currentStatus) {
                it[documentStatus] = nextStatus
            }
            it[updatedAt] = now
        }

        newVersion
    }

    /**
     * Update document status.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun updateDocumentStatus(
        documentId: DocumentId,
        tenantId: TenantId,
        status: DocumentStatus
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq documentId.value) and
                (DocumentDraftsTable.tenantId eq tenantId.value)
        }) {
            it[documentStatus] = status
            if (status != DocumentStatus.Rejected) {
                it[rejectReason] = null
            }
            it[updatedAt] = now
        } > 0
    }

    /**
     * Update linked contact and/or counterparty intent.
     * If the draft was previously confirmed, it transitions to NeedsReview.
     */
    suspend fun updateCounterparty(
        documentId: DocumentId,
        tenantId: TenantId,
        contactId: ContactId?,
        intent: CounterpartyIntent?,
        source: ContactLinkSource? = null,
        matchEvidence: MatchEvidence? = null
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = documentId.value
        val tenantIdUuid = tenantId.value
        val evidenceJson = matchEvidence?.let { json.encodeToString(it) }

        val current = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq docIdUuid) and
                    (DocumentDraftsTable.tenantId eq tenantIdUuid)
            }
            .singleOrNull() ?: return@newSuspendedTransaction false

        val currentStatus = current[DocumentDraftsTable.documentStatus]
        val shouldReview = currentStatus == DocumentStatus.Confirmed

        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq docIdUuid) and
                (DocumentDraftsTable.tenantId eq tenantIdUuid)
        }) {
            if (contactId != null) {
                it[linkedContactId] = contactId.value
                it[counterpartyIntent] = CounterpartyIntent.None
                it[linkedContactSource] = source
                if (evidenceJson != null) {
                    it[DocumentDraftsTable.matchEvidence] = evidenceJson
                }
            } else if (intent != null) {
                it[counterpartyIntent] = intent
                if (intent == CounterpartyIntent.None || intent == CounterpartyIntent.Pending) {
                    it[linkedContactId] = null
                    it[linkedContactSource] = null
                }
            }
            if (shouldReview) {
                it[documentStatus] = DocumentStatus.NeedsReview
            }
            it[updatedAt] = now
        } > 0
    }

    /**
     * Reject a draft with a reason.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun rejectDraft(
        documentId: DocumentId,
        tenantId: TenantId,
        reason: DocumentRejectReason
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq documentId.value) and
                (DocumentDraftsTable.tenantId eq tenantId.value)
        }) {
            it[documentStatus] = DocumentStatus.Rejected
            it[rejectReason] = reason
            it[updatedAt] = now
        } > 0
    }

    /**
     * List drafts by status with optional document type filter.
     * CRITICAL: Must filter by tenantId.
     *
     * @return Pair of (drafts, totalCount)
     */
    suspend fun listByStatus(
        tenantId: TenantId,
        statuses: List<DocumentStatus>,
        documentType: DocumentType? = null,
        page: Int = 0,
        limit: Int = 20
    ): Pair<List<DraftSummary>, Long> = newSuspendedTransaction {
        val tenantIdUuid = tenantId.value

        val baseQuery = DocumentDraftsTable.selectAll()
            .where {
                val conditions = (DocumentDraftsTable.tenantId eq tenantIdUuid) and
                    (DocumentDraftsTable.documentStatus inList statuses)
                if (documentType != null) {
                    conditions and (DocumentDraftsTable.documentType eq documentType)
                } else {
                    conditions
                }
            }

        val total = baseQuery.count()

        val drafts = baseQuery
            .orderBy(DocumentDraftsTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .offset((page * limit).toLong())
            .map { it.toDraftSummary() }

        drafts to total
    }

    /**
     * Update contact resolution results (suggestions, snapshot, evidence, and optional link).
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun updateContactResolution(
        documentId: DocumentId,
        tenantId: TenantId,
        contactSuggestions: List<SuggestedContact> = emptyList(),
        counterpartySnapshot: CounterpartySnapshot? = null,
        matchEvidence: MatchEvidence? = null,
        linkedContactId: ContactId? = null,
        linkedContactSource: ContactLinkSource? = null
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val suggestionsJson = json.encodeToString(contactSuggestions)
        val snapshotJson = counterpartySnapshot?.let { json.encodeToString(it) }

        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq documentId.value) and
                (DocumentDraftsTable.tenantId eq tenantId.value)
        }) {
            it[DocumentDraftsTable.contactSuggestions] = suggestionsJson
            it[DocumentDraftsTable.counterpartySnapshot] = snapshotJson

            if (linkedContactId != null) {
                it[DocumentDraftsTable.linkedContactId] = linkedContactId.value
                it[DocumentDraftsTable.linkedContactSource] = linkedContactSource
                it[DocumentDraftsTable.counterpartyIntent] = CounterpartyIntent.None
                it[DocumentDraftsTable.matchEvidence] = matchEvidence?.let { evidence -> json.encodeToString(evidence) }
            } else {
                it[DocumentDraftsTable.linkedContactId] = null
                it[DocumentDraftsTable.linkedContactSource] = null
                it[DocumentDraftsTable.matchEvidence] = null
            }

            it[DocumentDraftsTable.updatedAt] = now
        } > 0
    }

    /**
     * Delete a draft.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun delete(
        documentId: DocumentId,
        tenantId: TenantId
    ): Boolean = newSuspendedTransaction {
        DocumentDraftsTable.deleteWhere {
            (DocumentDraftsTable.documentId eq documentId.value) and
                (DocumentDraftsTable.tenantId eq tenantId.value)
        } > 0
    }

    // =========================================================================
    // DocumentStatusChecker Implementation
    // =========================================================================

    /**
     * Check if a document has been confirmed by the user.
     * Used by ChatAgent to enforce chat-only-for-confirmed policy.
     *
     * CRITICAL: Must filter by tenantId for multi-tenant security.
     */
    override suspend fun isConfirmed(
        tenantId: TenantId,
        documentId: DocumentId
    ): Boolean = newSuspendedTransaction {
        val draft = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.documentId eq documentId.value) and
                    (DocumentDraftsTable.tenantId eq tenantId.value)
            }
            .singleOrNull()

        draft?.get(DocumentDraftsTable.documentStatus) == DocumentStatus.Confirmed
    }

    private fun ResultRow.toDraftSummary(): DraftSummary {
        return DraftSummary(
            documentId = DocumentId(this[DocumentDraftsTable.documentId]),
            tenantId = TenantId(this[DocumentDraftsTable.tenantId]),
            documentStatus = this[DocumentDraftsTable.documentStatus],
            documentType = this[DocumentDraftsTable.documentType],
            extractedData = this[DocumentDraftsTable.extractedData]?.let { json.decodeFromString(it) },
            aiDraftData = this[DocumentDraftsTable.aiDraftData]?.let { json.decodeFromString(it) },
            aiDescription = this[DocumentDraftsTable.aiDescription],
            aiKeywords = this[DocumentDraftsTable.aiKeywords]?.let { json.decodeFromString(it) } ?: emptyList(),
            aiDraftSourceRunId = this[DocumentDraftsTable.aiDraftSourceRunId]
                ?.let { IngestionRunId.parse(it.toString()) },
            draftVersion = this[DocumentDraftsTable.draftVersion],
            draftEditedAt = this[DocumentDraftsTable.draftEditedAt],
            draftEditedBy = this[DocumentDraftsTable.draftEditedBy]?.let { UserId(it) },
            contactSuggestions = this[DocumentDraftsTable.contactSuggestions]
                ?.let { json.decodeFromString(it) }
                ?: emptyList(),
            counterpartySnapshot = this[DocumentDraftsTable.counterpartySnapshot]
                ?.let { json.decodeFromString(it) },
            matchEvidence = this[DocumentDraftsTable.matchEvidence]
                ?.let { json.decodeFromString(it) },
            linkedContactId = this[DocumentDraftsTable.linkedContactId]?.let { ContactId(it) },
            linkedContactSource = this[DocumentDraftsTable.linkedContactSource],
            counterpartyIntent = this[DocumentDraftsTable.counterpartyIntent],
            rejectReason = this[DocumentDraftsTable.rejectReason],
            lastSuccessfulRunId = this[DocumentDraftsTable.lastSuccessfulRunId]
                ?.let { IngestionRunId.parse(it.toString()) },
            createdAt = this[DocumentDraftsTable.createdAt],
            updatedAt = this[DocumentDraftsTable.updatedAt]
        )
    }
}
