package tech.dokus.database.repository.search

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.longLiteral
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.database.tables.search.SearchSignalStatsTable
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.SearchResultEntityType
import tech.dokus.domain.model.SearchSignalEventType
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID

data class SearchSignalStat(
    val signalType: SearchSignalEventType,
    val normalizedText: String,
    val displayText: String,
    val count: Long,
    val lastSeenAt: LocalDateTime,
)

class SearchSignalRepository {

    suspend fun upsertSignal(
        tenantId: TenantId,
        userId: UserId,
        signalType: SearchSignalEventType,
        normalizedText: String,
        displayText: String,
    ): Result<Unit> = runCatching {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val userUuid = UUID.fromString(userId.toString())
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        dbQuery {
            SearchSignalStatsTable.upsert(
                SearchSignalStatsTable.tenantId,
                SearchSignalStatsTable.userId,
                SearchSignalStatsTable.signalType,
                SearchSignalStatsTable.normalizedText,
                onUpdate = { stmt ->
                    stmt[SearchSignalStatsTable.count] = SearchSignalStatsTable.count + longLiteral(1L)
                    stmt[SearchSignalStatsTable.displayText] = displayText
                    stmt[SearchSignalStatsTable.lastSeenAt] = now
                }
            ) {
                it[id] = UUID.randomUUID()
                it[SearchSignalStatsTable.tenantId] = tenantUuid
                it[SearchSignalStatsTable.userId] = userUuid
                it[SearchSignalStatsTable.signalType] = signalType
                it[SearchSignalStatsTable.normalizedText] = normalizedText
                it[SearchSignalStatsTable.displayText] = displayText
                it[SearchSignalStatsTable.count] = 1L
                it[SearchSignalStatsTable.lastSeenAt] = now
                it[SearchSignalStatsTable.createdAt] = now
            }
        }
    }

    suspend fun topUserSignals(
        tenantId: TenantId,
        userId: UserId,
        limit: Int,
    ): Result<List<SearchSignalStat>> = runCatching {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val userUuid = UUID.fromString(userId.toString())
        val safeLimit = limit.coerceIn(1, 100)

        dbQuery {
            SearchSignalStatsTable.selectAll()
                .where {
                    (SearchSignalStatsTable.tenantId eq tenantUuid) and
                        (SearchSignalStatsTable.userId eq userUuid)
                }
                .orderBy(
                    SearchSignalStatsTable.lastSeenAt to SortOrder.DESC,
                    SearchSignalStatsTable.count to SortOrder.DESC,
                )
                .limit(safeLimit)
                .map { row ->
                    SearchSignalStat(
                        signalType = row[SearchSignalStatsTable.signalType],
                        normalizedText = row[SearchSignalStatsTable.normalizedText],
                        displayText = row[SearchSignalStatsTable.displayText],
                        count = row[SearchSignalStatsTable.count],
                        lastSeenAt = row[SearchSignalStatsTable.lastSeenAt],
                    )
                }
        }
    }

    suspend fun resolveEntityLabel(
        tenantId: TenantId,
        entityType: SearchResultEntityType,
        entityId: String,
    ): Result<String?> = runCatching {
        val tenantUuid = UUID.fromString(tenantId.toString())
        val entityUuid = parseUuid(entityId) ?: return@runCatching null

        dbQuery {
            when (entityType) {
                SearchResultEntityType.Document -> {
                    DocumentsTable.select(DocumentsTable.filename)
                        .where {
                            (DocumentsTable.tenantId eq tenantUuid) and
                                (DocumentsTable.id eq entityUuid)
                        }
                        .firstOrNull()
                        ?.get(DocumentsTable.filename)
                }

                SearchResultEntityType.Contact -> {
                    ContactsTable.select(ContactsTable.name)
                        .where {
                            (ContactsTable.tenantId eq tenantUuid) and
                                (ContactsTable.id eq entityUuid)
                        }
                        .firstOrNull()
                        ?.get(ContactsTable.name)
                }

                SearchResultEntityType.Transaction -> {
                    CashflowEntriesTable
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
                                (CashflowEntriesTable.id eq entityUuid)
                        }
                        .limit(1)
                        .firstOrNull()
                        ?.let { row ->
                            val documentName = row.getOrNull(DocumentsTable.filename)
                            val expenseDescription = row.getOrNull(ExpensesTable.description)
                            val invoiceNumber = row.getOrNull(InvoicesTable.invoiceNumber)
                            val contactName = row.getOrNull(ContactsTable.name)
                            when {
                                !documentName.isNullOrBlank() -> documentName
                                !expenseDescription.isNullOrBlank() -> expenseDescription
                                !invoiceNumber.isNullOrBlank() -> invoiceNumber
                                !contactName.isNullOrBlank() -> contactName
                                else -> row[CashflowEntriesTable.sourceType].name
                            }
                        }
                }
            }
        }
    }

    private fun parseUuid(value: String): UUID? =
        runCatching { UUID.fromString(value) }.getOrNull()
}
