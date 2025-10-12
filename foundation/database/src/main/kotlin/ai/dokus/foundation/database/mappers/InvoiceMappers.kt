package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.InvoicesTable
import ai.dokus.foundation.database.tables.InvoiceItemsTable
import ai.dokus.foundation.database.tables.ClientsTable
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
        id = this[InvoicesTable.id].value.toKotlinUuid(),
        tenantId = this[InvoicesTable.tenantId].value.toKotlinUuid(),
        clientId = this[InvoicesTable.clientId].value.toKotlinUuid(),
        invoiceNumber = this[InvoicesTable.invoiceNumber],
        issueDate = this[InvoicesTable.issueDate].toKotlinLocalDate(),
        dueDate = this[InvoicesTable.dueDate].toKotlinLocalDate(),
        subtotalAmount = this[InvoicesTable.subtotalAmount].toString(),
        vatAmount = this[InvoicesTable.vatAmount].toString(),
        totalAmount = this[InvoicesTable.totalAmount].toString(),
        paidAmount = this[InvoicesTable.paidAmount].toString(),
        status = this[InvoicesTable.status],
        currency = this[InvoicesTable.currency],
        notes = this[InvoicesTable.notes],
        termsAndConditions = this[InvoicesTable.termsAndConditions],
        peppolId = this[InvoicesTable.peppolId],
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
        id = this[InvoiceItemsTable.id].value.toKotlinUuid(),
        invoiceId = this[InvoiceItemsTable.invoiceId].value.toKotlinUuid(),
        description = this[InvoiceItemsTable.description],
        quantity = this[InvoiceItemsTable.quantity].toString(),
        unitPrice = this[InvoiceItemsTable.unitPrice].toString(),
        vatRate = this[InvoiceItemsTable.vatRate].toString(),
        lineTotal = this[InvoiceItemsTable.lineTotal].toString(),
        vatAmount = this[InvoiceItemsTable.vatAmount].toString(),
        sortOrder = this[InvoiceItemsTable.sortOrder]
    )
}

@OptIn(ExperimentalUuidApi::class)
object ClientMapper {

    fun ResultRow.toClient(): Client = Client(
        id = this[ClientsTable.id].value.toKotlinUuid(),
        tenantId = this[ClientsTable.tenantId].value.toKotlinUuid(),
        name = this[ClientsTable.name],
        email = this[ClientsTable.email],
        vatNumber = this[ClientsTable.vatNumber],
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