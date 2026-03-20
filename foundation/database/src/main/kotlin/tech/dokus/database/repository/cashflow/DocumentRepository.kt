package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toStdlibInstant
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.entity.DraftSummaryEntity
import tech.dokus.database.entity.IngestionRunSummaryEntity
import tech.dokus.database.mapper.toDocumentDto
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentFieldProvenance
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.CounterpartySnapshotDto
import tech.dokus.domain.model.contact.MatchEvidenceDto
import tech.dokus.domain.model.contact.SuggestedContactDto
import tech.dokus.domain.model.contact.isUnresolved
import tech.dokus.domain.model.toDirection
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.model.toSortDate
import tech.dokus.domain.repository.DocumentStatusChecker
import tech.dokus.domain.utils.json
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Result of listing documents with their optional drafts and ingestion info.
 */
data class DocumentWithDraftAndIngestion(
    val document: DocumentDto,
    val draft: DraftSummaryEntity?,
    val latestIngestion: IngestionRunSummaryEntity?
)

/**
 * Paginated result of document listing queries.
 */
data class DocumentListPage<T>(
    val items: List<T>,
    val totalCount: Long
)

data class DocumentOperationalCounts(
    val total: Long,
    val needsAttention: Long,
    val confirmed: Long
)

data class DocumentCreatePayload(
    val canonicalIdentityKey: String? = null,
)

