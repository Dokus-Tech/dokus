package tech.dokus.features.contacts.datasource

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto

/**
 * Cache-only data source for contacts.
 */
interface ContactCacheDataSource {
    suspend fun getCachedContacts(tenantId: TenantId): List<ContactDto>

    suspend fun cacheContacts(tenantId: TenantId, contacts: List<ContactDto>)
}
