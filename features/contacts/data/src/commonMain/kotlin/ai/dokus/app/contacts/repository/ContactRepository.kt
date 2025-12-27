package ai.dokus.app.contacts.repository

import ai.dokus.app.contacts.datasource.ContactLocalDataSource
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.TenantId
import tech.dokus.domain.model.ContactDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import tech.dokus.foundation.app.state.CacheState
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Repository implementing cache-first pattern for contact data.
 *
 * Data flow:
 * 1. Emit cached data immediately (if available)
 * 2. Start network refresh in background
 * 3. Update cache and emit fresh data on success
 * 4. Emit stale state with error on failure
 */
@OptIn(ExperimentalTime::class)
internal class ContactRepository(
    private val remoteDataSource: ContactRemoteDataSource,
    private val localDataSource: ContactLocalDataSource
) {

    /**
     * Observe contacts with cache-first strategy.
     *
     * @param tenantId Tenant to fetch contacts for
     * @param search Optional search query (only applied to network request)
     * @param isActive Optional active filter (only applied to network request)
     * @param forceRefresh If true, skip cached data and fetch from network immediately
     */
    fun observeContacts(
        tenantId: TenantId,
        search: String? = null,
        isActive: Boolean? = null,
        forceRefresh: Boolean = false
    ): Flow<CacheState<List<ContactDto>>> = flow {
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
        val result = remoteDataSource.listContacts(
            search = search,
            isActive = isActive,
            limit = 1000, // Fetch all for caching
            offset = 0
        )

        result
            .onSuccess { contacts ->
                val now = Clock.System.now().toEpochMilliseconds()

                // 3. Update cache
                localDataSource.deleteAll(tenantId) // Clear old data
                localDataSource.upsertAll(tenantId, contacts)
                localDataSource.setLastSyncTime(tenantId, now)

                // 4. Emit fresh state
                emit(CacheState.Fresh(contacts, now))
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
     * Get a single contact by ID with cache-first strategy.
     */
    fun observeContact(
        contactId: ContactId,
        tenantId: TenantId,
        forceRefresh: Boolean = false
    ): Flow<CacheState<ContactDto>> = flow {
        // 1. Try to emit cached data first
        if (!forceRefresh) {
            val cached = localDataSource.getById(contactId)
            if (cached != null) {
                val lastSync = localDataSource.getLastSyncTime(tenantId)
                emit(CacheState.Cached(cached, lastSync))
                emit(CacheState.Refreshing(cached))
            } else {
                emit(CacheState.loading())
            }
        } else {
            val cached = localDataSource.getById(contactId)
            emit(CacheState.Refreshing(cached))
        }

        // 2. Fetch from network
        val result = remoteDataSource.getContact(contactId)

        result
            .onSuccess { contact ->
                val now = Clock.System.now().toEpochMilliseconds()

                // Update cache
                localDataSource.upsertAll(tenantId, listOf(contact))

                emit(CacheState.Fresh(contact, now))
            }
            .onFailure { error ->
                val cached = localDataSource.getById(contactId)
                val lastSync = localDataSource.getLastSyncTime(tenantId)

                if (cached != null) {
                    emit(CacheState.Stale(cached, lastSync, error))
                } else {
                    emit(CacheState.Empty(isLoading = false))
                }
            }
    }

    /**
     * Get all cached contacts without network call.
     * Useful for quick offline access.
     */
    suspend fun getCachedContacts(tenantId: TenantId): List<ContactDto> {
        return localDataSource.getAll(tenantId)
    }

    /**
     * Get last sync time for contacts.
     */
    suspend fun getLastSyncTime(tenantId: TenantId): Long? {
        return localDataSource.getLastSyncTime(tenantId)
    }

    /**
     * Clear all cached contacts for a tenant.
     */
    suspend fun clearCache(tenantId: TenantId) {
        localDataSource.deleteAll(tenantId)
    }

    /**
     * Manually cache contacts (useful when contacts are fetched through other means).
     */
    suspend fun cacheContacts(tenantId: TenantId, contacts: List<ContactDto>) {
        val now = Clock.System.now().toEpochMilliseconds()
        localDataSource.upsertAll(tenantId, contacts)
        localDataSource.setLastSyncTime(tenantId, now)
    }
}
