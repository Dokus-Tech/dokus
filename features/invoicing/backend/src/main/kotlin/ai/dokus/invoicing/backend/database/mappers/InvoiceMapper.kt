package ai.dokus.invoicing.backend.database.mappers

import ai.dokus.invoicing.backend.database.tables.*
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.*
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object InvoiceMapper {

    fun ResultRow.toInvoice(): Invoice = Invoice(
        id = InvoiceId(this[InvoicesTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[InvoicesTable.tenantId].toKotlinUuid()),
        clientId = ClientId(this[InvoicesTable.clientId].value.toKotlinUuid()),
        invoiceNumber = InvoiceNumber(this[InvoicesTable.invoiceNumber]),
        issueDate = this[InvoicesTable.issueDate],
        dueDate = this[InvoicesTable.dueDate],
        subtotalAmount = Money(this[InvoicesTable.subtotalAmount].toString()),
        vatAmount = Money(this[InvoicesTable.vatAmount].toString()),
        totalAmount = Money(this[InvoicesTable.totalAmount].toString()),
        paidAmount = Money(this[InvoicesTable.paidAmount].toString()),
        status = this[InvoicesTable.status],
        paidAt = this[InvoicesTable.paidAt],
        notes = this[InvoicesTable.notes],
        createdAt = this[InvoicesTable.createdAt],
        updatedAt = this[InvoicesTable.updatedAt],
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