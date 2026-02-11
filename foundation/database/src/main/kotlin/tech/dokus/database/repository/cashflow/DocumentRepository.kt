package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.LowerCase
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.not
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.slice
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.DocumentDto
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

data class DocumentCreatePayload(
    val filename: String,
    val contentType: String,
    val sizeBytes: Long,
    val storageKey: String,
    val contentHash: String?,
    val source: DocumentSource = DocumentSource.Upload
)

/**
 * Repository for document CRUD operations.
 * Documents are pure file metadata. Entity linkage is handled by
 * financial entity tables (Invoice, Expense) which have documentId FK.
 *
 * CRITICAL: All queries filter by tenantId for security.
 */
@OptIn(ExperimentalUuidApi::class)
class DocumentRepository {

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
            it[DocumentsTable.contentHash] = payload.contentHash
            it[DocumentsTable.documentSource] = payload.source
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
                .select(DocumentsTable.contentHash)
                .where {
                    (DocumentsTable.id eq UUID.fromString(documentId.toString())) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .singleOrNull()
                ?.get(DocumentsTable.contentHash)
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
                    (DocumentsTable.contentHash eq contentHash) and
                        (DocumentsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toDocumentDto() }
                .singleOrNull()
        }

    /**
     * List all documents for a tenant with pagination.
     * CRITICAL: Must filter by tenantId.
     *
     * @return Pair of (documents, totalCount)
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        page: Int = 0,
        limit: Int = 20
    ): Pair<List<DocumentDto>, Long> = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())

        val baseQuery = DocumentsTable.selectAll()
            .where { DocumentsTable.tenantId eq tenantIdUuid }

        val total = baseQuery.count()

        val documents = baseQuery
            .orderBy(DocumentsTable.uploadedAt, SortOrder.DESC)
            .limit(limit)
            .offset((page * limit).toLong())
            .map { it.toDocumentDto() }

        documents to total
    }

    /**
     * List documents with optional drafts and latest ingestion info.
     * This is the primary query for the document list endpoint.
     *
     * Documents are returned regardless of whether they have drafts.
     * Filters:
     * - documentStatus: Only applies when draft exists (documents without drafts pass this filter)
     * - documentType: Only applies when draft exists
     * - ingestionStatus: Filters by latest ingestion status
     * - search: Filters by filename (ILIKE)
     *
     * CRITICAL: Must filter by tenantId.
     *
     * @return Pair of (documents with drafts/ingestion, totalCount)
     */
    suspend fun listWithDraftsAndIngestion(
        tenantId: TenantId,
        filter: DocumentListFilter? = null,
        documentStatus: DocumentStatus? = null,
        documentType: DocumentType? = null,
        ingestionStatus: IngestionStatus? = null,
        search: String? = null,
        page: Int = 0,
        limit: Int = 20
    ): Pair<List<DocumentWithDraftAndIngestion>, Long> = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())
        val trimmedSearch = search?.trim()?.takeIf { it.isNotEmpty() }

        // Precedence: when using the high-level filter, ignore lower-level status filters.
        val effectiveDocumentStatus = if (filter != null) null else documentStatus
        val effectiveIngestionStatus = if (filter != null) null else ingestionStatus

        // Latest ingestion selection is deterministic and DB-driven:
        // Processing > latest Succeeded/Failed (by finishedAt) > latest Queued (by queuedAt).
        val maxProcessingStartedAt = DocumentIngestionRunsTable.startedAt.max().alias("max_started_at")
        val processingMaxStartedAt = DocumentIngestionRunsTable
            .slice(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId, maxProcessingStartedAt)
            .select {
                (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                    (DocumentIngestionRunsTable.status eq IngestionStatus.Processing)
            }
            .groupBy(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId)
            .alias("processing_max_started_at")

        val processingRunId = DocumentIngestionRunsTable.id.max().alias("processing_run_id")
        val processingSelected = DocumentIngestionRunsTable
            .join(processingMaxStartedAt, joinType = JoinType.INNER, additionalConstraint = {
                (DocumentIngestionRunsTable.documentId eq processingMaxStartedAt[DocumentIngestionRunsTable.documentId]) and
                    (DocumentIngestionRunsTable.tenantId eq processingMaxStartedAt[DocumentIngestionRunsTable.tenantId]) and
                    (DocumentIngestionRunsTable.startedAt eq processingMaxStartedAt[maxProcessingStartedAt]) and
                    (DocumentIngestionRunsTable.status eq IngestionStatus.Processing)
            })
            .slice(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId, processingRunId)
            .selectAll()
            .groupBy(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId)
            .alias("processing_selected")

        val finishedStatuses = listOf(IngestionStatus.Succeeded, IngestionStatus.Failed)
        val maxFinishedAt = DocumentIngestionRunsTable.finishedAt.max().alias("max_finished_at")
        val finishedMaxFinishedAt = DocumentIngestionRunsTable
            .slice(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId, maxFinishedAt)
            .select {
                (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                    (DocumentIngestionRunsTable.status inList finishedStatuses)
            }
            .groupBy(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId)
            .alias("finished_max_finished_at")

        val finishedRunId = DocumentIngestionRunsTable.id.max().alias("finished_run_id")
        val finishedSelected = DocumentIngestionRunsTable
            .join(finishedMaxFinishedAt, joinType = JoinType.INNER, additionalConstraint = {
                (DocumentIngestionRunsTable.documentId eq finishedMaxFinishedAt[DocumentIngestionRunsTable.documentId]) and
                    (DocumentIngestionRunsTable.tenantId eq finishedMaxFinishedAt[DocumentIngestionRunsTable.tenantId]) and
                    (DocumentIngestionRunsTable.finishedAt eq finishedMaxFinishedAt[maxFinishedAt]) and
                    (DocumentIngestionRunsTable.status inList finishedStatuses)
            })
            .slice(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId, finishedRunId)
            .selectAll()
            .groupBy(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId)
            .alias("finished_selected")

        val maxQueuedAt = DocumentIngestionRunsTable.queuedAt.max().alias("max_queued_at")
        val queuedMaxQueuedAt = DocumentIngestionRunsTable
            .slice(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId, maxQueuedAt)
            .select {
                (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                    (DocumentIngestionRunsTable.status eq IngestionStatus.Queued)
            }
            .groupBy(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId)
            .alias("queued_max_queued_at")

        val queuedRunId = DocumentIngestionRunsTable.id.max().alias("queued_run_id")
        val queuedSelected = DocumentIngestionRunsTable
            .join(queuedMaxQueuedAt, joinType = JoinType.INNER, additionalConstraint = {
                (DocumentIngestionRunsTable.documentId eq queuedMaxQueuedAt[DocumentIngestionRunsTable.documentId]) and
                    (DocumentIngestionRunsTable.tenantId eq queuedMaxQueuedAt[DocumentIngestionRunsTable.tenantId]) and
                    (DocumentIngestionRunsTable.queuedAt eq queuedMaxQueuedAt[maxQueuedAt]) and
                    (DocumentIngestionRunsTable.status eq IngestionStatus.Queued)
            })
            .slice(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId, queuedRunId)
            .selectAll()
            .groupBy(DocumentIngestionRunsTable.documentId, DocumentIngestionRunsTable.tenantId)
            .alias("queued_selected")

        val finishedRun = DocumentIngestionRunsTable.alias("finished_run")

        val join = DocumentsTable
            .join(DocumentDraftsTable, joinType = JoinType.LEFT, onColumn = DocumentsTable.id, otherColumn = DocumentDraftsTable.documentId) {
                DocumentDraftsTable.tenantId eq DocumentsTable.tenantId
            }
            .join(processingSelected, joinType = JoinType.LEFT, additionalConstraint = {
                (processingSelected[DocumentIngestionRunsTable.documentId] eq DocumentsTable.id) and
                    (processingSelected[DocumentIngestionRunsTable.tenantId] eq DocumentsTable.tenantId)
            })
            .join(finishedSelected, joinType = JoinType.LEFT, additionalConstraint = {
                (finishedSelected[DocumentIngestionRunsTable.documentId] eq DocumentsTable.id) and
                    (finishedSelected[DocumentIngestionRunsTable.tenantId] eq DocumentsTable.tenantId)
            })
            .join(queuedSelected, joinType = JoinType.LEFT, additionalConstraint = {
                (queuedSelected[DocumentIngestionRunsTable.documentId] eq DocumentsTable.id) and
                    (queuedSelected[DocumentIngestionRunsTable.tenantId] eq DocumentsTable.tenantId)
            })
            .join(finishedRun, joinType = JoinType.LEFT, additionalConstraint = {
                finishedRun[DocumentIngestionRunsTable.id] eq finishedSelected[finishedRunId]
            })

        val processingRunIdExpr = processingSelected[processingRunId]
        val finishedRunIdExpr = finishedSelected[finishedRunId]
        val queuedRunIdExpr = queuedSelected[queuedRunId]
        val finishedRunStatusExpr = finishedRun[DocumentIngestionRunsTable.status]

        val invoiceExists = exists(
            InvoicesTable.slice(InvoicesTable.id).select {
                (InvoicesTable.tenantId eq DocumentsTable.tenantId) and
                    (InvoicesTable.documentId eq DocumentsTable.id)
            }
        )
        val expenseExists = exists(
            ExpensesTable.slice(ExpensesTable.id).select {
                (ExpensesTable.tenantId eq DocumentsTable.tenantId) and
                    (ExpensesTable.documentId eq DocumentsTable.id)
            }
        )
        val creditNoteExists = exists(
            CreditNotesTable.slice(CreditNotesTable.id).select {
                (CreditNotesTable.tenantId eq DocumentsTable.tenantId) and
                    (CreditNotesTable.documentId eq DocumentsTable.id)
            }
        )
        val entityExists = invoiceExists or expenseExists or creditNoteExists

        val confirmedStrict = (DocumentDraftsTable.documentStatus eq DocumentStatus.Confirmed) and entityExists
        val confirmedButNoEntity = (DocumentDraftsTable.documentStatus eq DocumentStatus.Confirmed) and not(entityExists)

        val draftMissing = DocumentDraftsTable.documentId.isNull()
        val draftNotRejected = DocumentDraftsTable.documentStatus.isNull() or
            (DocumentDraftsTable.documentStatus neq DocumentStatus.Rejected)

        val latestIsProcessing = processingRunIdExpr.isNotNull()
        val latestIsQueued = processingRunIdExpr.isNull() and finishedRunIdExpr.isNull() and queuedRunIdExpr.isNotNull()
        val latestIsFailed = processingRunIdExpr.isNull() and finishedRunIdExpr.isNotNull() and
            (finishedRunStatusExpr eq IngestionStatus.Failed)
        val latestIsSucceeded = processingRunIdExpr.isNull() and finishedRunIdExpr.isNotNull() and
            (finishedRunStatusExpr eq IngestionStatus.Succeeded)

        var whereOp = (DocumentsTable.tenantId eq tenantIdUuid) and draftNotRejected

        if (trimmedSearch != null) {
            whereOp = whereOp and (LowerCase(DocumentsTable.filename) like "%${trimmedSearch.lowercase()}%")
        }

        val requiresDraft = effectiveDocumentStatus != null || documentType != null
        if (requiresDraft) {
            whereOp = whereOp and DocumentDraftsTable.documentId.isNotNull()
        }

        if (effectiveDocumentStatus != null) {
            whereOp = whereOp and (DocumentDraftsTable.documentStatus eq effectiveDocumentStatus)
        }

        if (documentType != null) {
            whereOp = whereOp and (DocumentDraftsTable.documentType eq documentType)
        }

        if (effectiveIngestionStatus != null) {
            val ingestionFilterOp = when (effectiveIngestionStatus) {
                IngestionStatus.Processing -> latestIsProcessing
                IngestionStatus.Queued -> latestIsQueued
                IngestionStatus.Failed -> latestIsFailed
                IngestionStatus.Succeeded -> latestIsSucceeded
            }
            whereOp = whereOp and ingestionFilterOp
        }

        when (filter ?: DocumentListFilter.All) {
            DocumentListFilter.All -> Unit

            DocumentListFilter.Confirmed -> {
                whereOp = whereOp and confirmedStrict
            }

            DocumentListFilter.NeedsAttention -> {
                val isNotConfirmed =
                    DocumentDraftsTable.documentStatus.isNull() or
                        (DocumentDraftsTable.documentStatus neq DocumentStatus.Confirmed) or
                        not(entityExists)

                val ingestionNeedsAttention = latestIsQueued or latestIsProcessing or latestIsFailed
                val draftNeedsReview = DocumentDraftsTable.documentStatus eq DocumentStatus.NeedsReview
                val succeededButNoDraft = draftMissing and latestIsSucceeded

                whereOp =
                    whereOp and
                        (
                            confirmedButNoEntity or
                                (isNotConfirmed and (ingestionNeedsAttention or draftNeedsReview or succeededButNoDraft))
                            )
            }
        }

        val baseQuery = join
            .slice(
                DocumentsTable.id,
                processingRunIdExpr,
                finishedRunIdExpr,
                queuedRunIdExpr
            )
            .selectAll()
            .where { whereOp }

        val total = baseQuery.count()

        data class PageRow(
            val documentId: DocumentId,
            val latestRunId: IngestionRunId?
        )

        val offset = (page * limit).toLong()
        val pageRows = baseQuery
            .orderBy(DocumentsTable.uploadedAt to SortOrder.DESC, DocumentsTable.id to SortOrder.DESC)
            .limit(limit)
            .offset(offset)
            .mapNotNull { row ->
                val documentId = DocumentId.parse(row[DocumentsTable.id].toString())
                val processing = row.getOrNull(processingRunIdExpr)
                val finished = row.getOrNull(finishedRunIdExpr)
                val queued = row.getOrNull(queuedRunIdExpr)
                val latest = (processing ?: finished ?: queued)?.let { IngestionRunId.parse(it.toString()) }
                PageRow(documentId = documentId, latestRunId = latest)
            }

        if (pageRows.isEmpty()) {
            return@newSuspendedTransaction emptyList<DocumentWithDraftAndIngestion>() to total
        }

        val documentIds = pageRows.map { UUID.fromString(it.documentId.toString()) }
        val latestRunIds = pageRows.mapNotNull { row -> row.latestRunId?.let { UUID.fromString(it.toString()) } }

        val documentsById = DocumentsTable.selectAll()
            .where {
                (DocumentsTable.tenantId eq tenantIdUuid) and
                    (DocumentsTable.id inList documentIds)
            }
            .associate { row -> DocumentId.parse(row[DocumentsTable.id].toString()) to row.toDocumentDto() }

        val draftsByDocumentId = DocumentDraftsTable.selectAll()
            .where {
                (DocumentDraftsTable.tenantId eq tenantIdUuid) and
                    (DocumentDraftsTable.documentId inList documentIds)
            }
            .associate { row ->
                DocumentId.parse(row[DocumentDraftsTable.documentId].toString()) to row.toDraftSummary()
            }

        val latestIngestionsById = if (latestRunIds.isEmpty()) {
            emptyMap()
        } else {
            DocumentIngestionRunsTable.selectAll()
                .where {
                    (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                        (DocumentIngestionRunsTable.id inList latestRunIds)
                }
                .associate { row ->
                    IngestionRunId.parse(row[DocumentIngestionRunsTable.id].toString()) to row.toIngestionRunSummary()
                }
        }

        val results = pageRows.mapNotNull { row ->
            val document = documentsById[row.documentId] ?: return@mapNotNull null
            DocumentWithDraftAndIngestion(
                document = document,
                draft = draftsByDocumentId[row.documentId],
                latestIngestion = row.latestRunId?.let { latestIngestionsById[it] }
            )
        }

        results to total
    }

    private fun ResultRow.toIngestionRunSummary(): IngestionRunSummary {
        return IngestionRunSummary(
            id = IngestionRunId.parse(this[DocumentIngestionRunsTable.id].toString()),
            documentId = DocumentId.parse(this[DocumentIngestionRunsTable.documentId].toString()),
            tenantId = TenantId(this[DocumentIngestionRunsTable.tenantId].toKotlinUuid()),
            status = this[DocumentIngestionRunsTable.status],
            provider = this[DocumentIngestionRunsTable.provider],
            queuedAt = this[DocumentIngestionRunsTable.queuedAt],
            startedAt = this[DocumentIngestionRunsTable.startedAt],
            finishedAt = this[DocumentIngestionRunsTable.finishedAt],
            errorMessage = this[DocumentIngestionRunsTable.errorMessage],
            confidence = this[DocumentIngestionRunsTable.confidence]?.toDouble(),
            processingOutcome = this[DocumentIngestionRunsTable.processingOutcome],
            rawExtractionJson = this[DocumentIngestionRunsTable.rawExtractionJson],
            processingTrace = this[DocumentIngestionRunsTable.processingTrace]
        )
    }

    private fun ResultRow.toDraftSummary(): DraftSummary {
        return DraftSummary(
            documentId = DocumentId.parse(this[DocumentDraftsTable.documentId].toString()),
            tenantId = TenantId(this[DocumentDraftsTable.tenantId].toKotlinUuid()),
            documentStatus = this[DocumentDraftsTable.documentStatus],
            documentType = this[DocumentDraftsTable.documentType],
            extractedData = this[DocumentDraftsTable.extractedData]?.let { json.decodeFromString(it) },
            aiDraftData = this[DocumentDraftsTable.aiDraftData]?.let { json.decodeFromString(it) },
            aiDescription = this[DocumentDraftsTable.aiDescription],
            aiKeywords = this[DocumentDraftsTable.aiKeywords]?.let { json.decodeFromString(it) } ?: emptyList(),
            aiDraftSourceRunId = this[DocumentDraftsTable.aiDraftSourceRunId]?.let {
                IngestionRunId.parse(
                    it.toString()
                )
            },
            draftVersion = this[DocumentDraftsTable.draftVersion],
            draftEditedAt = this[DocumentDraftsTable.draftEditedAt],
            draftEditedBy = this[DocumentDraftsTable.draftEditedBy]?.let { UserId(it.toKotlinUuid()) },
            contactSuggestions = this[DocumentDraftsTable.contactSuggestions]
                ?.let { json.decodeFromString(it) }
                ?: emptyList(),
            counterpartySnapshot = this[DocumentDraftsTable.counterpartySnapshot]
                ?.let { json.decodeFromString(it) },
            matchEvidence = this[DocumentDraftsTable.matchEvidence]
                ?.let { json.decodeFromString(it) },
            linkedContactId = this[DocumentDraftsTable.linkedContactId]?.let { ContactId(it.toKotlinUuid()) },
            linkedContactSource = this[DocumentDraftsTable.linkedContactSource],
            counterpartyIntent = this[DocumentDraftsTable.counterpartyIntent],
            rejectReason = this[DocumentDraftsTable.rejectReason],
            lastSuccessfulRunId = this[DocumentDraftsTable.lastSuccessfulRunId]?.let {
                IngestionRunId.parse(
                    it.toString()
                )
            },
            createdAt = this[DocumentDraftsTable.createdAt],
            updatedAt = this[DocumentDraftsTable.updatedAt]
        )
    }

    /**
     * Delete a document.
     * CRITICAL: Must filter by tenantId.
     * Note: The actual file in MinIO should be deleted separately.
     * Note: Cascades to ingestion runs and drafts.
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

    private fun ResultRow.toDocumentDto(): DocumentDto {
        return DocumentDto(
            id = DocumentId.parse(this[DocumentsTable.id].toString()),
            tenantId = TenantId(this[DocumentsTable.tenantId].toKotlinUuid()),
            filename = this[DocumentsTable.filename],
            contentType = this[DocumentsTable.contentType],
            sizeBytes = this[DocumentsTable.sizeBytes],
            storageKey = this[DocumentsTable.storageKey],
            source = this[DocumentsTable.documentSource],
            uploadedAt = this[DocumentsTable.uploadedAt],
            downloadUrl = null // Generated on-demand by the service layer
        )
    }
}
