package tech.dokus.features.cashflow.presentation.review.route

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.presentation.documents.components.computeNeedsAttention
import tech.dokus.features.cashflow.presentation.documents.components.resolveCounterparty
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.app.shell.DocQueueStatus
import tech.dokus.foundation.app.shell.DocQueueStatusDetail

internal fun DocumentRecordDto.toDocQueueItem(): DocQueueItem {
    val vendorName = resolveCounterparty(this, "\u2014").ifBlank { "\u2014" }
    val queueAmount = extractQueueAmount(this)
    val status = extractQueueStatus(this)
    val statusDetail = extractQueueStatusDetail(this, status)
    return DocQueueItem(
        id = document.id,
        vendorName = DisplayName(vendorName),
        date = extractQueueDate(this),
        amount = queueAmount?.amount,
        currency = queueAmount?.currency ?: Currency.default,
        status = status,
        statusDetail = statusDetail,
    )
}

private fun extractQueueStatus(doc: DocumentRecordDto): DocQueueStatus {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val invoiceEntity = doc.confirmedEntity as? FinancialDocumentDto.InvoiceDto
    val draftDueDate = (doc.draft?.extractedData as? InvoiceDraftData)?.dueDate
    val ingestionStatus = doc.latestIngestion?.status

    return when {
        computeNeedsAttention(doc) -> DocQueueStatus.Review
        invoiceEntity != null &&
            (invoiceEntity.paidAt != null || invoiceEntity.paidAmount >= invoiceEntity.totalAmount) ->
            DocQueueStatus.Paid
        invoiceEntity != null && invoiceEntity.dueDate < today -> DocQueueStatus.Overdue
        invoiceEntity != null -> DocQueueStatus.Unpaid
        draftDueDate != null && draftDueDate < today -> DocQueueStatus.Overdue
        ingestionStatus == IngestionStatus.Queued || ingestionStatus == IngestionStatus.Processing ->
            DocQueueStatus.Processing
        else -> DocQueueStatus.Unpaid
    }
}

private fun extractQueueStatusDetail(
    doc: DocumentRecordDto,
    status: DocQueueStatus
): DocQueueStatusDetail? {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (status) {
        DocQueueStatus.Paid,
        DocQueueStatus.Unpaid -> null
        DocQueueStatus.Review -> {
            val ingestion = doc.latestIngestion?.status
            if (ingestion == IngestionStatus.Queued || ingestion == IngestionStatus.Processing) {
                DocQueueStatusDetail.Processing
            } else {
                null
            }
        }
        DocQueueStatus.Overdue -> {
            val dueDate = (doc.confirmedEntity as? FinancialDocumentDto.InvoiceDto)?.dueDate
                ?: (doc.draft?.extractedData as? InvoiceDraftData)?.dueDate
            if (dueDate != null && dueDate < today) {
                DocQueueStatusDetail.OverdueDays(dueDate.daysUntil(today))
            } else {
                null
            }
        }
        DocQueueStatus.Processing -> DocQueueStatusDetail.Processing
    }
}

private fun extractQueueDate(doc: DocumentRecordDto): LocalDate {
    val data = doc.draft?.extractedData
    return when (data) {
        is InvoiceDraftData -> data.issueDate
        is ReceiptDraftData -> data.date
        is CreditNoteDraftData -> data.issueDate
        else -> null
    } ?: doc.document.uploadedAt.date
}

private data class QueueAmount(
    val amount: Money,
    val currency: Currency,
)

private fun extractQueueAmount(doc: DocumentRecordDto): QueueAmount? {
    val data = doc.draft?.extractedData
    val amount: Money? = when (data) {
        is InvoiceDraftData -> data.totalAmount
        is ReceiptDraftData -> data.totalAmount
        is CreditNoteDraftData -> data.totalAmount
        else -> null
    }
    if (amount == null) return null
    val currency = when (data) {
        is InvoiceDraftData -> data.currency
        is ReceiptDraftData -> data.currency
        is CreditNoteDraftData -> data.currency
        else -> null
    }
    return QueueAmount(
        amount = amount,
        currency = currency ?: Currency.default,
    )
}
