package tech.dokus.features.ai.models

import tech.dokus.domain.Email
import tech.dokus.domain.enums.Country
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.CounterpartySnapshotDto
import tech.dokus.domain.model.contact.PostalAddressDto
import tech.dokus.features.ai.graph.sub.extraction.financial.CreditNoteExtractionResult
import tech.dokus.features.ai.graph.sub.extraction.financial.InvoiceExtractionResult

/**
 * Build the authoritative counterparty snapshot from extraction data.
 *
 * For invoices and credit notes, when [resolvedDirection] is known (INBOUND/OUTBOUND),
 * we use the seller/buyer fields directly — the AI's `counterparty` block is an LLM
 * opinion that can be wrong (e.g., picking the tenant as counterparty).
 *
 * Falls back to the AI counterparty field when direction is unknown or seller/buyer
 * data is insufficient.
 */
fun FinancialExtractionResult.toAuthoritativeCounterpartySnapshot(
    resolvedDirection: DocumentDirection = DocumentDirection.Unknown,
): CounterpartySnapshotDto? = when (this) {
    is FinancialExtractionResult.Invoice ->
        data.directionAwareSnapshot(resolvedDirection) ?: data.counterparty.toSnapshot()
    is FinancialExtractionResult.CreditNote ->
        data.directionAwareSnapshot(resolvedDirection) ?: data.counterparty.toSnapshot()
    is FinancialExtractionResult.Receipt -> data.counterparty.toSnapshot()
    is FinancialExtractionResult.BankStatement -> data.institutionName
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let { CounterpartySnapshotDto(name = it) }
    is FinancialExtractionResult.Quote,
    is FinancialExtractionResult.ProForma,
    is FinancialExtractionResult.PurchaseOrder,
    is FinancialExtractionResult.Unsupported -> null
}

/**
 * For invoices: use seller fields when INBOUND, buyer fields when OUTBOUND.
 * Returns null when direction is unknown or the chosen party has no usable data.
 */
private fun InvoiceExtractionResult.directionAwareSnapshot(
    direction: DocumentDirection,
): CounterpartySnapshotDto? {
    return when (direction) {
        DocumentDirection.Inbound -> sellerSnapshot()
        DocumentDirection.Outbound -> buyerSnapshot()
        DocumentDirection.Unknown,
        DocumentDirection.Neutral -> null
    }
}

/**
 * For credit notes: use seller fields when INBOUND, buyer fields when OUTBOUND.
 * Returns null when direction is unknown or the chosen party has no usable data.
 */
private fun CreditNoteExtractionResult.directionAwareSnapshot(
    direction: DocumentDirection,
): CounterpartySnapshotDto? {
    val name: String?
    val vat: VatNumber?
    when (direction) {
        DocumentDirection.Inbound -> { name = sellerName; vat = sellerVat }
        DocumentDirection.Outbound -> { name = buyerName; vat = buyerVat }
        DocumentDirection.Unknown,
        DocumentDirection.Neutral -> return null
    }
    val snapshot = CounterpartySnapshotDto(name = name.cleanText(), vatNumber = vat)
    return snapshot.takeIf { it.name != null || it.vatNumber != null }
}

private fun InvoiceExtractionResult.sellerSnapshot(): CounterpartySnapshotDto? {
    val snapshot = CounterpartySnapshotDto(
        name = sellerName.cleanText(),
        vatNumber = sellerVat,
        email = sellerEmail,
        address = PostalAddressDto(
            streetLine1 = sellerStreet.cleanText(),
            postalCode = sellerPostalCode.cleanText(),
            city = sellerCity.cleanText(),
            country = sellerCountry.toCountryOrNull(),
        ),
    )
    return snapshot.takeIf { it.name != null || it.vatNumber != null }
}

private fun InvoiceExtractionResult.buyerSnapshot(): CounterpartySnapshotDto? {
    val snapshot = CounterpartySnapshotDto(
        name = buyerName.cleanText(),
        vatNumber = buyerVat,
        email = buyerEmail,
        address = PostalAddressDto(
            streetLine1 = buyerStreet.cleanText(),
            postalCode = buyerPostalCode.cleanText(),
            city = buyerCity.cleanText(),
            country = buyerCountry.toCountryOrNull(),
        ),
    )
    return snapshot.takeIf { it.name != null || it.vatNumber != null }
}

private fun CounterpartyExtraction?.toSnapshot(): CounterpartySnapshotDto? {
    if (this == null) return null

    val cleanedName = name.cleanText()
    val cleanedVat = vatNumber.cleanText()?.let { VatNumber.tryNormalize(it) }
    val cleanedEmail = email.cleanText()?.let { Email.from(it) }
    val cleanedStreet = streetLine1.cleanText()
    val cleanedPostal = postalCode.cleanText()
    val cleanedCity = city.cleanText()
    val cleanedCountry = country.toCountryOrNull()

    val snapshot = CounterpartySnapshotDto(
        name = cleanedName,
        vatNumber = cleanedVat,
        iban = null,
        email = cleanedEmail,
        companyNumber = null,
        address = PostalAddressDto(
            streetLine1 = cleanedStreet,
            postalCode = cleanedPostal,
            city = cleanedCity,
            country = cleanedCountry
        )
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
