package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.ExpensesTable
import ai.dokus.foundation.database.utils.toKotlinLocalDate
import ai.dokus.foundation.database.utils.toKotlinLocalDateTime
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.Expense
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object ExpenseMapper {

    fun ResultRow.toExpense(): Expense = Expense(
        id = ExpenseId(this[ExpensesTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[ExpensesTable.tenantId].value.toKotlinUuid()),
        date = this[ExpensesTable.date].toKotlinLocalDate(),
        merchant = this[ExpensesTable.merchant],
        amount = Money(this[ExpensesTable.amount].toString()),
        vatAmount = this[ExpensesTable.vatAmount]?.let { Money(it.toString()) },
        vatRate = this[ExpensesTable.vatRate]?.let { VatRate(it.toString()) },
        category = this[ExpensesTable.category],
        description = this[ExpensesTable.description],
        receiptUrl = this[ExpensesTable.receiptUrl],
        receiptFilename = this[ExpensesTable.receiptFilename],
        isDeductible = this[ExpensesTable.isDeductible],
        deductiblePercentage = Percentage(this[ExpensesTable.deductiblePercentage].toString()),
        paymentMethod = this[ExpensesTable.paymentMethod],
        isRecurring = this[ExpensesTable.isRecurring],
        notes = this[ExpensesTable.notes],
        createdAt = this[ExpensesTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[ExpensesTable.updatedAt].toKotlinLocalDateTime()
    )
}
