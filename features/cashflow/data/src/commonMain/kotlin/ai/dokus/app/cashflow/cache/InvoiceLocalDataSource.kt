package ai.dokus.app.cashflow.cache

import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.FinancialDocumentDto.InvoiceDto
import kotlinx.coroutines.flow.Flow

/**
 * Local data source for cached invoices.
 * Provides offline access to previously fetched invoices stored in SQLDelight.
 *
 * Note: Timestamps are epoch milliseconds (Long) for simplicity.
 */
interface InvoiceLocalDataSource {

    /**
     * Observe all cached invoices for a tenant as a Flow.
     * Emits whenever the cache changes.
     */
    fun observeAll(tenantId: TenantId): Flow<List<InvoiceDto>>

    /**
     * Get all cached invoices for a tenant (one-shot).
     */
    suspend fun getAll(tenantId: TenantId): List<InvoiceDto>

    /**
     * Get a single cached invoice by ID.
     */
    suspend fun getById(id: InvoiceId): InvoiceDto?

    /**
     * Insert or update invoices in the cache.
     * Existing invoices with the same ID are replaced.
     */
    suspend fun upsertAll(tenantId: TenantId, invoices: List<InvoiceDto>)

    /**
     * Delete all cached invoices for a tenant.
     * Used before a full refresh.
     */
    suspend fun deleteAll(tenantId: TenantId)

    /**
     * Delete a specific cached invoice.
     */
    suspend fun deleteById(id: InvoiceId)

    /**
     * Get the last sync time for invoices (epoch milliseconds).
     */
    suspend fun getLastSyncTime(tenantId: TenantId): Long?

    /**
     * Update the last sync time for invoices (epoch milliseconds).
     */
    suspend fun setLastSyncTime(tenantId: TenantId, timeMillis: Long)

    /**
     * Get the number of cached invoices for a tenant.
     */
    suspend fun getCount(tenantId: TenantId): Long
}
