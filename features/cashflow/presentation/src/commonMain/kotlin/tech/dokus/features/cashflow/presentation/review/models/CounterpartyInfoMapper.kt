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
        address = toAddressUiModel(snapshot),
    )
}

private fun toAddressUiModel(snapshot: tech.dokus.domain.model.contact.CounterpartySnapshot): AddressUiModel? {
    val hasData = snapshot.streetLine1 != null || snapshot.city != null ||
        snapshot.postalCode != null || snapshot.country != null
    if (!hasData) return null
    return AddressUiModel(
        streetLine1 = snapshot.streetLine1?.trim()?.takeIf { it.isNotEmpty() },
        streetLine2 = null,
        city = snapshot.city?.trim()?.takeIf { it.isNotEmpty() },
        postalCode = snapshot.postalCode?.trim()?.takeIf { it.isNotEmpty() },
        country = snapshot.country?.dbValue?.trim()?.takeIf { it.isNotEmpty() },
    )
}
