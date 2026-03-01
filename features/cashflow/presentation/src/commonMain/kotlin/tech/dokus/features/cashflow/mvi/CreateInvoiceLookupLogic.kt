package tech.dokus.features.cashflow.mvi

import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate

internal const val CLIENT_LOOKUP_DEBOUNCE_MS = 300L
internal const val CLIENT_LOOKUP_LOCAL_MIN_QUERY_LENGTH = 2
internal const val CLIENT_LOOKUP_EXTERNAL_MIN_QUERY_LENGTH = 3
internal const val CLIENT_LOOKUP_LOCAL_LIMIT = 8
internal const val CLIENT_LOOKUP_EXTERNAL_LIMIT = 6

internal fun shouldLookupLocalClient(query: String): Boolean {
    return query.length >= CLIENT_LOOKUP_LOCAL_MIN_QUERY_LENGTH
}

internal fun shouldLookupExternalClient(query: String): Boolean {
    val vat = VatNumber.from(query)
    return vat?.isValid == true || query.length >= CLIENT_LOOKUP_EXTERNAL_MIN_QUERY_LENGTH
}

internal fun mergeClientLookupSuggestions(
    query: String,
    normalizedVat: VatNumber?,
    localResults: List<ContactDto>,
    externalResults: List<ExternalClientCandidate>
): List<ClientSuggestion> {
    val normalizedQuery = query.lowercase()
    val seenKeys = mutableSetOf<String>()
    val suggestions = mutableListOf<ClientSuggestion>()

    val orderedLocal = localResults.sortedWith(
        compareByDescending<ContactDto> {
            normalizedVat != null && it.vatNumber?.normalized == normalizedVat.normalized
        }.thenByDescending {
            it.name.value.lowercase() == normalizedQuery
        }.thenBy {
            it.name.value
        }
    )

    val orderedExternal = externalResults.sortedWith(
        compareByDescending<ExternalClientCandidate> {
            normalizedVat != null && it.vatNumber?.normalized == normalizedVat.normalized
        }.thenByDescending {
            it.name.lowercase() == normalizedQuery
        }.thenBy {
            it.name
        }
    )

    orderedLocal.forEach { contact ->
        val key = contact.vatNumber?.normalized ?: contact.name.value.lowercase()
        if (seenKeys.add(key)) {
            suggestions += ClientSuggestion.LocalContact(contact)
        }
    }

    orderedExternal.forEach { candidate ->
        val key = candidate.vatNumber?.normalized ?: candidate.name.lowercase()
        if (seenKeys.add(key)) {
            suggestions += ClientSuggestion.ExternalCompany(candidate)
        }
    }

    suggestions += ClientSuggestion.CreateManual(query)
    return suggestions
}
