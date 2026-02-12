package tech.dokus.database.repository.cashflow

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.plus
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.CashflowViewMode
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
 * Cashflow entries are projections of financial facts (Invoice, Expense).
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
        contactId: ContactId?
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
                it[CashflowEntriesTable.counterpartyId] = contactId?.let { id -> UUID.fromString(id.toString()) }
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
     * Get entry by source (Invoice/Expense ID).
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
     * Update an entry projection by source (Invoice/Expense).
     * This is used for safe "re-confirm" flows to update the projection from the latest draft.
     *
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun updateProjectionBySource(
        tenantId: TenantId,
        sourceType: CashflowSourceType,
        sourceId: UUID,
        documentId: DocumentId?,
        direction: CashflowDirection,
        eventDate: LocalDate,
        amountGross: Money,
        amountVat: Money,
        contactId: ContactId?
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = CashflowEntriesTable.update({
                (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (CashflowEntriesTable.sourceType eq sourceType) and
                    (CashflowEntriesTable.sourceId eq sourceId)
            }) {
                it[CashflowEntriesTable.direction] = direction
                it[CashflowEntriesTable.eventDate] = eventDate
                it[CashflowEntriesTable.amountGross] = amountGross.toDbDecimal()
                it[CashflowEntriesTable.amountVat] = amountVat.toDbDecimal()
                it[CashflowEntriesTable.remainingAmount] = amountGross.toDbDecimal()
                if (documentId != null) {
                    it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
                }
                it[CashflowEntriesTable.counterpartyId] = contactId?.let { id -> UUID.fromString(id.toString()) }
            }
            updated > 0
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
     * Bulk lookup: map document IDs to cashflow entry IDs.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun getIdsByDocumentIds(
        tenantId: TenantId,
        documentIds: List<DocumentId>
    ): Result<Map<DocumentId, CashflowEntryId>> = runCatching {
        if (documentIds.isEmpty()) return@runCatching emptyMap()

        val tenantUuid = UUID.fromString(tenantId.toString())
        val documentUuids = documentIds.map { id -> UUID.fromString(id.toString()) }

        dbQuery {
            CashflowEntriesTable
                .select(CashflowEntriesTable.documentId, CashflowEntriesTable.id)
                .where {
                    (CashflowEntriesTable.tenantId eq tenantUuid) and
                        (CashflowEntriesTable.documentId inList documentUuids)
                }
                .associate { row ->
                    val documentIdUuid = requireNotNull(row[CashflowEntriesTable.documentId])
                    val entryIdUuid = row[CashflowEntriesTable.id].value
                    DocumentId.parse(documentIdUuid.toString()) to CashflowEntryId.parse(entryIdUuid.toString())
                }
        }
    }

    /**
     * List entries for a tenant with optional filters.
     * Includes LEFT JOIN to contacts to fetch counterparty name.
     *
     * @param viewMode Determines date field filtering and sorting:
     *                 - Upcoming: filter by eventDate, sort ASC
     *                 - History: filter by paidAt, sort DESC
     * @param statuses Multi-status filter (e.g., [Open, Overdue])
     *
     * Cancelled entries are excluded by default.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun listEntries(
        tenantId: TenantId,
        viewMode: CashflowViewMode? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        direction: CashflowDirection? = null,
        statuses: List<CashflowEntryStatus>? = null
    ): Result<List<CashflowEntry>> = runCatching {
        dbQuery {
            var query = CashflowEntriesTable
                .join(
                    ContactsTable,
                    JoinType.LEFT,
                    onColumn = CashflowEntriesTable.counterpartyId,
                    otherColumn = ContactsTable.id
                )
                .selectAll()
                .where {
                    CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString())
                }

            // Exclude Cancelled by default (unless explicitly included in statuses)
            if (statuses == null || CashflowEntryStatus.Cancelled !in statuses) {
                query = query.andWhere { CashflowEntriesTable.status neq CashflowEntryStatus.Cancelled }
            }

            // Date filtering based on viewMode
            when (viewMode) {
                CashflowViewMode.Upcoming -> {
                    // Filter by eventDate
                    if (fromDate != null) {
                        query = query.andWhere { CashflowEntriesTable.eventDate greaterEq fromDate }
                    }
                    if (toDate != null) {
                        query = query.andWhere { CashflowEntriesTable.eventDate lessEq toDate }
                    }
                }
                CashflowViewMode.History -> {
                    // Filter by paidAt (using LocalDate as start/end of day in UTC)
                    if (fromDate != null) {
                        val startOfDay = LocalDateTime(fromDate.year, fromDate.monthNumber, fromDate.dayOfMonth, 0, 0, 0)
                        query = query.andWhere { CashflowEntriesTable.paidAt greaterEq startOfDay }
                    }
                    if (toDate != null) {
                        // End of day: next day at 00:00:00
                        val nextDay = toDate.plus(kotlinx.datetime.DatePeriod(days = 1))
                        val endOfDay = LocalDateTime(nextDay.year, nextDay.monthNumber, nextDay.dayOfMonth, 0, 0, 0)
                        query = query.andWhere { CashflowEntriesTable.paidAt less endOfDay }
                    }
                }
                null -> {
                    // No viewMode: use eventDate range (backward compatibility)
                    if (fromDate != null) {
                        query = query.andWhere { CashflowEntriesTable.eventDate greaterEq fromDate }
                    }
                    if (toDate != null) {
                        query = query.andWhere { CashflowEntriesTable.eventDate lessEq toDate }
                    }
                }
            }

            // Multi-status filtering
            if (!statuses.isNullOrEmpty()) {
                query = query.andWhere { CashflowEntriesTable.status inList statuses }
            }

            if (direction != null) {
                query = query.andWhere { CashflowEntriesTable.direction eq direction }
            }

            // Server-side sorting based on viewMode
            val sortOrder = when (viewMode) {
                CashflowViewMode.History -> CashflowEntriesTable.paidAt to SortOrder.DESC
                else -> CashflowEntriesTable.eventDate to SortOrder.ASC
            }

            query.orderBy(sortOrder)
                .map { row ->
                    mapRowToEntry(
                        row = row,
                        contactName = row.getOrNull(ContactsTable.name)
                    )
                }
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
     * When setting to PAID, paidAt MUST be provided.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun updateStatus(
        entryId: CashflowEntryId,
        tenantId: TenantId,
        newStatus: CashflowEntryStatus,
        paidAt: LocalDateTime? = null
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = CashflowEntriesTable.update({
                (CashflowEntriesTable.id eq UUID.fromString(entryId.toString())) and
                    (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[status] = newStatus
                if (paidAt != null) {
                    it[CashflowEntriesTable.paidAt] = paidAt
                }
            }
            updated > 0
        }
    }

    /**
     * Update both remaining amount and status atomically.
     * When transitioning to PAID, paidAt MUST be set.
     * CRITICAL: MUST filter by tenant_id.
     */
    suspend fun updateRemainingAmountAndStatus(
        entryId: CashflowEntryId,
        tenantId: TenantId,
        newRemainingAmount: Money,
        newStatus: CashflowEntryStatus,
        paidAt: LocalDateTime? = null
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = CashflowEntriesTable.update({
                (CashflowEntriesTable.id eq UUID.fromString(entryId.toString())) and
                    (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[remainingAmount] = newRemainingAmount.toDbDecimal()
                it[status] = newStatus
                if (paidAt != null) {
                    it[CashflowEntriesTable.paidAt] = paidAt
                }
            }
            updated > 0
        }
    }

    private fun mapRowToEntry(
        row: org.jetbrains.exposed.v1.core.ResultRow,
        contactName: String? = null
    ): CashflowEntry {
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
            paidAt = row[CashflowEntriesTable.paidAt],
            contactId = row[CashflowEntriesTable.counterpartyId]?.let { ContactId.parse(it.toString()) },
            contactName = contactName,
            description = null, // Will be AI-generated in future
            createdAt = row[CashflowEntriesTable.createdAt],
            updatedAt = row[CashflowEntriesTable.updatedAt]
        )
    }
}
