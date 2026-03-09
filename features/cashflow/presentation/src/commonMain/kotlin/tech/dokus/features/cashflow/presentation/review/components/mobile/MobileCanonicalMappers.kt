package tech.dokus.features.cashflow.presentation.review.components.mobile

import tech.dokus.domain.Money
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

internal fun lineItems(state: DocumentReviewState): List<FinancialLineItem> =
    when (val data = state.draftData) {
        is InvoiceDraftData -> data.lineItems
        is CreditNoteDraftData -> data.lineItems
        else -> emptyList()
    }

internal fun currencySign(state: DocumentReviewState): String =
    when (val data = state.draftData) {
        is InvoiceDraftData -> data.currency.displaySign
        is CreditNoteDraftData -> data.currency.displaySign
        else -> "\u20AC"
    }

internal fun lineAmount(item: FinancialLineItem, currencySign: String): String {
    val net = item.netAmount ?: item.unitPrice?.let { unit -> (item.quantity ?: 1L) * unit }
    return net?.let { "$currencySign${Money(it).toDisplayString()}" } ?: "\u2014"
}

internal fun DocumentReviewState.primaryDescription(): String {
    return when (val data = draftData) {
        is InvoiceDraftData -> data.notes ?: "Invoice"
        is CreditNoteDraftData -> data.reason ?: data.notes ?: "Credit note"
        else -> description
    }
}

internal fun DocumentReviewState.referenceNumber(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.invoiceNumber
    is CreditNoteDraftData -> data.creditNoteNumber
    else -> null
}

internal fun DocumentReviewState.issueDate(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.issueDate?.toString()
    is CreditNoteDraftData -> data.issueDate?.toString()
    else -> null
}

internal fun DocumentReviewState.dueDate(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.dueDate?.toString()
    else -> null
}

internal fun DocumentReviewState.subtotalAmount() = when (val data = draftData) {
    is InvoiceDraftData -> data.subtotalAmount
    is CreditNoteDraftData -> data.subtotalAmount
    else -> null
}

internal fun DocumentReviewState.vatAmount() = when (val data = draftData) {
    is InvoiceDraftData -> data.vatAmount
    is CreditNoteDraftData -> data.vatAmount
    else -> null
}

internal fun DocumentReviewState.bankDetails(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.iban?.value
    else -> null
}

internal fun DocumentReviewState.notes(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.notes
    is CreditNoteDraftData -> data.notes
    else -> null
}
