package tech.dokus.database.repository.peppol
import kotlin.uuid.Uuid

import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.peppol.PeppolTransmissionsTable
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolTransmissionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Repository for Peppol transmissions.
 * CRITICAL: All queries MUST filter by tenantId.
 */
class PeppolTransmissionRepository {
    private val logger = loggerFor()

    /**
     * Create a new transmission record.
     */
    suspend fun createTransmission(
        tenantId: TenantId,
        direction: PeppolTransmissionDirection,
        documentType: PeppolDocumentType,
        invoiceId: InvoiceId? = null,
        externalDocumentId: String? = null,
        recipientPeppolId: PeppolId? = null,
        senderPeppolId: PeppolId? = null
    ): Result<PeppolTransmissionDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val newId = Uuid.random()

        dbQuery {
            PeppolTransmissionsTable.insert {
                it[id] = newId
                it[PeppolTransmissionsTable.tenantId] = Uuid.parse(tenantId.toString())
                it[PeppolTransmissionsTable.direction] = direction
                it[PeppolTransmissionsTable.documentType] = documentType
                it[status] = PeppolStatus.Pending
                it[PeppolTransmissionsTable.invoiceId] = invoiceId?.let { inv -> Uuid.parse(inv.toString()) }
                it[PeppolTransmissionsTable.externalDocumentId] = externalDocumentId
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
     * Check if a transmission exists for a given external provider document ID.
     * Useful for deduping inbound inbox polling (e.g. weekly full sync).
     */
    suspend fun existsByExternalDocumentId(
        tenantId: TenantId,
        externalDocumentId: String
    ): Result<Boolean> = runCatching {
        dbQuery {
            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                        (PeppolTransmissionsTable.externalDocumentId eq externalDocumentId)
                }
                .limit(1)
                .any()
        }
    }

    /**
     * Get a transmission by external provider document ID.
     * Useful for inbound dedupe and safe retry logic.
     */
    suspend fun getByExternalDocumentId(
        tenantId: TenantId,
        externalDocumentId: String
    ): Result<PeppolTransmissionDto?> = runCatching {
        dbQuery {
            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                        (PeppolTransmissionsTable.externalDocumentId eq externalDocumentId)
                }
                .map { it.toDto() }
                .singleOrNull()
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
                    (PeppolTransmissionsTable.id eq Uuid.parse(transmissionId.toString())) and
                        (PeppolTransmissionsTable.tenantId eq Uuid.parse(tenantId.toString()))
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
                    (PeppolTransmissionsTable.invoiceId eq Uuid.parse(invoiceId.toString())) and
                        (PeppolTransmissionsTable.tenantId eq Uuid.parse(tenantId.toString()))
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
                .where { PeppolTransmissionsTable.tenantId eq Uuid.parse(tenantId.toString()) }

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
                (PeppolTransmissionsTable.id eq Uuid.parse(transmissionId.toString())) and
                    (PeppolTransmissionsTable.tenantId eq Uuid.parse(tenantId.toString()))
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
                .where { PeppolTransmissionsTable.id eq Uuid.parse(transmissionId.toString()) }
                .map { it.toDto() }
                .single()
        }
    }

    private fun ResultRow.toDto(): PeppolTransmissionDto = PeppolTransmissionDto(
        id = PeppolTransmissionId.parse(this[PeppolTransmissionsTable.id].value.toString()),
        tenantId = TenantId.parse(this[PeppolTransmissionsTable.tenantId].toString()),
        direction = this[PeppolTransmissionsTable.direction],
        documentType = this[PeppolTransmissionsTable.documentType],
        status = this[PeppolTransmissionsTable.status],
        invoiceId = this[PeppolTransmissionsTable.invoiceId]?.let { InvoiceId.parse(it.toString()) },
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
