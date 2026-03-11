package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.JoinType
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
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.isUnresolved
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.model.contact.SuggestedContact
import tech.dokus.domain.repository.DocumentStatusChecker
import tech.dokus.domain.utils.json
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Data class for draft summary.
 */
data class DraftSummary(
    val documentId: DocumentId,
    val tenantId: TenantId,
    val documentStatus: DocumentStatus,
    val documentType: DocumentType?,
    val extractedData: DocumentDraftData?,
    val aiKeywords: List<String> = emptyList(),
    val purposeBase: String? = null,
    val purposePeriodYear: Int? = null,
    val purposePeriodMonth: Int? = null,
    val purposeRendered: String? = null,
    val purposeSource: DocumentPurposeSource? = null,
    val purposeLocked: Boolean = false,
    val purposePeriodMode: PurposePeriodMode = PurposePeriodMode.IssueMonth,
    val counterpartyKey: String? = null,
    val merchantToken: String? = null,
    val aiDraftSourceRunId: IngestionRunId?,
    val draftVersion: Int,
    val draftEditedAt: LocalDateTime?,
    val draftEditedBy: UserId?,
    val counterparty: CounterpartyInfo? = null,
    val counterpartyDisplayName: String? = null,
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
@OptIn(ExperimentalUuidApi::class)
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
        aiKeywords: List<String> = emptyList(),
        force: Boolean = false
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = UUID.fromString(documentId.toString())
        val tenantIdUuid = UUID.fromString(tenantId.toString())
        val runIdUuid = UUID.fromString(runId.toString())
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
            val hasAiDraftRun = existing[DocumentDraftsTable.aiDraftSourceRunId] != null
            val shouldUpdateExtracted = force || currentVersion == 0

            DocumentDraftsTable.update({
                (DocumentDraftsTable.documentId eq docIdUuid) and
                    (DocumentDraftsTable.tenantId eq tenantIdUuid)
            }) {
                // aiDraftSourceRunId + keywords: set only on first successful run
                if (!hasAiDraftRun) {
                    it[DocumentDraftsTable.aiKeywords] = keywordsJson
                    it[aiDraftSourceRunId] = runIdUuid
                }

                // extracted_data: update only if force or not user-edited
                if (shouldUpdateExtracted) {
                    it[DocumentDraftsTable.extractedData] = json.encodeToString(extractedData)
                    it[DocumentDraftsTable.documentType] = documentType
                    it[documentStatus] = DocumentStatus.NeedsReview
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
        DocumentDraftsTable
            .join(ContactsTable, JoinType.LEFT, DocumentDraftsTable.linkedContactId, ContactsTable.id) {
                ContactsTable.tenantId eq DocumentDraftsTable.tenantId
            }
            .selectAll()
            .where {
                (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                    (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            .map { it.toDraftSummary(contactName = it.getOrNull(ContactsTable.name)) }
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
        val docIdUuid = UUID.fromString(documentId.toString())
        val tenantIdUuid = UUID.fromString(tenantId.toString())

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
            it[draftEditedBy] = UUID.fromString(userId.toString())
            if (nextStatus != currentStatus) {
                it[documentStatus] = nextStatus
            }
            it[updatedAt] = now
        }

        newVersion
    }

    suspend fun updateExtractedDataAndStatus(
        documentId: DocumentId,
        tenantId: TenantId,
        extractedData: DocumentDraftData,
        status: DocumentStatus
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentDraftsTable.extractedData] = json.encodeToString(extractedData)
            it[DocumentDraftsTable.documentType] = extractedData.toDocumentType()
            it[documentStatus] = status
            it[updatedAt] = now
        } > 0
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
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[documentStatus] = status
            if (status != DocumentStatus.Rejected) {
                it[rejectReason] = null
            }
            it[updatedAt] = now
        } > 0
    }

    /**
     * Update counterparty info on a draft.
     * If the draft was previously confirmed, it transitions to NeedsReview.
     */
    suspend fun updateCounterparty(
        documentId: DocumentId,
        tenantId: TenantId,
        counterparty: CounterpartyInfo
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = UUID.fromString(documentId.toString())
        val tenantIdUuid = UUID.fromString(tenantId.toString())

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
            when (counterparty) {
                is CounterpartyInfo.Linked -> {
                    it[linkedContactId] = UUID.fromString(counterparty.contactId.toString())
                    it[linkedContactSource] = counterparty.source
                    it[matchEvidence] = counterparty.evidence?.let { ev -> json.encodeToString(ev) }
                    it[pendingCreation] = false
                }
                is CounterpartyInfo.Unresolved -> {
                    it[linkedContactId] = null
                    it[linkedContactSource] = null
                    it[matchEvidence] = null
                    it[pendingCreation] = counterparty.pendingCreation
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
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
        val tenantIdUuid = UUID.fromString(tenantId.toString())

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
     * Update contact resolution results (snapshot, counterparty info).
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun updateContactResolution(
        documentId: DocumentId,
        tenantId: TenantId,
        counterpartySnapshot: CounterpartySnapshot? = null,
        counterparty: CounterpartyInfo
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val snapshotJson = counterpartySnapshot?.let { json.encodeToString(it) }

        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentDraftsTable.counterpartySnapshot] = snapshotJson

            when (counterparty) {
                is CounterpartyInfo.Linked -> {
                    it[DocumentDraftsTable.linkedContactId] = UUID.fromString(counterparty.contactId.toString())
                    it[DocumentDraftsTable.linkedContactSource] = counterparty.source
                    it[DocumentDraftsTable.matchEvidence] = counterparty.evidence?.let { ev -> json.encodeToString(ev) }
                    it[DocumentDraftsTable.contactSuggestions] = null
                    it[DocumentDraftsTable.pendingCreation] = false
                }
                is CounterpartyInfo.Unresolved -> {
                    it[DocumentDraftsTable.linkedContactId] = null
                    it[DocumentDraftsTable.linkedContactSource] = null
                    it[DocumentDraftsTable.matchEvidence] = null
                    it[DocumentDraftsTable.contactSuggestions] = counterparty.suggestions
                        .takeIf { s -> s.isNotEmpty() }
                        ?.let { s -> json.encodeToString(s) }
                    it[DocumentDraftsTable.pendingCreation] = counterparty.pendingCreation
                }
            }

            it[DocumentDraftsTable.updatedAt] = now
        } > 0
    }

    suspend fun updatePurposeContext(
        documentId: DocumentId,
        tenantId: TenantId,
        counterpartyKey: String?,
        merchantToken: String?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            counterpartyKey?.let { key -> it[DocumentDraftsTable.counterpartyKey] = key }
            merchantToken?.let { token -> it[DocumentDraftsTable.merchantToken] = token }
            it[DocumentDraftsTable.updatedAt] = now
        } > 0
    }

    @Suppress("LongParameterList")
    suspend fun updatePurposeFields(
        documentId: DocumentId,
        tenantId: TenantId,
        purposeBase: String?,
        purposePeriodYear: Int?,
        purposePeriodMonth: Int?,
        purposeRendered: String?,
        purposeSource: DocumentPurposeSource?,
        purposeLocked: Boolean,
        purposePeriodMode: PurposePeriodMode
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentDraftsTable.update({
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentDraftsTable.purposeBase] = purposeBase
            it[DocumentDraftsTable.purposePeriodYear] = purposePeriodYear
            it[DocumentDraftsTable.purposePeriodMonth] = purposePeriodMonth
            it[DocumentDraftsTable.purposeRendered] = purposeRendered
            it[DocumentDraftsTable.purposeSource] = purposeSource
            it[DocumentDraftsTable.purposeLocked] = purposeLocked
            it[DocumentDraftsTable.purposePeriodMode] = purposePeriodMode
            it[DocumentDraftsTable.updatedAt] = now
        } > 0
    }

    suspend fun listConfirmedPurposeBasesByCounterparty(
        tenantId: TenantId,
        counterpartyKey: String,
        documentType: DocumentType,
        limit: Int = 5
    ): List<String> = newSuspendedTransaction {
        DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentDraftsTable.counterpartyKey eq counterpartyKey) and
                    (DocumentDraftsTable.documentType eq documentType) and
                    (DocumentDraftsTable.documentStatus eq DocumentStatus.Confirmed)
            }
            .orderBy(DocumentDraftsTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .mapNotNull { row -> row[DocumentDraftsTable.purposeBase] }
    }

    suspend fun listConfirmedPurposeBasesByMerchantToken(
        tenantId: TenantId,
        merchantToken: String,
        documentType: DocumentType,
        limit: Int = 5
    ): List<String> = newSuspendedTransaction {
        DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentDraftsTable.merchantToken eq merchantToken) and
                    (DocumentDraftsTable.documentType eq documentType) and
                    (DocumentDraftsTable.documentStatus eq DocumentStatus.Confirmed)
            }
            .orderBy(DocumentDraftsTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .mapNotNull { row -> row[DocumentDraftsTable.purposeBase] }
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
            (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
                (DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString())) and
                    (DocumentDraftsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            .singleOrNull()

        draft?.get(DocumentDraftsTable.documentStatus) == DocumentStatus.Confirmed
    }

    private fun ResultRow.toDraftSummary(contactName: String? = null): DraftSummary {
        val counterpartyInfo = buildCounterpartyInfo(this)
        return DraftSummary(
            documentId = DocumentId.parse(this[DocumentDraftsTable.documentId].toString()),
            tenantId = TenantId(this[DocumentDraftsTable.tenantId].toKotlinUuid()),
            documentStatus = this[DocumentDraftsTable.documentStatus],
            documentType = this[DocumentDraftsTable.documentType],
            extractedData = this[DocumentDraftsTable.extractedData]?.let { json.decodeFromString(it) },
            aiKeywords = this[DocumentDraftsTable.aiKeywords]?.let { json.decodeFromString(it) } ?: emptyList(),
            purposeBase = this[DocumentDraftsTable.purposeBase],
            purposePeriodYear = this[DocumentDraftsTable.purposePeriodYear],
            purposePeriodMonth = this[DocumentDraftsTable.purposePeriodMonth],
            purposeRendered = this[DocumentDraftsTable.purposeRendered],
            purposeSource = this[DocumentDraftsTable.purposeSource],
            purposeLocked = this[DocumentDraftsTable.purposeLocked],
            purposePeriodMode = this[DocumentDraftsTable.purposePeriodMode],
            counterpartyKey = this[DocumentDraftsTable.counterpartyKey],
            merchantToken = this[DocumentDraftsTable.merchantToken],
            aiDraftSourceRunId = this[DocumentDraftsTable.aiDraftSourceRunId]
                ?.let { IngestionRunId.parse(it.toString()) },
            draftVersion = this[DocumentDraftsTable.draftVersion],
            draftEditedAt = this[DocumentDraftsTable.draftEditedAt],
            draftEditedBy = this[DocumentDraftsTable.draftEditedBy]?.let { UserId(it.toKotlinUuid()) },
            counterparty = counterpartyInfo,
            counterpartyDisplayName = contactName
                ?: if (counterpartyInfo.isUnresolved()) counterpartyInfo.snapshot?.name else null,
            rejectReason = this[DocumentDraftsTable.rejectReason],
            lastSuccessfulRunId = this[DocumentDraftsTable.lastSuccessfulRunId]
                ?.let { IngestionRunId.parse(it.toString()) },
            createdAt = this[DocumentDraftsTable.createdAt],
            updatedAt = this[DocumentDraftsTable.updatedAt]
        )
    }

    companion object {
        internal fun buildCounterpartyInfo(row: ResultRow): CounterpartyInfo? {
            val linkedContactId = row[DocumentDraftsTable.linkedContactId]
                ?.let { ContactId(it.toKotlinUuid()) }
            val linkedContactSource = row[DocumentDraftsTable.linkedContactSource]
            val matchEvidence = row[DocumentDraftsTable.matchEvidence]
                ?.let { json.decodeFromString<MatchEvidence>(it) }

            if (linkedContactId != null && linkedContactSource != null) {
                return CounterpartyInfo.Linked(
                    contactId = linkedContactId,
                    source = linkedContactSource,
                    evidence = matchEvidence,
                )
            }

            val snapshot = row[DocumentDraftsTable.counterpartySnapshot]
                ?.let { json.decodeFromString<CounterpartySnapshot>(it) }
            val suggestions = row[DocumentDraftsTable.contactSuggestions]
                ?.let { json.decodeFromString<List<SuggestedContact>>(it) }
                ?: emptyList()
            val pendingCreation = row[DocumentDraftsTable.pendingCreation]

            if (snapshot != null || suggestions.isNotEmpty() || pendingCreation) {
                return CounterpartyInfo.Unresolved(
                    snapshot = snapshot,
                    suggestions = suggestions,
                    pendingCreation = pendingCreation,
                )
            }

            return null
        }
    }
}
