package tech.dokus.database.repository.search

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.LowerCase
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.SearchAggregates
import tech.dokus.domain.model.SearchContactHit
import tech.dokus.domain.model.SearchCounts
import tech.dokus.domain.model.SearchDocumentHit
import tech.dokus.domain.model.SearchSuggestion
import tech.dokus.domain.model.SearchTransactionHit
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID

private const val DefaultSuggestionLimit = 8

class SearchRepository {

    suspend fun search(
        tenantId: TenantId,
        query: String,
        scope: UnifiedSearchScope,
        limit: Int,
        suggestionLimit: Int
    ): Result<UnifiedSearchResponse> = runCatching {
        val normalizedQuery = query.trim()
        val effectiveLimit = limit.coerceIn(1, 100)
        val effectiveSuggestionLimit = suggestionLimit.coerceIn(1, 50)

        if (normalizedQuery.isBlank()) {
            val suggestions = suggestions(
                tenantId = tenantId,
                limit = effectiveSuggestionLimit
            )
            return@runCatching UnifiedSearchResponse(
                query = "",
                scope = scope,
                suggestions = suggestions,
            )
        }

        val escaped = escapeLike(normalizedQuery.lowercase())
        val pattern = "%$escaped%"

        val documentCount = countDocuments(tenantId, pattern)
        val contactCount = countContacts(tenantId, pattern)
        val transactionCount = countTransactions(tenantId, pattern)

        val includeDocuments = scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Documents
        val includeContacts = scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Contacts
        val includeTransactions = scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Transactions

        val documents = if (includeDocuments) {
            searchDocuments(tenantId, pattern, effectiveLimit)
        } else {
            emptyList()
        }

        val contacts = if (includeContacts) {
            searchContacts(tenantId, pattern, effectiveLimit)
        } else {
            emptyList()
        }

        val transactions = if (includeTransactions) {
            searchTransactions(tenantId, pattern, effectiveLimit)
        } else {
            emptyList()
        }

        val aggregates = transactionAggregates(tenantId, pattern)

        UnifiedSearchResponse(
            query = normalizedQuery,
            scope = scope,
            counts = SearchCounts(
                all = documentCount + contactCount + transactionCount,
                documents = documentCount,
                contacts = contactCount,
                transactions = transactionCount,
            ),
            documents = documents,
            contacts = contacts,
            transactions = transactions,
            aggregates = aggregates,
        )
    }

    private suspend fun countDocuments(
        tenantId: TenantId,
        pattern: String
    ): Long = dbQuery {
        documentQuery(tenantId, pattern).count()
    }

    private suspend fun countContacts(
        tenantId: TenantId,
        pattern: String
    ): Long = dbQuery {
        contactQuery(tenantId, pattern).count()
    }

    private suspend fun countTransactions(
        tenantId: TenantId,
        pattern: String
    ): Long = dbQuery {
        transactionQuery(tenantId, pattern).count()
    }

    private suspend fun searchDocuments(
        tenantId: TenantId,
        pattern: String,
        limit: Int
    ): List<SearchDocumentHit> = dbQuery {
        documentQuery(tenantId, pattern)
            .orderBy(DocumentsTable.uploadedAt to SortOrder.DESC)
            .limit(limit)
            .map(::mapDocumentHit)
    }

    private suspend fun searchContacts(
        tenantId: TenantId,
        pattern: String,
        limit: Int
    ): List<SearchContactHit> = dbQuery {
        contactQuery(tenantId, pattern)
            .orderBy(ContactsTable.name to SortOrder.ASC)
            .limit(limit)
            .map(::mapContactHit)
    }

    private suspend fun searchTransactions(
        tenantId: TenantId,
        pattern: String,
        limit: Int
    ): List<SearchTransactionHit> = dbQuery {
        transactionQuery(tenantId, pattern)
            .orderBy(CashflowEntriesTable.eventDate to SortOrder.DESC)
            .limit(limit)
            .map(::mapTransactionHit)
    }

    private suspend fun transactionAggregates(
        tenantId: TenantId,
        pattern: String,
    ): SearchAggregates = dbQuery {
        val rows = transactionQuery(tenantId, pattern).toList()
        var total = Money.ZERO
        var incoming = Money.ZERO
        var outgoing = Money.ZERO

        rows.forEach { row ->
            val amount = Money.fromDbDecimal(row[CashflowEntriesTable.amountGross])
            total += amount
            when (row[CashflowEntriesTable.direction]) {
                CashflowDirection.In -> incoming += amount
                CashflowDirection.Out -> outgoing += amount
                else -> Unit
            }
        }

        SearchAggregates(
            transactionTotal = total,
            incomingTotal = incoming,
            outgoingTotal = outgoing,
        )
    }

