package tech.dokus.database.repository.search

import java.math.BigDecimal
import tech.dokus.database.entity.SearchContactHitEntity
import tech.dokus.database.mapper.from
import tech.dokus.database.entity.SearchDocumentHitEntity
import tech.dokus.database.entity.SearchTransactionHitEntity
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
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.SearchAggregatesDto
import tech.dokus.domain.model.SearchContactHitDto
import tech.dokus.domain.model.SearchCountsDto
import tech.dokus.domain.model.SearchDocumentHitDto
import tech.dokus.domain.model.SearchPreset
import tech.dokus.domain.model.SearchTransactionHitDto
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope
import tech.dokus.domain.model.contact.CounterpartySnapshotDto
import tech.dokus.domain.utils.json
import kotlinx.serialization.json.Json
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID
import tech.dokus.foundation.backend.utils.runSuspendCatching

class SearchRepository(
    private val searchSuggestionRepository: SearchSuggestionRepository,
) {

    suspend fun search(
        tenantId: TenantId,
        userId: UserId,
        query: String,
        scope: UnifiedSearchScope,
        preset: SearchPreset?,
        limit: Int,
        suggestionLimit: Int
    ): Result<UnifiedSearchResponse> = runSuspendCatching {
        val normalizedQuery = query.trim()
        val effectiveLimit = limit.coerceIn(1, 100)
        val effectiveSuggestionLimit = suggestionLimit.coerceIn(1, 50)

        if (preset != null) {
            val presetResult = searchSuggestionRepository.presetSearch(
                tenantId = tenantId,
                preset = preset,
                limit = effectiveLimit,
            )
            return@runSuspendCatching UnifiedSearchResponse(
                query = normalizedQuery,
                scope = UnifiedSearchScope.Transactions,
                counts = SearchCountsDto(
                    all = presetResult.count,
                    documents = 0,
                    contacts = 0,
                    transactions = presetResult.count,
                ),
                transactions = presetResult.transactions,
                suggestions = emptyList(),
                aggregates = presetResult.aggregates,
            )
        }

        if (normalizedQuery.isBlank()) {
            val suggestions = searchSuggestionRepository.personalizedSuggestions(
                tenantId = tenantId,
                userId = userId,
                limit = effectiveSuggestionLimit,
            )
            return@runSuspendCatching UnifiedSearchResponse(
                query = "",
                scope = scope,
                suggestions = suggestions,
            )
        }

        val escaped = escapeLike(normalizedQuery.lowercase())
        val pattern = "%$escaped%"
        val amountDecimal = parseAmountDecimal(normalizedQuery)

        val documentCount = countDocuments(tenantId, pattern, amountDecimal)
        val contactCount = countContacts(tenantId, pattern)
        val transactionCount = countTransactions(tenantId, pattern, amountDecimal)

        val includeDocuments = scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Documents
        val includeContacts = scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Contacts
        val includeTransactions = scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Transactions

        val documents = if (includeDocuments) {
            searchDocuments(tenantId, pattern, amountDecimal, effectiveLimit)
        } else {
            emptyList()
        }

        val contacts = if (includeContacts) {
            searchContacts(tenantId, pattern, effectiveLimit)
        } else {
            emptyList()
        }

        val transactions = if (includeTransactions) {
            searchTransactions(tenantId, pattern, amountDecimal, effectiveLimit)
        } else {
            emptyList()
        }

        val aggregates = transactionAggregates(tenantId, pattern, amountDecimal)

        UnifiedSearchResponse(
            query = normalizedQuery,
            scope = scope,
            counts = SearchCountsDto(
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
        pattern: String,
        amountDecimal: BigDecimal?,
    ): Long = dbQuery {
        documentQuery(tenantId, pattern, amountDecimal).count()
    }

    private suspend fun countContacts(
        tenantId: TenantId,
        pattern: String
    ): Long = dbQuery {
        contactQuery(tenantId, pattern).count()
    }

    private suspend fun countTransactions(
        tenantId: TenantId,
        pattern: String,
        amountDecimal: BigDecimal?,
    ): Long = dbQuery {
        transactionQuery(tenantId, pattern, amountDecimal).count()
    }

    private suspend fun searchDocuments(
        tenantId: TenantId,
        pattern: String,
        amountDecimal: BigDecimal?,
        limit: Int,
    ): List<SearchDocumentHitDto> = dbQuery {
        documentQuery(tenantId, pattern, amountDecimal)
            .orderBy(DocumentsTable.uploadedAt to SortOrder.DESC)
            .limit(limit)
            .map { SearchDocumentHitDto.from(SearchDocumentHitEntity.from(it)) }
    }

    private suspend fun searchContacts(
        tenantId: TenantId,
        pattern: String,
        limit: Int
    ): List<SearchContactHitDto> = dbQuery {
        contactQuery(tenantId, pattern)
            .orderBy(ContactsTable.name to SortOrder.ASC)
            .limit(limit)
            .map { SearchContactHitDto.from(SearchContactHitEntity.from(it)) }
    }

    private suspend fun searchTransactions(
        tenantId: TenantId,
        pattern: String,
        amountDecimal: BigDecimal?,
        limit: Int,
    ): List<SearchTransactionHitDto> = dbQuery {
        transactionQuery(tenantId, pattern, amountDecimal)
            .orderBy(CashflowEntriesTable.eventDate to SortOrder.DESC)
            .limit(limit)
            .map { SearchTransactionHitDto.from(SearchTransactionHitEntity.from(it)) }
    }

    private suspend fun transactionAggregates(
        tenantId: TenantId,
        pattern: String,
        amountDecimal: BigDecimal?,
    ): SearchAggregatesDto = dbQuery {
        val total = sumTransactionAmount(tenantId, pattern, amountDecimal, direction = null)
        val incoming = sumTransactionAmount(tenantId, pattern, amountDecimal, direction = CashflowDirection.In)
        val outgoing = sumTransactionAmount(tenantId, pattern, amountDecimal, direction = CashflowDirection.Out)
        SearchAggregatesDto(
            transactionTotal = total,
            incomingTotal = incoming,
            outgoingTotal = outgoing,
        )
    }

    private fun sumTransactionAmount(
        tenantId: TenantId,
        pattern: String,
        amountDecimal: BigDecimal?,
        direction: CashflowDirection?,
    ): Money {
        val amountSum = CashflowEntriesTable.amountGross.sum()
        val query = transactionQuery(tenantId, pattern, amountDecimal).apply {
            if (direction != null) {
                andWhere { CashflowEntriesTable.direction eq direction }
            }
            adjustSelect { select(amountSum) }
        }
        val sum = query.firstOrNull()?.get(amountSum) ?: return Money.zero(Currency.Eur)
        return Money.fromDbDecimal(sum, Currency.Eur)
    }

    private fun documentQuery(tenantId: TenantId, pattern: String, amountDecimal: BigDecimal? = null): Query {
        val tenantUuid = UUID.fromString(tenantId.toString())

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
                ContactsTable,
                joinType = JoinType.LEFT,
                onColumn = DocumentsTable.linkedContactId,
                otherColumn = ContactsTable.id,
                additionalConstraint = {
                    ContactsTable.tenantId eq DocumentsTable.tenantId
                }
            )
            .selectAll()
            .where {
                (DocumentsTable.tenantId eq tenantUuid) and
                    (DocumentsTable.documentStatus eq DocumentStatus.Confirmed) and
                    entityExists
            }

        val textMatch = (LowerCase(DocumentsTable.purposeRendered) like pattern) or
            (LowerCase(DocumentsTable.purposeBase) like pattern) or
            (LowerCase(DocumentsTable.aiKeywords) like pattern) or
            (LowerCase(DocumentsTable.counterpartySnapshot) like pattern) or
            (LowerCase(ContactsTable.name) like pattern) or
            (LowerCase(ContactsTable.email) like pattern) or
            (LowerCase(ContactsTable.vatNumber) like pattern) or
            (LowerCase(ContactsTable.companyNumber) like pattern)

        if (amountDecimal != null) {
            val invoiceAmountMatch = exists(
                InvoicesTable.select(InvoicesTable.id).where {
                    (InvoicesTable.tenantId eq DocumentsTable.tenantId) and
                        (InvoicesTable.documentId eq DocumentsTable.id) and
                        (InvoicesTable.totalAmount eq amountDecimal)
                }
            )
            val creditNoteAmountMatch = exists(
                CreditNotesTable.select(CreditNotesTable.id).where {
                    (CreditNotesTable.tenantId eq DocumentsTable.tenantId) and
                        (CreditNotesTable.documentId eq DocumentsTable.id) and
                        (CreditNotesTable.totalAmount eq amountDecimal)
                }
            )
            val expenseAmountMatch = exists(
                ExpensesTable.select(ExpensesTable.id).where {
                    (ExpensesTable.tenantId eq DocumentsTable.tenantId) and
                        (ExpensesTable.documentId eq DocumentsTable.id) and
                        (ExpensesTable.amount eq amountDecimal)
                }
            )
            query = query.andWhere {
                textMatch or invoiceAmountMatch or creditNoteAmountMatch or expenseAmountMatch
            }
        } else {
            query = query.andWhere { textMatch }
        }

        return query
    }

    private fun contactQuery(tenantId: TenantId, pattern: String): Query {
        var query = ContactsTable
            .selectAll()
            .where { ContactsTable.tenantId eq UUID.fromString(tenantId.toString()) }

        query = query.andWhere {
            (LowerCase(ContactsTable.name) like pattern) or
                (LowerCase(ContactsTable.email) like pattern) or
                (LowerCase(ContactsTable.vatNumber) like pattern) or
                (LowerCase(ContactsTable.companyNumber) like pattern)
        }

        return query
    }

    private fun transactionQuery(tenantId: TenantId, pattern: String, amountDecimal: BigDecimal? = null): Query {
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
                (CashflowEntriesTable.tenantId eq UUID.fromString(tenantId.toString())) and
                    (CashflowEntriesTable.status inList SearchStatuses)
            }

        val textMatch = (LowerCase(ContactsTable.name) like pattern) or
            (LowerCase(ContactsTable.email) like pattern) or
            (LowerCase(ContactsTable.vatNumber) like pattern) or
            (LowerCase(DocumentsTable.purposeRendered) like pattern) or
            (LowerCase(ExpensesTable.description) like pattern) or
            (LowerCase(InvoicesTable.invoiceNumber) like pattern) or
            (LowerCase(InvoicesTable.notes) like pattern)

        if (amountDecimal != null) {
            query = query.andWhere {
                textMatch or (CashflowEntriesTable.amountGross eq amountDecimal)
            }
        } else {
            query = query.andWhere { textMatch }
        }

        return query
    }

    /**
     * Escapes LIKE wildcards using backslash as the escape character.
     * Note: Relies on PostgreSQL's default `standard_conforming_strings = on` behavior
     * where `\` is treated as the LIKE escape character without an explicit ESCAPE clause.
     */
    private fun escapeLike(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    /**
     * Parses a search query as an amount in DB decimal format (e.g., "962.52" → BigDecimal("962.52")).
     * Handles comma as decimal separator (European format).
     * Returns null if the query doesn't look like a number.
     */
    private fun parseAmountDecimal(query: String): BigDecimal? {
        val cleaned = query.replace(",", ".").replace(" ", "").trim()
        if (cleaned.isEmpty()) return null
        return try {
            val value = BigDecimal(cleaned)
            if (value <= BigDecimal.ZERO || value > BigDecimal("999999999")) null
            else value.setScale(2, java.math.RoundingMode.HALF_UP)
        } catch (_: NumberFormatException) {
            null
        }
    }
}

private val SearchStatuses = listOf(
    CashflowEntryStatus.Open,
    CashflowEntryStatus.Overdue,
    CashflowEntryStatus.Paid,
)
