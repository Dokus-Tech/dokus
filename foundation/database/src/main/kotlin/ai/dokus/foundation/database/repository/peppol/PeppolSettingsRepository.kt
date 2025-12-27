package ai.dokus.foundation.database.repository.peppol

import ai.dokus.foundation.database.tables.peppol.PeppolSettingsTable
import ai.dokus.foundation.domain.ids.PeppolId
import ai.dokus.foundation.domain.ids.PeppolSettingsId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import tech.dokus.foundation.ktor.crypto.CredentialCryptoService
import tech.dokus.foundation.ktor.database.dbQuery
import tech.dokus.foundation.ktor.utils.loggerFor
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

/**
 * Repository for Peppol settings.
 * API credentials are encrypted at rest using AES-256-GCM.
 */
class PeppolSettingsRepository(
    private val cryptoService: CredentialCryptoService
) {
    private val logger = loggerFor()

    /**
     * Get Peppol settings for a tenant.
     * Note: This does NOT return API credentials.
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
     * Credentials are decrypted before being returned.
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
     * Credentials are encrypted before storage.
     */
    suspend fun saveSettings(
        tenantId: TenantId,
        request: SavePeppolSettingsRequest
    ): Result<PeppolSettingsDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = UUID.fromString(tenantId.toString())

        // Encrypt credentials before storage
        val encryptedApiKey = cryptoService.encrypt(request.apiKey)
        val encryptedApiSecret = cryptoService.encrypt(request.apiSecret)

        dbQuery {
            val existing = PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.tenantId eq tenantUuid }
                .singleOrNull()

            if (existing != null) {
                // Update existing
                PeppolSettingsTable.update({ PeppolSettingsTable.tenantId eq tenantUuid }) {
                    it[companyId] = request.companyId
                    it[apiKey] = encryptedApiKey
                    it[apiSecret] = encryptedApiSecret
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
                    it[apiKey] = encryptedApiKey
                    it[apiSecret] = encryptedApiSecret
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

    private fun ResultRow.toDtoWithCredentials(): PeppolSettingsWithCredentials {
        val encryptedApiKey = this[PeppolSettingsTable.apiKey]
        val encryptedApiSecret = this[PeppolSettingsTable.apiSecret]

        return PeppolSettingsWithCredentials(
            settings = toDto(),
            apiKey = cryptoService.decrypt(encryptedApiKey),
            apiSecret = cryptoService.decrypt(encryptedApiSecret)
        )
    }
}

/**
 * Internal class that includes credentials - never exposed via API.
 */
data class PeppolSettingsWithCredentials(
    val settings: PeppolSettingsDto,
    val apiKey: String,
    val apiSecret: String
)
