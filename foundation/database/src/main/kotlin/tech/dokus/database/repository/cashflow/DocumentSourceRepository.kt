package tech.dokus.database.repository.cashflow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentMatchType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentSourceStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import kotlin.math.min
import kotlin.time.Clock

data class DocumentSourceSummary(
    val id: DocumentSourceId,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val blobId: DocumentBlobId,
    val sourceChannel: DocumentSource,
    val arrivalAt: kotlinx.datetime.LocalDateTime,
    val contentHash: String?,
    val identityKeyHash: String?,
    val status: DocumentSourceStatus,
    val matchType: DocumentMatchType?,
    val isCorrective: Boolean,
    val extractedSnapshotJson: String?,
    val detachedAt: kotlinx.datetime.LocalDateTime?,
    val normalizedSupplierVat: String?,
    val normalizedDocumentNumber: String?,
    val documentType: DocumentType?,
    val direction: DocumentDirection?,
    val filename: String?,
    val inputHash: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long
)

data class DocumentSourceCreatePayload(
    val documentId: DocumentId,
    val blobId: DocumentBlobId,
    val sourceChannel: DocumentSource,
    val status: DocumentSourceStatus = DocumentSourceStatus.Linked,
    val matchType: DocumentMatchType? = null,
    val contentHash: String? = null,
    val identityKeyHash: String? = null,
    val isCorrective: Boolean = false,
    val extractedSnapshotJson: String? = null,
    val normalizedSupplierVat: String? = null,
    val normalizedDocumentNumber: String? = null,
    val documentType: DocumentType? = null,
    val direction: DocumentDirection? = null,
    val filename: String? = null
)

data class FuzzySourceCandidate(
    val sourceId: DocumentSourceId,
    val documentId: DocumentId,
    val normalizedDocumentNumber: String,
    val distance: Int
)

class DocumentSourceRepository {

    suspend fun create(
        tenantId: TenantId,
        payload: DocumentSourceCreatePayload
    ): DocumentSourceId = newSuspendedTransaction {
        val sourceId = DocumentSourceId.generate()
        DocumentSourcesTable.insert {
            it[id] = sourceId.value
            it[DocumentSourcesTable.tenantId] = tenantId.value
            it[documentId] = payload.documentId.value
            it[blobId] = payload.blobId.value
            it[sourceChannel] = payload.sourceChannel
            it[status] = payload.status
            it[matchType] = payload.matchType
            it[contentHash] = payload.contentHash
            it[identityKeyHash] = payload.identityKeyHash
            it[isCorrective] = payload.isCorrective
            it[extractedSnapshotJson] = payload.extractedSnapshotJson
            it[normalizedSupplierVat] = payload.normalizedSupplierVat
            it[normalizedDocumentNumber] = payload.normalizedDocumentNumber
            it[DocumentSourcesTable.documentType] = payload.documentType
            it[direction] = payload.direction
            it[filename] = payload.filename
        }
        sourceId
    }

    suspend fun getById(tenantId: TenantId, sourceId: DocumentSourceId): DocumentSourceSummary? =
        newSuspendedTransaction {
            (DocumentSourcesTable innerJoin DocumentBlobsTable)
                .selectAll()
                .where {
                    (DocumentSourcesTable.id eq sourceId.value) and
                        (DocumentSourcesTable.tenantId eq tenantId.value)
                }
                .map { it.toSourceSummary() }
                .singleOrNull()
        }

