package tech.dokus.backend.services.documents.confirmation

import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.backend.util.runSuspendCatching
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreateBillRequest
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID

/**
 * Confirms Bill documents: creates Bill entity + CashflowEntry (Direction.Out).
 *
 * Uses repositories for all persistence — no direct table access.
 */
class BillConfirmationService(
    private val billRepository: BillRepository,
    private val cashflowEntriesService: CashflowEntriesService,
    private val draftRepository: DocumentDraftRepository,
) {
    private val logger = loggerFor()

    @Suppress("ThrowsCount")
    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: InvoiceDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runSuspendCatching {
        logger.info("Confirming bill document: $documentId for tenant: $tenantId")

        ensureDraftConfirmable(draftRepository, tenantId, documentId)

        val issueDate = draftData.issueDate ?: throw DokusException.BadRequest("Issue date is required")
        val dueDate = draftData.dueDate ?: issueDate
        val subtotalAmount = draftData.subtotalAmount
        val vatAmount = draftData.vatAmount
        val amount = draftData.totalAmount
            ?: subtotalAmount?.let { sub -> sub + (vatAmount ?: Money.ZERO) }
            ?: throw DokusException.BadRequest("Amount is required")
        val vatRate = if (subtotalAmount != null && vatAmount != null && !subtotalAmount.isZero) {
            VatRate(((vatAmount.minor * 10000L) / subtotalAmount.minor).toInt())
        } else {
            null
        }

        val request = CreateBillRequest(
            supplierName = draftData.seller.name ?: draftData.customerName ?: "Unknown Supplier",
            supplierVatNumber = (draftData.seller.vat ?: draftData.customerVat)?.value,
            invoiceNumber = draftData.invoiceNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            amount = amount,
            vatAmount = vatAmount,
            vatRate = vatRate,
            category = ExpenseCategory.Other,
            notes = draftData.notes,
            documentId = documentId,
            contactId = linkedContactId,
            currency = draftData.currency
        )

        val bill = billRepository.createBill(tenantId, request).getOrThrow()

        val cashflowEntry = cashflowEntriesService.createFromBill(
            tenantId = tenantId,
            billId = UUID.fromString(bill.id.toString()),
            documentId = documentId,
            dueDate = dueDate,
            amountGross = bill.amount,
            amountVat = bill.vatAmount ?: Money.ZERO,
            contactId = linkedContactId
        ).getOrThrow()

        draftRepository.updateDocumentStatus(documentId, tenantId, DocumentStatus.Confirmed)

        logger.info("Bill confirmed: $documentId -> billId=${bill.id}, entryId=${cashflowEntry.id}")
        ConfirmationResult(entity = bill, cashflowEntryId = cashflowEntry.id, documentId = documentId)
    }
}
