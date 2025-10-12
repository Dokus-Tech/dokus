package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.tables.InvoiceItemsTable
import ai.dokus.foundation.database.tables.ClientsTable
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.model.InvoiceItem
import ai.dokus.foundation.domain.model.Client
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
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
        currency = this[InvoicesTable.currency],
        notes = this[InvoicesTable.notes],
        termsAndConditions = this[InvoicesTable.termsAndConditions],
        peppolId = this[InvoicesTable.peppolId]?.let { PeppolId(it) },
        peppolSentAt = this[InvoicesTable.peppolSentAt]?.toKotlinLocalDateTime(),
        peppolStatus = this[InvoicesTable.peppolStatus],
        paymentLink = this[InvoicesTable.paymentLink],
        paymentLinkExpiresAt = this[InvoicesTable.paymentLinkExpiresAt]?.toKotlinLocalDateTime(),
        paidAt = this[InvoicesTable.paidAt]?.toKotlinLocalDateTime(),
        paymentMethod = this[InvoicesTable.paymentMethod],
        createdAt = this[InvoicesTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[InvoicesTable.updatedAt].toKotlinLocalDateTime()
    )

    fun ResultRow.toInvoiceItem(): InvoiceItem = InvoiceItem(
        id = InvoiceItemId(this[InvoiceItemsTable.id].value.toKotlinUuid()),
        invoiceId = InvoiceId(this[InvoiceItemsTable.invoiceId].value.toKotlinUuid()),
        description = this[InvoiceItemsTable.description],
        quantity = Quantity(this[InvoiceItemsTable.quantity].toString()),
        unitPrice = Money(this[InvoiceItemsTable.unitPrice].toString()),
        vatRate = VatRate(this[InvoiceItemsTable.vatRate].toString()),
        lineTotal = Money(this[InvoiceItemsTable.lineTotal].toString()),
        vatAmount = Money(this[InvoiceItemsTable.vatAmount].toString()),
        sortOrder = this[InvoiceItemsTable.sortOrder]
    )
}

@OptIn(ExperimentalUuidApi::class)
object ClientMapper {

    fun ResultRow.toClient(): Client = Client(
        id = ClientId(this[ClientsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[ClientsTable.tenantId].value.toKotlinUuid()),
        name = this[ClientsTable.name],
        email = this[ClientsTable.email]?.let { Email(it) },
        vatNumber = this[ClientsTable.vatNumber]?.let { VatNumber(it) },
        addressLine1 = this[ClientsTable.addressLine1],
        addressLine2 = this[ClientsTable.addressLine2],
        city = this[ClientsTable.city],
        postalCode = this[ClientsTable.postalCode],
        country = this[ClientsTable.country],
        contactPerson = this[ClientsTable.contactPerson],
        phone = this[ClientsTable.phone],
        notes = this[ClientsTable.notes],
        isActive = this[ClientsTable.isActive],
        createdAt = this[ClientsTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[ClientsTable.updatedAt].toKotlinLocalDateTime()
    )
}