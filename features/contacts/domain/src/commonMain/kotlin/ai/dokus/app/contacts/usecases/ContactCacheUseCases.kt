package ai.dokus.app.contacts.usecases

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ContactDto

/**
 * Use case for getting cached contacts for offline support.
 */
interface GetCachedContactsUseCase {
    suspend operator fun invoke(tenantId: TenantId): List<ContactDto>
}

/**
 * Use case for caching contacts for offline support.
 */
interface CacheContactsUseCase {
    suspend operator fun invoke(tenantId: TenantId, contacts: List<ContactDto>)
}
