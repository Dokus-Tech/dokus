package tech.dokus.features.cashflow.presentation.review.models

import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

internal fun counterpartyInfo(state: DocumentReviewState.Content): CounterpartyInfo {
    fun clean(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }

    return when (state.editableData.documentType) {
        DocumentType.Invoice -> CounterpartyInfo(
            name = clean(state.editableData.invoice?.clientName),
            vatNumber = clean(state.editableData.invoice?.clientVatNumber),
            address = clean(state.editableData.invoice?.clientAddress),
        )
        DocumentType.Bill -> CounterpartyInfo(
            name = clean(state.editableData.bill?.supplierName),
            vatNumber = clean(state.editableData.bill?.supplierVatNumber),
            address = clean(state.editableData.bill?.supplierAddress),
        )
        else -> CounterpartyInfo(
            name = null,
            vatNumber = null,
            address = null,
        )
    }
}
