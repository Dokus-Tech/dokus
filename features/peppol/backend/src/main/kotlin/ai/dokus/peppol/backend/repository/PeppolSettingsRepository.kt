package ai.dokus.peppol.backend.repository

import ai.dokus.foundation.domain.ids.PeppolId
import ai.dokus.foundation.domain.ids.PeppolSettingsId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.peppol.backend.database.tables.PeppolSettingsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Repository for Peppol settings.
 * CRITICAL: API credentials should be encrypted before storage.
 */
class PeppolSettingsRepository {
    private val logger = LoggerFactory.getLogger(PeppolSettingsRepository::class.java)

    /**
     * Get Peppol settings for a tenant.
     */
    suspend fun getSettings(tenantId: TenantId): Result<PeppolSettingsDto?> = runCatching {
        dbQuery {
            PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.tenantId eq UUID.fromString(tenantId.toString()) }
                .map { it.toDto() }
                .singleOrNull()
        }
    }

    /**
     * Get settings with credentials (for internal use only).
     */
    suspend fun getSettingsWithCredentials(tenantId: TenantId): Result<PeppolSettingsWithCredentials?> = runCatching {
        dbQuery {
            PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.tenantId eq UUID.fromString(tenantId.toString()) }
                .map { it.toDtoWithCredentials() }
                .singleOrNull()
        }
    }

    /**
     * Save (create or update) Peppol settings for a tenant.
     */
    suspend fun saveSettings(
        tenantId: TenantId,
        request: SavePeppolSettingsRequest
    ): Result<PeppolSettingsDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = UUID.fromString(tenantId.toString())

        dbQuery {
            val existing = PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.tenantId eq tenantUuid }
                .singleOrNull()

            if (existing != null) {
                // Update existing
                PeppolSettingsTable.update({ PeppolSettingsTable.tenantId eq tenantUuid }) {
                    it[companyId] = request.companyId
                    it[apiKey] = request.apiKey  // TODO: Encrypt
                    it[apiSecret] = request.apiSecret  // TODO: Encrypt
                    it[peppolId] = request.peppolId
                    it[isEnabled] = request.isEnabled
                    it[testMode] = request.testMode
                    it[updatedAt] = now
                }

                PeppolSettingsTable.selectAll()
                    .where { PeppolSettingsTable.tenantId eq tenantUuid }
                    .map { it.toDto() }
                    .single()
            } else {
                // Create new
                val newId = UUID.randomUUID()
                PeppolSettingsTable.insert {
                    it[id] = newId
                    it[PeppolSettingsTable.tenantId] = tenantUuid
                    it[companyId] = request.companyId
                    it[apiKey] = request.apiKey  // TODO: Encrypt
                    it[apiSecret] = request.apiSecret  // TODO: Encrypt
                    it[peppolId] = request.peppolId
                    it[isEnabled] = request.isEnabled
                    it[testMode] = request.testMode
                    it[createdAt] = now
                    it[updatedAt] = now
                }

                PeppolSettingsTable.selectAll()
                    .where { PeppolSettingsTable.id eq newId }
                    .map { it.toDto() }
                    .single()
            }
        }
    }

    /**
     * Delete Peppol settings for a tenant.
     */
    suspend fun deleteSettings(tenantId: TenantId): Result<Boolean> = runCatching {
        dbQuery {
            val deleted = PeppolSettingsTable.deleteWhere {
                PeppolSettingsTable.tenantId eq UUID.fromString(tenantId.toString())
            }
            deleted > 0
        }
    }

    private fun ResultRow.toDto(): PeppolSettingsDto = PeppolSettingsDto(
        id = PeppolSettingsId.parse(this[PeppolSettingsTable.id].value.toString()),
        tenantId = TenantId.parse(this[PeppolSettingsTable.tenantId].toString()),
        companyId = this[PeppolSettingsTable.companyId],
        peppolId = PeppolId(this[PeppolSettingsTable.peppolId]),
        isEnabled = this[PeppolSettingsTable.isEnabled],
        testMode = this[PeppolSettingsTable.testMode],
        createdAt = this[PeppolSettingsTable.createdAt],
        updatedAt = this[PeppolSettingsTable.updatedAt]
    )

    private fun ResultRow.toDtoWithCredentials(): PeppolSettingsWithCredentials = PeppolSettingsWithCredentials(
        settings = toDto(),
        apiKey = this[PeppolSettingsTable.apiKey],  // TODO: Decrypt
        apiSecret = this[PeppolSettingsTable.apiSecret]  // TODO: Decrypt
    )
}

/**
 * Internal class that includes credentials - never exposed via API.
 */
data class PeppolSettingsWithCredentials(
    val settings: PeppolSettingsDto,
    val apiKey: String,
    val apiSecret: String
)
