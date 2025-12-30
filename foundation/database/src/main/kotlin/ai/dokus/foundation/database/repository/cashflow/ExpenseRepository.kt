package ai.dokus.foundation.database.repository.cashflow

import ai.dokus.foundation.database.tables.cashflow.ExpensesTable
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.toDbDecimal
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateExpenseRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.foundation.backend.database.dbQuery
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
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
                it[amount] = request.amount.toDbDecimal()
                it[vatAmount] = request.vatAmount?.let { amount -> amount.toDbDecimal() }
                it[vatRate] = request.vatRate?.let { rate -> rate.toDbDecimal() }
                it[category] = request.category
                it[description] = request.description
                it[documentId] = request.documentId?.let { docId -> UUID.fromString(docId.toString()) }
                it[isDeductible] = request.isDeductible ?: true
                it[deductiblePercentage] = request.deductiblePercentage?.let { pct -> pct.toDbDecimal() } ?: Percentage.FULL.toDbDecimal()
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
                    amount = Money.fromDbDecimal(row[ExpensesTable.amount]),
                    vatAmount = row[ExpensesTable.vatAmount]?.let { Money.fromDbDecimal(it) },
                    vatRate = row[ExpensesTable.vatRate]?.let { VatRate.fromDbDecimal(it) },
                    category = row[ExpensesTable.category],
                    description = row[ExpensesTable.description],
                    documentId = row[ExpensesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                    isDeductible = row[ExpensesTable.isDeductible],
                    deductiblePercentage = Percentage.fromDbDecimal(row[ExpensesTable.deductiblePercentage]),
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
                    amount = Money.fromDbDecimal(row[ExpensesTable.amount]),
                    vatAmount = row[ExpensesTable.vatAmount]?.let { Money.fromDbDecimal(it) },
                    vatRate = row[ExpensesTable.vatRate]?.let { VatRate.fromDbDecimal(it) },
                    category = row[ExpensesTable.category],
                    description = row[ExpensesTable.description],
                    documentId = row[ExpensesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                    isDeductible = row[ExpensesTable.isDeductible],
                    deductiblePercentage = Percentage.fromDbDecimal(row[ExpensesTable.deductiblePercentage]),
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
                        amount = Money.fromDbDecimal(row[ExpensesTable.amount]),
                        vatAmount = row[ExpensesTable.vatAmount]?.let { Money.fromDbDecimal(it) },
                        vatRate = row[ExpensesTable.vatRate]?.let { VatRate.fromDbDecimal(it) },
                        category = row[ExpensesTable.category],
                        description = row[ExpensesTable.description],
                        documentId = row[ExpensesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                        isDeductible = row[ExpensesTable.isDeductible],
                        deductiblePercentage = Percentage.fromDbDecimal(row[ExpensesTable.deductiblePercentage]),
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
                it[amount] = request.amount.toDbDecimal()
                it[vatAmount] = request.vatAmount?.let { amount -> amount.toDbDecimal() }
                it[vatRate] = request.vatRate?.let { rate -> rate.toDbDecimal() }
                it[category] = request.category
                it[description] = request.description
                it[documentId] = request.documentId?.let { docId -> UUID.fromString(docId.toString()) }
                it[isDeductible] = request.isDeductible ?: true
                it[deductiblePercentage] = request.deductiblePercentage?.let { pct -> pct.toDbDecimal() } ?: Percentage.FULL.toDbDecimal()
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
                    amount = Money.fromDbDecimal(row[ExpensesTable.amount]),
                    vatAmount = row[ExpensesTable.vatAmount]?.let { Money.fromDbDecimal(it) },
                    vatRate = row[ExpensesTable.vatRate]?.let { VatRate.fromDbDecimal(it) },
                    category = row[ExpensesTable.category],
                    description = row[ExpensesTable.description],
                    documentId = row[ExpensesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                    isDeductible = row[ExpensesTable.isDeductible],
                    deductiblePercentage = Percentage.fromDbDecimal(row[ExpensesTable.deductiblePercentage]),
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

    /**
     * Update expense's document reference.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun updateDocumentId(
        expenseId: ExpenseId,
        tenantId: TenantId,
        documentId: DocumentId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updatedRows = ExpensesTable.update({
                (ExpensesTable.id eq UUID.fromString(expenseId.toString())) and
                (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[ExpensesTable.documentId] = UUID.fromString(documentId.toString())
            }
            updatedRows > 0
        }
    }

    /**
     * Find expense by document ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun findByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId
    ): FinancialDocumentDto.ExpenseDto? = dbQuery {
        ExpensesTable.selectAll().where {
            (ExpensesTable.tenantId eq UUID.fromString(tenantId.toString())) and
            (ExpensesTable.documentId eq UUID.fromString(documentId.toString()))
        }.singleOrNull()?.let { row ->
            FinancialDocumentDto.ExpenseDto(
                id = ExpenseId.parse(row[ExpensesTable.id].value.toString()),
                tenantId = TenantId.parse(row[ExpensesTable.tenantId].toString()),
                date = row[ExpensesTable.date],
                merchant = row[ExpensesTable.merchant],
                amount = Money.fromDbDecimal(row[ExpensesTable.amount]),
                vatAmount = row[ExpensesTable.vatAmount]?.let { Money.fromDbDecimal(it) },
                vatRate = row[ExpensesTable.vatRate]?.let { VatRate.fromDbDecimal(it) },
                category = row[ExpensesTable.category],
                description = row[ExpensesTable.description],
                documentId = row[ExpensesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                isDeductible = row[ExpensesTable.isDeductible],
                deductiblePercentage = Percentage.fromDbDecimal(row[ExpensesTable.deductiblePercentage]),
                paymentMethod = row[ExpensesTable.paymentMethod],
                isRecurring = row[ExpensesTable.isRecurring],
                notes = row[ExpensesTable.notes],
                createdAt = row[ExpensesTable.createdAt],
                updatedAt = row[ExpensesTable.updatedAt]
            )
        }
    }
}
