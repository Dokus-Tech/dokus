package tech.dokus.features.ai.models

import tech.dokus.domain.Email
import tech.dokus.domain.enums.Country
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.CounterpartySnapshot

fun FinancialExtractionResult.toAuthoritativeCounterpartySnapshot(): CounterpartySnapshot? = when (this) {
    is FinancialExtractionResult.Invoice -> data.counterparty.toSnapshot()
    is FinancialExtractionResult.CreditNote -> data.counterparty.toSnapshot()
    is FinancialExtractionResult.Receipt -> data.counterparty.toSnapshot()
    is FinancialExtractionResult.Quote,
    is FinancialExtractionResult.ProForma,
    is FinancialExtractionResult.PurchaseOrder,
    is FinancialExtractionResult.Unsupported -> null
}

private fun CounterpartyExtraction?.toSnapshot(): CounterpartySnapshot? {
    if (this == null) return null

    val name = name.cleanText()
    val vat = vatNumber.cleanText()?.let { VatNumber.from(it) }?.takeIf { it.isValid }
    val email = email.cleanText()?.let { Email.from(it) }
    val streetLine1 = streetLine1.cleanText()
    val postalCode = postalCode.cleanText()
    val city = city.cleanText()
    val country = country.toCountryOrNull()

    val snapshot = CounterpartySnapshot(
        name = name,
        vatNumber = vat,
        iban = null,
        email = email,
        companyNumber = null,
        streetLine1 = streetLine1,
        streetLine2 = null,
        postalCode = postalCode,
        city = city,
        country = country
    )

    return snapshot.takeIf { it.name != null || it.vatNumber != null }
}

private fun String?.cleanText(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

private fun String?.toCountryOrNull(): Country? {
    val normalized = this
        ?.trim()
        ?.uppercase()
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    return when (normalized) {
        "BE", "BEL", "BELGIUM" -> Country.Belgium
        "NL", "NLD", "NETHERLANDS", "HOLLAND" -> Country.Netherlands
        "FR", "FRA", "FRANCE" -> Country.France
        else -> null
    }
}
