package tech.dokus.backend.services.cashflow.matching

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import tech.dokus.database.tables.banking.MatchPatternsTable
import tech.dokus.database.tables.banking.RejectedMatchPairsTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CashflowContactRef
import tech.dokus.database.entity.CashflowEntryEntity
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Data needed from an invoice for matching signals.
 */
data class InvoiceMatchMeta(
    val id: UUID,
    val status: InvoiceStatus,
    val structuredReference: String?,
    val invoiceNumber: String,
)

/**
 * A learned IBAN → contact mapping.
 */
data class MatchPattern(
    val counterpartyIban: String,
    val contactId: ContactId,
    val matchCount: Int,
)

/**
 * Repository layer for all matching-engine queries.
 */
@OptIn(ExperimentalUuidApi::class)
class MatchingRepository {

    /**
     * Load candidate cashflow entries that could match a transaction.
     * Filters: open/overdue, non-zero remaining, direction-coherent with the signed amount.
     */
    suspend fun loadCandidateEntries(
        tenantId: TenantId,
        direction: CashflowDirection,
    ): List<CashflowEntryEntity> = newSuspendedTransaction {
        val tenantUuid = tenantId.value.toJavaUuid()
        CashflowEntriesTable.selectAll().where {
            (CashflowEntriesTable.tenantId eq tenantUuid) and
                (CashflowEntriesTable.status inList listOf(CashflowEntryStatus.Open, CashflowEntryStatus.Overdue)) and
                (CashflowEntriesTable.direction eq direction)
        }.map { row ->
            CashflowEntryEntity(
                id = tech.dokus.domain.ids.CashflowEntryId.parse(row[CashflowEntriesTable.id].value.toString()),
                tenantId = tenantId,
                sourceType = row[CashflowEntriesTable.sourceType],
                sourceId = row[CashflowEntriesTable.sourceId].toString(),
                documentId = row[CashflowEntriesTable.documentId]?.let { DocumentId.parse(it.toString()) },
                direction = row[CashflowEntriesTable.direction],
                eventDate = row[CashflowEntriesTable.eventDate],
                amountGross = Money.fromDbDecimal(row[CashflowEntriesTable.amountGross]),
                amountVat = Money.fromDbDecimal(row[CashflowEntriesTable.amountVat]),
                remainingAmount = Money.fromDbDecimal(row[CashflowEntriesTable.remainingAmount]),
                currency = row[CashflowEntriesTable.currency],
                status = row[CashflowEntriesTable.status],
                paidAt = row[CashflowEntriesTable.paidAt],
                contact = row[CashflowEntriesTable.counterpartyId]?.let {
                    CashflowContactRef(id = ContactId.parse(it.toString()))
                },
                createdAt = row[CashflowEntriesTable.createdAt],
                updatedAt = row[CashflowEntriesTable.updatedAt],
            )
        }.filter { !it.remainingAmount.isZero }
    }

    /**
     * Load invoice metadata for structured reference / invoice number matching.
     */
    suspend fun loadInvoiceMeta(
        tenantId: TenantId,
        invoiceIds: Set<UUID>,
    ): Map<String, InvoiceMatchMeta> = newSuspendedTransaction {
        if (invoiceIds.isEmpty()) return@newSuspendedTransaction emptyMap()

        InvoicesTable.select(
            InvoicesTable.id,
            InvoicesTable.status,
            InvoicesTable.structuredCommunication,
            InvoicesTable.invoiceNumber,
        ).where {
            (InvoicesTable.tenantId eq tenantId.value.toJavaUuid()) and
                (InvoicesTable.id inList invoiceIds.toList())
        }.associate { row ->
            row[InvoicesTable.id].value.toString() to InvoiceMatchMeta(
                id = row[InvoicesTable.id].value,
                status = row[InvoicesTable.status],
                structuredReference = row[InvoicesTable.structuredCommunication],
                invoiceNumber = row[InvoicesTable.invoiceNumber],
            )
        }
    }

