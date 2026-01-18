package tech.dokus.database.repository.peppol

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.peppol.PeppolSettingsTable
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID

/**
 * Repository for Peppol settings.
 * Credentials are managed via environment variables, not stored in the database.
 */
class PeppolSettingsRepository {

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
     * Save (create or update) Peppol settings for a tenant.
     */
    suspend fun saveSettings(
        tenantId: TenantId,
        companyId: String,
        peppolId: String,
        isEnabled: Boolean = true,
        testMode: Boolean = false
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
                    it[PeppolSettingsTable.companyId] = companyId
                    it[PeppolSettingsTable.peppolId] = peppolId
                    it[PeppolSettingsTable.isEnabled] = isEnabled
                    it[PeppolSettingsTable.testMode] = testMode
                    it[updatedAt] = now
                }

                PeppolSettingsTable.selectAll()
                    .where { PeppolSettingsTable.tenantId eq tenantUuid }
                    .map { it.toDto() }
                    .single()
            } else {
                // Create new with generated webhook token
                val newId = UUID.randomUUID()
                val newWebhookToken = UUID.randomUUID().toString().replace("-", "")
                PeppolSettingsTable.insert {
                    it[id] = newId
                    it[PeppolSettingsTable.tenantId] = tenantUuid
                    it[PeppolSettingsTable.companyId] = companyId
                    it[PeppolSettingsTable.peppolId] = peppolId
                    it[PeppolSettingsTable.isEnabled] = isEnabled
                    it[PeppolSettingsTable.testMode] = testMode
                    it[webhookToken] = newWebhookToken
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

    /**
     * Find tenant ID by webhook token.
     * Used by webhook endpoint to resolve tenant from token.
     */
    suspend fun getTenantIdByWebhookToken(token: String): Result<TenantId?> = runCatching {
        dbQuery {
            PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.webhookToken eq token }
                .map { TenantId.parse(it[PeppolSettingsTable.tenantId].toString()) }
                .singleOrNull()
        }
    }

    /**
     * Get all enabled Peppol settings (for polling worker).
     */
    suspend fun getAllEnabled(): Result<List<PeppolSettingsDto>> = runCatching {
        dbQuery {
            PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.isEnabled eq true }
                .map { it.toDto() }
        }
    }

    /**
     * Update the lastFullSyncAt timestamp after a full sync.
     */
    suspend fun updateLastFullSyncAt(tenantId: TenantId): Result<Unit> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            PeppolSettingsTable.update({
                PeppolSettingsTable.tenantId eq UUID.fromString(tenantId.toString())
            }) {
                it[lastFullSyncAt] = now
                it[updatedAt] = now
            }
        }
    }

    private fun ResultRow.toDto(): PeppolSettingsDto = PeppolSettingsDto(
        id = PeppolSettingsId.parse(this[PeppolSettingsTable.id].value.toString()),
        tenantId = TenantId.parse(this[PeppolSettingsTable.tenantId].toString()),
        companyId = this[PeppolSettingsTable.companyId],
        peppolId = PeppolId(this[PeppolSettingsTable.peppolId]),
        isEnabled = this[PeppolSettingsTable.isEnabled],
        testMode = this[PeppolSettingsTable.testMode],
        webhookToken = this[PeppolSettingsTable.webhookToken],
        lastFullSyncAt = this[PeppolSettingsTable.lastFullSyncAt],
        createdAt = this[PeppolSettingsTable.createdAt],
        updatedAt = this[PeppolSettingsTable.updatedAt]
    )
}
