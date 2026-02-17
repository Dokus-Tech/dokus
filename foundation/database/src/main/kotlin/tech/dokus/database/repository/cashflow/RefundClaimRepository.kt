package tech.dokus.database.repository.cashflow
import kotlin.uuid.Uuid

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.cashflow.RefundClaimsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.RefundClaimStatus
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.RefundClaimId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.RefundClaimDto
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery

/**
 * Repository for managing refund claims.
 *
 * RefundClaims track expected refunds from credit notes.
 * They do NOT affect cashflow totals until settled.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return refund claims from different tenants
 * 3. All operations must be tenant-isolated
 */
class RefundClaimRepository {

    /**
     * Create a new refund claim for a credit note.
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createRefundClaim(
        tenantId: TenantId,
        creditNoteId: CreditNoteId,
        counterpartyId: ContactId,
        amount: Money,
        currency: Currency = Currency.Eur,
        expectedDate: LocalDate? = null
    ): Result<RefundClaimDto> = runCatching {
        dbQuery {
            val claimId = RefundClaimsTable.insertAndGetId {
                it[RefundClaimsTable.tenantId] = Uuid.parse(tenantId.toString())
                it[RefundClaimsTable.creditNoteId] = Uuid.parse(creditNoteId.toString())
                it[RefundClaimsTable.counterpartyId] = Uuid.parse(counterpartyId.toString())
                it[RefundClaimsTable.amount] = amount.toDbDecimal()
                it[RefundClaimsTable.currency] = currency
                it[RefundClaimsTable.expectedDate] = expectedDate
                it[RefundClaimsTable.status] = RefundClaimStatus.Open
            }

            // Fetch and return the created claim
            RefundClaimsTable.selectAll().where {
                (RefundClaimsTable.id eq claimId.value) and
                    (RefundClaimsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }.single().let { row ->
                mapRowToDto(row)
            }
        }
    }

    /**
     * Get a single refund claim by ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getRefundClaim(
        claimId: RefundClaimId,
        tenantId: TenantId
    ): Result<RefundClaimDto?> = runCatching {
        dbQuery {
            RefundClaimsTable.selectAll().where {
                (RefundClaimsTable.id eq Uuid.parse(claimId.toString())) and
                    (RefundClaimsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }.singleOrNull()?.let { row ->
                mapRowToDto(row)
            }
        }
    }

    /**
     * Get the open refund claim for a credit note (if any).
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getOpenClaimForCreditNote(
        tenantId: TenantId,
        creditNoteId: CreditNoteId
    ): RefundClaimDto? = dbQuery {
        RefundClaimsTable.selectAll().where {
            (RefundClaimsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                (RefundClaimsTable.creditNoteId eq Uuid.parse(creditNoteId.toString())) and
                (RefundClaimsTable.status eq RefundClaimStatus.Open)
        }.singleOrNull()?.let { row ->
            mapRowToDto(row)
        }
    }

    /**
     * List all refund claims for a tenant.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listRefundClaims(
        tenantId: TenantId,
        status: RefundClaimStatus? = null,
        counterpartyId: ContactId? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<RefundClaimDto>> = runCatching {
        dbQuery {
            var query = RefundClaimsTable.selectAll().where {
                RefundClaimsTable.tenantId eq Uuid.parse(tenantId.toString())
            }

            if (status != null) {
                query = query.andWhere { RefundClaimsTable.status eq status }
            }
            if (counterpartyId != null) {
                query = query.andWhere {
                    RefundClaimsTable.counterpartyId eq Uuid.parse(counterpartyId.toString())
                }
            }

            query.orderBy(RefundClaimsTable.createdAt to SortOrder.DESC)
                .limit(limit + offset)
                .map { row -> mapRowToDto(row) }
                .drop(offset)
        }
    }

    /**
     * List all open refund claims for a tenant (for dashboard).
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listOpenClaims(tenantId: TenantId): List<RefundClaimDto> = dbQuery {
        RefundClaimsTable.selectAll().where {
            (RefundClaimsTable.tenantId eq Uuid.parse(tenantId.toString())) and
                (RefundClaimsTable.status eq RefundClaimStatus.Open)
        }.orderBy(RefundClaimsTable.expectedDate to SortOrder.ASC)
            .map { row -> mapRowToDto(row) }
    }

    /**
     * Settle a refund claim.
     * Called when a refund payment is recorded.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun settleRefundClaim(
        claimId: RefundClaimId,
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val updatedRows = RefundClaimsTable.update({
                (RefundClaimsTable.id eq Uuid.parse(claimId.toString())) and
                    (RefundClaimsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }) {
                it[status] = RefundClaimStatus.Settled
                it[settledAt] = now
                it[RefundClaimsTable.cashflowEntryId] = Uuid.parse(cashflowEntryId.toString())
            }
            updatedRows > 0
        }
    }

    /**
     * Cancel a refund claim.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun cancelRefundClaim(
        claimId: RefundClaimId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = RefundClaimsTable.update({
                (RefundClaimsTable.id eq Uuid.parse(claimId.toString())) and
                    (RefundClaimsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }) {
                it[status] = RefundClaimStatus.Cancelled
            }
            updatedRows > 0
        }
    }

    /**
     * Delete a refund claim.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteRefundClaim(
        claimId: RefundClaimId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deletedRows = RefundClaimsTable.deleteWhere {
                (RefundClaimsTable.id eq Uuid.parse(claimId.toString())) and
                    (RefundClaimsTable.tenantId eq Uuid.parse(tenantId.toString()))
            }
            deletedRows > 0
        }
    }

    private fun mapRowToDto(row: org.jetbrains.exposed.v1.core.ResultRow): RefundClaimDto {
        return RefundClaimDto(
            id = RefundClaimId.parse(row[RefundClaimsTable.id].value.toString()),
            tenantId = TenantId.parse(row[RefundClaimsTable.tenantId].toString()),
            creditNoteId = CreditNoteId.parse(row[RefundClaimsTable.creditNoteId].toString()),
            counterpartyId = ContactId.parse(row[RefundClaimsTable.counterpartyId].toString()),
            amount = Money.fromDbDecimal(row[RefundClaimsTable.amount]),
            currency = row[RefundClaimsTable.currency],
            expectedDate = row[RefundClaimsTable.expectedDate],
            status = row[RefundClaimsTable.status],
            settledAt = row[RefundClaimsTable.settledAt],
            cashflowEntryId = row[RefundClaimsTable.cashflowEntryId]?.let {
                CashflowEntryId.parse(it.toString())
            },
            createdAt = row[RefundClaimsTable.createdAt],
            updatedAt = row[RefundClaimsTable.updatedAt]
        )
    }
}
