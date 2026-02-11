package tech.dokus.features.cashflow.presentation.review.models

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
                    draft.buyer.name ?: draft.seller.name ?: draft.customerName,
                    draft.buyer.vat ?: draft.seller.vat ?: draft.customerVat,
                    draft.buyer.iban ?: draft.seller.iban ?: draft.iban
                )
            }

            CounterpartyInfo(
                name = name?.trim()?.takeIf { it.isNotEmpty() },
                vatNumber = vat?.value,
                iban = iban?.value,
                address = null,
            )
        }
        is ReceiptDraftData -> CounterpartyInfo(
            name = draft.merchantName?.trim()?.takeIf { it.isNotEmpty() },
            vatNumber = draft.merchantVat?.value,
            iban = null,
            address = null,
        )
        is CreditNoteDraftData -> {
            val (name, vat) = when (draft.direction) {
                DocumentDirection.Inbound -> Pair(
                    draft.seller.name ?: draft.counterpartyName,
                    draft.seller.vat ?: draft.counterpartyVat
                )
                DocumentDirection.Outbound -> Pair(
                    draft.buyer.name ?: draft.counterpartyName,
                    draft.buyer.vat ?: draft.counterpartyVat
                )
                DocumentDirection.Unknown -> Pair(
                    draft.buyer.name ?: draft.seller.name ?: draft.counterpartyName,
                    draft.buyer.vat ?: draft.seller.vat ?: draft.counterpartyVat
                )
            }

            CounterpartyInfo(
                name = name?.trim()?.takeIf { it.isNotEmpty() },
                vatNumber = vat?.value,
                iban = null,
                address = null,
            )
        }
        null -> CounterpartyInfo(
            name = null,
            vatNumber = null,
            iban = null,
            address = null,
        )
    }
}
