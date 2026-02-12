package tech.dokus.features.ai.models

/**
 * Common counterparty fields shared by all financial extraction tool inputs.
 * Implementations must expose these properties so that [toCounterpartyExtraction]
 * can build a [CounterpartyExtraction] without per-document-type duplication.
 */
interface CounterpartyFields {
    val counterpartyName: String?
    val counterpartyVat: String?
    val counterpartyEmail: String?
    val counterpartyStreet: String?
    val counterpartyPostalCode: String?
    val counterpartyCity: String?
    val counterpartyCountry: String?
    val counterpartyRole: CounterpartyRole
    val counterpartyReasoning: String?
}

fun CounterpartyFields.toCounterpartyExtraction(): CounterpartyExtraction? {
    val hasAnyField = listOf(
        counterpartyName,
        counterpartyVat,
        counterpartyEmail,
        counterpartyStreet,
        counterpartyPostalCode,
        counterpartyCity,
        counterpartyCountry,
        counterpartyReasoning
    ).any { !it.isNullOrBlank() } || counterpartyRole != CounterpartyRole.Unknown

    if (!hasAnyField) return null

    return CounterpartyExtraction(
        name = counterpartyName,
        vatNumber = counterpartyVat,
        email = counterpartyEmail,
        streetLine1 = counterpartyStreet,
        postalCode = counterpartyPostalCode,
        city = counterpartyCity,
        country = counterpartyCountry,
        role = counterpartyRole,
        reasoning = counterpartyReasoning
    )
}
