package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.InvoiceEntity
import tech.dokus.database.entity.InvoiceItemEntity
import tech.dokus.database.tables.cashflow.InvoiceItemsTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.TenantId

fun InvoiceEntity.Companion.from(
    row: ResultRow,
    items: List<InvoiceItemEntity>,
): InvoiceEntity = InvoiceEntity(
    id = InvoiceId.parse(row[InvoicesTable.id].value.toString()),
    tenantId = TenantId.parse(row[InvoicesTable.tenantId].toString()),
    direction = row[InvoicesTable.direction],
    contactId = ContactId.parse(row[InvoicesTable.contactId].toString()),
    invoiceNumber = InvoiceNumber(row[InvoicesTable.invoiceNumber]),
    issueDate = row[InvoicesTable.issueDate],
    dueDate = row[InvoicesTable.dueDate],
    subtotalAmount = Money.fromDbDecimal(row[InvoicesTable.subtotalAmount]),
    vatAmount = Money.fromDbDecimal(row[InvoicesTable.vatAmount]),
    totalAmount = Money.fromDbDecimal(row[InvoicesTable.totalAmount]),
    paidAmount = Money.fromDbDecimal(row[InvoicesTable.paidAmount]),
    status = row[InvoicesTable.status],
    currency = row[InvoicesTable.currency],
    notes = row[InvoicesTable.notes],
    paymentTermsDays = row[InvoicesTable.paymentTermsDays],
    dueDateMode = row[InvoicesTable.dueDateMode],
    structuredCommunication = row[InvoicesTable.structuredCommunication]?.let(::StructuredCommunication),
    senderIban = row[InvoicesTable.senderIban]?.let(::Iban),
    senderBic = row[InvoicesTable.senderBic]?.let(::Bic),
    deliveryMethod = row[InvoicesTable.deliveryMethod],
    termsAndConditions = row[InvoicesTable.termsAndConditions],
    items = items,
    peppolId = row[InvoicesTable.peppolId]?.let(::PeppolId),
    peppolSentAt = row[InvoicesTable.peppolSentAt],
    peppolStatus = row[InvoicesTable.peppolStatus],
    documentId = row[InvoicesTable.documentId]?.let { DocumentId.parse(it.toString()) },
    paymentLink = row[InvoicesTable.paymentLink],
    paymentLinkExpiresAt = row[InvoicesTable.paymentLinkExpiresAt],
    paidAt = row[InvoicesTable.paidAt],
    paymentMethod = row[InvoicesTable.paymentMethod],
    createdAt = row[InvoicesTable.createdAt],
    updatedAt = row[InvoicesTable.updatedAt],
)

fun InvoiceItemEntity.Companion.from(
    row: ResultRow,
    invoiceId: InvoiceId,
): InvoiceItemEntity = InvoiceItemEntity(
    id = row[InvoiceItemsTable.id].value.toString(),
    invoiceId = invoiceId,
    description = row[InvoiceItemsTable.description],
    quantity = row[InvoiceItemsTable.quantity].toDouble(),
    unitPrice = Money.fromDbDecimal(row[InvoiceItemsTable.unitPrice]),
    vatRate = VatRate.fromDbDecimal(row[InvoiceItemsTable.vatRate]),
    lineTotal = Money.fromDbDecimal(row[InvoiceItemsTable.lineTotal]),
    vatAmount = Money.fromDbDecimal(row[InvoiceItemsTable.vatAmount]),
    sortOrder = row[InvoiceItemsTable.sortOrder],
)
