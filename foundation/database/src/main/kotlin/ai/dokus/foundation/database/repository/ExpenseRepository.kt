package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.mappers.ExpenseMapper.toExpense
import ai.dokus.foundation.database.tables.ExpensesTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.PaymentMethod
import ai.dokus.foundation.domain.model.Expense
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.sql.*
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class ExpenseRepository {
    private val logger = LoggerFactory.getLogger(ExpenseRepository::class.java)

    suspend fun create(
        tenantId: TenantId,
        date: LocalDate,
        merchant: String,
        amount: Money,
        vatAmount: Money? = null,
        vatRate: VatRate? = null,
        category: ExpenseCategory,
        description: String? = null,
        receiptUrl: String? = null,
        receiptFilename: String? = null,
        isDeductible: Boolean = true,
        deductiblePercentage: Percentage = Percentage.FULL,
        paymentMethod: PaymentMethod? = null,
        isRecurring: Boolean = false,
        notes: String? = null
    ): ExpenseId = dbQuery {
        val javaUuid = tenantId.value.toJavaUuid()

        val expenseId = ExpensesTable.insertAndGetId {
            it[ExpensesTable.tenantId] = javaUuid
            it[ExpensesTable.date] = date
            it[ExpensesTable.merchant] = merchant
            it[ExpensesTable.amount] = amount.value
            it[ExpensesTable.vatAmount] = vatAmount?.value
            it[ExpensesTable.vatRate] = vatRate?.let { rate -> BigDecimal(rate.value) }
            it[ExpensesTable.category] = category
            it[ExpensesTable.description] = description
            it[ExpensesTable.receiptUrl] = receiptUrl
            it[ExpensesTable.receiptFilename] = receiptFilename
            it[ExpensesTable.isDeductible] = isDeductible
            it[ExpensesTable.deductiblePercentage] = deductiblePercentage.value
            it[ExpensesTable.paymentMethod] = paymentMethod
            it[ExpensesTable.isRecurring] = isRecurring
            it[ExpensesTable.notes] = notes
        }.value

        logger.info("Created expense: $expenseId for tenant: $tenantId")
        ExpenseId(expenseId.toKotlinUuid())
    }

    suspend fun findById(id: ExpenseId, tenantId: TenantId): Expense? = dbQuery {
        val javaExpenseId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        ExpensesTable
            .selectAll()
            .where {
                (ExpensesTable.id eq javaExpenseId) and
                (ExpensesTable.tenantId eq javaTenantId)
            }
            .singleOrNull()
            ?.toExpense()
    }

    suspend fun findByTenant(
        tenantId: TenantId,
        category: ExpenseCategory? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        isDeductible: Boolean? = null,
        search: String? = null,
        limit: Int = 100,
        offset: Int = 0
    ): List<Expense> = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        val query = ExpensesTable
            .selectAll()
            .where { ExpensesTable.tenantId eq javaTenantId }

        // Apply category filter
        category?.let { cat ->
            query.andWhere { ExpensesTable.category eq cat }
        }

        // Apply date range filter
        fromDate?.let { from ->
            query.andWhere { ExpensesTable.date greaterEq from }
        }
        toDate?.let { to ->
            query.andWhere { ExpensesTable.date lessEq to }
        }

        // Apply deductibility filter
        isDeductible?.let { deductible ->
            query.andWhere { ExpensesTable.isDeductible eq deductible }
        }

        // Apply search filter
        search?.let { searchTerm ->
            query.andWhere {
                (ExpensesTable.merchant like "%$searchTerm%") or
                (ExpensesTable.description like "%$searchTerm%")
            }
        }

        query
            .orderBy(ExpensesTable.date to SortOrder.DESC)
            .limit(limit, offset.toLong())
            .map { it.toExpense() }
    }

    suspend fun findByDateRange(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<Expense> = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        ExpensesTable
            .selectAll()
            .where {
                (ExpensesTable.tenantId eq javaTenantId) and
                (ExpensesTable.date greaterEq fromDate) and
                (ExpensesTable.date lessEq toDate)
            }
            .orderBy(ExpensesTable.date to SortOrder.DESC)
            .map { it.toExpense() }
    }

    suspend fun update(
        id: ExpenseId,
        tenantId: TenantId,
        date: LocalDate? = null,
        merchant: String? = null,
        amount: Money? = null,
        vatAmount: Money? = null,
        vatRate: VatRate? = null,
        category: ExpenseCategory? = null,
        description: String? = null,
        receiptUrl: String? = null,
        receiptFilename: String? = null,
        isDeductible: Boolean? = null,
        deductiblePercentage: Percentage? = null,
        paymentMethod: PaymentMethod? = null,
        isRecurring: Boolean? = null,
        notes: String? = null
    ): Boolean = dbQuery {
        val javaExpenseId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        val updated = ExpensesTable.update({
            (ExpensesTable.id eq javaExpenseId) and
            (ExpensesTable.tenantId eq javaTenantId)
        }) {
            date?.let { value -> it[ExpensesTable.date] = value }
            merchant?.let { value -> it[ExpensesTable.merchant] = value }
            amount?.let { value -> it[ExpensesTable.amount] = value.value }
            vatAmount?.let { value -> it[ExpensesTable.vatAmount] = value.value }
            vatRate?.let { rate -> it[ExpensesTable.vatRate] = BigDecimal(rate.value) }
            category?.let { value -> it[ExpensesTable.category] = value }
            description?.let { value -> it[ExpensesTable.description] = value }
            receiptUrl?.let { value -> it[ExpensesTable.receiptUrl] = value }
            receiptFilename?.let { value -> it[ExpensesTable.receiptFilename] = value }
            isDeductible?.let { value -> it[ExpensesTable.isDeductible] = value }
            deductiblePercentage?.let { value -> it[ExpensesTable.deductiblePercentage] = value.value }
            paymentMethod?.let { value -> it[ExpensesTable.paymentMethod] = value }
            isRecurring?.let { value -> it[ExpensesTable.isRecurring] = value }
            notes?.let { value -> it[ExpensesTable.notes] = value }
        }

        if (updated > 0) {
            logger.info("Updated expense: $id for tenant: $tenantId")
        }

        updated > 0
    }

    suspend fun delete(id: ExpenseId, tenantId: TenantId): Boolean = dbQuery {
        val javaExpenseId = id.value.toJavaUuid()
        val javaTenantId = tenantId.value.toJavaUuid()

        val deleted = ExpensesTable.deleteWhere {
            (ExpensesTable.id eq javaExpenseId) and
            (ExpensesTable.tenantId eq javaTenantId)
        }

        if (deleted > 0) {
            logger.info("Deleted expense: $id for tenant: $tenantId")
        }

        deleted > 0
    }

    suspend fun countByTenant(
        tenantId: TenantId,
        category: ExpenseCategory? = null,
        isDeductible: Boolean? = null
    ): Long = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        val query = ExpensesTable
            .select(ExpensesTable.id.count())
            .where { ExpensesTable.tenantId eq javaTenantId }

        category?.let { cat ->
            query.andWhere { ExpensesTable.category eq cat }
        }

        isDeductible?.let { deductible ->
            query.andWhere { ExpensesTable.isDeductible eq deductible }
        }

        query.single()[ExpensesTable.id.count()]
    }

    /**
     * Calculate total expenses by category for reporting
     */
    suspend fun sumByCategory(
        tenantId: TenantId,
        category: ExpenseCategory,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Money = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        val query = ExpensesTable
            .select(ExpensesTable.amount.sum())
            .where {
                (ExpensesTable.tenantId eq javaTenantId) and
                (ExpensesTable.category eq category)
            }

        fromDate?.let { from ->
            query.andWhere { ExpensesTable.date greaterEq from }
        }
        toDate?.let { to ->
            query.andWhere { ExpensesTable.date lessEq to }
        }

        val sum = query.singleOrNull()?.get(ExpensesTable.amount.sum())
        Money(sum ?: BigDecimal.ZERO)
    }

    /**
     * Calculate total VAT from expenses for VAT return filing
     */
    suspend fun sumVatByDateRange(
        tenantId: TenantId,
        fromDate: LocalDate,
        toDate: LocalDate,
        isDeductible: Boolean = true
    ): Money = dbQuery {
        val javaTenantId = tenantId.value.toJavaUuid()

        val sum = ExpensesTable
            .select(ExpensesTable.vatAmount.sum())
            .where {
                (ExpensesTable.tenantId eq javaTenantId) and
                (ExpensesTable.date greaterEq fromDate) and
                (ExpensesTable.date lessEq toDate) and
                (ExpensesTable.isDeductible eq isDeductible)
            }
            .singleOrNull()
            ?.get(ExpensesTable.vatAmount.sum())

        Money(sum ?: BigDecimal.ZERO)
    }
}
