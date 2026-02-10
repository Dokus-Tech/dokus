package tech.dokus.backend.services.documents.confirmation

import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import tech.dokus.database.repository.cashflow.BillRepository
import tech.dokus.database.tables.cashflow.BillsTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID

/**
 * Confirms Bill documents: creates Bill entity + CashflowEntry (Direction.Out).
 */
class BillConfirmationService(
    private val billRepository: BillRepository,
) {
    private val logger = loggerFor()

    @Suppress("ThrowsCount")
    suspend fun confirm(
        tenantId: TenantId,
        documentId: DocumentId,
        draftData: BillDraftData,
        linkedContactId: ContactId?
    ): Result<ConfirmationResult> = runCatching {
        logger.info("Confirming bill document: $documentId for tenant: $tenantId")

        val (billId, cashflowEntryId) = dbQuery {
            ensureDraftConfirmable(tenantId, documentId)

            val issueDate = draftData.issueDate ?: throw DokusException.BadRequest("Issue date is required")
            val dueDate = draftData.dueDate ?: issueDate
            val subtotalAmount = draftData.subtotalAmount
            val vatAmount = draftData.vatAmount
            val amount = draftData.totalAmount ?: subtotalAmount
                ?: throw DokusException.BadRequest("Amount is required")
            val category = ExpenseCategory.Other
            val vatRate = if (subtotalAmount != null && vatAmount != null && !subtotalAmount.isZero) {
                VatRate(((vatAmount.minor * 10000L) / subtotalAmount.minor).toInt())
            } else {
                null
            }

            val billId = BillsTable.insertAndGetId {
                it[BillsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[BillsTable.supplierName] = draftData.supplierName ?: "Unknown Supplier"
                it[BillsTable.supplierVatNumber] = draftData.supplierVat?.value
                it[BillsTable.invoiceNumber] = draftData.invoiceNumber
                it[BillsTable.issueDate] = issueDate
                it[BillsTable.dueDate] = dueDate
                it[BillsTable.amount] = amount.toDbDecimal()
                it[BillsTable.vatAmount] = vatAmount?.toDbDecimal()
                it[BillsTable.vatRate] = vatRate?.toDbDecimal()
                it[BillsTable.status] = BillStatus.Pending
                it[BillsTable.category] = category
                it[BillsTable.description] = null
                it[BillsTable.notes] = draftData.notes
                it[BillsTable.currency] = draftData.currency
                it[BillsTable.documentId] = UUID.fromString(documentId.toString())
                it[BillsTable.contactId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
            }.value

            val entryId = CashflowEntriesTable.insertAndGetId {
                it[CashflowEntriesTable.tenantId] = UUID.fromString(tenantId.toString())
                it[CashflowEntriesTable.sourceType] = CashflowSourceType.Bill
                it[CashflowEntriesTable.sourceId] = billId
                it[CashflowEntriesTable.documentId] = UUID.fromString(documentId.toString())
                it[CashflowEntriesTable.direction] = CashflowDirection.Out
                it[CashflowEntriesTable.eventDate] = dueDate
                it[CashflowEntriesTable.amountGross] = amount.toDbDecimal()
                it[CashflowEntriesTable.amountVat] = (draftData.vatAmount ?: Money.ZERO).toDbDecimal()
                it[CashflowEntriesTable.remainingAmount] = amount.toDbDecimal()
                it[CashflowEntriesTable.status] = CashflowEntryStatus.Open
                it[CashflowEntriesTable.counterpartyId] = linkedContactId?.let { id -> UUID.fromString(id.toString()) }
            }.value

            markDraftConfirmed(tenantId, documentId)

            billId to CashflowEntryId.parse(entryId.toString())
        }

        // Fetch full entity outside transaction
        val bill = billRepository.getBill(
            billId = BillId.parse(billId.toString()),
            tenantId = tenantId
        ).getOrThrow() ?: throw DokusException.InternalError("Bill not found after confirmation")

        logger.info("Bill confirmed: $documentId -> billId=$billId, entryId=$cashflowEntryId")
        ConfirmationResult(entity = bill, cashflowEntryId = cashflowEntryId, documentId = documentId)
    }
}
