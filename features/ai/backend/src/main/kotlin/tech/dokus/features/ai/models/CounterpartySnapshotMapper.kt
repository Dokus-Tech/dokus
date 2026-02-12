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

    val cleanedName = name.cleanText()
    val cleanedVat = vatNumber.cleanText()?.let { VatNumber.from(it) }?.takeIf { it.isValid }
    val cleanedEmail = email.cleanText()?.let { Email.from(it) }
    val cleanedStreet = streetLine1.cleanText()
    val cleanedPostal = postalCode.cleanText()
    val cleanedCity = city.cleanText()
    val cleanedCountry = country.toCountryOrNull()

    val snapshot = CounterpartySnapshot(
        name = cleanedName,
        vatNumber = cleanedVat,
        iban = null,
        email = cleanedEmail,
        companyNumber = null,
        streetLine1 = cleanedStreet,
        streetLine2 = null,
        postalCode = cleanedPostal,
        city = cleanedCity,
        country = cleanedCountry
    )

    return snapshot.takeIf { it.name != null || it.vatNumber != null }
}

private fun String?.cleanText(): String? = this
    ?.trim()
    ?.takeIf { it.isNotEmpty() }

private val countryAliases: Map<String, Country> = buildMap {
    for (country in Country.entries) {
        put(country.dbValue, country)
    }
    // Common AI-extracted aliases beyond the ISO-2 dbValue
    put("BEL", Country.Belgium)
    put("BELGIUM", Country.Belgium)
    put("BELGIQUE", Country.Belgium)
    put("NLD", Country.Netherlands)
    put("NETHERLANDS", Country.Netherlands)
    put("HOLLAND", Country.Netherlands)
    put("FRA", Country.France)
    put("FRANCE", Country.France)
}

private fun String?.toCountryOrNull(): Country? {
    val normalized = this
        ?.trim()
        ?.uppercase()
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    return countryAliases[normalized]
}
