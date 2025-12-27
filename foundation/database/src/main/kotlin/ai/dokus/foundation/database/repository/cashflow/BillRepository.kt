package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.tables.cashflow.BillsTable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import ai.dokus.foundation.domain.enums.BillStatus
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.ids.BillId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.MarkBillPaidRequest
import ai.dokus.foundation.domain.model.common.PaginatedResponse
import tech.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.util.UUID

/**
 * Repository for managing bills (incoming supplier invoices)
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return bills from different tenants
 * 3. All operations must be tenant-isolated
 */
class BillRepository {

    /**
     * Create a new bill
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createBill(
        tenantId: TenantId,
        request: CreateBillRequest
    ): Result<FinancialDocumentDto.BillDto> = runCatching {
        dbQuery {
            val billId = BillsTable.insertAndGetId {
                it[BillsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[supplierName] = request.supplierName
                it[supplierVatNumber] = request.supplierVatNumber
                it[invoiceNumber] = request.invoiceNumber
                it[issueDate] = request.issueDate
                it[dueDate] = request.dueDate
                it[amount] = java.math.BigDecimal(request.amount.value)
                it[vatAmount] = request.vatAmount?.let { amount -> java.math.BigDecimal(amount.value) }
                it[vatRate] = request.vatRate?.let { rate -> java.math.BigDecimal(rate.value) }
                it[status] = BillStatus.Pending
                it[category] = request.category
                it[description] = request.description
                it[notes] = request.notes
                it[documentId] = request.documentId?.let { id -> UUID.fromString(id.toString()) }
            }

            // Fetch and return the created bill
            BillsTable.selectAll().where {
                (BillsTable.id eq billId.value) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                FinancialDocumentDto.BillDto(
                    id = BillId.parse(row[BillsTable.id].value.toString()),
                    tenantId = TenantId.parse(row[BillsTable.tenantId].toString()),
                    supplierName = row[BillsTable.supplierName],
                    supplierVatNumber = row[BillsTable.supplierVatNumber],
                    invoiceNumber = row[BillsTable.invoiceNumber],
                    issueDate = row[BillsTable.issueDate],
                    dueDate = row[BillsTable.dueDate],
                    amount = Money(row[BillsTable.amount].toString()),
                    vatAmount = row[BillsTable.vatAmount]?.let { Money(it.toString()) },
                    vatRate = row[BillsTable.vatRate]?.let { VatRate(it.toString()) },
                    status = row[BillsTable.status],
                    category = row[BillsTable.category],
                    currency = row[BillsTable.currency],
                    description = row[BillsTable.description],
                    documentId = row[BillsTable.documentId]?.let { ai.dokus.foundation.domain.ids.DocumentId.parse(it.toString()) },
                    paidAt = row[BillsTable.paidAt],
                    paidAmount = row[BillsTable.paidAmount]?.let { Money(it.toString()) },
                    paymentMethod = row[BillsTable.paymentMethod],
                    paymentReference = row[BillsTable.paymentReference],
                    notes = row[BillsTable.notes],
                    createdAt = row[BillsTable.createdAt],
                    updatedAt = row[BillsTable.updatedAt]
                )
            }
        }
    }

    /**
     * Get a single bill by ID
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getBill(
        billId: BillId,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.BillDto?> = runCatching {
        dbQuery {
            BillsTable.selectAll().where {
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.let { row ->
                FinancialDocumentDto.BillDto(
                    id = BillId.parse(row[BillsTable.id].value.toString()),
                    tenantId = TenantId.parse(row[BillsTable.tenantId].toString()),
                    supplierName = row[BillsTable.supplierName],
                    supplierVatNumber = row[BillsTable.supplierVatNumber],
                    invoiceNumber = row[BillsTable.invoiceNumber],
                    issueDate = row[BillsTable.issueDate],
                    dueDate = row[BillsTable.dueDate],
                    amount = Money(row[BillsTable.amount].toString()),
                    vatAmount = row[BillsTable.vatAmount]?.let { Money(it.toString()) },
                    vatRate = row[BillsTable.vatRate]?.let { VatRate(it.toString()) },
                    status = row[BillsTable.status],
                    category = row[BillsTable.category],
                    currency = row[BillsTable.currency],
                    description = row[BillsTable.description],
                    documentId = row[BillsTable.documentId]?.let { ai.dokus.foundation.domain.ids.DocumentId.parse(it.toString()) },
                    paidAt = row[BillsTable.paidAt],
                    paidAmount = row[BillsTable.paidAmount]?.let { Money(it.toString()) },
                    paymentMethod = row[BillsTable.paymentMethod],
                    paymentReference = row[BillsTable.paymentReference],
                    notes = row[BillsTable.notes],
                    createdAt = row[BillsTable.createdAt],
                    updatedAt = row[BillsTable.updatedAt]
                )
            }
        }
    }

    /**
     * List bills for a tenant with optional filters
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listBills(
        tenantId: TenantId,
        status: BillStatus? = null,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto.BillDto>> = runCatching {
        dbQuery {
            var query = BillsTable.selectAll().where {
                BillsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            // Apply filters
            if (status != null) {
                query = query.andWhere { BillsTable.status eq status }
            }
            if (category != null) {
                query = query.andWhere { BillsTable.category eq category }
            }
            if (fromDate != null) {
                query = query.andWhere { BillsTable.issueDate greaterEq fromDate }
            }
            if (toDate != null) {
                query = query.andWhere { BillsTable.issueDate lessEq toDate }
            }

            val total = query.count()

            // Apply pagination and ordering (using same pattern as ExpenseRepository)
            val items = query.orderBy(BillsTable.dueDate to SortOrder.ASC)
                .limit(limit + offset)
                .map { row ->
                    FinancialDocumentDto.BillDto(
                        id = BillId.parse(row[BillsTable.id].value.toString()),
                        tenantId = TenantId.parse(row[BillsTable.tenantId].toString()),
                        supplierName = row[BillsTable.supplierName],
                        supplierVatNumber = row[BillsTable.supplierVatNumber],
                        invoiceNumber = row[BillsTable.invoiceNumber],
                        issueDate = row[BillsTable.issueDate],
                        dueDate = row[BillsTable.dueDate],
                        amount = Money(row[BillsTable.amount].toString()),
                        vatAmount = row[BillsTable.vatAmount]?.let { Money(it.toString()) },
                        vatRate = row[BillsTable.vatRate]?.let { VatRate(it.toString()) },
                        status = row[BillsTable.status],
                        category = row[BillsTable.category],
                        currency = row[BillsTable.currency],
                        description = row[BillsTable.description],
                        documentId = row[BillsTable.documentId]?.let { ai.dokus.foundation.domain.ids.DocumentId.parse(it.toString()) },
                        paidAt = row[BillsTable.paidAt],
                        paidAmount = row[BillsTable.paidAmount]?.let { Money(it.toString()) },
                        paymentMethod = row[BillsTable.paymentMethod],
                        paymentReference = row[BillsTable.paymentReference],
                            notes = row[BillsTable.notes],
                        createdAt = row[BillsTable.createdAt],
                        updatedAt = row[BillsTable.updatedAt]
                    )
                }
                .drop(offset)

            PaginatedResponse(
                items = items,
                total = total,
                limit = limit,
                offset = offset
            )
        }
    }

    /**
     * List overdue bills for a tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listOverdueBills(tenantId: TenantId): Result<List<FinancialDocumentDto.BillDto>> = runCatching {
        dbQuery {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

            BillsTable.selectAll().where {
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (BillsTable.dueDate less today) and
                (BillsTable.status inList listOf(BillStatus.Pending, BillStatus.Draft))
            }.orderBy(BillsTable.dueDate to SortOrder.ASC)
                .map { row ->
                    FinancialDocumentDto.BillDto(
                        id = BillId.parse(row[BillsTable.id].value.toString()),
                        tenantId = TenantId.parse(row[BillsTable.tenantId].toString()),
                        supplierName = row[BillsTable.supplierName],
                        supplierVatNumber = row[BillsTable.supplierVatNumber],
                        invoiceNumber = row[BillsTable.invoiceNumber],
                        issueDate = row[BillsTable.issueDate],
                        dueDate = row[BillsTable.dueDate],
                        amount = Money(row[BillsTable.amount].toString()),
                        vatAmount = row[BillsTable.vatAmount]?.let { Money(it.toString()) },
                        vatRate = row[BillsTable.vatRate]?.let { VatRate(it.toString()) },
                        status = row[BillsTable.status],
                        category = row[BillsTable.category],
                        currency = row[BillsTable.currency],
                        description = row[BillsTable.description],
                        documentId = row[BillsTable.documentId]?.let { ai.dokus.foundation.domain.ids.DocumentId.parse(it.toString()) },
                        paidAt = row[BillsTable.paidAt],
                        paidAmount = row[BillsTable.paidAmount]?.let { Money(it.toString()) },
                        paymentMethod = row[BillsTable.paymentMethod],
                        paymentReference = row[BillsTable.paymentReference],
                            notes = row[BillsTable.notes],
                        createdAt = row[BillsTable.createdAt],
                        updatedAt = row[BillsTable.updatedAt]
                    )
                }
        }
    }

    /**
     * Update bill status
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateBillStatus(
        billId: BillId,
        tenantId: TenantId,
        status: BillStatus
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = BillsTable.update({
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[BillsTable.status] = status
            }
            updatedRows > 0
        }
    }

    /**
     * Update bill
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateBill(
        billId: BillId,
        tenantId: TenantId,
        request: CreateBillRequest
    ): Result<FinancialDocumentDto.BillDto> = runCatching {
        dbQuery {
            // Verify bill exists and belongs to tenant
            val exists = BillsTable.selectAll().where {
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Bill not found or access denied")
            }

            // Update bill
            BillsTable.update({
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[supplierName] = request.supplierName
                it[supplierVatNumber] = request.supplierVatNumber
                it[invoiceNumber] = request.invoiceNumber
                it[issueDate] = request.issueDate
                it[dueDate] = request.dueDate
                it[amount] = java.math.BigDecimal(request.amount.value)
                it[vatAmount] = request.vatAmount?.let { amount -> java.math.BigDecimal(amount.value) }
                it[vatRate] = request.vatRate?.let { rate -> java.math.BigDecimal(rate.value) }
                it[category] = request.category
                it[description] = request.description
                it[notes] = request.notes
                it[documentId] = request.documentId?.let { id -> UUID.fromString(id.toString()) }
            }

            // Fetch and return the updated bill
            BillsTable.selectAll().where {
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                FinancialDocumentDto.BillDto(
                    id = BillId.parse(row[BillsTable.id].value.toString()),
                    tenantId = TenantId.parse(row[BillsTable.tenantId].toString()),
                    supplierName = row[BillsTable.supplierName],
                    supplierVatNumber = row[BillsTable.supplierVatNumber],
                    invoiceNumber = row[BillsTable.invoiceNumber],
                    issueDate = row[BillsTable.issueDate],
                    dueDate = row[BillsTable.dueDate],
                    amount = Money(row[BillsTable.amount].toString()),
                    vatAmount = row[BillsTable.vatAmount]?.let { Money(it.toString()) },
                    vatRate = row[BillsTable.vatRate]?.let { VatRate(it.toString()) },
                    status = row[BillsTable.status],
                    category = row[BillsTable.category],
                    currency = row[BillsTable.currency],
                    description = row[BillsTable.description],
                    documentId = row[BillsTable.documentId]?.let { ai.dokus.foundation.domain.ids.DocumentId.parse(it.toString()) },
                    paidAt = row[BillsTable.paidAt],
                    paidAmount = row[BillsTable.paidAmount]?.let { Money(it.toString()) },
                    paymentMethod = row[BillsTable.paymentMethod],
                    paymentReference = row[BillsTable.paymentReference],
                    notes = row[BillsTable.notes],
                    createdAt = row[BillsTable.createdAt],
                    updatedAt = row[BillsTable.updatedAt]
                )
            }
        }
    }

    /**
     * Mark bill as paid
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun markBillPaid(
        billId: BillId,
        tenantId: TenantId,
        request: MarkBillPaidRequest
    ): Result<FinancialDocumentDto.BillDto> = runCatching {
        dbQuery {
            // Verify bill exists and belongs to tenant
            val exists = BillsTable.selectAll().where {
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Bill not found or access denied")
            }

            // Update bill payment details
            BillsTable.update({
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[status] = BillStatus.Paid
                it[paidAt] = request.paidAt.toDateTime()
                it[paidAmount] = java.math.BigDecimal(request.paidAmount.value)
                it[paymentMethod] = request.paymentMethod
                it[paymentReference] = request.paymentReference
            }

            // Fetch and return the updated bill
            BillsTable.selectAll().where {
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                FinancialDocumentDto.BillDto(
                    id = BillId.parse(row[BillsTable.id].value.toString()),
                    tenantId = TenantId.parse(row[BillsTable.tenantId].toString()),
                    supplierName = row[BillsTable.supplierName],
                    supplierVatNumber = row[BillsTable.supplierVatNumber],
                    invoiceNumber = row[BillsTable.invoiceNumber],
                    issueDate = row[BillsTable.issueDate],
                    dueDate = row[BillsTable.dueDate],
                    amount = Money(row[BillsTable.amount].toString()),
                    vatAmount = row[BillsTable.vatAmount]?.let { Money(it.toString()) },
                    vatRate = row[BillsTable.vatRate]?.let { VatRate(it.toString()) },
                    status = row[BillsTable.status],
                    category = row[BillsTable.category],
                    currency = row[BillsTable.currency],
                    description = row[BillsTable.description],
                    documentId = row[BillsTable.documentId]?.let { ai.dokus.foundation.domain.ids.DocumentId.parse(it.toString()) },
                    paidAt = row[BillsTable.paidAt],
                    paidAmount = row[BillsTable.paidAmount]?.let { Money(it.toString()) },
                    paymentMethod = row[BillsTable.paymentMethod],
                    paymentReference = row[BillsTable.paymentReference],
                    notes = row[BillsTable.notes],
                    createdAt = row[BillsTable.createdAt],
                    updatedAt = row[BillsTable.updatedAt]
                )
            }
        }
    }

    /**
     * Delete bill
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteBill(
        billId: BillId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deletedRows = BillsTable.deleteWhere {
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            deletedRows > 0
        }
    }

    /**
     * Check if a bill exists and belongs to the tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun exists(
        billId: BillId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            BillsTable.selectAll().where {
                (BillsTable.id eq UUID.fromString(billId.toString())) and
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0
        }
    }

    /**
     * Get bill statistics for cashflow overview
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getBillStatistics(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): Result<BillStatistics> = runCatching {
        dbQuery {
            val bills = BillsTable.selectAll().where {
                (BillsTable.tenantId eq UUID.fromString(tenantId.toString())) and
                (BillsTable.issueDate greaterEq fromDate) and
                (BillsTable.issueDate lessEq toDate)
            }.toList()

            val total = bills.sumOf { java.math.BigDecimal(it[BillsTable.amount].toString()) }
            val paid = bills.filter { it[BillsTable.status] == BillStatus.Paid }
                .sumOf { java.math.BigDecimal(it[BillsTable.paidAmount]?.toString() ?: it[BillsTable.amount].toString()) }
            val pending = bills.filter { it[BillsTable.status] in listOf(BillStatus.Pending, BillStatus.Draft, BillStatus.Scheduled) }
                .sumOf { java.math.BigDecimal(it[BillsTable.amount].toString()) }

            BillStatistics(
                total = Money(total.toString()),
                paid = Money(paid.toString()),
                pending = Money(pending.toString()),
                count = bills.size
            )
        }
    }

    /**
     * Helper function to convert LocalDate to LocalDateTime at noon
     */
    private fun LocalDate.toDateTime(): kotlinx.datetime.LocalDateTime {
        return kotlinx.datetime.LocalDateTime(this.year, this.monthNumber, this.dayOfMonth, 12, 0, 0)
    }
}

/**
 * Bill statistics for cashflow overview
 */
data class BillStatistics(
    val total: Money,
    val paid: Money,
    val pending: Money,
    val count: Int
)
