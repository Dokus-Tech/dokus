package tech.dokus.features.contacts.datasource

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal class ContactCacheDataSourceImpl(
    private val localDataSource: ContactLocalDataSource
) : ContactCacheDataSource {
    override suspend fun getCachedContacts(tenantId: TenantId): List<ContactDto> {
        return localDataSource.getAll(tenantId)
    }

    override suspend fun cacheContacts(tenantId: TenantId, contacts: List<ContactDto>) {
        val now = Clock.System.now().toEpochMilliseconds()
        localDataSource.upsertAll(tenantId, contacts)
        localDataSource.setLastSyncTime(tenantId, now)
    }
}
