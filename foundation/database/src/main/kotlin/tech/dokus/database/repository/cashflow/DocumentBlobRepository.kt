package tech.dokus.database.repository.cashflow

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.domain.ids.DocumentBlobId
import tech.dokus.domain.ids.TenantId
import java.sql.SQLException

/** PostgreSQL SQL state for unique_violation. */
private const val UNIQUE_VIOLATION_STATE = "23505"

data class DocumentBlobSummary(
    val id: DocumentBlobId,
    val tenantId: TenantId,
    val inputHash: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long
)

data class DocumentBlobCreatePayload(
    val inputHash: String,
    val storageKey: String,
    val contentType: String,
    val sizeBytes: Long
)

class DocumentBlobRepository {

    suspend fun getByInputHash(tenantId: TenantId, inputHash: String): DocumentBlobSummary? =
        newSuspendedTransaction {
            DocumentBlobsTable.selectAll()
                .where {
                    (DocumentBlobsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                        (DocumentBlobsTable.inputHash eq inputHash)
                }
                .map { it.toBlobSummary() }
                .singleOrNull()
        }

    suspend fun createIfAbsent(
        tenantId: TenantId,
        payload: DocumentBlobCreatePayload
    ): DocumentBlobSummary = newSuspendedTransaction {
        val tenantUuid = Uuid.parse(tenantId.toString())
        val existing = DocumentBlobsTable.selectAll()
            .where {
                (DocumentBlobsTable.tenantId eq tenantUuid) and
                    (DocumentBlobsTable.inputHash eq payload.inputHash)
            }
            .singleOrNull()
            ?.toBlobSummary()
        if (existing != null) return@newSuspendedTransaction existing

        val newId = DocumentBlobId.generate()
        try {
            DocumentBlobsTable.insert {
                it[id] = Uuid.parse(newId.toString())
                it[DocumentBlobsTable.tenantId] = tenantUuid
                it[inputHash] = payload.inputHash
                it[storageKey] = payload.storageKey
                it[contentType] = payload.contentType
                it[sizeBytes] = payload.sizeBytes
            }
        } catch (e: SQLException) {
            // Only handle unique constraint violations (concurrent insert race).
            // Re-throw everything else (network errors, disk failures, etc.) so
            // real database problems are not silently masked as duplicates.
            if (e.sqlState != UNIQUE_VIOLATION_STATE) throw e
            val raced = DocumentBlobsTable.selectAll()
                .where {
                    (DocumentBlobsTable.tenantId eq tenantUuid) and
                        (DocumentBlobsTable.inputHash eq payload.inputHash)
                }
                .singleOrNull()
                ?.toBlobSummary()
            if (raced != null) return@newSuspendedTransaction raced
            throw e
        }

        DocumentBlobSummary(
            id = newId,
            tenantId = tenantId,
            inputHash = payload.inputHash,
            storageKey = payload.storageKey,
            contentType = payload.contentType,
            sizeBytes = payload.sizeBytes
        )
    }

    private fun ResultRow.toBlobSummary(): DocumentBlobSummary {
        return DocumentBlobSummary(
            id = DocumentBlobId(this[DocumentBlobsTable.id].value),
            tenantId = TenantId(this[DocumentBlobsTable.tenantId]),
            inputHash = this[DocumentBlobsTable.inputHash],
            storageKey = this[DocumentBlobsTable.storageKey],
            contentType = this[DocumentBlobsTable.contentType],
            sizeBytes = this[DocumentBlobsTable.sizeBytes]
        )
    }
}
