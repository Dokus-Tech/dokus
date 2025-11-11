package ai.dokus.expense.backend.database.mappers

import ai.dokus.expense.backend.database.tables.ExpensesTable
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.Expense
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object ExpenseMapper {

    fun ResultRow.toExpense(): Expense = Expense(
        id = ExpenseId(this[ExpensesTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[ExpensesTable.tenantId].toKotlinUuid()),
        date = this[ExpensesTable.date],
        merchant = this[ExpensesTable.merchant],
        amount = Money(this[ExpensesTable.amount].toString()),
        vatAmount = this[ExpensesTable.vatAmount]?.toString()?.let { Money(it) },
        vatRate = this[ExpensesTable.vatRate]?.toString()?.let { VatRate(it) },
        category = this[ExpensesTable.category],
        description = this[ExpensesTable.description],
        receiptUrl = this[ExpensesTable.receiptUrl],
        receiptFilename = this[ExpensesTable.receiptFilename],
        isDeductible = this[ExpensesTable.isDeductible],
        deductiblePercentage = Percentage(this[ExpensesTable.deductiblePercentage].toString()),
        paymentMethod = this[ExpensesTable.paymentMethod],
        isRecurring = this[ExpensesTable.isRecurring],
        notes = this[ExpensesTable.notes],
        createdAt = this[ExpensesTable.createdAt],
        updatedAt = this[ExpensesTable.updatedAt]
    )
}
