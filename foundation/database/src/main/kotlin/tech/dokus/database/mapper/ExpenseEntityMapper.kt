package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.ExpenseEntity
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId

fun ExpenseEntity.Companion.from(row: ResultRow): ExpenseEntity = ExpenseEntity(
    id = ExpenseId.parse(row[ExpensesTable.id].value.toString()),
    tenantId = TenantId.parse(row[ExpensesTable.tenantId].toString()),
    date = row[ExpensesTable.date],
    merchant = row[ExpensesTable.merchant],
    amount = Money.fromDbDecimal(row[ExpensesTable.amount], Currency.Eur),
    vatAmount = row[ExpensesTable.vatAmount]?.let { Money.fromDbDecimal(it, Currency.Eur) },
    vatRate = row[ExpensesTable.vatRate]?.let { VatRate.fromDbDecimal(it) },
    category = row[ExpensesTable.category],
    description = row[ExpensesTable.description],
    documentId = row[ExpensesTable.documentId]?.let { DocumentId.parse(it.toString()) },
    contactId = row[ExpensesTable.contactId]?.let { ContactId.parse(it.toString()) },
    isDeductible = row[ExpensesTable.isDeductible],
    deductiblePercentage = Percentage.fromDbDecimal(row[ExpensesTable.deductiblePercentage]),
    paymentMethod = row[ExpensesTable.paymentMethod],
    isRecurring = row[ExpensesTable.isRecurring],
    notes = row[ExpensesTable.notes],
    confirmedAt = row[ExpensesTable.confirmedAt],
    confirmedBy = row[ExpensesTable.confirmedBy]?.let { UserId.parse(it.toString()) },
    createdAt = row[ExpensesTable.createdAt],
    updatedAt = row[ExpensesTable.updatedAt],
)
