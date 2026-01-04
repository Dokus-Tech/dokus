package tech.dokus.features.contacts.usecases

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.datasource.ContactCacheDataSource

internal class GetCachedContactsUseCaseImpl(
    private val cacheDataSource: ContactCacheDataSource
) : GetCachedContactsUseCase {
    override suspend fun invoke(tenantId: TenantId): List<ContactDto> {
        return cacheDataSource.getCachedContacts(tenantId)
    }
}

internal class CacheContactsUseCaseImpl(
    private val cacheDataSource: ContactCacheDataSource
) : CacheContactsUseCase {
    override suspend fun invoke(tenantId: TenantId, contacts: List<ContactDto>) {
        cacheDataSource.cacheContacts(tenantId, contacts)
    }
}
