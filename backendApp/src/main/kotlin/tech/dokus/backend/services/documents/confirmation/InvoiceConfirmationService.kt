package tech.dokus.backend.services.documents.confirmation

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.services.InvoiceNumberGenerator
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.InvoiceItemsTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import java.math.BigDecimal
import java.util.UUID

/**
 * Confirms Invoice documents: creates Invoice entity + CashflowEntry (Direction.In).
 *
 * Invoice number generation uses a separate transaction (by design in [InvoiceNumberGenerator]),
 * so invoice numbers may be consumed even if confirmation fails.
 */
class InvoiceConfirmationService(
    private val invoiceNumberGenerator: InvoiceNumberGenerator,
    private val invoiceRepository: InvoiceRepository,
) {
    private val logger = loggerFor()

    @Suppress("CyclomaticComplexMethod")
    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: InvoiceDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runCatching {
        logger.info("Confirming invoice document: $documentId for tenant: $tenantId")

        val contactId = linkedContactId
            ?: throw DokusException.BadRequest("Invoice requires a linked contact")

        // Invoice number generation uses separate transaction (by design)
        val invoiceNumber = invoiceNumberGenerator.generateInvoiceNumber(tenantId).getOrThrow()

        val (invoiceId, cashflowEntryId) = dbQuery {
            ensureDraftConfirmable(tenantId, documentId)

            val items = if (draftData.lineItems.isNotEmpty()) {
                draftData.lineItems.mapIndexed { index, item ->
                    val quantity = item.quantity?.takeIf { it > 0 } ?: 1L
                    val unitPrice = item.unitPrice?.let { Money(it) }
                        ?: item.netAmount?.let { net ->
                            Money(net / quantity)
                        }
                    val lineTotal = item.netAmount?.let { Money(it) }
                        ?: unitPrice?.let { Money(it.minor * quantity) }
                        ?: Money.ZERO
                    val vatRate = item.vatRate?.let { VatRate(it) } ?: VatRate.ZERO
                    val vatAmount = vatRate.applyTo(lineTotal)

                    InvoiceItemDto(
                        description = item.description.ifBlank { "Item" },
                        quantity = quantity.toDouble(),
                        unitPrice = unitPrice ?: lineTotal,
                        vatRate = vatRate,
                        lineTotal = lineTotal,
                        vatAmount = vatAmount,
                        sortOrder = index
                    )
                }
            } else {
                val base = draftData.subtotalAmount ?: draftData.totalAmount ?: Money.ZERO
                val vat = draftData.vatAmount ?: Money.ZERO
                val vatRate = if (!base.isZero) {
                    VatRate(((vat.minor * 10000L) / base.minor).toInt())
                } else {
                    VatRate.ZERO
                }
                listOf(
                    InvoiceItemDto(
                        description = "Services",
                        quantity = 1.0,
                        unitPrice = base,
                        vatRate = vatRate,
                        lineTotal = base,
                        vatAmount = vat,
                        sortOrder = 0
                    )
                )
            }

            val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
            val issueDate = draftData.issueDate ?: today
            val dueDate = draftData.dueDate ?: issueDate.plus(DatePeriod(days = 30))

            val subtotalAmount = draftData.subtotalAmount?.toDbDecimal()
                ?: items.sumOf { it.lineTotal.toDbDecimal() }
            val vatAmount = draftData.vatAmount?.toDbDecimal()
                ?: items.sumOf { it.vatAmount.toDbDecimal() }
            val totalAmount = draftData.totalAmount?.toDbDecimal()
                ?: (subtotalAmount + vatAmount)

            val invoiceId = InvoicesTable.insertAndGetId {
                it[InvoicesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[InvoicesTable.contactId] = UUID.fromString(contactId.toString())
                it[InvoicesTable.invoiceNumber] = invoiceNumber
                it[InvoicesTable.issueDate] = issueDate
                it[InvoicesTable.dueDate] = dueDate
                it[InvoicesTable.subtotalAmount] = subtotalAmount
                it[InvoicesTable.vatAmount] = vatAmount
                it[InvoicesTable.totalAmount] = totalAmount
                it[InvoicesTable.paidAmount] = Money.ZERO.toDbDecimal()
                it[InvoicesTable.status] = InvoiceStatus.Draft
                it[InvoicesTable.notes] = draftData.notes
                it[InvoicesTable.documentId] = UUID.fromString(documentId.toString())
            }.value

            items.forEachIndexed { index, item ->
                InvoiceItemsTable.insert {
                    it[InvoiceItemsTable.invoiceId] = invoiceId
                    it[InvoiceItemsTable.description] = item.description
                    it[InvoiceItemsTable.quantity] = BigDecimal.valueOf(item.quantity)
                    it[InvoiceItemsTable.unitPrice] = item.unitPrice.toDbDecimal()
                    it[InvoiceItemsTable.vatRate] = item.vatRate.toDbDecimal()
                    it[InvoiceItemsTable.lineTotal] = item.lineTotal.toDbDecimal()
                    it[InvoiceItemsTable.vatAmount] = item.vatAmount.toDbDecimal()
                    it[InvoiceItemsTable.sortOrder] = index
                }
            }

            val entryId = CashflowEntriesTable.insertAndGetId {
                it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[CashflowEntriesTable.sourceType] = CashflowSourceType.Invoice
                it[CashflowEntriesTable.sourceId] = invoiceId
                it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
                it[CashflowEntriesTable.direction] = CashflowDirection.In
                it[CashflowEntriesTable.eventDate] = dueDate
                it[CashflowEntriesTable.amountGross] = totalAmount
                it[CashflowEntriesTable.amountVat] = vatAmount
                it[CashflowEntriesTable.remainingAmount] = totalAmount
                it[CashflowEntriesTable.status] = CashflowEntryStatus.Open
                it[CashflowEntriesTable.counterpartyId] = UUID.fromString(contactId.toString())
            }.value

            markDraftConfirmed(tenantId, documentId)

            invoiceId to CashflowEntryId.parse(entryId.toString())
        }

        // Fetch full entity outside transaction
        val invoice = invoiceRepository.getInvoice(
            invoiceId = InvoiceId.parse(invoiceId.toString()),
            tenantId = tenantId
        ).getOrThrow() ?: throw DokusException.InternalError("Invoice not found after confirmation")

        logger.info("Invoice confirmed: $documentId -> invoiceId=$invoiceId, entryId=$cashflowEntryId")
        ConfirmationResult(entity = invoice, cashflowEntryId = cashflowEntryId, documentId = documentId)
    }
}