    /**
     * Check whether a (transaction, document) pair has been rejected.
     */
    suspend fun isRejectedPair(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        documentId: DocumentId,
    ): Boolean = newSuspendedTransaction {
        RejectedMatchPairsTable.selectAll().where {
            (RejectedMatchPairsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (RejectedMatchPairsTable.transactionId eq transactionId.value.toJavaUuid()) and
                (RejectedMatchPairsTable.documentId eq documentId.value.toJavaUuid())
        }.count() > 0
    }

    /**
     * Batch-check rejected pairs for a transaction against multiple documents.
     * Returns the set of documentIds that are rejected.
     */
    suspend fun loadRejectedDocumentIds(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        documentIds: List<DocumentId>,
    ): Set<DocumentId> = newSuspendedTransaction {
        if (documentIds.isEmpty()) return@newSuspendedTransaction emptySet()

        RejectedMatchPairsTable.select(RejectedMatchPairsTable.documentId).where {
            (RejectedMatchPairsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (RejectedMatchPairsTable.transactionId eq transactionId.value.toJavaUuid()) and
                (RejectedMatchPairsTable.documentId inList documentIds.map { it.value.toJavaUuid() })
        }.map { DocumentId.parse(it[RejectedMatchPairsTable.documentId].toString()) }.toSet()
    }

    /**
     * Look up learned IBAN → contact patterns for this tenant.
     */
    suspend fun loadMatchPatterns(
        tenantId: TenantId,
        counterpartyIban: String,
    ): List<MatchPattern> = newSuspendedTransaction {
        MatchPatternsTable.selectAll().where {
            (MatchPatternsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (MatchPatternsTable.counterpartyIban eq counterpartyIban)
        }.map { row ->
            MatchPattern(
                counterpartyIban = row[MatchPatternsTable.counterpartyIban],
                contactId = ContactId.parse(row[MatchPatternsTable.contactId].toString()),
                matchCount = row[MatchPatternsTable.matchCount],
            )
        }
    }

    /**
     * Record or increment a learned IBAN → contact pattern.
     */
    suspend fun upsertMatchPattern(
        tenantId: TenantId,
        counterpartyIban: String,
        contactId: ContactId,
    ) = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = tenantId.value.toJavaUuid()

        // Check if exists and increment
        val existing = MatchPatternsTable.selectAll().where {
            (MatchPatternsTable.tenantId eq tenantUuid) and
                (MatchPatternsTable.counterpartyIban eq counterpartyIban) and
                (MatchPatternsTable.contactId eq contactId.value.toJavaUuid())
        }.singleOrNull()

        if (existing != null) {
            val currentCount = existing[MatchPatternsTable.matchCount]
            MatchPatternsTable.update({
                (MatchPatternsTable.tenantId eq tenantUuid) and
                    (MatchPatternsTable.counterpartyIban eq counterpartyIban) and
                    (MatchPatternsTable.contactId eq contactId.value.toJavaUuid())
            }) {
                it[MatchPatternsTable.matchCount] = currentCount + 1
                it[MatchPatternsTable.lastMatchedAt] = now
            }
        } else {
            MatchPatternsTable.upsert(
                MatchPatternsTable.tenantId,
                MatchPatternsTable.counterpartyIban,
                MatchPatternsTable.contactId,
            ) {
                it[MatchPatternsTable.id] = UUID.randomUUID()
                it[MatchPatternsTable.tenantId] = tenantUuid
                it[MatchPatternsTable.counterpartyIban] = counterpartyIban
                it[MatchPatternsTable.contactId] = contactId.value.toJavaUuid()
                it[MatchPatternsTable.matchCount] = 1
                it[MatchPatternsTable.lastMatchedAt] = now
            }
        }
    }

    /**
     * Record a rejected match pair.
     */
    suspend fun insertRejectedPair(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        documentId: DocumentId,
        rejectedBy: UUID?,
    ) = newSuspendedTransaction {
        RejectedMatchPairsTable.upsert(
            RejectedMatchPairsTable.tenantId,
            RejectedMatchPairsTable.transactionId,
            RejectedMatchPairsTable.documentId,
        ) {
            it[RejectedMatchPairsTable.id] = UUID.randomUUID()
            it[RejectedMatchPairsTable.tenantId] = tenantId.value.toJavaUuid()
            it[RejectedMatchPairsTable.transactionId] = transactionId.value.toJavaUuid()
            it[RejectedMatchPairsTable.documentId] = documentId.value.toJavaUuid()
            it[RejectedMatchPairsTable.rejectedBy] = rejectedBy
        }
    }
}