    suspend fun listByDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        includeDetached: Boolean = false
    ): List<DocumentSourceSummary> = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val docUuid = documentId.value
        val rows = (DocumentSourcesTable innerJoin DocumentBlobsTable)
            .selectAll()
            .where {
                (DocumentSourcesTable.tenantId eq tenantUuid) and
                    (DocumentSourcesTable.documentId eq docUuid)
            }
            .orderBy(DocumentSourcesTable.arrivalAt, SortOrder.DESC)
            .map { it.toSourceSummary() }

        if (includeDetached) rows else rows.filter { it.status != DocumentSourceStatus.Detached }
    }

    suspend fun countLinkedSources(tenantId: TenantId, documentId: DocumentId): Int = newSuspendedTransaction {
        DocumentSourcesTable
            .selectAll()
            .where {
                (DocumentSourcesTable.tenantId eq tenantId.value) and
                    (DocumentSourcesTable.documentId eq documentId.value) and
                    (DocumentSourcesTable.status eq DocumentSourceStatus.Linked)
            }
            .count()
            .toInt()
    }

    suspend fun countSources(
        tenantId: TenantId,
        documentId: DocumentId,
        includeDetached: Boolean = true
    ): Int = newSuspendedTransaction {
        DocumentSourcesTable
            .selectAll()
            .where {
                var where = (DocumentSourcesTable.tenantId eq tenantId.value) and
                    (DocumentSourcesTable.documentId eq documentId.value)
                if (!includeDetached) {
                    where = where and (DocumentSourcesTable.status neq DocumentSourceStatus.Detached)
                }
                where
            }
            .count()
            .toInt()
    }

    suspend fun findLinkedDocumentByInputHash(
        tenantId: TenantId,
        inputHash: String
    ): DocumentId? = newSuspendedTransaction {
        (DocumentSourcesTable innerJoin DocumentBlobsTable)
            .selectAll()
            .where {
                (DocumentSourcesTable.tenantId eq tenantId.value) and
                    (DocumentBlobsTable.inputHash eq inputHash) and
                    (DocumentSourcesTable.status eq DocumentSourceStatus.Linked)
            }
            .orderBy(DocumentSourcesTable.arrivalAt, SortOrder.DESC)
            .firstOrNull()
            ?.let { DocumentId(it[DocumentSourcesTable.documentId]) }
    }

    suspend fun findLinkedDocumentByContentHash(
        tenantId: TenantId,
        contentHash: String,
        excludeSourceId: DocumentSourceId? = null
    ): DocumentId? = newSuspendedTransaction {
        (DocumentSourcesTable innerJoin DocumentBlobsTable)
            .selectAll()
            .where {
                var where = (DocumentSourcesTable.tenantId eq tenantId.value) and
                    (DocumentSourcesTable.contentHash eq contentHash) and
                    (DocumentSourcesTable.status eq DocumentSourceStatus.Linked)
                if (excludeSourceId != null) {
                    where = where and (DocumentSourcesTable.id neq excludeSourceId.value)
                }
                where
            }
            .orderBy(DocumentSourcesTable.arrivalAt, SortOrder.DESC)
            .firstOrNull()
            ?.let { DocumentId(it[DocumentSourcesTable.documentId]) }
    }

    suspend fun findLinkedDocumentByIdentityKeyHash(
        tenantId: TenantId,
        identityKeyHash: String,
        excludeDocumentId: DocumentId? = null
    ): DocumentId? = newSuspendedTransaction {
        (DocumentSourcesTable innerJoin DocumentBlobsTable)
            .selectAll()
            .where {
                var where = (DocumentSourcesTable.tenantId eq tenantId.value) and
                    (DocumentSourcesTable.identityKeyHash eq identityKeyHash) and
                    (DocumentSourcesTable.status eq DocumentSourceStatus.Linked)
                if (excludeDocumentId != null) {
                    where = where and (DocumentSourcesTable.documentId neq excludeDocumentId.value)
                }
                where
            }
            .orderBy(DocumentSourcesTable.arrivalAt, SortOrder.DESC)
            .firstOrNull()
            ?.let { DocumentId(it[DocumentSourcesTable.documentId]) }
    }

    suspend fun findFuzzyCandidates(
        tenantId: TenantId,
        normalizedSupplierVat: String,
        normalizedDocumentNumber: String,
        documentType: DocumentType,
        direction: DocumentDirection,
        excludeDocumentId: DocumentId? = null,
        maxDistance: Int = 2
    ): List<FuzzySourceCandidate> = newSuspendedTransaction {
        val tenantUuid = tenantId.value
        val candidates = (DocumentSourcesTable innerJoin DocumentBlobsTable)
            .selectAll()
            .where {
                var where = (DocumentSourcesTable.tenantId eq tenantUuid) and
                    (DocumentSourcesTable.status eq DocumentSourceStatus.Linked) and
                    (DocumentSourcesTable.normalizedSupplierVat eq normalizedSupplierVat) and
                    (DocumentSourcesTable.documentType eq documentType) and
                    (DocumentSourcesTable.direction eq direction)
                if (excludeDocumentId != null) {
                    where = where and (DocumentSourcesTable.documentId neq excludeDocumentId.value)
                }
                where
            }
            .mapNotNull { row ->
                val candidateNumber = row[DocumentSourcesTable.normalizedDocumentNumber] ?: return@mapNotNull null
                val distance = levenshtein(normalizedDocumentNumber, candidateNumber)
                if (distance > maxDistance) return@mapNotNull null
                FuzzySourceCandidate(
                    sourceId = DocumentSourceId(row[DocumentSourcesTable.id].value),
                    documentId = DocumentId(row[DocumentSourcesTable.documentId]),
                    normalizedDocumentNumber = candidateNumber,
                    distance = distance
                )
            }

        candidates.sortedBy { it.distance }
    }

    suspend fun updateMatchingFingerprint(
        tenantId: TenantId,
        sourceId: DocumentSourceId,
        contentHash: String?,
        identityKeyHash: String?,
        normalizedSupplierVat: String?,
        normalizedDocumentNumber: String?,
        documentType: DocumentType?,
        direction: DocumentDirection?,
        extractedSnapshotJson: String?,
        matchType: DocumentMatchType?
    ): Boolean = newSuspendedTransaction {
        DocumentSourcesTable.update({
            (DocumentSourcesTable.id eq sourceId.value) and
                (DocumentSourcesTable.tenantId eq tenantId.value)
        }) {
            it[DocumentSourcesTable.contentHash] = contentHash
            it[DocumentSourcesTable.identityKeyHash] = identityKeyHash
            it[DocumentSourcesTable.normalizedSupplierVat] = normalizedSupplierVat
            it[DocumentSourcesTable.normalizedDocumentNumber] = normalizedDocumentNumber
            it[DocumentSourcesTable.documentType] = documentType
            it[DocumentSourcesTable.direction] = direction
            if (extractedSnapshotJson != null) {
                it[DocumentSourcesTable.extractedSnapshotJson] = extractedSnapshotJson
            }
            it[DocumentSourcesTable.matchType] = matchType
        } > 0
    }

    suspend fun reassignToDocument(
        tenantId: TenantId,
        sourceId: DocumentSourceId,
        documentId: DocumentId,
        status: DocumentSourceStatus,
        matchType: DocumentMatchType?
    ): Boolean = newSuspendedTransaction {
        DocumentSourcesTable.update({
            (DocumentSourcesTable.id eq sourceId.value) and
                (DocumentSourcesTable.tenantId eq tenantId.value)
        }) {
            it[DocumentSourcesTable.documentId] = documentId.value
            it[DocumentSourcesTable.status] = status
            it[DocumentSourcesTable.matchType] = matchType
            it[detachedAt] = null
        } > 0
    }

    suspend fun markDetached(
        tenantId: TenantId,
        sourceId: DocumentSourceId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        DocumentSourcesTable.update({
            (DocumentSourcesTable.id eq sourceId.value) and
                (DocumentSourcesTable.tenantId eq tenantId.value)
        }) {
            it[status] = DocumentSourceStatus.Detached
            it[detachedAt] = now
        } > 0
    }

    suspend fun deleteById(
        tenantId: TenantId,
        sourceId: DocumentSourceId
    ): Boolean = newSuspendedTransaction {
        DocumentSourcesTable.deleteWhere {
            (DocumentSourcesTable.id eq sourceId.value) and
                (DocumentSourcesTable.tenantId eq tenantId.value)
        } > 0
    }

    suspend fun selectDefaultSource(
        tenantId: TenantId,
        documentId: DocumentId
    ): DocumentSourceSummary? {
        val sources = listByDocument(tenantId, documentId, includeDetached = false)
        return selectDefaultSourceFromList(sources)
    }

    private fun ResultRow.toSourceSummary(): DocumentSourceSummary {
        return DocumentSourceSummary(
            id = DocumentSourceId(this[DocumentSourcesTable.id].value),
            tenantId = TenantId(this[DocumentSourcesTable.tenantId]),
            documentId = DocumentId(this[DocumentSourcesTable.documentId]),
            blobId = DocumentBlobId(this[DocumentSourcesTable.blobId]),
            sourceChannel = this[DocumentSourcesTable.sourceChannel],
            arrivalAt = this[DocumentSourcesTable.arrivalAt],
            contentHash = this[DocumentSourcesTable.contentHash],
            identityKeyHash = this[DocumentSourcesTable.identityKeyHash],
            status = this[DocumentSourcesTable.status],
            matchType = this[DocumentSourcesTable.matchType],
            isCorrective = this[DocumentSourcesTable.isCorrective],
            extractedSnapshotJson = this[DocumentSourcesTable.extractedSnapshotJson],
            detachedAt = this[DocumentSourcesTable.detachedAt],
            normalizedSupplierVat = this[DocumentSourcesTable.normalizedSupplierVat],
            normalizedDocumentNumber = this[DocumentSourcesTable.normalizedDocumentNumber],
            documentType = this[DocumentSourcesTable.documentType],
            direction = this[DocumentSourcesTable.direction],
            filename = this[DocumentSourcesTable.filename],
            inputHash = this[DocumentBlobsTable.inputHash],
            storageKey = this[DocumentBlobsTable.storageKey],
            contentType = this[DocumentBlobsTable.contentType],
            sizeBytes = this[DocumentBlobsTable.sizeBytes]
        )
    }

    private fun levenshtein(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length

        var previous = IntArray(right.length + 1) { it }
        var current = IntArray(right.length + 1)

        for (i in left.indices) {
            current[0] = i + 1
            for (j in right.indices) {
                val cost = if (left[i] == right[j]) 0 else 1
                current[j + 1] = min(
                    min(current[j] + 1, previous[j + 1] + 1),
                    previous[j] + cost
                )
            }
            val tmp = previous
            previous = current
            current = tmp
        }

        return previous[right.length]
    }
}

private val SOURCE_TRUST_PRIORITY = mapOf(
    DocumentSource.Peppol to 4,
    DocumentSource.Email to 3,
    DocumentSource.Upload to 2,
    DocumentSource.Manual to 1
)

fun selectDefaultSourceFromList(sources: List<DocumentSourceSummary>): DocumentSourceSummary? {
    return sources
        .filter { it.status == DocumentSourceStatus.Linked }
        .maxWithOrNull(
            compareBy<DocumentSourceSummary> { SOURCE_TRUST_PRIORITY[it.sourceChannel] ?: 0 }
                .thenBy { it.arrivalAt }
        )
}
