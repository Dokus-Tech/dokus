package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.ExpenseEntity
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.TenantId

fun ExpenseEntity.Companion.from(row: ResultRow): ExpenseEntity = ExpenseEntity(
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
    contactId = row[ExpensesTable.contactId]?.let { ContactId.parse(it.toString()) },
    isDeductible = row[ExpensesTable.isDeductible],
    deductiblePercentage = Percentage.fromDbDecimal(row[ExpensesTable.deductiblePercentage]),
    paymentMethod = row[ExpensesTable.paymentMethod],
    isRecurring = row[ExpensesTable.isRecurring],
    notes = row[ExpensesTable.notes],
    createdAt = row[ExpensesTable.createdAt],
    updatedAt = row[ExpensesTable.updatedAt],
)
