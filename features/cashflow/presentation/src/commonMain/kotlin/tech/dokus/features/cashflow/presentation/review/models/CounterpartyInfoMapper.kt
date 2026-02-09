package tech.dokus.features.cashflow.presentation.review.models

import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

internal fun counterpartyInfo(state: DocumentReviewState.Content): CounterpartyInfo {
    fun clean(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    return when (state.editableData.documentType) {
        DocumentType.Invoice -> CounterpartyInfo(
            name = clean(state.editableData.invoice?.customerName),
            vatNumber = clean(state.editableData.invoice?.customerVatNumber),
            address = null,
        )
        DocumentType.Bill -> CounterpartyInfo(
            name = clean(state.editableData.bill?.supplierName),
            vatNumber = clean(state.editableData.bill?.supplierVatNumber),
            address = null,
        )
        DocumentType.Receipt -> CounterpartyInfo(
            name = clean(state.editableData.receipt?.merchantName),
            vatNumber = clean(state.editableData.receipt?.merchantVatNumber),
            address = null,
        )
        DocumentType.CreditNote -> CounterpartyInfo(
            name = clean(state.editableData.creditNote?.counterpartyName),
            vatNumber = clean(state.editableData.creditNote?.counterpartyVatNumber),
            address = null,
        )
        else -> CounterpartyInfo(
            name = null,
            vatNumber = null,
            address = null,
        )
    }
}
