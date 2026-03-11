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
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.model.contact.MatchEvidence
import tech.dokus.domain.model.contact.SuggestedContact
import tech.dokus.domain.model.contact.isUnresolved
import tech.dokus.domain.model.toDirection
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.repository.DocumentStatusChecker
import tech.dokus.domain.utils.json
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Result of listing documents with their optional drafts and ingestion info.
 */
data class DocumentWithDraftAndIngestion(
    val document: DocumentDto,
    val draft: DraftSummary?,
    val latestIngestion: IngestionRunSummary?
)

/**
 * Paginated result of document listing queries.
 */
data class DocumentListPage<T>(
    val items: List<T>,
    val totalCount: Long
)

data class DocumentOperationalCounts(
    val needsAttention: Long,
    val confirmed: Long
)

data class DocumentCreatePayload(
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val storageKey: String,
    val canonicalContentHash: String?,
    val canonicalIdentityKey: String? = null,
    val effectiveOrigin: DocumentSource = DocumentSource.Upload
)

/**
 * Data class for draft summary.
 */
data class DraftSummary(
    val documentId: DocumentId,
    val tenantId: TenantId,
    val documentStatus: DocumentStatus,
    val documentType: DocumentType?,
    val direction: DocumentDirection = DocumentDirection.Unknown,
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
        DocumentsTable.insert {
            it[DocumentsTable.id] = UUID.fromString(id.toString())
            it[DocumentsTable.tenantId] = UUID.fromString(tenantId.toString())
            it[DocumentsTable.filename] = payload.filename
            it[DocumentsTable.contentType] = payload.contentType
            it[DocumentsTable.sizeBytes] = payload.sizeBytes
            it[DocumentsTable.storageKey] = payload.storageKey
            it[DocumentsTable.canonicalContentHash] = payload.canonicalContentHash
            it[DocumentsTable.canonicalIdentityKey] = payload.canonicalIdentityKey
            it[DocumentsTable.effectiveOrigin] = payload.effectiveOrigin
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
     * Get the content hash for a document by ID.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getContentHash(tenantId: TenantId, documentId: DocumentId): String? =
        newSuspendedTransaction {
            DocumentsTable
                .select(DocumentsTable.canonicalContentHash)
                .where {
                    (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .singleOrNull()
                ?.get(DocumentsTable.canonicalContentHash)
        }

    /**
     * Get a document by storage key.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByStorageKey(tenantId: TenantId, storageKey: String): DocumentDto? =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.storageKey eq storageKey) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toDocumentDto() }
                .singleOrNull()
        }

    /**
     * Get a document by content hash.
     * CRITICAL: Must filter by tenantId.
     */
    suspend fun getByContentHash(tenantId: TenantId, contentHash: String): DocumentDto? =
        newSuspendedTransaction {
            DocumentsTable.selectAll()
                .where {
                    (DocumentsTable.canonicalContentHash eq contentHash) and
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

    suspend fun updateCanonicalContentHash(
        tenantId: TenantId,
        documentId: DocumentId,
        canonicalContentHash: String?
    ): Boolean = newSuspendedTransaction {
        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.canonicalContentHash] = canonicalContentHash
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
            page = page,
            limit = limit
        )
    }

    suspend fun getOperationalCounts(
        tenantId: TenantId
    ): DocumentOperationalCounts {
        DocumentIngestionRunRepository().recoverStaleProcessingRunsForTenant(tenantId)
        return DocumentOperationalCounts(
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
        force: Boolean = false
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
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

        val currentVersion = existing[DocumentsTable.draftVersion]
        val hasAiDraftRun = existing[DocumentsTable.aiDraftSourceRunId] != null
        val shouldUpdateExtracted = force || currentVersion == 0

        DocumentsTable.update({
            (DocumentsTable.id eq docIdUuid) and
                (DocumentsTable.tenantId eq tenantIdUuid)
        }) {
            // aiDraftSourceRunId + keywords: set only on first successful run
            if (!hasAiDraftRun) {
                it[DocumentsTable.aiKeywords] = keywordsJson
                it[aiDraftSourceRunId] = runIdUuid
            }

            // canonical_data: update only if force or not user-edited
            if (shouldUpdateExtracted) {
                it[DocumentsTable.canonicalData] = json.encodeToString(extractedData)
                it[DocumentsTable.documentType] = documentType
                it[DocumentsTable.direction] = extractedData.toDirection()
                it[DocumentsTable.documentStatus] = DocumentStatus.NeedsReview
                if (keywordsJson != null) {
                    it[DocumentsTable.aiKeywords] = keywordsJson
                }
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
    ): DraftSummary? = newSuspendedTransaction {
        DocumentsTable
            .join(ContactsTable, JoinType.LEFT, DocumentsTable.linkedContactId, ContactsTable.id) {
                ContactsTable.tenantId eq DocumentsTable.tenantId
            }
            .selectAll()
            .where {
                (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                    (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
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
            it[canonicalData] = json.encodeToString(updatedData)
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
        DocumentsTable.update({
            (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
        }) {
            it[DocumentsTable.canonicalData] = json.encodeToString(extractedData)
            it[DocumentsTable.documentType] = extractedData.toDocumentType()
            it[DocumentsTable.direction] = extractedData.toDirection()
            it[DocumentsTable.documentStatus] = status
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
            .map { it.toDraftSummary() }

        drafts to total
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
        counterpartySnapshot: CounterpartySnapshot? = null,
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

    // =========================================================================
    // Mappers
    // =========================================================================

    private fun ResultRow.toDraftSummary(contactName: String? = null): DraftSummary {
        val counterpartyInfo = buildCounterpartyInfo(this)
        return DraftSummary(
            documentId = DocumentId.parse(this[DocumentsTable.id].toString()),
            tenantId = TenantId(this[DocumentsTable.tenantId].toKotlinUuid()),
            documentStatus = this[DocumentsTable.documentStatus] ?: DocumentStatus.NeedsReview,
            documentType = this[DocumentsTable.documentType],
            direction = this[DocumentsTable.direction] ?: DocumentDirection.Unknown,
            extractedData = this[DocumentsTable.canonicalData]?.let { json.decodeFromString(it) },
            aiKeywords = this[DocumentsTable.aiKeywords]?.let { json.decodeFromString(it) } ?: emptyList(),
            purposeBase = this[DocumentsTable.purposeBase],
            purposePeriodYear = this[DocumentsTable.purposePeriodYear],
            purposePeriodMonth = this[DocumentsTable.purposePeriodMonth],
            purposeRendered = this[DocumentsTable.purposeRendered],
            purposeSource = this[DocumentsTable.purposeSource],
            purposeLocked = this[DocumentsTable.purposeLocked],
            purposePeriodMode = this[DocumentsTable.purposePeriodMode],
            counterpartyKey = this[DocumentsTable.counterpartyKey],
            merchantToken = this[DocumentsTable.merchantToken],
            aiDraftSourceRunId = this[DocumentsTable.aiDraftSourceRunId]
                ?.let { IngestionRunId.parse(it.toString()) },
            draftVersion = this[DocumentsTable.draftVersion],
            draftEditedAt = this[DocumentsTable.draftEditedAt],
            draftEditedBy = this[DocumentsTable.draftEditedBy]?.let { UserId(it.toKotlinUuid()) },
            counterparty = counterpartyInfo,
            counterpartyDisplayName = contactName
                ?: if (counterpartyInfo.isUnresolved()) counterpartyInfo.snapshot?.name else null,
            rejectReason = this[DocumentsTable.rejectReason],
            lastSuccessfulRunId = this[DocumentsTable.lastSuccessfulRunId]
                ?.let { IngestionRunId.parse(it.toString()) },
            createdAt = this[DocumentsTable.uploadedAt],
            updatedAt = this[DocumentsTable.updatedAt]
        )
    }

    companion object {
        internal fun buildCounterpartyInfo(row: ResultRow): CounterpartyInfo? {
            val linkedContactId = row[DocumentsTable.linkedContactId]
                ?.let { ContactId(it.toKotlinUuid()) }
            val linkedContactSource = row[DocumentsTable.linkedContactSource]
            val matchEvidence = row[DocumentsTable.matchEvidence]
                ?.let { json.decodeFromString<MatchEvidence>(it) }

            if (linkedContactId != null && linkedContactSource != null) {
                return CounterpartyInfo.Linked(
                    contactId = linkedContactId,
                    source = linkedContactSource,
                    evidence = matchEvidence,
                )
            }

            val snapshot = row[DocumentsTable.counterpartySnapshot]
                ?.let { json.decodeFromString<CounterpartySnapshot>(it) }
            val suggestions = row[DocumentsTable.contactSuggestions]
                ?.let { json.decodeFromString<List<SuggestedContact>>(it) }
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
