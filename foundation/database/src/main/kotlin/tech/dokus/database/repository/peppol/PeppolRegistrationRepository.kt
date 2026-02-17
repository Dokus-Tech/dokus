package tech.dokus.database.repository.peppol
import kotlin.uuid.Uuid

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.peppol.PeppolRegistrationTable
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.ids.PeppolRegistrationId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.foundation.backend.database.dbQuery

/**
 * Repository for PEPPOL registration state management.
 */
class PeppolRegistrationRepository {

    /**
     * Get PEPPOL registration for a tenant.
     */
    suspend fun getRegistration(tenantId: TenantId): Result<PeppolRegistrationDto?> = runCatching {
        dbQuery {
            PeppolRegistrationTable.selectAll()
                .where { PeppolRegistrationTable.tenantId eq Uuid.parse(tenantId.toString()) }
                .map { it.toDto() }
                .singleOrNull()
        }
    }

    /**
     * Create a new PEPPOL registration for a tenant.
     */
    suspend fun createRegistration(
        tenantId: TenantId,
        peppolId: String,
        status: PeppolRegistrationStatus = PeppolRegistrationStatus.NotConfigured,
        testMode: Boolean = false
    ): Result<PeppolRegistrationDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = Uuid.parse(tenantId.toString())
        val newId = Uuid.random()

        dbQuery {
            PeppolRegistrationTable.insert {
                it[id] = newId
                it[PeppolRegistrationTable.tenantId] = tenantUuid
                it[PeppolRegistrationTable.peppolId] = peppolId
                it[PeppolRegistrationTable.status] = status
                it[PeppolRegistrationTable.testMode] = testMode
                it[createdAt] = now
                it[updatedAt] = now
            }

            PeppolRegistrationTable.selectAll()
                .where { PeppolRegistrationTable.id eq newId }
                .map { it.toDto() }
                .single()
        }
    }

    /**
     * Update registration status.
     */
    suspend fun updateStatus(
        tenantId: TenantId,
        status: PeppolRegistrationStatus,
        errorMessage: String? = null
    ): Result<Unit> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            PeppolRegistrationTable.update({
                PeppolRegistrationTable.tenantId eq Uuid.parse(tenantId.toString())
            }) {
                it[PeppolRegistrationTable.status] = status
                it[PeppolRegistrationTable.errorMessage] = errorMessage
                it[updatedAt] = now
            }
        }
    }

    /**
     * Update registration capabilities.
     */
    suspend fun updateCapabilities(
        tenantId: TenantId,
        canReceive: Boolean,
        canSend: Boolean
    ): Result<Unit> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            PeppolRegistrationTable.update({
                PeppolRegistrationTable.tenantId eq Uuid.parse(tenantId.toString())
            }) {
                it[PeppolRegistrationTable.canReceive] = canReceive
                it[PeppolRegistrationTable.canSend] = canSend
                it[updatedAt] = now
            }
        }
    }

    /**
     * Set registration to WAITING_TRANSFER status.
     */
    suspend fun setWaitingForTransfer(tenantId: TenantId): Result<Unit> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            PeppolRegistrationTable.update({
                PeppolRegistrationTable.tenantId eq Uuid.parse(tenantId.toString())
            }) {
                it[status] = PeppolRegistrationStatus.WaitingTransfer
                it[waitingSince] = now
                it[errorMessage] = null
                it[updatedAt] = now
            }
        }
    }

    /**
     * Record that we polled for transfer status.
     */
    suspend fun recordPoll(tenantId: TenantId): Result<Unit> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            PeppolRegistrationTable.update({
                PeppolRegistrationTable.tenantId eq Uuid.parse(tenantId.toString())
            }) {
                it[lastPolledAt] = now
                it[updatedAt] = now
            }
        }
    }

    /**
     * Update Recommand company ID after successful registration.
     */
    suspend fun updateRecommandCompanyId(
        tenantId: TenantId,
        companyId: String
    ): Result<Unit> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            PeppolRegistrationTable.update({
                PeppolRegistrationTable.tenantId eq Uuid.parse(tenantId.toString())
            }) {
                it[recommandCompanyId] = companyId
                it[updatedAt] = now
            }
        }
    }

    /**
     * List all registrations in WAITING_TRANSFER status (for polling service).
     */
    suspend fun listPendingTransfers(): Result<List<PeppolRegistrationDto>> = runCatching {
        dbQuery {
            PeppolRegistrationTable.selectAll()
                .where { PeppolRegistrationTable.status eq PeppolRegistrationStatus.WaitingTransfer }
                .map { it.toDto() }
        }
    }

    private fun ResultRow.toDto(): PeppolRegistrationDto = PeppolRegistrationDto(
        id = PeppolRegistrationId.parse(this[PeppolRegistrationTable.id].value.toString()),
        tenantId = TenantId.parse(this[PeppolRegistrationTable.tenantId].toString()),
        peppolId = this[PeppolRegistrationTable.peppolId],
        recommandCompanyId = this[PeppolRegistrationTable.recommandCompanyId],
        status = this[PeppolRegistrationTable.status],
        canReceive = this[PeppolRegistrationTable.canReceive],
        canSend = this[PeppolRegistrationTable.canSend],
        testMode = this[PeppolRegistrationTable.testMode],
        waitingSince = this[PeppolRegistrationTable.waitingSince],
        lastPolledAt = this[PeppolRegistrationTable.lastPolledAt],
        errorMessage = this[PeppolRegistrationTable.errorMessage],
        createdAt = this[PeppolRegistrationTable.createdAt],
        updatedAt = this[PeppolRegistrationTable.updatedAt]
    )
}
