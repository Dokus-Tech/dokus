package tech.dokus.features.cashflow.presentation.review.models

import tech.dokus.domain.model.contact.isUnresolved
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

internal fun counterpartyInfo(state: DocumentReviewState): CounterpartyInfo {
    val counterparty = state.documentRecord?.draft?.counterparty
    val snapshot = if (counterparty.isUnresolved()) counterparty.snapshot else null
    if (snapshot == null) {
        return CounterpartyInfo(
            name = null,
            vatNumber = null,
            iban = null,
            address = null,
        )
    }

    return CounterpartyInfo(
        name = snapshot.name?.trim()?.takeIf { it.isNotEmpty() },
        vatNumber = snapshot.vatNumber?.value,
        iban = snapshot.iban?.value,
        address = buildAddress(snapshot),
    )
}

private fun buildAddress(snapshot: tech.dokus.domain.model.contact.CounterpartySnapshot): String? {
    val parts = listOfNotNull(
        snapshot.streetLine1?.trim()?.takeIf { it.isNotEmpty() },
        listOfNotNull(
            snapshot.postalCode?.trim()?.takeIf { it.isNotEmpty() },
            snapshot.city?.trim()?.takeIf { it.isNotEmpty() }
        ).takeIf { it.isNotEmpty() }?.joinToString(" "),
        snapshot.country?.dbValue?.trim()?.takeIf { it.isNotEmpty() },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
}
