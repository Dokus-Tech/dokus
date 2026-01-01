package tech.dokus.features.contacts.usecases

import tech.dokus.features.contacts.repository.ContactRepository
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto

internal class GetCachedContactsUseCaseImpl(
    private val repository: ContactRepository
) : GetCachedContactsUseCase {
    override suspend fun invoke(tenantId: TenantId): List<ContactDto> {
        return repository.getCachedContacts(tenantId)
    }
}

internal class CacheContactsUseCaseImpl(
    private val repository: ContactRepository
) : CacheContactsUseCase {
    override suspend fun invoke(tenantId: TenantId, contacts: List<ContactDto>) {
        repository.cacheContacts(tenantId, contacts)
    }
}
