package ai.dokus.app.contacts.datasource

import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.TenantId
import tech.dokus.domain.model.ContactDto
import kotlinx.coroutines.flow.Flow

/**
 * Local data source for cached contacts.
 * Provides offline access to previously fetched contacts stored in SQLDelight.
 *
 * Note: Timestamps are epoch milliseconds (Long) for simplicity.
 */
interface ContactLocalDataSource {

    /**
     * Observe all cached contacts for a tenant as a Flow.
     * Emits whenever the cache changes.
     */
    fun observeAll(tenantId: TenantId): Flow<List<ContactDto>>

    /**
     * Get all cached contacts for a tenant (one-shot).
     */
    suspend fun getAll(tenantId: TenantId): List<ContactDto>

    /**
     * Get a single cached contact by ID.
     */
    suspend fun getById(id: ContactId): ContactDto?

    /**
     * Insert or update contacts in the cache.
     * Existing contacts with the same ID are replaced.
     */
    suspend fun upsertAll(tenantId: TenantId, contacts: List<ContactDto>)

    /**
     * Delete all cached contacts for a tenant.
     * Used before a full refresh.
     */
    suspend fun deleteAll(tenantId: TenantId)

    /**
     * Delete a specific cached contact.
     */
    suspend fun deleteById(id: ContactId)

    /**
     * Get the last sync time for contacts (epoch milliseconds).
     */
    suspend fun getLastSyncTime(tenantId: TenantId): Long?

    /**
     * Update the last sync time for contacts (epoch milliseconds).
     */
    suspend fun setLastSyncTime(tenantId: TenantId, timeMillis: Long)

    /**
     * Get the number of cached contacts for a tenant.
     */
    suspend fun getCount(tenantId: TenantId): Long
}
