package tech.dokus.features.cashflow.presentation.review.models

import androidx.compose.runtime.Immutable

@Immutable
data class AddressUiModel(
    val streetLine1: String?,
    val streetLine2: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
) {
    val formatted: String? = run {
        val parts = listOfNotNull(
            streetLine1?.trim()?.takeIf { it.isNotEmpty() },
            listOfNotNull(
                postalCode?.trim()?.takeIf { it.isNotEmpty() },
                city?.trim()?.takeIf { it.isNotEmpty() },
            ).takeIf { it.isNotEmpty() }?.joinToString(" "),
            country?.trim()?.takeIf { it.isNotEmpty() },
        )
        parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }
}
