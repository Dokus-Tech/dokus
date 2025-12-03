package ai.dokus.media.backend.repository

import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.MediaExtraction
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.database.tables.media.MediaTable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

data class MediaRecord(
    val dto: MediaDto,
    val storageKey: String,
    val storageBucket: String
)

class MediaRepository(
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun create(
        mediaId: MediaId,
        tenantId: TenantId,
        filename: String,
        mimeType: String,
        sizeBytes: Long,
        status: MediaStatus,
        storageKey: String,
        storageBucket: String,
        processingSummary: String? = null,
        attachedEntityType: EntityType? = null,
        attachedEntityId: String? = null
    ): Result<MediaRecord> = runCatching {
        dbQuery {
            MediaTable.insert {
                it[id] = EntityID(UUID.fromString(mediaId.toString()), MediaTable)
                it[MediaTable.tenantId] = UUID.fromString(tenantId.toString())
                it[MediaTable.filename] = filename
                it[MediaTable.mimeType] = mimeType
                it[MediaTable.sizeBytes] = sizeBytes
                it[MediaTable.status] = status
                it[MediaTable.processingSummary] = processingSummary
                it[MediaTable.storageKey] = storageKey
                it[MediaTable.storageBucket] = storageBucket
                it[MediaTable.attachedEntityType] = attachedEntityType
                it[MediaTable.attachedEntityId] = attachedEntityId
            }.resultedValues?.singleOrNull()?.let { toRecord(it) }
                ?: throw IllegalStateException("Failed to insert media record")
        }
    }

    suspend fun get(
        mediaId: MediaId,
        tenantId: TenantId
    ): Result<MediaRecord?> = runCatching {
        dbQuery {
            MediaTable.selectAll()
                .where {
                    (MediaTable.id eq UUID.fromString(mediaId.toString())) and
                        (MediaTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .singleOrNull()
                ?.let { toRecord(it) }
        }
    }

    suspend fun list(
        tenantId: TenantId,
        status: MediaStatus?,
        limit: Int,
        offset: Int
    ): Result<List<MediaRecord>> = runCatching {
        dbQuery {
            val baseQuery = MediaTable.selectAll().where {
                MediaTable.tenantId eq UUID.fromString(tenantId.toString())
            }
            val filtered = status?.let { baseQuery.andWhere { MediaTable.status eq it } } ?: baseQuery

            filtered
                .orderBy(MediaTable.createdAt to SortOrder.DESC)
                .limit(limit)
                .map { toRecord(it) }
        }
    }

    suspend fun attach(
        mediaId: MediaId,
        tenantId: TenantId,
        entityType: EntityType,
        entityId: String
    ): Result<MediaRecord?> = runCatching {
        dbQuery {
            MediaTable.update(
                where = {
                    (MediaTable.id eq UUID.fromString(mediaId.toString())) and
                        (MediaTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
            ) {
                it[attachedEntityType] = entityType
                it[attachedEntityId] = entityId
                it[updatedAt] = CurrentDateTime
            }
        }
        get(mediaId, tenantId).getOrThrow()
    }

    suspend fun updateProcessing(
        mediaId: MediaId,
        tenantId: TenantId,
        status: MediaStatus,
        processingSummary: String?,
        extraction: MediaExtraction?,
        errorMessage: String?,
        attachedEntityType: EntityType?,
        attachedEntityId: String?
    ): Result<MediaRecord?> = runCatching {
        dbQuery {
            MediaTable.update(
                where = {
                    (MediaTable.id eq UUID.fromString(mediaId.toString())) and
                        (MediaTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
            ) {
                it[MediaTable.status] = status
                it[MediaTable.processingSummary] = processingSummary
                it[MediaTable.errorMessage] = errorMessage
                it[MediaTable.extractionJson] = extraction?.let { json.encodeToString(MediaExtraction.serializer(), it) }
                it[MediaTable.attachedEntityType] = attachedEntityType
                it[MediaTable.attachedEntityId] = attachedEntityId
                it[updatedAt] = CurrentDateTime
            }
        }
        get(mediaId, tenantId).getOrThrow()
    }

    private fun toRecord(row: ResultRow): MediaRecord {
        val extractionJson = row[MediaTable.extractionJson]
        val extraction = extractionJson?.let { json.decodeFromString(MediaExtraction.serializer(), it) }

        val dto = MediaDto(
            id = MediaId.parse(row[MediaTable.id].value.toString()),
            tenantId = TenantId.parse(row[MediaTable.tenantId].toString()),
            filename = row[MediaTable.filename],
            mimeType = row[MediaTable.mimeType],
            sizeBytes = row[MediaTable.sizeBytes],
            status = row[MediaTable.status],
            processingSummary = row[MediaTable.processingSummary],
            extraction = extraction,
            attachedEntityType = row[MediaTable.attachedEntityType],
            attachedEntityId = row[MediaTable.attachedEntityId],
            errorMessage = row[MediaTable.errorMessage],
            createdAt = row[MediaTable.createdAt],
            updatedAt = row[MediaTable.updatedAt],
            downloadUrl = null
        )

        return MediaRecord(
            dto = dto,
            storageKey = row[MediaTable.storageKey],
            storageBucket = row[MediaTable.storageBucket]
        )
    }
}
