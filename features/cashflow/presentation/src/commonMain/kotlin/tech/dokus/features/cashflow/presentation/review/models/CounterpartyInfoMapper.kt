package tech.dokus.features.cashflow.presentation.review.models

import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

internal fun counterpartyInfo(state: DocumentReviewState.Content): CounterpartyInfo {
    return when (val draft = state.draftData) {
        is InvoiceDraftData -> {
            val (name, vat, iban) = when (draft.direction) {
                DocumentDirection.Inbound -> Triple(
                    draft.seller.name ?: draft.customerName,
                    draft.seller.vat ?: draft.customerVat,
                    draft.seller.iban ?: draft.iban
                )
                DocumentDirection.Outbound -> Triple(
                    draft.buyer.name ?: draft.customerName,
                    draft.buyer.vat ?: draft.customerVat,
                    draft.buyer.iban ?: draft.iban
                )
                DocumentDirection.Unknown -> Triple(
                    draft.customerName ?: draft.buyer.name ?: draft.seller.name,
                    draft.customerVat ?: draft.buyer.vat ?: draft.seller.vat,
                    draft.iban ?: draft.buyer.iban ?: draft.seller.iban
                )
            }

            CounterpartyInfo(
                name = name?.trim()?.takeIf { it.isNotEmpty() },
                vatNumber = vat?.value,
                iban = iban?.value,
                address = null,
            )
        }
        is BillDraftData -> CounterpartyInfo(
            name = (draft.supplierName ?: draft.seller.name)?.trim()?.takeIf { it.isNotEmpty() },
            vatNumber = (draft.supplierVat ?: draft.seller.vat)?.value,
            iban = (draft.iban ?: draft.seller.iban)?.value,
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
