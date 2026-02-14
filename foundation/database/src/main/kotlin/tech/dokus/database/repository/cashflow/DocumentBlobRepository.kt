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
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

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

@OptIn(ExperimentalUuidApi::class)
class DocumentBlobRepository {

    suspend fun getByInputHash(tenantId: TenantId, inputHash: String): DocumentBlobSummary? =
        newSuspendedTransaction {
            DocumentBlobsTable.selectAll()
                .where {
                    (DocumentBlobsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                        (DocumentBlobsTable.inputHash eq inputHash)
                }
                .map { it.toBlobSummary() }
                .singleOrNull()
        }

    suspend fun createIfAbsent(
        tenantId: TenantId,
        payload: DocumentBlobCreatePayload
    ): DocumentBlobSummary = newSuspendedTransaction {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val existing = DocumentBlobsTable.selectAll()
            .where {
                (DocumentBlobsTable.tenantId eq tenantUuid) and
                    (DocumentBlobsTable.inputHash eq payload.inputHash)
            }
            .singleOrNull()
            ?.toBlobSummary()
        if (existing != null) return@newSuspendedTransaction existing

        val newId = DocumentBlobId.generate()
        runCatching {
            DocumentBlobsTable.insert {
                it[id] = UUID.fromString(newId.toString())
                it[DocumentBlobsTable.tenantId] = tenantUuid
                it[inputHash] = payload.inputHash
                it[storageKey] = payload.storageKey
                it[contentType] = payload.contentType
                it[sizeBytes] = payload.sizeBytes
            }
        }.onFailure {
            // Unique race: fetch winner.
            val raced = DocumentBlobsTable.selectAll()
                .where {
                    (DocumentBlobsTable.tenantId eq tenantUuid) and
                        (DocumentBlobsTable.inputHash eq payload.inputHash)
                }
                .singleOrNull()
                ?.toBlobSummary()
            if (raced != null) return@newSuspendedTransaction raced
            throw it
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
            id = DocumentBlobId(this[DocumentBlobsTable.id].value.toKotlinUuid()),
            tenantId = TenantId(this[DocumentBlobsTable.tenantId].toKotlinUuid()),
            inputHash = this[DocumentBlobsTable.inputHash],
            storageKey = this[DocumentBlobsTable.storageKey],
            contentType = this[DocumentBlobsTable.contentType],
            sizeBytes = this[DocumentBlobsTable.sizeBytes]
        )
    }
}
