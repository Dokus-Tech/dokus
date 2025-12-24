package ai.dokus.app.contacts.cache

import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.ContactDto
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SQLDelight implementation of ContactLocalDataSource.
 * Stores contacts as JSON blobs for simplicity and flexibility.
 */
internal class ContactLocalDataSourceImpl(
    private val database: ContactsCacheDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ContactLocalDataSource {

    private val contactQueries get() = database.contactQueries
    private val metadataQueries get() = database.cacheMetadataQueries

    override fun observeAll(tenantId: TenantId): Flow<List<ContactDto>> {
        return contactQueries.selectAllByTenant(tenantId.value)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.mapNotNull { row ->
                    runCatching { json.decodeFromString<ContactDto>(row.data_) }.getOrNull()
                }
            }
    }

    override suspend fun getAll(tenantId: TenantId): List<ContactDto> = withContext(Dispatchers.IO) {
        contactQueries.selectAllByTenant(tenantId.value)
            .executeAsList()
            .mapNotNull { row ->
                runCatching { json.decodeFromString<ContactDto>(row.data_) }.getOrNull()
            }
    }

    override suspend fun getById(id: ContactId): ContactDto? = withContext(Dispatchers.IO) {
        contactQueries.selectById(id.value)
            .executeAsOneOrNull()
            ?.let { row ->
                runCatching { json.decodeFromString<ContactDto>(row.data_) }.getOrNull()
            }
    }

    override suspend fun upsertAll(tenantId: TenantId, contacts: List<ContactDto>) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        database.transaction {
            contacts.forEach { contact ->
                val jsonData = json.encodeToString(contact)
                contactQueries.upsert(
                    id = contact.id.value,
                    tenant_id = tenantId.value,
                    data_ = jsonData,
                    cached_at = now,
                    server_updated_at = contact.updatedAt.toInstant(TimeZone.UTC).toEpochMilliseconds()
                )
            }
        }

        // Update metadata
        val count = contactQueries.countByTenant(tenantId.value).executeAsOne()
        metadataQueries.upsert(
            entity_type = ENTITY_TYPE,
            tenant_id = tenantId.value,
            last_synced_at = now,
            item_count = count
        )
    }

    override suspend fun deleteAll(tenantId: TenantId) = withContext(Dispatchers.IO) {
        contactQueries.deleteAllByTenant(tenantId.value)
    }

    override suspend fun deleteById(id: ContactId) = withContext(Dispatchers.IO) {
        contactQueries.deleteById(id.value)
    }

    override suspend fun getLastSyncTime(tenantId: TenantId): Instant? = withContext(Dispatchers.IO) {
        metadataQueries.getLastSyncTime(ENTITY_TYPE, tenantId.value)
            .executeAsOneOrNull()
            ?.last_synced_at
            ?.let { Instant.fromEpochMilliseconds(it) }
    }

    override suspend fun setLastSyncTime(tenantId: TenantId, time: Instant) = withContext(Dispatchers.IO) {
        val count = contactQueries.countByTenant(tenantId.value).executeAsOne()
        metadataQueries.upsert(
            entity_type = ENTITY_TYPE,
            tenant_id = tenantId.value,
            last_synced_at = time.toEpochMilliseconds(),
            item_count = count
        )
    }

    override suspend fun getCount(tenantId: TenantId): Long = withContext(Dispatchers.IO) {
        contactQueries.countByTenant(tenantId.value).executeAsOne()
    }

    companion object {
        private const val ENTITY_TYPE = "contact"
    }
}
