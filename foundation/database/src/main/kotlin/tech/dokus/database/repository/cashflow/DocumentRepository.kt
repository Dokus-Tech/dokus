package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
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
 * financial entity tables (Invoice, Bill, Expense) which have documentId FK.
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
     * - draftStatus: Only applies when draft exists (documents without drafts pass this filter)
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
        draftStatus: DraftStatus? = null,
        documentType: DocumentType? = null,
        ingestionStatus: IngestionStatus? = null,
        search: String? = null,
        page: Int = 0,
        limit: Int = 20
    ): Pair<List<DocumentWithDraftAndIngestion>, Long> = newSuspendedTransaction {
        val tenantIdUuid = UUID.fromString(tenantId.toString())

        // Step 1: Query documents with optional LEFT JOIN to drafts
        // We use a subquery approach since Exposed doesn't support complex LEFT JOINs well
        val documentsQuery = DocumentsTable
            .leftJoin(DocumentDraftsTable)
            .selectAll()
            .where { DocumentsTable.tenantId eq tenantIdUuid }

        // Apply search filter on filename (case-insensitive)
        val filteredQuery = if (!search.isNullOrBlank()) {
            documentsQuery.andWhere {
                DocumentsTable.filename like "%$search%"
            }
        } else {
            documentsQuery
        }

        // Apply draft status filter (null-safe: documents without drafts pass)
        // When draft doesn't exist (LEFT JOIN null), the document should still be included
        val statusFilteredQuery = if (draftStatus != null) {
            filteredQuery.andWhere {
                DocumentDraftsTable.tenantId.isNull() or
                    (DocumentDraftsTable.draftStatus eq draftStatus)
            }
        } else {
            filteredQuery
        }

        // Apply document type filter (null-safe: documents without drafts pass)
        val typeFilteredQuery = if (documentType != null) {
            statusFilteredQuery.andWhere {
                DocumentDraftsTable.tenantId.isNull() or
                    (DocumentDraftsTable.documentType eq documentType)
            }
        } else {
            statusFilteredQuery
        }

        // Get total count before pagination
        val total = typeFilteredQuery.count()

        // Get paginated results
        val rows = typeFilteredQuery
            .orderBy(DocumentsTable.uploadedAt, SortOrder.DESC)
            .limit(limit)
            .offset((page * limit).toLong())
            .toList()

        // Step 2: For each document, get the latest ingestion run
        // We need to do this as separate queries due to the priority logic
        val documentIds = rows.map { UUID.fromString(it[DocumentsTable.id].toString()) }

        // Build result list
        val results = rows.mapNotNull { row ->
            val documentId = DocumentId.parse(row[DocumentsTable.id].toString())
            val document = row.toDocumentDto()

            // Extract draft if present
            val draft = if (row.getOrNull(DocumentDraftsTable.documentId) != null) {
                row.toDraftSummary()
            } else {
                null
            }

            // Get latest ingestion for this document (using priority logic)
            val latestIngestion = getLatestIngestionForDocument(documentId, tenantIdUuid)

            // Apply ingestion status filter
            if (ingestionStatus != null && latestIngestion?.status != ingestionStatus) {
                return@mapNotNull null
            }

            DocumentWithDraftAndIngestion(
                document = document,
                draft = draft,
                latestIngestion = latestIngestion
            )
        }

        // Adjust total count if ingestion filter was applied
        // (This is a simplification - ideally we'd filter in the query)
        val adjustedTotal = if (ingestionStatus != null) {
            results.size.toLong()
        } else {
            total
        }

        results to adjustedTotal
    }

    /**
     * Get the latest ingestion run for a document with priority logic.
     * Priority: Processing > latest Succeeded/Failed (by finishedAt) > latest Queued (by queuedAt)
     */
    private fun getLatestIngestionForDocument(
        documentId: DocumentId,
        tenantIdUuid: UUID
    ): IngestionRunSummary? {
        val docIdUuid = UUID.fromString(documentId.toString())

        // First, check for Processing status
        val processing = DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq docIdUuid) and
                    (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                    (DocumentIngestionRunsTable.status eq IngestionStatus.Processing)
            }
            .map { it.toIngestionRunSummary() }
            .firstOrNull()

        if (processing != null) return processing

        // Then, check for latest Succeeded/Failed by finishedAt
        val finished = DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq docIdUuid) and
                    (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                    (DocumentIngestionRunsTable.status inList listOf(IngestionStatus.Succeeded, IngestionStatus.Failed))
            }
            .orderBy(DocumentIngestionRunsTable.finishedAt, SortOrder.DESC_NULLS_LAST)
            .map { it.toIngestionRunSummary() }
            .firstOrNull()

        if (finished != null) return finished

        // Finally, check for latest Queued by queuedAt
        return DocumentIngestionRunsTable.selectAll()
            .where {
                (DocumentIngestionRunsTable.documentId eq docIdUuid) and
                    (DocumentIngestionRunsTable.tenantId eq tenantIdUuid) and
                    (DocumentIngestionRunsTable.status eq IngestionStatus.Queued)
            }
            .orderBy(DocumentIngestionRunsTable.queuedAt, SortOrder.DESC)
            .map { it.toIngestionRunSummary() }
            .firstOrNull()
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
            draftStatus = this[DocumentDraftsTable.draftStatus],
            documentType = this[DocumentDraftsTable.documentType],
            extractedData = this[DocumentDraftsTable.extractedData]?.let { json.decodeFromString(it) },
            aiDraftData = this[DocumentDraftsTable.aiDraftData]?.let { json.decodeFromString(it) },
            aiDraftSourceRunId = this[DocumentDraftsTable.aiDraftSourceRunId]?.let {
                IngestionRunId.parse(
                    it.toString()
                )
            },
            draftVersion = this[DocumentDraftsTable.draftVersion],
            draftEditedAt = this[DocumentDraftsTable.draftEditedAt],
            draftEditedBy = this[DocumentDraftsTable.draftEditedBy]?.let { UserId(it.toKotlinUuid()) },
            suggestedContactId = this[DocumentDraftsTable.suggestedContactId]?.let { ContactId(it.toKotlinUuid()) },
            contactSuggestionConfidence = this[DocumentDraftsTable.contactSuggestionConfidence],
            contactSuggestionReason = this[DocumentDraftsTable.contactSuggestionReason],
            linkedContactId = this[DocumentDraftsTable.linkedContactId]?.let { ContactId(it.toKotlinUuid()) },
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
