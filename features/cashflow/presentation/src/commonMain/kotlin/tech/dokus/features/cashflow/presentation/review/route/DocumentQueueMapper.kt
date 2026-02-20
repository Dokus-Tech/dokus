package tech.dokus.features.cashflow.presentation.review.route

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.presentation.documents.components.computeNeedsAttention
import tech.dokus.features.cashflow.presentation.documents.components.resolveCounterparty
import tech.dokus.foundation.app.shell.DocQueueItem

private val MonthAbbreviations = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

internal fun DocumentRecordDto.toDocQueueItem(): DocQueueItem {
    val vendorName = resolveCounterparty(this, "\u2014")
    val date = extractQueueDate(this)
    val amount = extractQueueAmount(this)
    val isConfirmed = !computeNeedsAttention(this)
    return DocQueueItem(
        id = document.id,
        vendorName = vendorName,
        date = "${date.day} ${MonthAbbreviations[date.month.ordinal]}",
        amount = amount,
        isConfirmed = isConfirmed,
    )
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
