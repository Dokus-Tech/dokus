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
data class SearchCounts(
    val all: Long = 0,
    val documents: Long = 0,
    val contacts: Long = 0,
    val transactions: Long = 0,
)

@Serializable
data class SearchDocumentHit(
    val documentId: DocumentId,
    val filename: String,
    val documentType: DocumentType? = null,
    val status: DocumentStatus? = null,
    val counterpartyName: String? = null,
    val counterpartyVat: String? = null,
)

@Serializable
data class SearchContactHit(
    val contactId: ContactId,
    val name: String,
    val email: String? = null,
    val vatNumber: String? = null,
    val companyNumber: String? = null,
    val isActive: Boolean = true,
)

@Serializable
data class SearchTransactionHit(
    val entryId: CashflowEntryId,
    val displayText: String,
    val status: CashflowEntryStatus,
    val date: LocalDate,
    val amount: Money,
    val direction: CashflowDirection,
    val contactName: String? = null,
    val documentFilename: String? = null,
)

@Serializable
data class SearchSuggestion(
    val label: String,
    val countHint: Long = 0,
    val actionQuery: String? = null,
    val actionScope: UnifiedSearchScope? = null,
    val actionPreset: SearchPreset? = null,
)

@Serializable
data class SearchAggregates(
    val transactionTotal: Money = Money.ZERO,
    val incomingTotal: Money = Money.ZERO,
    val outgoingTotal: Money = Money.ZERO,
)

@Serializable
data class UnifiedSearchResponse(
    val query: String,
    val scope: UnifiedSearchScope,
    val counts: SearchCounts = SearchCounts(),
    val documents: List<SearchDocumentHit> = emptyList(),
    val contacts: List<SearchContactHit> = emptyList(),
    val transactions: List<SearchTransactionHit> = emptyList(),
    val suggestions: List<SearchSuggestion> = emptyList(),
    val aggregates: SearchAggregates = SearchAggregates(),
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
