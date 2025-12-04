package ai.dokus.peppol.backend.repository

import ai.dokus.foundation.domain.enums.PeppolDocumentType
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.ids.BillId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.PeppolId
import ai.dokus.foundation.domain.ids.PeppolTransmissionId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.PeppolTransmissionDto
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.database.tables.peppol.PeppolTransmissionsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Repository for Peppol transmissions.
 * CRITICAL: All queries MUST filter by tenantId.
 */
class PeppolTransmissionRepository {
    private val logger = LoggerFactory.getLogger(PeppolTransmissionRepository::class.java)

    /**
     * Create a new transmission record.
     */
    suspend fun createTransmission(
        tenantId: TenantId,
        direction: PeppolTransmissionDirection,
        documentType: PeppolDocumentType,
        invoiceId: InvoiceId? = null,
        billId: BillId? = null,
        recipientPeppolId: PeppolId? = null,
        senderPeppolId: PeppolId? = null
    ): Result<PeppolTransmissionDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val newId = UUID.randomUUID()

        dbQuery {
            PeppolTransmissionsTable.insert {
                it[id] = newId
                it[PeppolTransmissionsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[PeppolTransmissionsTable.direction] = direction
                it[PeppolTransmissionsTable.documentType] = documentType
                it[status] = PeppolStatus.Pending
                it[PeppolTransmissionsTable.invoiceId] = invoiceId?.let { inv -> UUID.fromString(inv.toString()) }
                it[PeppolTransmissionsTable.billId] = billId?.let { b -> UUID.fromString(b.toString()) }
                it[PeppolTransmissionsTable.recipientPeppolId] = recipientPeppolId?.value
                it[PeppolTransmissionsTable.senderPeppolId] = senderPeppolId?.value
                it[createdAt] = now
                it[updatedAt] = now
            }

            PeppolTransmissionsTable.selectAll()
                .where { PeppolTransmissionsTable.id eq newId }
                .map { it.toDto() }
                .single()
        }
    }

    /**
     * Get a transmission by ID.
     */
    suspend fun getTransmission(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId
    ): Result<PeppolTransmissionDto?> = runCatching {
        dbQuery {
            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                    (PeppolTransmissionsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .map { it.toDto() }
                .singleOrNull()
        }
    }

    /**
     * Get transmission by invoice ID.
     */
    suspend fun getTransmissionByInvoiceId(
        invoiceId: InvoiceId,
        tenantId: TenantId
    ): Result<PeppolTransmissionDto?> = runCatching {
        dbQuery {
            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.invoiceId eq UUID.fromString(invoiceId.toString())) and
                    (PeppolTransmissionsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
                .orderBy(PeppolTransmissionsTable.createdAt to SortOrder.DESC)
                .map { it.toDto() }
                .firstOrNull()
        }
    }

    /**
     * List transmissions for a tenant with optional filters.
     */
    suspend fun listTransmissions(
        tenantId: TenantId,
        direction: PeppolTransmissionDirection? = null,
        status: PeppolStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PeppolTransmissionDto>> = runCatching {
        dbQuery {
            var query = PeppolTransmissionsTable.selectAll()
                .where { PeppolTransmissionsTable.tenantId eq UUID.fromString(tenantId.toString()) }

            direction?.let {
                query = query.andWhere { PeppolTransmissionsTable.direction eq it }
            }

            status?.let {
                query = query.andWhere { PeppolTransmissionsTable.status eq it }
            }

            query
                .orderBy(PeppolTransmissionsTable.createdAt to SortOrder.DESC)
                .limit(limit + offset)
                .map { it.toDto() }
                .drop(offset)
        }
    }

    /**
     * Update transmission status and details after sending.
     */
    suspend fun updateTransmissionResult(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId,
        status: PeppolStatus,
        externalDocumentId: String? = null,
        errorMessage: String? = null,
        rawRequest: String? = null,
        rawResponse: String? = null,
        transmittedAt: LocalDateTime? = null
    ): Result<PeppolTransmissionDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        dbQuery {
            PeppolTransmissionsTable.update({
                (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                (PeppolTransmissionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[PeppolTransmissionsTable.status] = status
                it[PeppolTransmissionsTable.externalDocumentId] = externalDocumentId
                it[PeppolTransmissionsTable.errorMessage] = errorMessage
                it[PeppolTransmissionsTable.rawRequest] = rawRequest
                it[PeppolTransmissionsTable.rawResponse] = rawResponse
                it[PeppolTransmissionsTable.transmittedAt] = transmittedAt
                it[updatedAt] = now
            }

            PeppolTransmissionsTable.selectAll()
                .where { PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString()) }
                .map { it.toDto() }
                .single()
        }
    }

    /**
     * Link a bill to an inbound transmission.
     */
    suspend fun linkBillToTransmission(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId,
        billId: BillId
    ): Result<Boolean> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        dbQuery {
            val updated = PeppolTransmissionsTable.update({
                (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                (PeppolTransmissionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[PeppolTransmissionsTable.billId] = UUID.fromString(billId.toString())
                it[updatedAt] = now
            }
            updated > 0
        }
    }

    private fun ResultRow.toDto(): PeppolTransmissionDto = PeppolTransmissionDto(
        id = PeppolTransmissionId.parse(this[PeppolTransmissionsTable.id].value.toString()),
        tenantId = TenantId.parse(this[PeppolTransmissionsTable.tenantId].toString()),
        direction = this[PeppolTransmissionsTable.direction],
        documentType = this[PeppolTransmissionsTable.documentType],
        status = this[PeppolTransmissionsTable.status],
        invoiceId = this[PeppolTransmissionsTable.invoiceId]?.let { InvoiceId.parse(it.toString()) },
        billId = this[PeppolTransmissionsTable.billId]?.let { BillId.parse(it.toString()) },
        externalDocumentId = this[PeppolTransmissionsTable.externalDocumentId],
        recipientPeppolId = this[PeppolTransmissionsTable.recipientPeppolId]?.let { PeppolId(it) },
        senderPeppolId = this[PeppolTransmissionsTable.senderPeppolId]?.let { PeppolId(it) },
        errorMessage = this[PeppolTransmissionsTable.errorMessage],
        rawRequest = this[PeppolTransmissionsTable.rawRequest],
        rawResponse = this[PeppolTransmissionsTable.rawResponse],
        transmittedAt = this[PeppolTransmissionsTable.transmittedAt],
        createdAt = this[PeppolTransmissionsTable.createdAt],
        updatedAt = this[PeppolTransmissionsTable.updatedAt]
    )
}
