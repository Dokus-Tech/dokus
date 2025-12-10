package ai.dokus.foundation.database.repository.reporting

import ai.dokus.foundation.database.tables.reporting.VatReturnsTable
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.enums.VatReturnStatus
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatReturnId
import ai.dokus.foundation.domain.model.VatReturnDto
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * Repository for managing VAT returns and reporting.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. Use NUMERIC for money to avoid rounding errors
 */
@OptIn(ExperimentalUuidApi::class)
class ReportingRepository {

    /**
     * Create a new VAT return.
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createVatReturn(
        tenantId: TenantId,
        periodStart: LocalDate,
        periodEnd: LocalDate,
        year: Int,
        quarter: Int,
        totalSales: Money,
        vatCollected: Money,
        totalPurchases: Money,
        vatPaid: Money,
        vatDue: Money,
        status: VatReturnStatus
    ): Result<VatReturnDto> = runCatching {
        dbQuery {
            val id = VatReturnsTable.insert {
                it[VatReturnsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[VatReturnsTable.periodStart] = periodStart
                it[VatReturnsTable.periodEnd] = periodEnd
                it[VatReturnsTable.quarterYear] = year
                it[VatReturnsTable.quarter] = quarter
                it[VatReturnsTable.totalSales] = java.math.BigDecimal(totalSales.value)
                it[VatReturnsTable.vatCollected] = java.math.BigDecimal(vatCollected.value)
                it[VatReturnsTable.totalPurchases] = java.math.BigDecimal(totalPurchases.value)
                it[VatReturnsTable.vatPaid] = java.math.BigDecimal(vatPaid.value)
                it[VatReturnsTable.vatDue] = java.math.BigDecimal(vatDue.value)
                it[VatReturnsTable.status] = status
            } get VatReturnsTable.id

            VatReturnsTable.selectAll().where {
                VatReturnsTable.id eq id.value
            }.single().toVatReturnDto()
        }
    }

    /**
     * Get a VAT return by ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getVatReturn(
        vatReturnId: VatReturnId,
        tenantId: TenantId
    ): Result<VatReturnDto?> = runCatching {
        dbQuery {
            VatReturnsTable.selectAll().where {
                (VatReturnsTable.id eq UUID.fromString(vatReturnId.toString())) and
                (VatReturnsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.toVatReturnDto()
        }
    }

    /**
     * Get VAT return for a specific quarter.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getVatReturnForQuarter(
        tenantId: TenantId,
        year: Int,
        quarter: Int
    ): Result<VatReturnDto?> = runCatching {
        dbQuery {
            VatReturnsTable.selectAll().where {
                (VatReturnsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (VatReturnsTable.quarterYear eq year) and
                (VatReturnsTable.quarter eq quarter)
            }.singleOrNull()?.toVatReturnDto()
        }
    }

    /**
     * List VAT returns for a tenant.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listVatReturns(
        tenantId: TenantId,
        status: VatReturnStatus? = null,
        year: Int? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<VatReturnDto>> = runCatching {
        dbQuery {
            var query = VatReturnsTable.selectAll().where {
                VatReturnsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            status?.let {
                query = query.andWhere { VatReturnsTable.status eq it }
            }
            year?.let {
                query = query.andWhere { VatReturnsTable.quarterYear eq it }
            }

            query.orderBy(VatReturnsTable.quarterYear to SortOrder.DESC, VatReturnsTable.quarter to SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toVatReturnDto() }
        }
    }

    /**
     * Update VAT return status to submitted.
     */
    suspend fun markAsSubmitted(
        vatReturnId: VatReturnId,
        tenantId: TenantId,
        submittedAt: LocalDateTime,
        referenceNumber: String?
    ): Result<Boolean> = runCatching {
        dbQuery {
            VatReturnsTable.update({
                (VatReturnsTable.id eq UUID.fromString(vatReturnId.toString())) and
                (VatReturnsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[status] = VatReturnStatus.Submitted
                it[VatReturnsTable.submittedAt] = submittedAt
                it[VatReturnsTable.referenceNumber] = referenceNumber
                it[updatedAt] = submittedAt
            } > 0
        }
    }

    /**
     * Update VAT return status.
     */
    suspend fun updateStatus(
        vatReturnId: VatReturnId,
        tenantId: TenantId,
        newStatus: VatReturnStatus
    ): Result<Boolean> = runCatching {
        dbQuery {
            VatReturnsTable.update({
                (VatReturnsTable.id eq UUID.fromString(vatReturnId.toString())) and
                (VatReturnsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[status] = newStatus
            } > 0
        }
    }

    private fun ResultRow.toVatReturnDto(): VatReturnDto {
        // Note: Mapping table columns to DTO fields
        // Table has more detailed columns than DTO expects
        return VatReturnDto(
            id = VatReturnId.parse(this[VatReturnsTable.id].value.toString()),
            tenantId = TenantId.parse(this[VatReturnsTable.tenantId].toString()),
            quarter = this[VatReturnsTable.quarter],
            year = this[VatReturnsTable.quarterYear],
            salesVat = Money(this[VatReturnsTable.vatCollected].toString()),
            purchaseVat = Money(this[VatReturnsTable.vatPaid].toString()),
            netVat = Money(this[VatReturnsTable.vatDue].toString()),
            status = this[VatReturnsTable.status],
            filedAt = this[VatReturnsTable.submittedAt],
            paidAt = null, // Table doesn't have paidAt column
            createdAt = this[VatReturnsTable.createdAt],
            updatedAt = this[VatReturnsTable.updatedAt]
        )
    }
}
