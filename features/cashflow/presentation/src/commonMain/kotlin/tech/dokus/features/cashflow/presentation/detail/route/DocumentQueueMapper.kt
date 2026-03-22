package tech.dokus.features.cashflow.presentation.detail.route

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.currency
import tech.dokus.domain.model.sortDate
import tech.dokus.domain.model.totalAmount
import tech.dokus.features.cashflow.presentation.documents.components.computeNeedsAttention
import tech.dokus.features.cashflow.presentation.documents.components.resolveCounterparty
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.app.shell.DocQueueStatus
import tech.dokus.foundation.app.shell.DocQueueStatusDetail
import kotlin.time.Clock

internal fun DocumentDetailDto.toDocQueueItem(): DocQueueItem {
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

internal fun DocumentListItemDto.toDocQueueItem(): DocQueueItem {
    val vendorName = counterpartyDisplayName?.takeIf { it.isNotBlank() } ?: "\u2014"
    val status = extractListItemQueueStatus(this)
    val statusDetail = if (status == DocQueueStatus.Processing) DocQueueStatusDetail.Processing else null
    return DocQueueItem(
        id = documentId,
        vendorName = DisplayName(vendorName),
        date = sortDate,
        amount = totalAmount,
        currency = currency ?: Currency.default,
        status = status,
        statusDetail = statusDetail,
    )
}

private fun extractListItemQueueStatus(item: DocumentListItemDto): DocQueueStatus {
    val ingestion = item.ingestionStatus
    return when {
        ingestion == IngestionStatus.Queued || ingestion == IngestionStatus.Processing ->
            DocQueueStatus.Processing
        ingestion == null -> DocQueueStatus.Processing
        item.documentStatus == DocumentStatus.NeedsReview || item.documentStatus == null ->
            DocQueueStatus.Review
        item.documentStatus == DocumentStatus.Confirmed -> DocQueueStatus.Unpaid
        else -> DocQueueStatus.Review
    }
}

private fun extractQueueStatus(doc: DocumentDetailDto): DocQueueStatus {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val confirmedInvoice = doc.draft?.content as? DocDto.Invoice.Confirmed
    val draftDueDate = (doc.draft?.content as? DocDto.Invoice)?.dueDate
    val ingestionStatus = doc.latestIngestion?.status

    return when {
        computeNeedsAttention(doc) -> DocQueueStatus.Review
        confirmedInvoice != null &&
            (confirmedInvoice.paymentInfo != null ||
                confirmedInvoice.totalAmount?.let { confirmedInvoice.paidAmount >= it } == true) ->
            DocQueueStatus.Paid
        confirmedInvoice != null && confirmedInvoice.dueDate?.let { it < today } == true ->
            DocQueueStatus.Overdue
        confirmedInvoice != null -> DocQueueStatus.Unpaid
        draftDueDate != null && draftDueDate < today -> DocQueueStatus.Overdue
        ingestionStatus == IngestionStatus.Queued || ingestionStatus == IngestionStatus.Processing ->
            DocQueueStatus.Processing
        else -> DocQueueStatus.Unpaid
    }
}

private fun extractQueueStatusDetail(
    doc: DocumentDetailDto,
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
            val dueDate = (doc.draft?.content as? DocDto.Invoice)?.dueDate
            if (dueDate != null && dueDate < today) {
                DocQueueStatusDetail.OverdueDays(dueDate.daysUntil(today))
            } else {
                null
            }
        }
        DocQueueStatus.Processing -> DocQueueStatusDetail.Processing
    }
}

private fun extractQueueDate(doc: DocumentDetailDto): LocalDate {
    return doc.draft?.content?.sortDate ?: doc.document.uploadedAt.date
}

private data class QueueAmount(
    val amount: Money,
    val currency: Currency,
)

private fun extractQueueAmount(doc: DocumentDetailDto): QueueAmount? {
    val content = doc.draft?.content ?: return null
    val amount = content.totalAmount ?: return null
    return QueueAmount(
        amount = amount,
        currency = content.currency,
    )
}
