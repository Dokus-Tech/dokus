package tech.dokus.features.cashflow.presentation.review.models

import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

internal fun counterpartyInfo(state: DocumentReviewState.Content): CounterpartyInfo {
    return when (val draft = state.draftData) {
        is InvoiceDraftData -> CounterpartyInfo(
            name = draft.customerName?.trim()?.takeIf { it.isNotEmpty() },
            vatNumber = draft.customerVat?.value,
            iban = draft.iban?.value,
            address = null,
        )
        is BillDraftData -> CounterpartyInfo(
            name = draft.supplierName?.trim()?.takeIf { it.isNotEmpty() },
            vatNumber = draft.supplierVat?.value,
            iban = draft.iban?.value,
            address = null,
        )
        is ReceiptDraftData -> CounterpartyInfo(
            name = draft.merchantName?.trim()?.takeIf { it.isNotEmpty() },
            vatNumber = draft.merchantVat?.value,
            iban = null,
            address = null,
        )
        is CreditNoteDraftData -> CounterpartyInfo(
            name = draft.counterpartyName?.trim()?.takeIf { it.isNotEmpty() },
            vatNumber = draft.counterpartyVat?.value,
            iban = null,
            address = null,
        )
        null -> CounterpartyInfo(
            name = null,
            vatNumber = null,
            iban = null,
            address = null,
        )
    }
}
