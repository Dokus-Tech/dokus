package tech.dokus.database.repository.peppol

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.mapper.toPeppolSettingsDto
import tech.dokus.database.tables.peppol.PeppolSettingsTable
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import tech.dokus.foundation.backend.utils.runSuspendCatching

/**
 * Repository for Peppol settings.
 * Credentials are managed via environment variables, not stored in the database.
 */
@OptIn(ExperimentalUuidApi::class)
class PeppolSettingsRepository {

    /**
     * Get Peppol settings for a tenant.
     */
    suspend fun getSettings(tenantId: TenantId): Result<PeppolSettingsDto?> = runSuspendCatching {
        dbQuery {
            PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.tenantId eq tenantId.value.toJavaUuid() }
                .map { it.toPeppolSettingsDto() }
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
    ): Result<PeppolSettingsDto> = runSuspendCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = tenantId.value.toJavaUuid()

        dbQuery {
            val existing = PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.tenantId eq tenantUuid }
                .singleOrNull()

            if (existing != null) {
                val existingWebhookToken = existing[PeppolSettingsTable.webhookToken]
                val webhookToken = existingWebhookToken.takeIf { it.isNotBlank() }
                    ?: UUID.randomUUID().toString().replace("-", "")

                // Update existing
                PeppolSettingsTable.update({ PeppolSettingsTable.tenantId eq tenantUuid }) {
                    it[PeppolSettingsTable.companyId] = companyId
                    it[PeppolSettingsTable.peppolId] = peppolId
                    it[PeppolSettingsTable.isEnabled] = isEnabled
                    it[PeppolSettingsTable.testMode] = testMode
                    it[PeppolSettingsTable.webhookToken] = webhookToken
                    it[updatedAt] = now
                }

                PeppolSettingsTable.selectAll()
                    .where { PeppolSettingsTable.tenantId eq tenantUuid }
                    .map { it.toPeppolSettingsDto() }
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
                    .map { it.toPeppolSettingsDto() }
                    .single()
            }
        }
    }

    /**
     * Delete Peppol settings for a tenant.
     */
    suspend fun deleteSettings(tenantId: TenantId): Result<Boolean> = runSuspendCatching {
        dbQuery {
            val deleted = PeppolSettingsTable.deleteWhere {
                PeppolSettingsTable.tenantId eq tenantId.value.toJavaUuid()
            }
            deleted > 0
        }
    }

    /**
     * Find tenant ID by webhook token.
     * Used by webhook endpoint to resolve tenant from token.
     */
    suspend fun getTenantIdByWebhookToken(token: String): Result<TenantId?> = runSuspendCatching {
        dbQuery {
            PeppolSettingsTable.selectAll()
                .where {
                    (PeppolSettingsTable.webhookToken eq token) and
                        (PeppolSettingsTable.isEnabled eq true)
                }
                .map { TenantId.parse(it[PeppolSettingsTable.tenantId].toString()) }
                .singleOrNull()
        }
    }

    suspend fun getEnabledSettingsByCompanyId(companyId: String): Result<PeppolSettingsDto?> = runSuspendCatching {
        dbQuery {
            PeppolSettingsTable.selectAll()
                .where {
                    (PeppolSettingsTable.companyId eq companyId) and
                        (PeppolSettingsTable.isEnabled eq true)
                }
                .map { it.toPeppolSettingsDto() }
                .singleOrNull()
        }
    }

    /**
     * Debounce webhook-triggered poll requests across all app instances.
     *
     * @return true when the slot is acquired and poll may proceed; false when debounced.
     */
    suspend fun tryAcquireWebhookPollSlot(
        tenantId: TenantId,
        now: LocalDateTime,
        debounceSeconds: Long
    ): Result<Boolean> = runSuspendCatching {
        val threshold = now.toInstant(TimeZone.UTC)
            .minus(debounceSeconds.seconds)
            .toLocalDateTime(TimeZone.UTC)
        dbQuery {
            val updated = PeppolSettingsTable.update({
                (PeppolSettingsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (PeppolSettingsTable.isEnabled eq true) and
                    (
                        (PeppolSettingsTable.lastWebhookPollTriggeredAt eq null) or
                            (PeppolSettingsTable.lastWebhookPollTriggeredAt lessEq threshold)
                        )
            }) {
                it[PeppolSettingsTable.lastWebhookPollTriggeredAt] = now
                it[updatedAt] = now
            }
            updated > 0
        }
    }

    /**
     * Get all enabled Peppol settings (for polling worker).
     */
    suspend fun getAllEnabled(): Result<List<PeppolSettingsDto>> = runSuspendCatching {
        dbQuery {
            PeppolSettingsTable.selectAll()
                .where { PeppolSettingsTable.isEnabled eq true }
                .map { it.toPeppolSettingsDto() }
        }
    }

    /**
     * Update the lastFullSyncAt timestamp after a full sync.
     */
    suspend fun updateLastFullSyncAt(tenantId: TenantId): Result<Unit> = runSuspendCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            PeppolSettingsTable.update({
                PeppolSettingsTable.tenantId eq tenantId.value.toJavaUuid()
            }) {
                it[lastFullSyncAt] = now
                it[updatedAt] = now
            }
        }
    }

}
