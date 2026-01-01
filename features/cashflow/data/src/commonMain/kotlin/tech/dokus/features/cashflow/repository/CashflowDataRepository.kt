package tech.dokus.features.cashflow.repository

import tech.dokus.features.cashflow.cache.InvoiceLocalDataSource
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.FinancialDocumentDto.InvoiceDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate
import tech.dokus.foundation.app.state.CacheState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository implementing cache-first pattern for invoice data.
 *
 * Data flow:
 * 1. Emit cached data immediately (if available)
 * 2. Start network refresh in background
 * 3. Update cache and emit fresh data on success
 * 4. Emit stale state with error on failure
 */
@OptIn(ExperimentalTime::class)
class CashflowDataRepository(
    private val remoteDataSource: CashflowRemoteDataSource,
    private val localDataSource: InvoiceLocalDataSource
) {

    /**
     * Observe invoices with cache-first strategy.
     *
     * @param tenantId Tenant to fetch invoices for
     * @param status Optional status filter (only applied to network request)
     * @param fromDate Optional start date filter (only applied to network request)
     * @param toDate Optional end date filter (only applied to network request)
     * @param forceRefresh If true, skip cached data and fetch from network immediately
     */
    fun observeInvoices(
        tenantId: TenantId,
        status: InvoiceStatus? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        forceRefresh: Boolean = false
    ): Flow<CacheState<List<InvoiceDto>>> = flow {
        // 1. Try to emit cached data first (unless force refresh)
        if (!forceRefresh) {
            val cached = localDataSource.getAll(tenantId)
            val lastSync = localDataSource.getLastSyncTime(tenantId)

            if (cached.isNotEmpty()) {
                emit(CacheState.Cached(cached, lastSync))
                // Then transition to refreshing state
                emit(CacheState.Refreshing(cached))
            } else {
                // No cache, show loading
                emit(CacheState.loading())
            }
        } else {
            // Force refresh - show refreshing with any existing cached data
            val cached = localDataSource.getAll(tenantId)
            emit(CacheState.Refreshing(cached.takeIf { it.isNotEmpty() }))
        }

        // 2. Fetch from network
        val result = remoteDataSource.listInvoices(
            status = status,
            fromDate = fromDate,
            toDate = toDate,
            limit = 1000, // Fetch all for caching
            offset = 0
        )

        result
            .onSuccess { response ->
                val invoices = response.items
                val now = Clock.System.now().toEpochMilliseconds()

                // 3. Update cache
                localDataSource.deleteAll(tenantId) // Clear old data
                localDataSource.upsertAll(tenantId, invoices)
                localDataSource.setLastSyncTime(tenantId, now)

                // 4. Emit fresh state
                emit(CacheState.Fresh(invoices, now))
            }
            .onFailure { error ->
                // 5. Emit stale state with cached data
                val cached = localDataSource.getAll(tenantId)
                val lastSync = localDataSource.getLastSyncTime(tenantId)

                if (cached.isNotEmpty()) {
                    emit(CacheState.Stale(cached, lastSync, error))
                } else {
                    emit(CacheState.Empty(isLoading = false))
                }
            }
    }

    /**
     * Get a single invoice by ID with cache-first strategy.
     */
    fun observeInvoice(
        invoiceId: InvoiceId,
        tenantId: TenantId,
        forceRefresh: Boolean = false
    ): Flow<CacheState<InvoiceDto>> = flow {
        // 1. Try to emit cached data first
        if (!forceRefresh) {
            val cached = localDataSource.getById(invoiceId)
            if (cached != null) {
                val lastSync = localDataSource.getLastSyncTime(tenantId)
                emit(CacheState.Cached(cached, lastSync))
                emit(CacheState.Refreshing(cached))
            } else {
                emit(CacheState.loading())
            }
        } else {
            val cached = localDataSource.getById(invoiceId)
            emit(CacheState.Refreshing(cached))
        }

        // 2. Fetch from network
        val result = remoteDataSource.getInvoice(invoiceId)

        result
            .onSuccess { invoice ->
                val now = Clock.System.now().toEpochMilliseconds()

                // Update cache
                localDataSource.upsertAll(tenantId, listOf(invoice))

                emit(CacheState.Fresh(invoice, now))
            }
            .onFailure { error ->
                val cached = localDataSource.getById(invoiceId)
                val lastSync = localDataSource.getLastSyncTime(tenantId)

                if (cached != null) {
                    emit(CacheState.Stale(cached, lastSync, error))
                } else {
                    emit(CacheState.Empty(isLoading = false))
                }
            }
    }

    /**
     * Get all cached invoices without network call.
     * Useful for quick offline access.
     */
    suspend fun getCachedInvoices(tenantId: TenantId): List<InvoiceDto> {
        return localDataSource.getAll(tenantId)
    }

    /**
     * Get last sync time for invoices.
     */
    suspend fun getLastSyncTime(tenantId: TenantId): Long? {
        return localDataSource.getLastSyncTime(tenantId)
    }

    /**
     * Clear all cached invoices for a tenant.
     */
    suspend fun clearCache(tenantId: TenantId) {
        localDataSource.deleteAll(tenantId)
    }
}
