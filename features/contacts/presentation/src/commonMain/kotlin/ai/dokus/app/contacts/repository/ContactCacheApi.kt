package ai.dokus.app.contacts.repository

import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ContactDto
import kotlinx.coroutines.flow.Flow
import tech.dokus.foundation.app.state.CacheState

/**
 * Interface for contact caching operations.
 * Enables testability through dependency injection of test doubles.
 */
interface ContactCacheApi {
    /**
     * Observe contacts with cache-first strategy.
     */
    fun observeContacts(
        tenantId: TenantId,
        search: String? = null,
        isActive: Boolean? = null,
        forceRefresh: Boolean = false
    ): Flow<CacheState<List<ContactDto>>>

    /**
     * Get a single contact by ID with cache-first strategy.
     */
    fun observeContact(
        contactId: ContactId,
        tenantId: TenantId,
        forceRefresh: Boolean = false
    ): Flow<CacheState<ContactDto>>

    /**
     * Get all cached contacts without network call.
     */
    suspend fun getCachedContacts(tenantId: TenantId): List<ContactDto>

    /**
     * Get last sync time for contacts.
     */
    suspend fun getLastSyncTime(tenantId: TenantId): Long?

    /**
     * Clear all cached contacts for a tenant.
     */
    suspend fun clearCache(tenantId: TenantId)

    /**
     * Manually cache contacts.
     */
    suspend fun cacheContacts(tenantId: TenantId, contacts: List<ContactDto>)
}
