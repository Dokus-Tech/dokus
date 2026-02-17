package tech.dokus.backend.services.documents.confirmation
import kotlin.uuid.Uuid

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.backend.util.isUniqueViolation
import tech.dokus.backend.util.runSuspendCatching
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Confirms invoice documents and projects cashflow from invoice direction.
 *
 * Uses repositories for all persistence — no direct table access.
 */
class InvoiceConfirmationService(
    private val invoiceRepository: InvoiceRepository,
    private val cashflowEntriesService: CashflowEntriesService,
    private val draftRepository: DocumentDraftRepository,
) {
    private val logger = loggerFor()

    @Suppress("CyclomaticComplexMethod")
    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: InvoiceDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runSuspendCatching {
        logger.info("Confirming invoice document: $documentId for tenant: $tenantId")

        val draft = requireConfirmableDraft(draftRepository, tenantId, documentId)
        val isReconfirm = draft.documentStatus == DocumentStatus.NeedsReview

        val contactId = linkedContactId ?: draft.linkedContactId
            ?: throw DokusException.BadRequest("Invoice requires a linked contact")

        val items = buildInvoiceItems(draftData)
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val issueDate = draftData.issueDate ?: today
        val dueDate = draftData.dueDate ?: issueDate.plus(DatePeriod(days = 30))
        val direction = draftData.direction
        if (direction == DocumentDirection.Unknown) {
            throw DokusException.BadRequest("Invoice direction is unknown")
        }

        val request = CreateInvoiceRequest(
            contactId = contactId,
            direction = direction,
            items = items,
            issueDate = issueDate,
            dueDate = dueDate,
            notes = draftData.notes,
            documentId = documentId,
            subtotalAmount = draftData.subtotalAmount,
            vatAmount = draftData.vatAmount,
            totalAmount = draftData.totalAmount
        )

        val existingInvoice = invoiceRepository.findByDocumentId(tenantId, documentId)
        val invoice = when {
            existingInvoice == null -> {
                invoiceRepository.createInvoice(tenantId, request).getOrElse { t ->
                    if (!t.isUniqueViolation()) throw t
                    invoiceRepository.findByDocumentId(tenantId, documentId) ?: throw t
                }
            }

            isReconfirm -> {
                if (existingInvoice.status != InvoiceStatus.Draft) {
                    throw DokusException.BadRequest("Cannot re-confirm invoice in status: ${existingInvoice.status}")
                }
                invoiceRepository.updateInvoice(existingInvoice.id, tenantId, request).getOrThrow()
            }

            else -> existingInvoice
        }

        val cashflowEntry = if (existingInvoice != null && isReconfirm) {
            cashflowEntriesService.updateFromInvoice(
                tenantId = tenantId,
                invoiceId = Uuid.parse(invoice.id.toString()),
                documentId = documentId,
                dueDate = dueDate,
                amountGross = invoice.totalAmount,
                amountVat = invoice.vatAmount,
                direction = invoice.direction,
                contactId = contactId
            ).getOrThrow()
        } else {
            cashflowEntriesService.createFromInvoice(
                tenantId = tenantId,
                invoiceId = Uuid.parse(invoice.id.toString()),
                documentId = documentId,
                dueDate = dueDate,
                amountGross = invoice.totalAmount,
                amountVat = invoice.vatAmount,
                direction = invoice.direction,
                contactId = contactId
            ).getOrThrow()
        }

        draftRepository.updateDocumentStatus(documentId, tenantId, DocumentStatus.Confirmed)

        logger.info("Invoice confirmed: $documentId -> invoiceId=${invoice.id}, entryId=${cashflowEntry.id}")
        ConfirmationResult(entity = invoice, cashflowEntryId = cashflowEntry.id, documentId = documentId)
    }

    private fun buildInvoiceItems(draftData: InvoiceDraftData): List<InvoiceItemDto> {
        if (draftData.lineItems.isNotEmpty()) {
            return draftData.lineItems.mapIndexed { index, item ->
                val quantity = item.quantity?.takeIf { it > 0 } ?: 1L
                val unitPrice = item.unitPrice?.let { Money(it) }
                    ?: item.netAmount?.let { net -> Money(net / quantity) }
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
        }

        val base = draftData.subtotalAmount ?: draftData.totalAmount ?: Money.ZERO
        val vat = draftData.vatAmount ?: Money.ZERO
        val vatRate = if (!base.isZero) {
            VatRate(((vat.minor * 10000L) / base.minor).toInt())
        } else {
            VatRate.ZERO
        }
        return listOf(
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
}