/**
 * Unified repository for document CRUD and draft operations.
 * CRITICAL: All queries filter by tenantId for security.
 *
 * Implements DocumentStatusChecker for chat confirmation checks.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentRepository : DocumentStatusChecker {

    // =========================================================================
    // Document CRUD
    // =========================================================================

    /**
     * Create a new document record.
     */
    suspend fun create(
        tenantId: TenantId,
        payload: DocumentCreatePayload
    ): DocumentId = newSuspendedTransaction {
        val id = DocumentId.generate()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentsTable.insert {
            it[DocumentsTable.id] = UUID.fromString(id.toString())
            it[DocumentsTable.tenantId] = UUID.fromString(tenantId.toString())
            it[DocumentsTable.canonicalIdentityKey] = payload.canonicalIdentityKey
            it[DocumentsTable.sortDate] = now.date
        }
        id
    }

    /**
     * Get a document by ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getById(tenantId: TenantId, documentId: DocumentId): DocumentDto? =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toDocumentDto() }
                .singleOrNull()
        }

    /**
     * Get a document by identity key hash.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByIdentityKeyHash(tenantId: TenantId, identityKeyHash: String): DocumentDto? =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.canonicalIdentityKey eq identityKeyHash) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toDocumentDto() }
                .singleOrNull()
        }

    suspend fun updateCanonicalIdentityKey(
        tenantId: TenantId,
        documentId: DocumentId,
        canonicalIdentityKey: String?
    ): Boolean = newSuspendedTransaction {
        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.canonicalIdentityKey] = canonicalIdentityKey
        } > 0
    }

    /**
     * List all documents for a tenant with pagination.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        page: Int = 0,
        limit: Int = 20
    ): DocumentListPage<DocumentDto> = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())

        val baseQuery = DocumentsTable.selectAll()
            .where { DocumentsTable.tenantId eq tenantIdUuid }

        val total = baseQuery.count()

        val documents = baseQuery
            .orderBy(DocumentsTable.uploadedAt, SortOrder.DESC)
            .limit(limit)
            .offset((page * limit).toLong())
            .map { it.toDocumentDto() }

        DocumentListPage(documents, total)
    }

    /**
     * List documents with optional drafts and latest ingestion info.
     * This is the primary query for the document list endpoint.
     *
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun listWithDraftsAndIngestion(
        tenantId: TenantId,
        filter: DocumentListFilter? = null,
        documentStatus: DocumentStatus? = null,
        documentType: DocumentType? = null,
        ingestionStatus: IngestionStatus? = null,
        sortBy: String? = null,
        page: Int = 0,
        limit: Int = 20
    ): DocumentListPage<DocumentWithDraftAndIngestion> {
        DocumentIngestionRunRepository().recoverStaleProcessingRunsForTenant(tenantId)
        return DocumentListingQuery.listWithDraftsAndIngestion(
            tenantId = tenantId,
            filter = filter,
            documentStatus = documentStatus,
            documentType = documentType,
            ingestionStatus = ingestionStatus,
            sortBy = sortBy,
            page = page,
            limit = limit
        )
    }

    suspend fun getOperationalCounts(
        tenantId: TenantId
    ): DocumentOperationalCounts {
        DocumentIngestionRunRepository().recoverStaleProcessingRunsForTenant(tenantId)
        return DocumentOperationalCounts(
            total = DocumentListingQuery.countWithDraftsAndIngestion(
                tenantId = tenantId,
                filter = DocumentListFilter.All,
                documentStatus = null,
                documentType = null,
                ingestionStatus = null
            ),
            needsAttention = DocumentListingQuery.countWithDraftsAndIngestion(
                tenantId = tenantId,
                filter = DocumentListFilter.NeedsAttention,
                documentStatus = null,
                documentType = null,
                ingestionStatus = null
            ),
            confirmed = DocumentListingQuery.countWithDraftsAndIngestion(
                tenantId = tenantId,
                filter = DocumentListFilter.Confirmed,
                documentStatus = null,
                documentType = null,
                ingestionStatus = null
            )
        )
    }

    // ── Processing health ────────────────────────────────────────────

    data class ProcessingHealthStats(
        val totalProcessedLast30Days: Int,
        val needsReviewCount: Int,
        val failedCount: Int,
        val eligibleForReprocessCount: Int,
    )

    /**
     * Shared eligibility predicate for reprocessing.
     * A document is eligible when:
     * - Not Confirmed
     * - NeedsReview OR has no successful run (lastSuccessfulRunId IS NULL)
     * - Processing version is older than current (or null)
     *
     * Used by both [getProcessingHealthStats] and [findDocumentsEligibleForReprocess]
     * to guarantee the same criteria.
     */
    private fun isEligibleForReprocess(
        status: DocumentStatus?,
        lastSuccessfulRunId: Any?,
        processingVersion: Int?,
        currentProcessingVersion: Int,
    ): Boolean {
        if (status == DocumentStatus.Confirmed) return false
        val isCandidate = status == DocumentStatus.NeedsReview || lastSuccessfulRunId == null
        if (!isCandidate) return false
        val version = processingVersion ?: 0
        return version < currentProcessingVersion
    }

    /**
     * Compute processing health stats for the workspace.
     * Scoped to last 30 days. Joins with ingestion runs to check processing version.
     * CRITICAL: filters by tenantId.
     */
    suspend fun getProcessingHealthStats(
        tenantId: TenantId,
        currentProcessingVersion: Int,
    ): ProcessingHealthStats = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val cutoff = Clock.System.now().minus(30.days).toLocalDateTime(TimeZone.UTC)
        val runs = DocumentIngestionRunsTable

        val rows = DocumentsTable
            .join(runs, JoinType.LEFT, additionalConstraint = {
                DocumentsTable.lastSuccessfulRunId eq runs.id
            })
            .selectAll()
            .where {
                (DocumentsTable.tenantId eq tenantUuid) and
                    (DocumentsTable.updatedAt greaterEq cutoff)
            }
            .toList()

        var total = 0
        var needsReview = 0
        var failed = 0
        var eligible = 0

        for (row in rows) {
            total++
            val status = row[DocumentsTable.documentStatus]
            val lastRunId = row.getOrNull(DocumentsTable.lastSuccessfulRunId)

            when (status) {
                DocumentStatus.NeedsReview -> needsReview++
                DocumentStatus.Rejected,
                DocumentStatus.Confirmed,
                DocumentStatus.Unsupported,
                null -> {}
            }

            if (lastRunId == null && status != DocumentStatus.Confirmed) {
                failed++
            }

            if (isEligibleForReprocess(status, lastRunId, row.getOrNull(runs.processingVersion), currentProcessingVersion)) {
                eligible++
            }
        }

        ProcessingHealthStats(
            totalProcessedLast30Days = total,
            needsReviewCount = needsReview,
            failedCount = failed,
            eligibleForReprocessCount = eligible,
        )
    }

    data class DocumentReprocessCandidate(
        val documentId: DocumentId,
        val sourceId: DocumentSourceId?,
    )

    /**
     * Find documents eligible for bulk reprocessing.
     * Uses the same eligibility criteria as [getProcessingHealthStats].
     * Excludes documents with active (Queued/Processing) ingestion runs.
     *
     * Safety: reuses existing source, does not mutate document, does not
     * overwrite confirmed truth — only identifies candidates for new runs.
     *
     * CRITICAL: filters by tenantId.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun findDocumentsEligibleForReprocess(
        tenantId: TenantId,
        currentProcessingVersion: Int,
        limit: Int = 500,
    ): List<DocumentReprocessCandidate> = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val cutoff = Clock.System.now().minus(30.days).toLocalDateTime(TimeZone.UTC)
        val runs = DocumentIngestionRunsTable

        // Same eligibility criteria as getProcessingHealthStats:
        // not Confirmed, (NeedsReview OR no successful run), old/null processing version
        val candidates = DocumentsTable
            .join(runs, JoinType.LEFT, additionalConstraint = {
                DocumentsTable.lastSuccessfulRunId eq runs.id
            })
            .select(DocumentsTable.id)
            .where {
                (DocumentsTable.tenantId eq tenantUuid) and
                    (DocumentsTable.updatedAt greaterEq cutoff) and
                    (
                        DocumentsTable.documentStatus.isNull() or
                            (DocumentsTable.documentStatus neq DocumentStatus.Confirmed)
                        ) and
                    (
                        (DocumentsTable.documentStatus eq DocumentStatus.NeedsReview) or
                            (DocumentsTable.lastSuccessfulRunId.isNull())
                        ) and
                    ((runs.processingVersion.isNull()) or (runs.processingVersion less currentProcessingVersion))
            }
            .limit(limit)
            .map { row -> DocumentId(row[DocumentsTable.id].value.toKotlinUuid()) }

        // Filter out documents that already have active runs
        val activeDocIds = if (candidates.isNotEmpty()) {
            DocumentIngestionRunsTable
                .select(DocumentIngestionRunsTable.documentId)
                .where {
                    (DocumentIngestionRunsTable.tenantId eq tenantUuid) and
                        (DocumentIngestionRunsTable.documentId inList candidates.map { UUID.fromString(it.toString()) }) and
                        (DocumentIngestionRunsTable.status inList listOf(IngestionStatus.Queued, IngestionStatus.Processing))
                }
                .map { it[DocumentIngestionRunsTable.documentId] }
                .toSet()
        } else {
            emptySet()
        }

        candidates
            .filter { UUID.fromString(it.toString()) !in activeDocIds }
            .map { docId -> DocumentReprocessCandidate(documentId = docId, sourceId = null) }
    }

    /**
     * Delete a document.
     * CRITICAL: Must filter by tenantId.
     * Note: The actual file in MinIO should be deleted separately.
     * Note: Cascades to ingestion runs.
     */
    suspend fun delete(tenantId: TenantId, documentId: DocumentId): Boolean =
        newSuspendedTransaction {
            DocumentsTable.deleteWhere {
                (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                    (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
            } > 0
        }

    /**
     * Check if a document exists.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun exists(tenantId: TenantId, documentId: DocumentId): Boolean =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .count() > 0
        }

    // =========================================================================
    // Draft Operations
    // =========================================================================

    /**
     * Create or update a draft from an ingestion run result.
     *
     * Rules:
     * - ai_draft_source_run_id: Set ONLY if null (from first successful run, immutable)
     * - canonical_data: Set only if force=true OR draftVersion == 0 (not user-edited)
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
        force: Boolean = false,
        fieldProvenance: DocumentFieldProvenance? = null
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toStdlibInstant().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = UUID.fromString(documentId.toString())
        val tenantIdUuid = UUID.fromString(tenantId.toString())
        val runIdUuid = UUID.fromString(runId.toString())
        val keywordsJson = aiKeywords.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it) }

        // Get current document state for draft update rules
        val existing = DocumentsTable.selectAll()
            .where {
                (DocumentsTable.id eq docIdUuid) and
                    (DocumentsTable.tenantId eq tenantIdUuid)
            }
            .singleOrNull() ?: return@newSuspendedTransaction false

        val hasAiDraftRun = existing[DocumentsTable.aiDraftSourceRunId] != null

        DocumentsTable.update({
            (DocumentsTable.id eq docIdUuid) and
                (DocumentsTable.tenantId eq tenantIdUuid)
        }) {
            // aiDraftSourceRunId + keywords: set only on first successful run
            if (!hasAiDraftRun) {
                it[DocumentsTable.aiKeywords] = keywordsJson
                it[aiDraftSourceRunId] = runIdUuid
            }

            it[DocumentsTable.documentType] = extractedData.toDocumentType()
            it[DocumentsTable.direction] = extractedData.toDirection()
            it[DocumentsTable.documentStatus] = DocumentStatus.NeedsReview
            it[DocumentsTable.sortDate] = extractedData.toSortDate() ?: existing[DocumentsTable.uploadedAt].date
            if (keywordsJson != null) {
                it[DocumentsTable.aiKeywords] = keywordsJson
            }
            if (fieldProvenance != null) {
                it[DocumentsTable.fieldProvenance] = json.encodeToString(fieldProvenance)
            }

            it[lastSuccessfulRunId] = runIdUuid
            it[updatedAt] = now
        } > 0
    }

    /**
     * Get a draft by document ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getDraftByDocumentId(
        documentId: DocumentId,
        tenantId: TenantId
    ): DraftSummaryEntity? = newSuspendedTransaction {
        DocumentsTable
            .join(ContactsTable, JoinType.LEFT, DocumentsTable.linkedContactId, ContactsTable.id) {
                ContactsTable.tenantId eq DocumentsTable.tenantId
            }
            .selectAll()
            .where {
                (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                    (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            .map { DraftSummaryEntity.from(it, contactName = it.getOrNull(ContactsTable.name)) }
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
        val current = DocumentsTable.selectAll()
            .where {
                (DocumentsTable.id eq docIdUuid) and
                    (DocumentsTable.tenantId eq tenantIdUuid)
            }
            .singleOrNull() ?: return@newSuspendedTransaction null

        val currentVersion = current[DocumentsTable.draftVersion]
        val newVersion = currentVersion + 1

        val currentStatus = current[DocumentsTable.documentStatus]
        val nextStatus = if (currentStatus == DocumentStatus.Confirmed) {
            // If a confirmed draft is edited, require review again.
            DocumentStatus.NeedsReview
        } else {
            currentStatus
        }

        DocumentsTable.update({
            (DocumentsTable.id eq docIdUuid) and
                (DocumentsTable.tenantId eq tenantIdUuid)
        }) {
            it[DocumentsTable.documentType] = updatedData.toDocumentType()
            it[direction] = updatedData.toDirection()
            it[DocumentsTable.sortDate] = updatedData.toSortDate() ?: current[DocumentsTable.uploadedAt].date
            it[draftVersion] = newVersion
            it[draftEditedAt] = now
            it[draftEditedBy] = UUID.fromString(userId.toString())
            if (nextStatus != currentStatus) {
                it[DocumentsTable.documentStatus] = nextStatus
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
        val extractedSortDate = extractedData.toSortDate()
        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.documentType] = extractedData.toDocumentType()
            it[DocumentsTable.direction] = extractedData.toDirection()
            it[DocumentsTable.documentStatus] = status
            if (extractedSortDate != null) {
                it[DocumentsTable.sortDate] = extractedSortDate
            }
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
        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.documentStatus] = status
            if (status != DocumentStatus.Rejected) {
                it[rejectReason] = null
            }
            it[updatedAt] = now
        } > 0
    }

    /**
     * Update counterparty info on a document.
     * If the document was previously confirmed, it transitions to NeedsReview.
     */
    suspend fun updateCounterparty(
        documentId: DocumentId,
        tenantId: TenantId,
        counterparty: CounterpartyInfo
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val docIdUuid = UUID.fromString(documentId.toString())
        val tenantIdUuid = UUID.fromString(tenantId.toString())

        val current = DocumentsTable.selectAll()
            .where {
                (DocumentsTable.id eq docIdUuid) and
                    (DocumentsTable.tenantId eq tenantIdUuid)
            }
            .singleOrNull() ?: return@newSuspendedTransaction false

        val currentStatus = current[DocumentsTable.documentStatus]
        val shouldReview = currentStatus == DocumentStatus.Confirmed

        DocumentsTable.update({
            (DocumentsTable.id eq docIdUuid) and
                (DocumentsTable.tenantId eq tenantIdUuid)
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
                it[DocumentsTable.documentStatus] = DocumentStatus.NeedsReview
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
        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.documentStatus] = DocumentStatus.Rejected
            it[rejectReason] = reason
            it[updatedAt] = now
        } > 0
    }

    /**
     * List drafts by status with optional document type filter.
     * CRITICAL: Must filter by tenantId.
     *
     */
    suspend fun listByStatus(
        tenantId: TenantId,
        statuses: List<DocumentStatus>,
        documentType: DocumentType? = null,
        page: Int = 0,
        limit: Int = 20
    ): DocumentListPage<DraftSummaryEntity> = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())

        val baseQuery = DocumentsTable.selectAll()
            .where {
                val conditions = (DocumentsTable.tenantId eq tenantIdUuid) and
                    (DocumentsTable.documentStatus inList statuses)
                if (documentType != null) {
                    conditions and (DocumentsTable.documentType eq documentType)
                } else {
                    conditions
                }
            }

        val total = baseQuery.count()

        val drafts = baseQuery
            .orderBy(DocumentsTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .offset((page * limit).toLong())
            .map { DraftSummaryEntity.from(it) }

        DocumentListPage(drafts, total)
    }

    // =========================================================================
    // Contact Resolution
    // =========================================================================

    /**
     * Update contact resolution results (snapshot, counterparty info).
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun updateContactResolution(
        documentId: DocumentId,
        tenantId: TenantId,
        counterpartySnapshot: CounterpartySnapshotDto? = null,
        counterparty: CounterpartyInfo
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val snapshotJson = counterpartySnapshot?.let { json.encodeToString(it) }

        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.counterpartySnapshot] = snapshotJson

            when (counterparty) {
                is CounterpartyInfo.Linked -> {
                    it[DocumentsTable.linkedContactId] = UUID.fromString(counterparty.contactId.toString())
                    it[DocumentsTable.linkedContactSource] = counterparty.source
                    it[DocumentsTable.matchEvidence] = counterparty.evidence?.let { ev -> json.encodeToString(ev) }
                    it[DocumentsTable.contactSuggestions] = null
                    it[DocumentsTable.pendingCreation] = false
                }
                is CounterpartyInfo.Unresolved -> {
                    it[DocumentsTable.linkedContactId] = null
                    it[DocumentsTable.linkedContactSource] = null
                    it[DocumentsTable.matchEvidence] = null
                    it[DocumentsTable.contactSuggestions] = counterparty.suggestions
                        .takeIf { s -> s.isNotEmpty() }
                        ?.let { s -> json.encodeToString(s) }
                    it[DocumentsTable.pendingCreation] = counterparty.pendingCreation
                }
            }

            it[DocumentsTable.updatedAt] = now
        } > 0
    }

    // =========================================================================
    // Purpose Fields
    // =========================================================================

    suspend fun updatePurposeContext(
        documentId: DocumentId,
        tenantId: TenantId,
        counterpartyKey: String?,
        merchantToken: String?
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            counterpartyKey?.let { key -> it[DocumentsTable.counterpartyKey] = key }
            merchantToken?.let { token -> it[DocumentsTable.merchantToken] = token }
            it[DocumentsTable.updatedAt] = now
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
        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.purposeBase] = purposeBase
            it[DocumentsTable.purposePeriodYear] = purposePeriodYear
            it[DocumentsTable.purposePeriodMonth] = purposePeriodMonth
            it[DocumentsTable.purposeRendered] = purposeRendered
            it[DocumentsTable.purposeSource] = purposeSource
            it[DocumentsTable.purposeLocked] = purposeLocked
            it[DocumentsTable.purposePeriodMode] = purposePeriodMode
            it[DocumentsTable.updatedAt] = now
        } > 0
    }

    suspend fun listConfirmedPurposeBasesByCounterparty(
        tenantId: TenantId,
        counterpartyKey: String,
        documentType: DocumentType,
        limit: Int = 5
    ): List<String> = newSuspendedTransaction {
        DocumentsTable.selectAll()
            .where {
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentsTable.counterpartyKey eq counterpartyKey) and
                    (DocumentsTable.documentType eq documentType) and
                    (DocumentsTable.documentStatus eq DocumentStatus.Confirmed)
            }
            .orderBy(DocumentsTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .mapNotNull { row -> row[DocumentsTable.purposeBase] }
    }

    suspend fun listConfirmedPurposeBasesByMerchantToken(
        tenantId: TenantId,
        merchantToken: String,
        documentType: DocumentType,
        limit: Int = 5
    ): List<String> = newSuspendedTransaction {
        DocumentsTable.selectAll()
            .where {
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (DocumentsTable.merchantToken eq merchantToken) and
                    (DocumentsTable.documentType eq documentType) and
                    (DocumentsTable.documentStatus eq DocumentStatus.Confirmed)
            }
            .orderBy(DocumentsTable.updatedAt, SortOrder.DESC)
            .limit(limit)
            .mapNotNull { row -> row[DocumentsTable.purposeBase] }
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
        val draft = DocumentsTable.selectAll()
            .where {
                (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                    (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            .singleOrNull()

        draft?.get(DocumentsTable.documentStatus) == DocumentStatus.Confirmed
    }

    companion object {
        internal fun buildCounterpartyInfo(row: ResultRow): CounterpartyInfo? {
            val linkedContactId = row[DocumentsTable.linkedContactId]
                ?.let { ContactId(it.toKotlinUuid()) }
            val linkedContactSource = row[DocumentsTable.linkedContactSource]
            val matchEvidence = row[DocumentsTable.matchEvidence]
                ?.let { json.decodeFromString<MatchEvidenceDto>(it) }

            if (linkedContactId != null && linkedContactSource != null) {
                return CounterpartyInfo.Linked(
                    contactId = linkedContactId,
                    source = linkedContactSource,
                    evidence = matchEvidence,
                )
            }

            val snapshot = row[DocumentsTable.counterpartySnapshot]
                ?.let { json.decodeFromString<CounterpartySnapshotDto>(it) }
            val suggestions = row[DocumentsTable.contactSuggestions]
                ?.let { json.decodeFromString<List<SuggestedContactDto>>(it) }
                ?: emptyList()
            val pendingCreation = row[DocumentsTable.pendingCreation]

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
