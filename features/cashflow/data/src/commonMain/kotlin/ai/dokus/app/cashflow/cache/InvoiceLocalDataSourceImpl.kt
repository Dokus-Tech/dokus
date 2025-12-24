package ai.dokus.app.cashflow.cache

import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.FinancialDocumentDto.InvoiceDto
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SQLDelight implementation of InvoiceLocalDataSource.
 * Stores invoices as JSON blobs for simplicity and flexibility.
 */
internal class InvoiceLocalDataSourceImpl(
    private val database: CashflowCacheDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : InvoiceLocalDataSource {

    private val invoiceQueries get() = database.invoiceQueries
    private val metadataQueries get() = database.cacheMetadataQueries

    override fun observeAll(tenantId: TenantId): Flow<List<InvoiceDto>> {
        return invoiceQueries.selectAllByTenant(tenantId.value)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.mapNotNull { row ->
                    runCatching { json.decodeFromString<InvoiceDto>(row.data_) }.getOrNull()
                }
            }
    }

    override suspend fun getAll(tenantId: TenantId): List<InvoiceDto> = withContext(Dispatchers.IO) {
        invoiceQueries.selectAllByTenant(tenantId.value)
            .executeAsList()
            .mapNotNull { row ->
                runCatching { json.decodeFromString<InvoiceDto>(row.data_) }.getOrNull()
            }
    }

    override suspend fun getById(id: InvoiceId): InvoiceDto? = withContext(Dispatchers.IO) {
        invoiceQueries.selectById(id.value)
            .executeAsOneOrNull()
            ?.let { row ->
                runCatching { json.decodeFromString<InvoiceDto>(row.data_) }.getOrNull()
            }
    }

    override suspend fun upsertAll(tenantId: TenantId, invoices: List<InvoiceDto>) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.transaction {
            invoices.forEach { invoice ->
                val jsonData = json.encodeToString(invoice)
                invoiceQueries.upsert(
                    id = invoice.id.value,
                    tenant_id = tenantId.value,
                    data_ = jsonData,
                    cached_at = now,
                    server_updated_at = invoice.updatedAt.toInstant(kotlinx.datetime.TimeZone.UTC).toEpochMilliseconds()
                )
            }
        }

        // Update metadata
        val count = invoiceQueries.countByTenant(tenantId.value).executeAsOne()
        metadataQueries.upsert(
            entity_type = ENTITY_TYPE,
            tenant_id = tenantId.value,
            last_synced_at = now,
            item_count = count
        )
    }

    override suspend fun deleteAll(tenantId: TenantId) = withContext(Dispatchers.IO) {
        invoiceQueries.deleteAllByTenant(tenantId.value)
    }

    override suspend fun deleteById(id: InvoiceId) = withContext(Dispatchers.IO) {
        invoiceQueries.deleteById(id.value)
    }

    override suspend fun getLastSyncTime(tenantId: TenantId): Instant? = withContext(Dispatchers.IO) {
        metadataQueries.getLastSyncTime(ENTITY_TYPE, tenantId.value)
            .executeAsOneOrNull()
            ?.last_synced_at
            ?.let { Instant.fromEpochMilliseconds(it) }
    }

    override suspend fun setLastSyncTime(tenantId: TenantId, time: Instant) = withContext(Dispatchers.IO) {
        val count = invoiceQueries.countByTenant(tenantId.value).executeAsOne()
        metadataQueries.upsert(
            entity_type = ENTITY_TYPE,
            tenant_id = tenantId.value,
            last_synced_at = time.toEpochMilliseconds(),
            item_count = count
        )
    }

    override suspend fun getCount(tenantId: TenantId): Long = withContext(Dispatchers.IO) {
        invoiceQueries.countByTenant(tenantId.value).executeAsOne()
    }

    companion object {
        private const val ENTITY_TYPE = "invoice"
    }
}
