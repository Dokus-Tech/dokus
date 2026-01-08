package tech.dokus.database.repository.cashflow

import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID

/**
 * Repository for managing cashflow entries.
 *
 * Cashflow entries are projections of financial facts (Invoice, Bill, Expense).
 * They are created during document confirmation and updated when payments are recorded.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return entries from different tenants
 * 3. All operations must be tenant-isolated
 */
class CashflowEntriesRepository {

    /**
     * Create a new cashflow entry.
     * CRITICAL: MUST include tenant_id for multi-tenancy security.
     */
    suspend fun createEntry(
        tenantId: TenantId,
        sourceType: CashflowSourceType,
        sourceId: UUID,
        documentId: DocumentId?,
        direction: CashflowDirection,
        eventDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        counterpartyId: ContactId?
    ): Result<CashflowEntry> = runCatching {
        dbQuery {
            val entryId = CashflowEntriesTable.insertAndGetId {
                it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[CashflowEntriesTable.sourceType] = sourceType
                it[CashflowEntriesTable.sourceId] = sourceId
                it[CashflowEntriesTable.documentId] = documentId?.let { id -> UUID.fromString(id.toString()) }
                it[CashflowEntriesTable.direction] = direction
                it[CashflowEntriesTable.eventDate] = eventDate
                it[CashflowEntriesTable.amountGross] = amountGross.toDbDecimal()
                it[CashflowEntriesTable.amountVat] = amountVat.toDbDecimal()
                it[CashflowEntriesTable.remainingAmount] = amountGross.toDbDecimal()
                it[CashflowEntriesTable.status] = CashflowEntryStatus.Open
                it[CashflowEntriesTable.counterpartyId] = counterpartyId?.let { id -> UUID.fromString(id.toString()) }
            }

            mapRowToEntry(
                CashflowEntriesTable.selectAll().where {
                    (CashflowEntriesTable.id eq entryId.value) and
                        (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString()))
                }.single()
            )
        }
    }

    /**
     * Get entry by ID.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun getEntry(
        entryId: CashflowEntryId,
        tenantId: TenantId
    ): Result<CashflowEntry?> = runCatching {
        dbQuery {
            CashflowEntriesTable.selectAll().where {
                (CashflowEntriesTable.id eq UUID.fromString(entryId.toString())) and
                    (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.let { mapRowToEntry(it) }
        }
    }

    /**
     * Get entry by source (Invoice/Bill/Expense ID).
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun getBySource(
        tenantId: TenantId,
        sourceType: CashflowSourceType,
        sourceId: UUID
    ): Result<CashflowEntry?> = runCatching {
        dbQuery {
            CashflowEntriesTable.selectAll().where {
                (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (CashflowEntriesTable.sourceType eq sourceType) and
                    (CashflowEntriesTable.sourceId eq sourceId)
            }.singleOrNull()?.let { mapRowToEntry(it) }
        }
    }

    /**
     * Get entry by document ID.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun getByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): Result<CashflowEntry?> = runCatching {
        dbQuery {
            CashflowEntriesTable.selectAll().where {
                (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (CashflowEntriesTable.documentId eq UUID.fromString(documentId.toString()))
            }.singleOrNull()?.let { mapRowToEntry(it) }
        }
    }

    /**
     * List entries for a tenant with optional filters.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun listEntries(
        tenantId: TenantId,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        direction: CashflowDirection? = null,
        status: CashflowEntryStatus? = null
    ): Result<List<CashflowEntry>> = runCatching {
        dbQuery {
            var query = CashflowEntriesTable.selectAll().where {
                CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            if (fromDate != null) {
                query = query.andWhere { CashflowEntriesTable.eventDate greaterEq fromDate }
            }
            if (toDate != null) {
                query = query.andWhere { CashflowEntriesTable.eventDate lessEq toDate }
            }
            if (direction != null) {
                query = query.andWhere { CashflowEntriesTable.direction eq direction }
            }
            if (status != null) {
                query = query.andWhere { CashflowEntriesTable.status eq status }
            }

            query.orderBy(CashflowEntriesTable.eventDate to SortOrder.ASC)
                .map { mapRowToEntry(it) }
        }
    }

    /**
     * Update remaining amount after payment.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun updateRemainingAmount(
        entryId: CashflowEntryId,
        tenantId: TenantId,
        newRemainingAmount: Money
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = CashflowEntriesTable.update({
                (CashflowEntriesTable.id eq UUID.fromString(entryId.toString())) and
                    (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[remainingAmount] = newRemainingAmount.toDbDecimal()
            }
            updated > 0
        }
    }

    /**
     * Update entry status.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun updateStatus(
        entryId: CashflowEntryId,
        tenantId: TenantId,
        newStatus: CashflowEntryStatus
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = CashflowEntriesTable.update({
                (CashflowEntriesTable.id eq UUID.fromString(entryId.toString())) and
                    (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[status] = newStatus
            }
            updated > 0
        }
    }

    /**
     * Update both remaining amount and status atomically.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun updateRemainingAmountAndStatus(
        entryId: CashflowEntryId,
        tenantId: TenantId,
        newRemainingAmount: Money,
        newStatus: CashflowEntryStatus
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = CashflowEntriesTable.update({
                (CashflowEntriesTable.id eq UUID.fromString(entryId.toString())) and
                    (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[remainingAmount] = newRemainingAmount.toDbDecimal()
                it[status] = newStatus
            }
            updated > 0
        }
    }

    private fun mapRowToEntry(row: org.jetbrains.exposed.v1.core.ResultRow): CashflowEntry {
        return CashflowEntry(
            id = CashflowEntryId.parse(row[CashflowEntriesTable.id].value.toString()),
            tenantId = TenantId.parse(row[CashflowEntriesTable.tenantId].toString()),
            sourceType = row[CashflowEntriesTable.sourceType],
            sourceId = row[CashflowEntriesTable.sourceId].toString(),
            documentId = row[CashflowEntriesTable.documentId]?.let { DocumentId.parse(it.toString()) },
            direction = row[CashflowEntriesTable.direction],
            eventDate = row[CashflowEntriesTable.eventDate],
            amountGross = Money.fromDbDecimal(row[CashflowEntriesTable.amountGross]),
            amountVat = Money.fromDbDecimal(row[CashflowEntriesTable.amountVat]),
            remainingAmount = Money.fromDbDecimal(row[CashflowEntriesTable.remainingAmount]),
            currency = row[CashflowEntriesTable.currency],
            status = row[CashflowEntriesTable.status],
            counterpartyId = row[CashflowEntriesTable.counterpartyId]?.let { ContactId.parse(it.toString()) },
            createdAt = row[CashflowEntriesTable.createdAt],
            updatedAt = row[CashflowEntriesTable.updatedAt]
        )
    }
}
