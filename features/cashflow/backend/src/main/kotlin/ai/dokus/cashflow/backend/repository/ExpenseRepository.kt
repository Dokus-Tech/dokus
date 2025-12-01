package ai.dokus.cashflow.backend.repository

import ai.dokus.cashflow.backend.database.tables.ExpensesTable
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Percentage
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.CreateExpenseRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.util.UUID

/**
 * Repository for managing expenses
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. NEVER return expenses from different tenants
 * 3. All operations must be tenant-isolated
 */
class ExpenseRepository {

    /**
     * Create a new expense
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun createExpense(tenantId: TenantId, request: CreateExpenseRequest): Result<FinancialDocumentDto.ExpenseDto> = runCatching {
        dbQuery {
            val expenseId = ExpensesTable.insertAndGetId {
                it[ExpensesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[date] = request.date
                it[merchant] = request.merchant
                it[amount] = java.math.BigDecimal(request.amount.value)
                it[vatAmount] = request.vatAmount?.let { amount -> java.math.BigDecimal(amount.value) }
                it[vatRate] = request.vatRate?.let { rate -> java.math.BigDecimal(rate.value) }
                it[category] = request.category
                it[description] = request.description
                it[receiptUrl] = request.receiptUrl
                it[receiptFilename] = request.receiptFilename
                it[isDeductible] = request.isDeductible ?: true
                it[deductiblePercentage] = request.deductiblePercentage?.let { pct -> java.math.BigDecimal(pct.value) } ?: java.math.BigDecimal("100.00")
                it[paymentMethod] = request.paymentMethod
                it[isRecurring] = request.isRecurring ?: false
                it[notes] = request.notes
            }

            // Manually fetch and return the created expense
            ExpensesTable.selectAll().where {
                (ExpensesTable.id eq expenseId.value) and
                (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                FinancialDocumentDto.ExpenseDto(
                    id = ExpenseId.parse(row[ExpensesTable.id].value.toString()),
                    tenantId = TenantId.parse(row[ExpensesTable.tenantId].toString()),
                    date = row[ExpensesTable.date],
                    merchant = row[ExpensesTable.merchant],
                    amount = Money(row[ExpensesTable.amount].toString()),
                    vatAmount = row[ExpensesTable.vatAmount]?.let { Money(it.toString()) },
                    vatRate = row[ExpensesTable.vatRate]?.let { VatRate(it.toString()) },
                    category = row[ExpensesTable.category],
                    description = row[ExpensesTable.description],
                    receiptUrl = row[ExpensesTable.receiptUrl],
                    receiptFilename = row[ExpensesTable.receiptFilename],
                    isDeductible = row[ExpensesTable.isDeductible],
                    deductiblePercentage = Percentage(row[ExpensesTable.deductiblePercentage].toString()),
                    paymentMethod = row[ExpensesTable.paymentMethod],
                    isRecurring = row[ExpensesTable.isRecurring],
                    notes = row[ExpensesTable.notes],
                    createdAt = row[ExpensesTable.createdAt],
                    updatedAt = row[ExpensesTable.updatedAt]
                )
            }
        }
    }

    /**
     * Get a single expense by ID
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getExpense(
        expenseId: ExpenseId,
        tenantId: TenantId
    ): Result<FinancialDocumentDto.ExpenseDto?> = runCatching {
        dbQuery {
            ExpensesTable.selectAll().where {
                (ExpensesTable.id eq UUID.fromString(expenseId.toString())) and
                (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.let { row ->
                FinancialDocumentDto.ExpenseDto(
                    id = ExpenseId.parse(row[ExpensesTable.id].value.toString()),
                    tenantId = TenantId.parse(row[ExpensesTable.tenantId].toString()),
                    date = row[ExpensesTable.date],
                    merchant = row[ExpensesTable.merchant],
                    amount = Money(row[ExpensesTable.amount].toString()),
                    vatAmount = row[ExpensesTable.vatAmount]?.let { Money(it.toString()) },
                    vatRate = row[ExpensesTable.vatRate]?.let { VatRate(it.toString()) },
                    category = row[ExpensesTable.category],
                    description = row[ExpensesTable.description],
                    receiptUrl = row[ExpensesTable.receiptUrl],
                    receiptFilename = row[ExpensesTable.receiptFilename],
                    isDeductible = row[ExpensesTable.isDeductible],
                    deductiblePercentage = Percentage(row[ExpensesTable.deductiblePercentage].toString()),
                    paymentMethod = row[ExpensesTable.paymentMethod],
                    isRecurring = row[ExpensesTable.isRecurring],
                    notes = row[ExpensesTable.notes],
                    createdAt = row[ExpensesTable.createdAt],
                    updatedAt = row[ExpensesTable.updatedAt]
                )
            }
        }
    }

    /**
     * List expenses for a tenant with optional filters
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listExpenses(
        tenantId: TenantId,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<PaginatedResponse<FinancialDocumentDto.ExpenseDto>> = runCatching {
        dbQuery {
            var query = ExpensesTable.selectAll().where {
                ExpensesTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            // Apply filters
            if (category != null) {
                query = query.andWhere { ExpensesTable.category eq category }
            }
            if (fromDate != null) {
                query = query.andWhere { ExpensesTable.date greaterEq fromDate }
            }
            if (toDate != null) {
                query = query.andWhere { ExpensesTable.date lessEq toDate }
            }

            val total = query.count()

            // Apply pagination and ordering
            val items = query.orderBy(ExpensesTable.date to SortOrder.DESC)
                .limit(limit + offset)
                .map { row ->
                    FinancialDocumentDto.ExpenseDto(
                        id = ExpenseId.parse(row[ExpensesTable.id].value.toString()),
                        tenantId = TenantId.parse(row[ExpensesTable.tenantId].toString()),
                        date = row[ExpensesTable.date],
                        merchant = row[ExpensesTable.merchant],
                        amount = Money(row[ExpensesTable.amount].toString()),
                        vatAmount = row[ExpensesTable.vatAmount]?.let { Money(it.toString()) },
                        vatRate = row[ExpensesTable.vatRate]?.let { VatRate(it.toString()) },
                        category = row[ExpensesTable.category],
                        description = row[ExpensesTable.description],
                        receiptUrl = row[ExpensesTable.receiptUrl],
                        receiptFilename = row[ExpensesTable.receiptFilename],
                        isDeductible = row[ExpensesTable.isDeductible],
                        deductiblePercentage = Percentage(row[ExpensesTable.deductiblePercentage].toString()),
                        paymentMethod = row[ExpensesTable.paymentMethod],
                        isRecurring = row[ExpensesTable.isRecurring],
                        notes = row[ExpensesTable.notes],
                        createdAt = row[ExpensesTable.createdAt],
                        updatedAt = row[ExpensesTable.updatedAt]
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
     * Update expense
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateExpense(
        expenseId: ExpenseId,
        tenantId: TenantId,
        request: CreateExpenseRequest
    ): Result<FinancialDocumentDto.ExpenseDto> = runCatching {
        dbQuery {
            // Verify expense exists and belongs to tenant
            val exists = ExpensesTable.selectAll().where {
                (ExpensesTable.id eq UUID.fromString(expenseId.toString())) and
                (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0

            if (!exists) {
                throw IllegalArgumentException("Expense not found or access denied")
            }

            // Update expense
            ExpensesTable.update({
                (ExpensesTable.id eq UUID.fromString(expenseId.toString())) and
                (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[date] = request.date
                it[merchant] = request.merchant
                it[amount] = java.math.BigDecimal(request.amount.value)
                it[vatAmount] = request.vatAmount?.let { amount -> java.math.BigDecimal(amount.value) }
                it[vatRate] = request.vatRate?.let { rate -> java.math.BigDecimal(rate.value) }
                it[category] = request.category
                it[description] = request.description
                it[receiptUrl] = request.receiptUrl
                it[receiptFilename] = request.receiptFilename
                it[isDeductible] = request.isDeductible ?: true
                it[deductiblePercentage] = request.deductiblePercentage?.let { pct -> java.math.BigDecimal(pct.value) } ?: java.math.BigDecimal("100.00")
                it[paymentMethod] = request.paymentMethod
                it[isRecurring] = request.isRecurring ?: false
                it[notes] = request.notes
            }

            // Manually fetch and return the updated expense
            ExpensesTable.selectAll().where {
                (ExpensesTable.id eq UUID.fromString(expenseId.toString())) and
                (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.single().let { row ->
                FinancialDocumentDto.ExpenseDto(
                    id = ExpenseId.parse(row[ExpensesTable.id].value.toString()),
                    tenantId = TenantId.parse(row[ExpensesTable.tenantId].toString()),
                    date = row[ExpensesTable.date],
                    merchant = row[ExpensesTable.merchant],
                    amount = Money(row[ExpensesTable.amount].toString()),
                    vatAmount = row[ExpensesTable.vatAmount]?.let { Money(it.toString()) },
                    vatRate = row[ExpensesTable.vatRate]?.let { VatRate(it.toString()) },
                    category = row[ExpensesTable.category],
                    description = row[ExpensesTable.description],
                    receiptUrl = row[ExpensesTable.receiptUrl],
                    receiptFilename = row[ExpensesTable.receiptFilename],
                    isDeductible = row[ExpensesTable.isDeductible],
                    deductiblePercentage = Percentage(row[ExpensesTable.deductiblePercentage].toString()),
                    paymentMethod = row[ExpensesTable.paymentMethod],
                    isRecurring = row[ExpensesTable.isRecurring],
                    notes = row[ExpensesTable.notes],
                    createdAt = row[ExpensesTable.createdAt],
                    updatedAt = row[ExpensesTable.updatedAt]
                )
            }
        }
    }

    /**
     * Delete expense
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun deleteExpense(
        expenseId: ExpenseId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val deletedRows = ExpensesTable.deleteWhere {
                (ExpensesTable.id eq UUID.fromString(expenseId.toString())) and
                (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }
            deletedRows > 0
        }
    }

    /**
     * Check if an expense exists and belongs to the tenant
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun exists(
        expenseId: ExpenseId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            ExpensesTable.selectAll().where {
                (ExpensesTable.id eq UUID.fromString(expenseId.toString())) and
                (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.count() > 0
        }
    }
}
