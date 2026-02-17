package tech.dokus.database.repository.peppol
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.peppol.PeppolDirectoryCacheTable
import tech.dokus.domain.enums.PeppolLookupSource
import tech.dokus.domain.enums.PeppolLookupStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolResolution
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.uuid.Uuid

/**
 * Repository for PEPPOL directory cache.
 *
 * TTL policy:
 * - Found/NotFound: 14 days
 * - Error: 1 day
 * - Manual entries: never expire
 *
 * Staleness detection:
 * - Cache is stale if expiresAt is in the past
 * - Cache is stale if vatNumberSnapshot != contact's current vatNumber
 * - Cache is stale if companyNumberSnapshot != contact's current companyNumber
 */
class PeppolDirectoryCacheRepository {
    private val logger = loggerFor()

    companion object {
        val FOUND_TTL = 14.days
        val NOT_FOUND_TTL = 14.days
        val ERROR_TTL = 1.days
    }

    /**
     * Get cache entry for a contact.
     * Returns null if no entry exists.
     */
    suspend fun getByContactId(tenantId: TenantId, contactId: ContactId): Result<PeppolResolution?> = runCatching {
        dbQuery {
            PeppolDirectoryCacheTable.selectAll()
                .where {
                    (PeppolDirectoryCacheTable.tenantId eq tenantId.value) and
                        (PeppolDirectoryCacheTable.contactId eq contactId.value)
                }
                .map { it.toResolution() }
                .singleOrNull()
        }
    }

    /**
     * Insert or update a cache entry.
     */
    suspend fun upsert(
        tenantId: TenantId,
        contactId: ContactId,
        status: PeppolLookupStatus,
        participantId: String?,
        scheme: String?,
        supportedDocTypes: List<String>,
        source: PeppolLookupSource,
        vatNumberSnapshot: String?,
        companyNumberSnapshot: String?,
        errorMessage: String?
    ): Result<PeppolResolution> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = tenantId.value
        val contactUuid = contactId.value

        // Calculate expiry based on status and source
        val expiresAt = when {
            source == PeppolLookupSource.Manual -> null // Manual entries never expire
            status == PeppolLookupStatus.Found -> now.plusDuration(FOUND_TTL)
            status == PeppolLookupStatus.NotFound -> now.plusDuration(NOT_FOUND_TTL)
            status == PeppolLookupStatus.Error -> now.plusDuration(ERROR_TTL)
            else -> now.plusDuration(NOT_FOUND_TTL)
        }

        val docTypesJson = if (supportedDocTypes.isEmpty()) null else Json.encodeToString(supportedDocTypes)

