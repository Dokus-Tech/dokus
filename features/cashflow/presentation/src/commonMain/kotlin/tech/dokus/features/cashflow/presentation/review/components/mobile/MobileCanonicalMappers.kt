package tech.dokus.features.cashflow.presentation.review.components.mobile

import tech.dokus.domain.Money
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

internal fun lineItems(state: DocumentReviewState.Content): List<FinancialLineItem> =
    when (val data = state.draftData) {
        is InvoiceDraftData -> data.lineItems
        is CreditNoteDraftData -> data.lineItems
        else -> emptyList()
    }

internal fun currencySign(state: DocumentReviewState.Content): String =
    when (val data = state.draftData) {
        is InvoiceDraftData -> data.currency.displaySign
        is CreditNoteDraftData -> data.currency.displaySign
        else -> "\u20AC"
    }

internal fun lineAmount(item: FinancialLineItem, currencySign: String): String {
    val net = item.netAmount ?: item.unitPrice?.let { unit -> (item.quantity ?: 1L) * unit }
    return net?.let { "$currencySign${Money(it).toDisplayString()}" } ?: "\u2014"
}

internal fun DocumentReviewState.Content.primaryDescription(): String {
    return when (val data = draftData) {
        is InvoiceDraftData -> data.notes ?: "Invoice"
        is CreditNoteDraftData -> data.reason ?: data.notes ?: "Credit note"
        else -> description
    }
}

internal fun DocumentReviewState.Content.referenceNumber(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.invoiceNumber
    is CreditNoteDraftData -> data.creditNoteNumber
    else -> null
}

internal fun DocumentReviewState.Content.issueDate(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.issueDate?.toString()
    is CreditNoteDraftData -> data.issueDate?.toString()
    else -> null
}

internal fun DocumentReviewState.Content.dueDate(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.dueDate?.toString()
    else -> null
}

internal fun DocumentReviewState.Content.subtotalAmount() = when (val data = draftData) {
    is InvoiceDraftData -> data.subtotalAmount
    is CreditNoteDraftData -> data.subtotalAmount
    else -> null
}

internal fun DocumentReviewState.Content.vatAmount() = when (val data = draftData) {
    is InvoiceDraftData -> data.vatAmount
    is CreditNoteDraftData -> data.vatAmount
    else -> null
}

internal fun DocumentReviewState.Content.bankDetails(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.iban?.value
    else -> null
}

internal fun DocumentReviewState.Content.notes(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.notes
    is CreditNoteDraftData -> data.notes
    else -> null
}