    private suspend fun suggestions(
        tenantId: TenantId,
        limit: Int = DefaultSuggestionLimit
    ): List<SearchSuggestion> = dbQuery {
        val tenantUuid = tenantId.asUuid()

        val counterparties = CashflowEntriesTable
            .join(
                ContactsTable,
                joinType = JoinType.LEFT,
                onColumn = CashflowEntriesTable.counterpartyId,
                otherColumn = ContactsTable.id,
                additionalConstraint = {
                    ContactsTable.tenantId eq CashflowEntriesTable.tenantId
                }
            )
            .select(ContactsTable.name)
            .where {
                (CashflowEntriesTable.tenantId eq tenantUuid) and
                    (CashflowEntriesTable.status inList SearchStatuses)
            }
            .mapNotNull { row -> row.getOrNull(ContactsTable.name) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { SearchSuggestion(label = it.key, countHint = it.value.toLong()) }

        val defaultTerms = listOf(
            SearchSuggestion(label = "overdue", countHint = 0),
            SearchSuggestion(label = "paid", countHint = 0),
            SearchSuggestion(label = "this month", countHint = 0),
            SearchSuggestion(label = "documents", countHint = 0),
            SearchSuggestion(label = "contacts", countHint = 0),
        )

        (counterparties + defaultTerms)
            .distinctBy { it.label.lowercase() }
            .take(limit)
    }

    private fun documentQuery(tenantId: TenantId, pattern: String): Query {
        val tenantUuid = tenantId.asUuid()

        val invoiceExists = exists(
            InvoicesTable.select(InvoicesTable.id).where {
                (InvoicesTable.tenantId eq DocumentsTable.tenantId) and
                    (InvoicesTable.documentId eq DocumentsTable.id)
            }
        )
        val creditNoteExists = exists(
            CreditNotesTable.select(CreditNotesTable.id).where {
                (CreditNotesTable.tenantId eq DocumentsTable.tenantId) and
                    (CreditNotesTable.documentId eq DocumentsTable.id)
            }
        )
        val expenseExists = exists(
            ExpensesTable.select(ExpensesTable.id).where {
                (ExpensesTable.tenantId eq DocumentsTable.tenantId) and
                    (ExpensesTable.documentId eq DocumentsTable.id)
            }
        )
        val entityExists = invoiceExists or creditNoteExists or expenseExists

        var query = DocumentsTable
            .join(
                DocumentDraftsTable,
                joinType = JoinType.INNER,
                onColumn = DocumentsTable.id,
                otherColumn = DocumentDraftsTable.documentId,
                additionalConstraint = {
                    DocumentDraftsTable.tenantId eq DocumentsTable.tenantId
                }
            )
            .join(
                ContactsTable,
                joinType = JoinType.LEFT,
                onColumn = DocumentDraftsTable.linkedContactId,
                otherColumn = ContactsTable.id,
                additionalConstraint = {
                    ContactsTable.tenantId eq DocumentsTable.tenantId
                }
            )
            .selectAll()
            .where {
                (DocumentsTable.tenantId eq tenantUuid) and
                    (DocumentDraftsTable.documentStatus eq DocumentStatus.Confirmed) and
                    entityExists
            }

        query = query.andWhere {
            (LowerCase(DocumentsTable.filename) like pattern) or
                (LowerCase(DocumentDraftsTable.aiDescription) like pattern) or
                (LowerCase(DocumentDraftsTable.aiKeywords) like pattern) or
                (LowerCase(DocumentDraftsTable.counterpartySnapshot) like pattern) or
                (LowerCase(ContactsTable.name) like pattern) or
                (LowerCase(ContactsTable.email) like pattern) or
                (LowerCase(ContactsTable.vatNumber) like pattern) or
                (LowerCase(ContactsTable.companyNumber) like pattern)
        }

        return query
    }

    private fun contactQuery(tenantId: TenantId, pattern: String): Query {
        var query = ContactsTable
            .selectAll()
            .where { ContactsTable.tenantId eq tenantId.asUuid() }

        query = query.andWhere {
            (LowerCase(ContactsTable.name) like pattern) or
                (LowerCase(ContactsTable.email) like pattern) or
                (LowerCase(ContactsTable.vatNumber) like pattern) or
                (LowerCase(ContactsTable.companyNumber) like pattern)
        }

        return query
    }

    private fun transactionQuery(tenantId: TenantId, pattern: String): Query {
        var query = CashflowEntriesTable
            .join(
                ContactsTable,
                joinType = JoinType.LEFT,
                onColumn = CashflowEntriesTable.counterpartyId,
                otherColumn = ContactsTable.id,
                additionalConstraint = {
                    ContactsTable.tenantId eq CashflowEntriesTable.tenantId
                }
            )
            .join(
                DocumentsTable,
                joinType = JoinType.LEFT,
                onColumn = CashflowEntriesTable.documentId,
                otherColumn = DocumentsTable.id,
                additionalConstraint = {
                    DocumentsTable.tenantId eq CashflowEntriesTable.tenantId
                }
            )
            .join(
                ExpensesTable,
                joinType = JoinType.LEFT,
                onColumn = CashflowEntriesTable.sourceId,
                otherColumn = ExpensesTable.id,
                additionalConstraint = {
                    (ExpensesTable.tenantId eq CashflowEntriesTable.tenantId) and
                        (CashflowEntriesTable.sourceType eq CashflowSourceType.Expense)
                }
            )
            .join(
                InvoicesTable,
                joinType = JoinType.LEFT,
                onColumn = CashflowEntriesTable.sourceId,
                otherColumn = InvoicesTable.id,
                additionalConstraint = {
                    (InvoicesTable.tenantId eq CashflowEntriesTable.tenantId) and
                        (CashflowEntriesTable.sourceType eq CashflowSourceType.Invoice)
                }
            )
            .selectAll()
            .where {
                (CashflowEntriesTable.tenantId eq tenantId.asUuid()) and
                    (CashflowEntriesTable.status inList SearchStatuses)
            }

        query = query.andWhere {
            (LowerCase(ContactsTable.name) like pattern) or
                (LowerCase(ContactsTable.email) like pattern) or
                (LowerCase(ContactsTable.vatNumber) like pattern) or
                (LowerCase(DocumentsTable.filename) like pattern) or
                (LowerCase(ExpensesTable.description) like pattern) or
                (LowerCase(InvoicesTable.invoiceNumber) like pattern) or
                (LowerCase(InvoicesTable.notes) like pattern)
        }

        return query
    }

    private fun mapDocumentHit(row: ResultRow): SearchDocumentHit {
        val snapshot = row.getOrNull(DocumentDraftsTable.counterpartySnapshot)
            ?.let { json.decodeFromStringOrNull<CounterpartySnapshot>(it) }
        return SearchDocumentHit(
            documentId = DocumentId.parse(row[DocumentsTable.id].value.toString()),
            filename = row[DocumentsTable.filename],
            documentType = row[DocumentDraftsTable.documentType],
            status = row[DocumentDraftsTable.documentStatus],
            counterpartyName = row.getOrNull(ContactsTable.name) ?: snapshot?.name,
            counterpartyVat = row.getOrNull(ContactsTable.vatNumber) ?: snapshot?.vatNumber?.value,
        )
    }

    private fun mapContactHit(row: ResultRow): SearchContactHit = SearchContactHit(
        contactId = ContactId.parse(row[ContactsTable.id].value.toString()),
        name = row[ContactsTable.name],
        email = row[ContactsTable.email],
        vatNumber = row[ContactsTable.vatNumber],
        companyNumber = row[ContactsTable.companyNumber],
        isActive = row[ContactsTable.isActive],
    )

    private fun mapTransactionHit(row: ResultRow): SearchTransactionHit {
        val direction = row[CashflowEntriesTable.direction]
        val absoluteAmount = Money.fromDbDecimal(row[CashflowEntriesTable.amountGross])
        val signedAmount = if (direction == CashflowDirection.Out) -absoluteAmount else absoluteAmount
        val contactName = row.getOrNull(ContactsTable.name)
        val filename = row.getOrNull(DocumentsTable.filename)
        val expenseDescription = row.getOrNull(ExpensesTable.description)
        val invoiceNumber = row.getOrNull(InvoicesTable.invoiceNumber)
        val displayText = when {
            !filename.isNullOrBlank() -> filename
            !expenseDescription.isNullOrBlank() -> expenseDescription
            !invoiceNumber.isNullOrBlank() -> invoiceNumber
            !contactName.isNullOrBlank() -> contactName
            else -> row[CashflowEntriesTable.sourceType].name
        }

        return SearchTransactionHit(
            entryId = CashflowEntryId.parse(row[CashflowEntriesTable.id].value.toString()),
            displayText = displayText,
            status = row[CashflowEntriesTable.status],
            date = row[CashflowEntriesTable.eventDate],
            amount = signedAmount,
            direction = direction,
            contactName = contactName,
            documentFilename = filename,
        )
    }

    private fun TenantId.asUuid(): UUID = UUID.fromString(toString())

    private fun escapeLike(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }
}

private val SearchStatuses = listOf(
    CashflowEntryStatus.Open,
    CashflowEntryStatus.Overdue,
    CashflowEntryStatus.Paid,
)

private inline fun <reified T> kotlinx.serialization.json.Json.decodeFromStringOrNull(value: String): T? =
    runCatching { decodeFromString<T>(value) }.getOrNull()
