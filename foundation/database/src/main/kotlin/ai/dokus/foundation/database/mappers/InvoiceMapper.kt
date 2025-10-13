package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.*
import ai.dokus.foundation.database.utils.toKotlinLocalDate
import ai.dokus.foundation.database.utils.toKotlinLocalDateTime
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.*
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object InvoiceMapper {

    fun ResultRow.toInvoice(): Invoice = Invoice(
        id = InvoiceId(this[InvoicesTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[InvoicesTable.tenantId].value.toKotlinUuid()),
        clientId = ClientId(this[InvoicesTable.clientId].value.toKotlinUuid()),
        invoiceNumber = InvoiceNumber(this[InvoicesTable.invoiceNumber]),
        issueDate = this[InvoicesTable.issueDate].toKotlinLocalDate(),
        dueDate = this[InvoicesTable.dueDate].toKotlinLocalDate(),
        subtotalAmount = Money(this[InvoicesTable.subtotalAmount].toString()),
        vatAmount = Money(this[InvoicesTable.vatAmount].toString()),
        totalAmount = Money(this[InvoicesTable.totalAmount].toString()),
        paidAmount = Money(this[InvoicesTable.paidAmount].toString()),
        status = this[InvoicesTable.status],
        paidAt = this[InvoicesTable.paidAt]?.toKotlinLocalDateTime(),
        notes = this[InvoicesTable.notes],
        createdAt = this[InvoicesTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[InvoicesTable.updatedAt].toKotlinLocalDateTime(),
        items = emptyList() // Will be populated separately
    )

    fun ResultRow.toInvoiceItem(): InvoiceItem = InvoiceItem(
        description = this[InvoiceItemsTable.description],
        quantity = Quantity(this[InvoiceItemsTable.quantity].toString()),
        unitPrice = Money(this[InvoiceItemsTable.unitPrice].toString()),
        vatRate = VatRate(this[InvoiceItemsTable.vatRate].toString()),
        lineTotal = Money(this[InvoiceItemsTable.lineTotal].toString()),
        vatAmount = Money(this[InvoiceItemsTable.vatAmount].toString())
    )
}