        dbQuery {
            val existing = PeppolDirectoryCacheTable.selectAll()
                .where {
                    (PeppolDirectoryCacheTable.tenantId eq tenantUuid) and
                        (PeppolDirectoryCacheTable.contactId eq contactUuid)
                }
                .singleOrNull()

            if (existing != null) {
                PeppolDirectoryCacheTable.update({
                    (PeppolDirectoryCacheTable.tenantId eq tenantUuid) and
                        (PeppolDirectoryCacheTable.contactId eq contactUuid)
                }) {
                    it[PeppolDirectoryCacheTable.status] = status
                    it[PeppolDirectoryCacheTable.participantId] = participantId
                    it[PeppolDirectoryCacheTable.scheme] = scheme
                    it[PeppolDirectoryCacheTable.supportedDocTypes] = docTypesJson
                    it[PeppolDirectoryCacheTable.lookupSource] = source
                    it[PeppolDirectoryCacheTable.vatNumberSnapshot] = vatNumberSnapshot
                    it[PeppolDirectoryCacheTable.companyNumberSnapshot] = companyNumberSnapshot
                    it[PeppolDirectoryCacheTable.lastCheckedAt] = now
                    it[PeppolDirectoryCacheTable.expiresAt] = expiresAt
                    it[PeppolDirectoryCacheTable.errorMessage] = errorMessage
                    it[PeppolDirectoryCacheTable.updatedAt] = now
                }
            } else {
                val newId = Uuid.random()
                PeppolDirectoryCacheTable.insert {
                    it[id] = newId
                    it[PeppolDirectoryCacheTable.tenantId] = tenantUuid
                    it[PeppolDirectoryCacheTable.contactId] = contactUuid
                    it[PeppolDirectoryCacheTable.status] = status
                    it[PeppolDirectoryCacheTable.participantId] = participantId
                    it[PeppolDirectoryCacheTable.scheme] = scheme
                    it[PeppolDirectoryCacheTable.supportedDocTypes] = docTypesJson
                    it[PeppolDirectoryCacheTable.lookupSource] = source
                    it[PeppolDirectoryCacheTable.vatNumberSnapshot] = vatNumberSnapshot
                    it[PeppolDirectoryCacheTable.companyNumberSnapshot] = companyNumberSnapshot
                    it[PeppolDirectoryCacheTable.lastCheckedAt] = now
                    it[PeppolDirectoryCacheTable.expiresAt] = expiresAt
                    it[PeppolDirectoryCacheTable.errorMessage] = errorMessage
                    it[PeppolDirectoryCacheTable.createdAt] = now
                    it[PeppolDirectoryCacheTable.updatedAt] = now
                }
            }

            PeppolDirectoryCacheTable.selectAll()
                .where {
                    (PeppolDirectoryCacheTable.tenantId eq tenantUuid) and
                        (PeppolDirectoryCacheTable.contactId eq contactUuid)
                }
                .map { it.toResolution() }
                .single()
        }
    }

    /**
     * Delete cache entry for a contact.
     */
    suspend fun invalidateForContact(tenantId: TenantId, contactId: ContactId): Result<Boolean> = runCatching {
        dbQuery {
            val deleted = PeppolDirectoryCacheTable.deleteWhere {
                (PeppolDirectoryCacheTable.tenantId eq tenantId.value) and
                    (PeppolDirectoryCacheTable.contactId eq contactId.value)
            }
            deleted > 0
        }
    }

    /**
     * Check if cache entry is stale (expired or identifiers changed).
     */
    fun isStale(
        resolution: PeppolResolution,
        currentVatNumber: String?,
        currentCompanyNumber: String?
    ): Boolean {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // Check if expired (null = never expires)
        val expired = resolution.expiresAt?.let { it < now } ?: false

        // Check if identifiers changed
        val vatChanged = resolution.vatNumberSnapshot != currentVatNumber
        val companyChanged = resolution.companyNumberSnapshot != currentCompanyNumber

        return expired || vatChanged || companyChanged
    }

    /**
     * Delete all expired entries for a tenant.
     * Returns number of deleted rows.
     */
    suspend fun deleteExpired(tenantId: TenantId): Result<Int> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            PeppolDirectoryCacheTable.deleteWhere {
                (PeppolDirectoryCacheTable.tenantId eq tenantId.value) and
                    (PeppolDirectoryCacheTable.expiresAt.isNotNull()) and
                    (PeppolDirectoryCacheTable.expiresAt less now)
            }
        }
    }

    private fun ResultRow.toResolution(): PeppolResolution {
        val docTypesJson = this[PeppolDirectoryCacheTable.supportedDocTypes]
        val supportedDocTypes = if (docTypesJson.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { Json.decodeFromString<List<String>>(docTypesJson) }.getOrElse { emptyList() }
        }

        return PeppolResolution(
            contactId = ContactId(this[PeppolDirectoryCacheTable.contactId]),
            status = this[PeppolDirectoryCacheTable.status],
            participantId = this[PeppolDirectoryCacheTable.participantId],
            scheme = this[PeppolDirectoryCacheTable.scheme],
            supportedDocTypes = supportedDocTypes,
            source = this[PeppolDirectoryCacheTable.lookupSource],
            vatNumberSnapshot = this[PeppolDirectoryCacheTable.vatNumberSnapshot],
            companyNumberSnapshot = this[PeppolDirectoryCacheTable.companyNumberSnapshot],
            lastCheckedAt = this[PeppolDirectoryCacheTable.lastCheckedAt],
            expiresAt = this[PeppolDirectoryCacheTable.expiresAt],
            errorMessage = this[PeppolDirectoryCacheTable.errorMessage]
        )
    }

    private fun LocalDateTime.plusDuration(duration: kotlin.time.Duration): LocalDateTime {
        val instant = Clock.System.now()
        val newInstant = instant + duration
        return newInstant.toLocalDateTime(TimeZone.UTC)
    }
}
