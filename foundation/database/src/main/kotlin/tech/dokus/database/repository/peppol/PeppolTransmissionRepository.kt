package tech.dokus.database.repository.peppol

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
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
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Internal transmission projection used by workers/reconciliation.
 * Includes fields that are intentionally hidden from public DTOs.
 */
data class PeppolTransmissionInternal(
    val id: PeppolTransmissionId,
    val tenantId: TenantId,
    val direction: PeppolTransmissionDirection,
    val documentType: PeppolDocumentType,
    val status: PeppolStatus,
    val invoiceId: InvoiceId? = null,
    val externalDocumentId: String? = null,
    val idempotencyKey: String,
    val recipientPeppolId: PeppolId? = null,
    val senderPeppolId: PeppolId? = null,
    val errorMessage: String? = null,
    val providerErrorCode: String? = null,
    val providerErrorMessage: String? = null,
    val attemptCount: Int = 0,
    val nextRetryAt: LocalDateTime? = null,
    val lastAttemptAt: LocalDateTime? = null,
    val rawRequest: String? = null,
    val rawResponse: String? = null,
    val rawUblXmlKey: String? = null,
    val transmittedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Repository for Peppol transmissions.
 * CRITICAL: All tenant-scoped queries MUST filter by tenantId.
 */
@OptIn(ExperimentalUuidApi::class)
class PeppolTransmissionRepository {
    private val logger = loggerFor()

    private val outboundClaimableStatuses = listOf(PeppolStatus.Queued, PeppolStatus.FailedRetryable)

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
        senderPeppolId: PeppolId? = null,
        idempotencyKey: String? = null,
        rawUblXmlKey: String? = null
    ): Result<PeppolTransmissionDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val newId = UUID.randomUUID()
        val tenantUuid = tenantId.value.toJavaUuid()

        dbQuery {
            PeppolTransmissionsTable.insert {
                it[id] = newId
                it[PeppolTransmissionsTable.tenantId] = tenantUuid
                it[PeppolTransmissionsTable.direction] = direction
                it[PeppolTransmissionsTable.documentType] = documentType
                it[status] = PeppolStatus.Pending
                it[PeppolTransmissionsTable.idempotencyKey] = idempotencyKey ?: "tx-$newId"
                it[PeppolTransmissionsTable.invoiceId] = invoiceId?.let { inv -> UUID.fromString(inv.toString()) }
                it[PeppolTransmissionsTable.externalDocumentId] = externalDocumentId
                it[PeppolTransmissionsTable.recipientPeppolId] = recipientPeppolId?.value
                it[PeppolTransmissionsTable.senderPeppolId] = senderPeppolId?.value
                it[PeppolTransmissionsTable.rawUblXmlKey] = rawUblXmlKey
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
     * Idempotent enqueue for outbound sends.
     * If (tenantId, idempotencyKey) already exists, returns the existing row.
     */
    suspend fun upsertOutboundQueued(
        tenantId: TenantId,
        documentType: PeppolDocumentType,
        invoiceId: InvoiceId,
        recipientPeppolId: PeppolId,
        idempotencyKey: String,
        rawRequest: String,
        rawUblXmlKey: String? = null
    ): Result<PeppolTransmissionInternal> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = tenantId.value.toJavaUuid()
        val invoiceUuid = UUID.fromString(invoiceId.toString())

        dbQuery {
            val existing = PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.tenantId eq tenantUuid) and
                        (PeppolTransmissionsTable.idempotencyKey eq idempotencyKey)
                }
                .singleOrNull()
                ?.toInternal()
            if (existing != null) {
                return@dbQuery existing
            }

            try {
                PeppolTransmissionsTable.insert {
                    it[PeppolTransmissionsTable.tenantId] = tenantUuid
                    it[PeppolTransmissionsTable.direction] = PeppolTransmissionDirection.Outbound
                    it[PeppolTransmissionsTable.documentType] = documentType
                    it[PeppolTransmissionsTable.status] = PeppolStatus.Queued
                    it[PeppolTransmissionsTable.idempotencyKey] = idempotencyKey
                    it[PeppolTransmissionsTable.invoiceId] = invoiceUuid
                    it[PeppolTransmissionsTable.recipientPeppolId] = recipientPeppolId.value
                    it[PeppolTransmissionsTable.rawRequest] = rawRequest
                    it[PeppolTransmissionsTable.rawUblXmlKey] = rawUblXmlKey
                    it[PeppolTransmissionsTable.attemptCount] = 0
                    it[PeppolTransmissionsTable.nextRetryAt] = null
                    it[PeppolTransmissionsTable.lastAttemptAt] = null
                    it[PeppolTransmissionsTable.providerErrorCode] = null
                    it[PeppolTransmissionsTable.providerErrorMessage] = null
                    it[PeppolTransmissionsTable.errorMessage] = null
                    it[PeppolTransmissionsTable.transmittedAt] = null
                    it[PeppolTransmissionsTable.createdAt] = now
                    it[PeppolTransmissionsTable.updatedAt] = now
                }
            } catch (e: ExposedSQLException) {
                // Concurrent insert with same (tenant_id, idempotency_key): reload existing row.
                // SQLSTATE 23505 = unique_violation
                val isUniqueViolation = e.cause?.let { cause ->
                    (cause as? java.sql.SQLException)?.sqlState == "23505"
                } ?: false
                if (!isUniqueViolation) throw e
                logger.debug(
                    "Concurrent PEPPOL idempotency insert detected for tenant={} key={}",
                    tenantId,
                    idempotencyKey
                )
            }

            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.tenantId eq tenantUuid) and
                        (PeppolTransmissionsTable.idempotencyKey eq idempotencyKey)
                }
                .map { it.toInternal() }
                .single()
        }
    }

    /**
     * Claim due outbound transmissions with CAS semantics.
     * Only QUEUED and FAILED_RETRYABLE are claimable.
     */
    suspend fun claimDueOutbound(
        now: LocalDateTime,
        limit: Int
    ): Result<List<PeppolTransmissionInternal>> = runCatching {
        dbQuery {
            val candidates = PeppolTransmissionsTable.selectAll()
                .where {
                        (PeppolTransmissionsTable.direction eq PeppolTransmissionDirection.Outbound) and
                        (PeppolTransmissionsTable.status inList outboundClaimableStatuses) and
                        (
                            (PeppolTransmissionsTable.nextRetryAt eq null) or
                                (PeppolTransmissionsTable.nextRetryAt lessEq now)
                            )
                }
                .orderBy(PeppolTransmissionsTable.createdAt to SortOrder.ASC)
                .limit(limit)
                .map { it.toInternal() }

            val claimed = mutableListOf<PeppolTransmissionInternal>()
            for (candidate in candidates) {
                val updated = PeppolTransmissionsTable.update({
                    (PeppolTransmissionsTable.id eq UUID.fromString(candidate.id.toString())) and
                        (PeppolTransmissionsTable.direction eq PeppolTransmissionDirection.Outbound) and
                        (PeppolTransmissionsTable.status inList outboundClaimableStatuses) and
                        (
                            (PeppolTransmissionsTable.nextRetryAt eq null) or
                                (PeppolTransmissionsTable.nextRetryAt lessEq now)
                            )
                }) {
                    it[status] = PeppolStatus.Sending
                    it[lastAttemptAt] = now
                    it[attemptCount] = candidate.attemptCount + 1
                    it[nextRetryAt] = null
                    it[updatedAt] = now
                }

                if (updated > 0) {
                    claimed += candidate.copy(
                        status = PeppolStatus.Sending,
                        attemptCount = candidate.attemptCount + 1,
                        lastAttemptAt = now,
                        nextRetryAt = null,
                        updatedAt = now
                    )
                }
            }

            claimed
        }
    }

    /**
     * Recover rows that are stuck in SENDING due to worker crash or lease expiration.
     */
    suspend fun recoverStaleOutboundSending(
        staleBefore: LocalDateTime,
        retryAt: LocalDateTime
    ): Result<Int> = runCatching {
        dbQuery {
            PeppolTransmissionsTable.update({
                (PeppolTransmissionsTable.direction eq PeppolTransmissionDirection.Outbound) and
                    (PeppolTransmissionsTable.status eq PeppolStatus.Sending) and
                    (PeppolTransmissionsTable.updatedAt lessEq staleBefore)
            }) {
                it[status] = PeppolStatus.FailedRetryable
                it[nextRetryAt] = retryAt
                it[providerErrorCode] = "LEASE_RECOVERED"
                it[providerErrorMessage] = "Recovered stale outbound sending lease"
                it[errorMessage] = "Recovered stale outbound sending lease"
                it[updatedAt] = retryAt
            }
        }
    }

    suspend fun markOutboundSent(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId,
        externalDocumentId: String?,
        rawResponse: String?,
        transmittedAt: LocalDateTime
    ): Result<Boolean> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        dbQuery {
            val updated = PeppolTransmissionsTable.update({
                (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                    (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (PeppolTransmissionsTable.status eq PeppolStatus.Sending)
            }) {
                it[status] = PeppolStatus.Sent
                it[PeppolTransmissionsTable.externalDocumentId] = externalDocumentId
                it[PeppolTransmissionsTable.rawResponse] = rawResponse
                it[PeppolTransmissionsTable.transmittedAt] = transmittedAt
                it[PeppolTransmissionsTable.errorMessage] = null
                it[PeppolTransmissionsTable.providerErrorCode] = null
                it[PeppolTransmissionsTable.providerErrorMessage] = null
                it[PeppolTransmissionsTable.nextRetryAt] = null
                it[updatedAt] = now
            }
            updated > 0
        }
    }

    suspend fun markOutboundRetryable(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId,
        providerErrorCode: String,
        providerErrorMessage: String,
        retryAt: LocalDateTime,
        rawResponse: String? = null
    ): Result<Boolean> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        dbQuery {
            val updated = PeppolTransmissionsTable.update({
                (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                    (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (PeppolTransmissionsTable.status eq PeppolStatus.Sending)
            }) {
                it[status] = PeppolStatus.FailedRetryable
                it[PeppolTransmissionsTable.providerErrorCode] = providerErrorCode.take(100)
                it[PeppolTransmissionsTable.providerErrorMessage] = providerErrorMessage.take(4000)
                it[PeppolTransmissionsTable.errorMessage] = providerErrorMessage.take(4000)
                it[PeppolTransmissionsTable.nextRetryAt] = retryAt
                if (rawResponse != null) {
                    it[PeppolTransmissionsTable.rawResponse] = rawResponse
                }
                it[updatedAt] = now
            }
            updated > 0
        }
    }

    suspend fun markOutboundPermanentFailure(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId,
        providerErrorCode: String,
        providerErrorMessage: String,
        rawResponse: String? = null
    ): Result<Boolean> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        dbQuery {
            val updated = PeppolTransmissionsTable.update({
                (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                    (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (PeppolTransmissionsTable.status eq PeppolStatus.Sending)
            }) {
                it[status] = PeppolStatus.Failed
                it[PeppolTransmissionsTable.providerErrorCode] = providerErrorCode.take(100)
                it[PeppolTransmissionsTable.providerErrorMessage] = providerErrorMessage.take(4000)
                it[PeppolTransmissionsTable.errorMessage] = providerErrorMessage.take(4000)
                it[PeppolTransmissionsTable.nextRetryAt] = null
                if (rawResponse != null) {
                    it[PeppolTransmissionsTable.rawResponse] = rawResponse
                }
                it[updatedAt] = now
            }
            updated > 0
        }
    }

    suspend fun listOutboundForReconciliation(
        olderThan: LocalDateTime,
        limit: Int
    ): Result<List<PeppolTransmissionInternal>> = runCatching {
        val reconcilable = listOf(
            PeppolStatus.Pending,
            PeppolStatus.Queued,
            PeppolStatus.Sending,
            PeppolStatus.Sent,
            PeppolStatus.FailedRetryable
        )

        dbQuery {
            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.direction eq PeppolTransmissionDirection.Outbound) and
                        (PeppolTransmissionsTable.status inList reconcilable) and
                        (PeppolTransmissionsTable.externalDocumentId neq null) and
                        (PeppolTransmissionsTable.updatedAt lessEq olderThan)
                }
                .orderBy(PeppolTransmissionsTable.updatedAt to SortOrder.ASC)
                .limit(limit)
                .map { it.toInternal() }
        }
    }

    suspend fun getOutboundByExternalDocumentIdInternal(
        tenantId: TenantId,
        externalDocumentId: String
    ): Result<PeppolTransmissionInternal?> = runCatching {
        dbQuery {
            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                        (PeppolTransmissionsTable.direction eq PeppolTransmissionDirection.Outbound) and
                        (PeppolTransmissionsTable.externalDocumentId eq externalDocumentId)
                }
                .map { it.toInternal() }
                .singleOrNull()
        }
    }

    suspend fun applyProviderStatusMonotonic(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId,
        status: PeppolStatus,
        canTransition: (from: PeppolStatus, to: PeppolStatus) -> Boolean,
        externalDocumentId: String? = null,
        providerErrorCode: String? = null,
        providerErrorMessage: String? = null,
        transmittedAt: LocalDateTime? = null
    ): Result<Boolean> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        dbQuery {
            val current = PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                        (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid())
                }
                .map { it.toInternal() }
                .singleOrNull()
                ?: return@dbQuery false

            if (!canTransition(current.status, status)) {
                return@dbQuery false
            }

            val updated = PeppolTransmissionsTable.update({
                (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                    (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (PeppolTransmissionsTable.status eq current.status)
            }) {
                it[PeppolTransmissionsTable.status] = status
                it[PeppolTransmissionsTable.externalDocumentId] = externalDocumentId ?: current.externalDocumentId
                it[PeppolTransmissionsTable.providerErrorCode] = providerErrorCode ?: current.providerErrorCode
                it[PeppolTransmissionsTable.providerErrorMessage] =
                    providerErrorMessage?.take(4000) ?: current.providerErrorMessage
                it[PeppolTransmissionsTable.errorMessage] =
                    providerErrorMessage?.take(4000) ?: current.errorMessage
                it[PeppolTransmissionsTable.transmittedAt] = transmittedAt ?: current.transmittedAt
                if (status == PeppolStatus.Delivered || status == PeppolStatus.Rejected || status == PeppolStatus.Sent) {
                    it[PeppolTransmissionsTable.nextRetryAt] = null
                }
                it[updatedAt] = now
            }

            updated > 0
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
                    (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid()) and
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
                    (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid()) and
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
                    (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                        (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid())
                }
                .map { it.toDto() }
                .singleOrNull()
        }
    }

    suspend fun getTransmissionInternal(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId
    ): Result<PeppolTransmissionInternal?> = runCatching {
        dbQuery {
            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                        (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid())
                }
                .map { it.toInternal() }
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
                        (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid())
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
                .where { PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid() }

            direction?.let {
                query = query.andWhere { PeppolTransmissionsTable.direction eq it }
            }

            status?.let {
                query = query.andWhere { PeppolTransmissionsTable.status eq it }
            }

            query
                .orderBy(PeppolTransmissionsTable.createdAt to SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toDto() }
        }
    }

    /**
     * Update transmission status and details.
     */
    suspend fun updateTransmissionResult(
        transmissionId: PeppolTransmissionId,
        tenantId: TenantId,
        status: PeppolStatus,
        externalDocumentId: String? = null,
        errorMessage: String? = null,
        rawRequest: String? = null,
        rawResponse: String? = null,
        transmittedAt: LocalDateTime? = null,
        providerErrorCode: String? = null,
        providerErrorMessage: String? = null,
        attemptCount: Int? = null,
        nextRetryAt: LocalDateTime? = null,
        lastAttemptAt: LocalDateTime? = null,
        clearFailureDetails: Boolean = false
    ): Result<PeppolTransmissionDto> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        dbQuery {
            val current = PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                        (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid())
                }
                .singleOrNull()
                ?: throw IllegalStateException("Transmission not found: $transmissionId")

            PeppolTransmissionsTable.update({
                (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                    (PeppolTransmissionsTable.tenantId eq tenantId.value.toJavaUuid())
            }) {
                it[PeppolTransmissionsTable.status] = status
                it[PeppolTransmissionsTable.externalDocumentId] = externalDocumentId ?: current[PeppolTransmissionsTable.externalDocumentId]
                it[PeppolTransmissionsTable.errorMessage] = when {
                    clearFailureDetails -> errorMessage
                    errorMessage != null -> errorMessage
                    else -> current[PeppolTransmissionsTable.errorMessage]
                }
                it[PeppolTransmissionsTable.rawRequest] = rawRequest ?: current[PeppolTransmissionsTable.rawRequest]
                it[PeppolTransmissionsTable.rawResponse] = rawResponse ?: current[PeppolTransmissionsTable.rawResponse]
                it[PeppolTransmissionsTable.transmittedAt] = transmittedAt ?: current[PeppolTransmissionsTable.transmittedAt]
                it[PeppolTransmissionsTable.providerErrorCode] = when {
                    clearFailureDetails -> providerErrorCode
                    providerErrorCode != null -> providerErrorCode
                    else -> current[PeppolTransmissionsTable.providerErrorCode]
                }
                it[PeppolTransmissionsTable.providerErrorMessage] = when {
                    clearFailureDetails -> providerErrorMessage
                    providerErrorMessage != null -> providerErrorMessage
                    else -> current[PeppolTransmissionsTable.providerErrorMessage]
                }
                it[PeppolTransmissionsTable.attemptCount] = attemptCount ?: current[PeppolTransmissionsTable.attemptCount]
                it[PeppolTransmissionsTable.nextRetryAt] = when {
                    clearFailureDetails -> nextRetryAt
                    nextRetryAt != null -> nextRetryAt
                    else -> current[PeppolTransmissionsTable.nextRetryAt]
                }
                it[PeppolTransmissionsTable.lastAttemptAt] = lastAttemptAt ?: current[PeppolTransmissionsTable.lastAttemptAt]
                it[updatedAt] = now
            }

            PeppolTransmissionsTable.selectAll()
                .where {
                    (PeppolTransmissionsTable.id eq UUID.fromString(transmissionId.toString())) and
                        (PeppolTransmissionsTable.tenantId eq UUID.fromString(tenantId.toString()))
                }
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
        attemptCount = this[PeppolTransmissionsTable.attemptCount],
        nextRetryAt = this[PeppolTransmissionsTable.nextRetryAt],
        lastAttemptAt = this[PeppolTransmissionsTable.lastAttemptAt],
        providerErrorCode = this[PeppolTransmissionsTable.providerErrorCode],
        providerErrorMessage = this[PeppolTransmissionsTable.providerErrorMessage],
        transmittedAt = this[PeppolTransmissionsTable.transmittedAt],
        createdAt = this[PeppolTransmissionsTable.createdAt],
        updatedAt = this[PeppolTransmissionsTable.updatedAt]
    )

    private fun ResultRow.toInternal(): PeppolTransmissionInternal = PeppolTransmissionInternal(
        id = PeppolTransmissionId.parse(this[PeppolTransmissionsTable.id].value.toString()),
        tenantId = TenantId.parse(this[PeppolTransmissionsTable.tenantId].toString()),
        direction = this[PeppolTransmissionsTable.direction],
        documentType = this[PeppolTransmissionsTable.documentType],
        status = this[PeppolTransmissionsTable.status],
        invoiceId = this[PeppolTransmissionsTable.invoiceId]?.let { InvoiceId.parse(it.toString()) },
        externalDocumentId = this[PeppolTransmissionsTable.externalDocumentId],
        idempotencyKey = this[PeppolTransmissionsTable.idempotencyKey],
        recipientPeppolId = this[PeppolTransmissionsTable.recipientPeppolId]?.let { PeppolId(it) },
        senderPeppolId = this[PeppolTransmissionsTable.senderPeppolId]?.let { PeppolId(it) },
        errorMessage = this[PeppolTransmissionsTable.errorMessage],
        providerErrorCode = this[PeppolTransmissionsTable.providerErrorCode],
        providerErrorMessage = this[PeppolTransmissionsTable.providerErrorMessage],
        attemptCount = this[PeppolTransmissionsTable.attemptCount],
        nextRetryAt = this[PeppolTransmissionsTable.nextRetryAt],
        lastAttemptAt = this[PeppolTransmissionsTable.lastAttemptAt],
        rawRequest = this[PeppolTransmissionsTable.rawRequest],
        rawResponse = this[PeppolTransmissionsTable.rawResponse],
        rawUblXmlKey = this[PeppolTransmissionsTable.rawUblXmlKey],
        transmittedAt = this[PeppolTransmissionsTable.transmittedAt],
        createdAt = this[PeppolTransmissionsTable.createdAt],
        updatedAt = this[PeppolTransmissionsTable.updatedAt]
    )
}
