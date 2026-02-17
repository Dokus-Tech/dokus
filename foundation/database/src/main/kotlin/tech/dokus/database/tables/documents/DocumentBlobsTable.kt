package tech.dokus.database.tables.documents

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable

private const val HashLength = 64
private const val StorageKeyMaxLength = 500
private const val ContentTypeMaxLength = 100

/**
 * Physical stored artifacts keyed by exact byte hash.
 * One blob can back many source arrivals.
 */
object DocumentBlobsTable : UuidTable("document_blobs") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    val inputHash = varchar("input_hash", HashLength)
    val storageKey = varchar("storage_key", StorageKeyMaxLength)
    val contentType = varchar("content_type", ContentTypeMaxLength)
    val sizeBytes = long("size_bytes")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, inputHash)
        uniqueIndex(tenantId, storageKey)
    }
}
