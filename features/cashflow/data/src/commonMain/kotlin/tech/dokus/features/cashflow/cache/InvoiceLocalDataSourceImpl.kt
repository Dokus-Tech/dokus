package tech.dokus.features.cashflow.cache

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.FinancialDocumentDto.InvoiceDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * SQLDelight implementation of InvoiceLocalDataSource.
 * Stores invoices as JSON blobs for simplicity and flexibility.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class, ExperimentalTime::class)
internal class InvoiceLocalDataSourceImpl(
    private val database: CashflowCacheDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : InvoiceLocalDataSource {

    private val invoiceQueries get() = database.invoiceQueries
    private val metadataQueries get() = database.cacheMetadataQueries

    override fun observeAll(tenantId: TenantId): Flow<List<InvoiceDto>> {
        return invoiceQueries.selectAllByTenant(tenantId.value.toString())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.mapNotNull { row ->
                    runCatching { json.decodeFromString<InvoiceDto>(row.data_) }.getOrNull()
                }
            }
    }

    override suspend fun getAll(tenantId: TenantId): List<InvoiceDto> = withContext(Dispatchers.Default) {
        invoiceQueries.selectAllByTenant(tenantId.value.toString())
            .executeAsList()
            .mapNotNull { row ->
                runCatching { json.decodeFromString<InvoiceDto>(row.data_) }.getOrNull()
            }
    }

    override suspend fun getById(id: InvoiceId): InvoiceDto? = withContext(Dispatchers.Default) {
        invoiceQueries.selectById(id.value.toString())
            .executeAsOneOrNull()
            ?.let { row ->
                runCatching { json.decodeFromString<InvoiceDto>(row.data_) }.getOrNull()
            }
    }

    override suspend fun upsertAll(tenantId: TenantId, invoices: List<InvoiceDto>) {
        withContext(Dispatchers.Default) {
            val now = Clock.System.now().toEpochMilliseconds()
            val tenantIdStr = tenantId.value.toString()
            database.transaction {
                invoices.forEach { invoice ->
                    val jsonData = json.encodeToString(invoice)
                    // Use createdAt as fallback since updatedAt may require experimental APIs
                    val serverUpdatedAt = runCatching {
                        invoice.updatedAt.toInstant(TimeZone.UTC).toEpochMilliseconds()
                    }.getOrElse { now }
                    invoiceQueries.upsert(
                        id = invoice.id.value.toString(),
                        tenant_id = tenantIdStr,
                        data_ = jsonData,
                        cached_at = now,
                        server_updated_at = serverUpdatedAt
                    )
                }
            }

            // Update metadata
            val count = invoiceQueries.countByTenant(tenantIdStr).executeAsOne()
            metadataQueries.upsert(
                entity_type = ENTITY_TYPE,
                tenant_id = tenantIdStr,
                last_synced_at = now,
                item_count = count
            )
        }
    }

    override suspend fun deleteAll(tenantId: TenantId) {
        withContext(Dispatchers.Default) {
            invoiceQueries.deleteAllByTenant(tenantId.value.toString())
        }
    }

    override suspend fun deleteById(id: InvoiceId) {
        withContext(Dispatchers.Default) {
            invoiceQueries.deleteById(id.value.toString())
        }
    }

    override suspend fun getLastSyncTime(tenantId: TenantId): Long? = withContext(Dispatchers.Default) {
        metadataQueries.getLastSyncTime(ENTITY_TYPE, tenantId.value.toString())
            .executeAsOneOrNull()
    }

    override suspend fun setLastSyncTime(tenantId: TenantId, timeMillis: Long) {
        withContext(Dispatchers.Default) {
            val tenantIdStr = tenantId.value.toString()
            val count = invoiceQueries.countByTenant(tenantIdStr).executeAsOne()
            metadataQueries.upsert(
                entity_type = ENTITY_TYPE,
                tenant_id = tenantIdStr,
                last_synced_at = timeMillis,
                item_count = count
            )
        }
    }

    override suspend fun getCount(tenantId: TenantId): Long = withContext(Dispatchers.Default) {
        invoiceQueries.countByTenant(tenantId.value.toString()).executeAsOne()
    }

    companion object {
        private const val ENTITY_TYPE = "invoice"
    }
}
