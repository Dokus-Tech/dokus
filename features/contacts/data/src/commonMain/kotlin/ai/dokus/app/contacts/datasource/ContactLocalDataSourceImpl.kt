package ai.dokus.app.contacts.datasource

import ai.dokus.app.contacts.cache.ContactsCacheDatabase

import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ContactDto
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * SQLDelight implementation of ContactLocalDataSource.
 * Stores contacts as JSON blobs for simplicity and flexibility.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class, ExperimentalTime::class)
internal class ContactLocalDataSourceImpl(
    private val database: ContactsCacheDatabase,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ContactLocalDataSource {

    private val contactQueries get() = database.contactQueries
    private val metadataQueries get() = database.cacheMetadataQueries

    override fun observeAll(tenantId: TenantId): Flow<List<ContactDto>> {
        return contactQueries.selectAllByTenant(tenantId.value.toString())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.mapNotNull { row ->
                    runCatching { json.decodeFromString<ContactDto>(row.data_) }.getOrNull()
                }
            }
    }

    override suspend fun getAll(tenantId: TenantId): List<ContactDto> = withContext(Dispatchers.Default) {
        contactQueries.selectAllByTenant(tenantId.value.toString())
            .executeAsList()
            .mapNotNull { row ->
                runCatching { json.decodeFromString<ContactDto>(row.data_) }.getOrNull()
            }
    }

    override suspend fun getById(id: ContactId): ContactDto? = withContext(Dispatchers.Default) {
        contactQueries.selectById(id.value.toString())
            .executeAsOneOrNull()
            ?.let { row ->
                runCatching { json.decodeFromString<ContactDto>(row.data_) }.getOrNull()
            }
    }

    override suspend fun upsertAll(tenantId: TenantId, contacts: List<ContactDto>) {
        withContext(Dispatchers.Default) {
            val now = Clock.System.now().toEpochMilliseconds()
            val tenantIdStr = tenantId.value.toString()
            database.transaction {
                contacts.forEach { contact ->
                    val jsonData = json.encodeToString(contact)
                    // Use createdAt as fallback since updatedAt may require experimental APIs
                    val serverUpdatedAt = runCatching {
                        contact.updatedAt.toInstant(TimeZone.UTC).toEpochMilliseconds()
                    }.getOrElse { now }
                    contactQueries.upsert(
                        id = contact.id.value.toString(),
                        tenant_id = tenantIdStr,
                        data_ = jsonData,
                        cached_at = now,
                        server_updated_at = serverUpdatedAt
                    )
                }
            }

            // Update metadata
            val count = contactQueries.countByTenant(tenantIdStr).executeAsOne()
            metadataQueries.upsert(
                entity_type = ENTITY_TYPE,
                tenant_id = tenantIdStr,
                last_synced_at = now,
                item_count = count
            )
        }
    }

    override suspend fun deleteAll(tenantId: TenantId) {
        withContext(Dispatchers.Default) {
            contactQueries.deleteAllByTenant(tenantId.value.toString())
        }
    }

    override suspend fun deleteById(id: ContactId) {
        withContext(Dispatchers.Default) {
            contactQueries.deleteById(id.value.toString())
        }
    }

    override suspend fun getLastSyncTime(tenantId: TenantId): Long? = withContext(Dispatchers.Default) {
        metadataQueries.getLastSyncTime(ENTITY_TYPE, tenantId.value.toString())
            .executeAsOneOrNull()
    }

    override suspend fun setLastSyncTime(tenantId: TenantId, timeMillis: Long) {
        withContext(Dispatchers.Default) {
            val tenantIdStr = tenantId.value.toString()
            val count = contactQueries.countByTenant(tenantIdStr).executeAsOne()
            metadataQueries.upsert(
                entity_type = ENTITY_TYPE,
                tenant_id = tenantIdStr,
                last_synced_at = timeMillis,
                item_count = count
            )
        }
    }

    override suspend fun getCount(tenantId: TenantId): Long = withContext(Dispatchers.Default) {
        contactQueries.countByTenant(tenantId.value.toString()).executeAsOne()
    }

    companion object {
        private const val ENTITY_TYPE = "contact"
    }
}
