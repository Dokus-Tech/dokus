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
    val addr = snapshot.address
    val hasData = addr.streetLine1 != null || addr.city != null ||
        addr.postalCode != null || addr.country != null
    if (!hasData) return null
    return AddressUiModel(
        streetLine1 = addr.streetLine1?.trim()?.takeIf { it.isNotEmpty() },
        streetLine2 = addr.streetLine2?.trim()?.takeIf { it.isNotEmpty() },
        city = addr.city?.trim()?.takeIf { it.isNotEmpty() },
        postalCode = addr.postalCode?.trim()?.takeIf { it.isNotEmpty() },
        country = addr.country?.dbValue?.trim()?.takeIf { it.isNotEmpty() },
    )
}
