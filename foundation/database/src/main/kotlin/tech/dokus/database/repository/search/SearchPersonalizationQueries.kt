package tech.dokus.database.repository.search

import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
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
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.SearchAggregates
import tech.dokus.domain.model.SearchPreset
import tech.dokus.domain.model.SearchTransactionHit
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID

private val TransactionSearchStatuses = listOf(
    CashflowEntryStatus.Open,
    CashflowEntryStatus.Overdue,
    CashflowEntryStatus.Paid,
)
private val PresetStatuses = listOf(
    CashflowEntryStatus.Open,
    CashflowEntryStatus.Overdue,
)

data class PresetSearchResult(
    val transactions: List<SearchTransactionHit>,
    val count: Long,
    val aggregates: SearchAggregates,
)

data class TenantEntitySample(
    val label: String,
    val seenAt: LocalDateTime,
)

class SearchPersonalizationQueries {

    suspend fun presetSearch(
        tenantId: TenantId,
        preset: SearchPreset,
        limit: Int,
    ): PresetSearchResult {
        val safeLimit = limit.coerceIn(1, 100)
        val count = presetCount(tenantId, preset)
        val transactions = presetTransactions(tenantId, preset, safeLimit)
        val aggregates = presetAggregates(tenantId, preset)
        return PresetSearchResult(
            transactions = transactions,
            count = count,
            aggregates = aggregates,
        )
    }

    suspend fun presetCount(
        tenantId: TenantId,
        preset: SearchPreset,
    ): Long = dbQuery {
        presetTransactionQuery(tenantId, preset).count()
    }

    suspend fun tenantEntitySamples(tenantId: TenantId): List<TenantEntitySample> = dbQuery {
        cashflowContactSamples(tenantId) +
            confirmedDocumentContactSamples(tenantId) +
            activeContactSamples(tenantId)
    }

    private suspend fun presetTransactions(
        tenantId: TenantId,
        preset: SearchPreset,
        limit: Int,
    ): List<SearchTransactionHit> = dbQuery {
        presetTransactionQuery(tenantId, preset)
            .orderBy(CashflowEntriesTable.eventDate to SortOrder.DESC)
            .limit(limit)
            .map(::mapTransactionHit)
    }

    private suspend fun presetAggregates(
        tenantId: TenantId,
        preset: SearchPreset,
    ): SearchAggregates = dbQuery {
        val rows = presetTransactionQuery(tenantId, preset).toList()
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

    private fun presetTransactionQuery(
        tenantId: TenantId,
        preset: SearchPreset,
    ): Query {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val today = utcToday()
        val upperBound = today.plus(DatePeriod(days = 30))

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
                (CashflowEntriesTable.tenantId eq tenantUuid) and
                    (CashflowEntriesTable.status inList PresetStatuses)
            }

        query = when (preset) {
            SearchPreset.OverdueInvoices -> {
                query.andWhere { CashflowEntriesTable.eventDate less today }
            }

            SearchPreset.UpcomingPayments -> {
                query.andWhere {
                    (CashflowEntriesTable.eventDate greaterEq today) and
                        (CashflowEntriesTable.eventDate lessEq upperBound)
                }
            }
        }
        return query
    }

    private fun cashflowContactSamples(tenantId: TenantId): List<TenantEntitySample> {
        val tenantUuid = UUID.fromString(tenantId.toString())
        return CashflowEntriesTable
            .join(
                ContactsTable,
                joinType = JoinType.LEFT,
                onColumn = CashflowEntriesTable.counterpartyId,
                otherColumn = ContactsTable.id,
                additionalConstraint = {
                    ContactsTable.tenantId eq CashflowEntriesTable.tenantId
                }
            )
            .select(ContactsTable.name, CashflowEntriesTable.updatedAt)
            .where {
                (CashflowEntriesTable.tenantId eq tenantUuid) and
                    (CashflowEntriesTable.status inList TransactionSearchStatuses)
            }
            .mapNotNull { row ->
                val label = row.getOrNull(ContactsTable.name)?.trim().orEmpty()
                if (label.isBlank()) {
                    null
                } else {
                    TenantEntitySample(label = label, seenAt = row[CashflowEntriesTable.updatedAt])
                }
            }
    }

    private fun confirmedDocumentContactSamples(tenantId: TenantId): List<TenantEntitySample> {
        val tenantUuid = UUID.fromString(tenantId.toString())
        return DocumentsTable
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
                joinType = JoinType.INNER,
                onColumn = DocumentDraftsTable.linkedContactId,
                otherColumn = ContactsTable.id,
                additionalConstraint = {
                    ContactsTable.tenantId eq DocumentsTable.tenantId
                }
            )
            .select(ContactsTable.name, DocumentsTable.uploadedAt)
            .where {
                (DocumentsTable.tenantId eq tenantUuid) and
                    (DocumentDraftsTable.documentStatus eq DocumentStatus.Confirmed)
            }
            .mapNotNull { row ->
                val label = row.getOrNull(ContactsTable.name)?.trim().orEmpty()
                if (label.isBlank()) {
                    null
                } else {
                    TenantEntitySample(label = label, seenAt = row[DocumentsTable.uploadedAt])
                }
            }
    }

    private fun activeContactSamples(tenantId: TenantId): List<TenantEntitySample> {
        val tenantUuid = UUID.fromString(tenantId.toString())
        return ContactsTable
            .select(ContactsTable.name, ContactsTable.updatedAt)
            .where {
                (ContactsTable.tenantId eq tenantUuid) and
                    (ContactsTable.isActive eq true)
            }
            .mapNotNull { row ->
                val label = row[ContactsTable.name].trim()
                if (label.isBlank()) {
                    null
                } else {
                    TenantEntitySample(label = label, seenAt = row[ContactsTable.updatedAt])
                }
            }
    }

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

    private fun utcToday(): LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.UTC).date
}
