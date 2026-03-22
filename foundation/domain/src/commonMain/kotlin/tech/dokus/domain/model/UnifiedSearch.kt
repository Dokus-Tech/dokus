package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId

@Serializable
enum class UnifiedSearchScope {
    All,
    Documents,
    Contacts,
    Transactions,
}

@Serializable
enum class SearchPreset {
    OverdueInvoices,
    UpcomingPayments,
}

@Serializable
enum class SearchSignalEventType {
    QueryCommitted,
    SuggestionSelected,
    ResultOpened,
}

@Serializable
enum class SearchResultEntityType {
    Document,
    Contact,
    Transaction,
}

@Serializable
data class SearchCountsDto(
    val all: Long = 0,
    val documents: Long = 0,
    val contacts: Long = 0,
    val transactions: Long = 0,
)

@Serializable
data class SearchDocumentHitDto(
    val documentId: DocumentId,
    val filename: String,
    val documentType: DocumentType? = null,
    val status: DocumentStatus? = null,
    val counterpartyName: String? = null,
    val counterpartyVat: String? = null,
    val amount: Money? = null,
) {
    companion object
}

@Serializable
data class SearchContactHitDto(
    val contactId: ContactId,
    val name: String,
    val email: String? = null,
    val vatNumber: String? = null,
    val companyNumber: String? = null,
    val isActive: Boolean = true,
) {
    companion object
}

@Serializable
data class SearchTransactionHitDto(
    val entryId: CashflowEntryId,
    val displayText: String,
    val status: CashflowEntryStatus,
    val date: LocalDate,
    val amount: Money,
    val direction: CashflowDirection,
    val contactName: String? = null,
    val documentFilename: String? = null,
    val documentId: DocumentId? = null,
) {
    companion object
}

@Serializable
data class SearchSuggestionDto(
    val label: String,
    val countHint: Long = 0,
    val actionQuery: String? = null,
    val actionScope: UnifiedSearchScope? = null,
    val actionPreset: SearchPreset? = null,
)

@Serializable
data class SearchAggregatesDto(
    val transactionTotal: Money = Money.ZERO,
    val incomingTotal: Money = Money.ZERO,
    val outgoingTotal: Money = Money.ZERO,
)

@Serializable
data class UnifiedSearchResponse(
    val query: String,
    val scope: UnifiedSearchScope,
    val counts: SearchCountsDto = SearchCountsDto(),
    val documents: List<SearchDocumentHitDto> = emptyList(),
    val contacts: List<SearchContactHitDto> = emptyList(),
    val transactions: List<SearchTransactionHitDto> = emptyList(),
    val suggestions: List<SearchSuggestionDto> = emptyList(),
    val aggregates: SearchAggregatesDto = SearchAggregatesDto(),
)

@Serializable
data class SearchSignalEventRequest(
    val eventType: SearchSignalEventType,
    val query: String? = null,
    val scope: UnifiedSearchScope? = null,
    val suggestionLabel: String? = null,
    val resultEntityType: SearchResultEntityType? = null,
    val resultEntityId: String? = null,
)
