package tech.dokus.features.cashflow.presentation.review.route

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import tech.dokus.domain.Money
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.presentation.documents.components.computeNeedsAttention
import tech.dokus.features.cashflow.presentation.documents.components.resolveCounterparty
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.app.shell.DocQueueStatus

private val MonthAbbreviations = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

internal fun DocumentRecordDto.toDocQueueItem(): DocQueueItem {
    val vendorName = resolveCounterparty(this, "\u2014")
    val date = extractQueueDate(this)
    val amount = extractQueueAmount(this)
    val status = extractQueueStatus(this)
    val statusDetail = extractQueueStatusDetail(this, status)
    return DocQueueItem(
        id = document.id,
        vendorName = vendorName,
        date = "${date.day} ${MonthAbbreviations[date.month.ordinal]}",
        amount = amount,
        status = status,
        statusDetail = statusDetail,
    )
}

private fun extractQueueStatus(doc: DocumentRecordDto): DocQueueStatus {
    if (computeNeedsAttention(doc)) return DocQueueStatus.Review

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val invoiceEntity = doc.confirmedEntity as? FinancialDocumentDto.InvoiceDto
    if (invoiceEntity != null) {
        if (invoiceEntity.paidAt != null || invoiceEntity.paidAmount >= invoiceEntity.totalAmount) {
            return DocQueueStatus.Paid
        }
        return if (invoiceEntity.dueDate < today) {
            DocQueueStatus.Overdue
        } else {
            DocQueueStatus.Unpaid
        }
    }

    val dueDate = (doc.draft?.extractedData as? InvoiceDraftData)?.dueDate
    if (dueDate != null && dueDate < today) {
        return DocQueueStatus.Overdue
    }
    return DocQueueStatus.Unpaid
}

private fun extractQueueStatusDetail(doc: DocumentRecordDto, status: DocQueueStatus): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (status) {
        DocQueueStatus.Paid -> "Paid"
        DocQueueStatus.Unpaid -> "Unpaid"
        DocQueueStatus.Review -> {
            val ingestion = doc.latestIngestion?.status
            if (ingestion == IngestionStatus.Queued || ingestion == IngestionStatus.Processing) {
                "Processing"
            } else {
                "Review"
            }
        }
        DocQueueStatus.Overdue -> {
            val dueDate = (doc.confirmedEntity as? FinancialDocumentDto.InvoiceDto)?.dueDate
                ?: (doc.draft?.extractedData as? InvoiceDraftData)?.dueDate
            if (dueDate != null && dueDate < today) {
                "${dueDate.daysUntil(today)}d"
            } else {
                "Overdue"
            }
        }
        DocQueueStatus.Processing -> "Processing"
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

private fun extractQueueAmount(doc: DocumentRecordDto): String {
    val data = doc.draft?.extractedData
    val amount: Money? = when (data) {
        is InvoiceDraftData -> data.totalAmount
        is ReceiptDraftData -> data.totalAmount
        is CreditNoteDraftData -> data.totalAmount
        else -> null
    }
    if (amount == null) return "\u2014"
    val currency = when (data) {
        is InvoiceDraftData -> data.currency
        is ReceiptDraftData -> data.currency
        is CreditNoteDraftData -> data.currency
        else -> null
    }
    return "${currency?.displaySign ?: "\u20AC"}${amount.toDisplayString()}"
}
