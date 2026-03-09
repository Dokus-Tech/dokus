package tech.dokus.database.repository.banking

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.banking.BankTransactionsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.PaymentCandidateTier
import tech.dokus.domain.ids.BankConnectionId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.toDbDecimal
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

data class BankTransactionCreate(
    val rowHash: String,
    val transactionFingerprint: String,
    val transactionDate: LocalDate,
    val signedAmount: Money,
    val counterpartyName: String? = null,
    val counterpartyIban: String? = null,
    val structuredCommunicationRaw: String? = null,
    val normalizedStructuredCommunication: String? = null,
    val descriptionRaw: String? = null,
    val rowConfidence: Double? = null,
    val largeAmountFlag: Boolean = false,
)

@OptIn(ExperimentalUuidApi::class)
class BankTransactionRepository {
    suspend fun replaceForDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        rows: List<BankTransactionCreate>
    ): List<BankTransactionDto> = newSuspendedTransaction {
        val tenantUuid = tenantId.value.toJavaUuid()
        val documentUuid = documentId.value.toJavaUuid()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        BankTransactionsTable.deleteWhere {
            (BankTransactionsTable.tenantId eq tenantUuid) and
                (BankTransactionsTable.documentId eq documentUuid) and
                (BankTransactionsTable.status neq BankTransactionStatus.Linked)
        }

        val existingRowHashes = BankTransactionsTable.select(BankTransactionsTable.rowHash).where {
            (BankTransactionsTable.tenantId eq tenantUuid) and
                (BankTransactionsTable.documentId eq documentUuid)
        }.mapNotNull { it[BankTransactionsTable.rowHash] }
            .toHashSet()

        val rowsToInsert = rows
            .distinctBy { it.rowHash }
            .filterNot { it.rowHash in existingRowHashes }

        if (rowsToInsert.isNotEmpty()) {
            BankTransactionsTable.batchInsert(rowsToInsert) { row ->
                this[BankTransactionsTable.id] = UUID.randomUUID()
                this[BankTransactionsTable.tenantId] = tenantUuid
                this[BankTransactionsTable.txSource] = BankTransactionSource.BankImport
                this[BankTransactionsTable.documentId] = documentUuid
                this[BankTransactionsTable.rowHash] = row.rowHash
                this[BankTransactionsTable.transactionFingerprint] = row.transactionFingerprint
                this[BankTransactionsTable.transactionDate] = row.transactionDate
                this[BankTransactionsTable.signedAmount] = row.signedAmount.toDbDecimal()
                this[BankTransactionsTable.counterpartyName] = row.counterpartyName
                this[BankTransactionsTable.counterpartyIban] = row.counterpartyIban
                this[BankTransactionsTable.structuredCommunicationRaw] = row.structuredCommunicationRaw
                this[BankTransactionsTable.normalizedStructuredCommunication] = row.normalizedStructuredCommunication
                this[BankTransactionsTable.descriptionRaw] = row.descriptionRaw
                this[BankTransactionsTable.rowConfidence] = row.rowConfidence?.toBigDecimal()
                this[BankTransactionsTable.largeAmountFlag] = row.largeAmountFlag
                this[BankTransactionsTable.status] = BankTransactionStatus.Unmatched
                this[BankTransactionsTable.createdAt] = now
                this[BankTransactionsTable.updatedAt] = now
            }
        }

        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantUuid) and
                (BankTransactionsTable.documentId eq documentUuid)
        }.orderBy(BankTransactionsTable.transactionDate, SortOrder.DESC).map { it.toDto() }
    }

    suspend fun listSelectable(
        tenantId: TenantId,
        statuses: List<BankTransactionStatus> = listOf(
            BankTransactionStatus.Unmatched,
            BankTransactionStatus.Suggested
        )
    ): List<BankTransactionDto> = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.status inList statuses)
        }.orderBy(
            BankTransactionsTable.transactionDate to SortOrder.DESC,
            BankTransactionsTable.createdAt to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun listByDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): List<BankTransactionDto> = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.documentId eq documentId.value.toJavaUuid())
        }.orderBy(
            BankTransactionsTable.transactionDate to SortOrder.DESC,
            BankTransactionsTable.createdAt to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun findById(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): BankTransactionDto? = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }.singleOrNull()?.toDto()
    }

    suspend fun listSuggestionsForEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): List<BankTransactionDto> = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.suggestedCashflowEntryId eq cashflowEntryId.value.toJavaUuid())
        }.orderBy(
            BankTransactionsTable.suggestedScore to SortOrder.DESC,
            BankTransactionsTable.transactionDate to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun clearSuggestionsForEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): Int = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.suggestedCashflowEntryId eq cashflowEntryId.value.toJavaUuid()) and
                (BankTransactionsTable.status neq BankTransactionStatus.Linked)
        }) {
            it[suggestedCashflowEntryId] = null
            it[suggestedScore] = null
            it[suggestedTier] = null
            it[status] = BankTransactionStatus.Unmatched
            it[updatedAt] = now
        }
    }

    suspend fun setSuggestion(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId,
        score: Double,
        tier: PaymentCandidateTier
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[suggestedCashflowEntryId] = cashflowEntryId.value.toJavaUuid()
            it[suggestedScore] = score.toBigDecimal()
            it[suggestedTier] = tier
            it[status] = if (tier == PaymentCandidateTier.Strong) {
                BankTransactionStatus.Suggested
            } else {
                BankTransactionStatus.Unmatched
            }
            it[updatedAt] = now
        } > 0
    }

    suspend fun markLinked(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[status] = BankTransactionStatus.Linked
            it[linkedCashflowEntryId] = cashflowEntryId.value.toJavaUuid()
            it[suggestedCashflowEntryId] = cashflowEntryId.value.toJavaUuid()
            it[updatedAt] = now
        } > 0
    }

    suspend fun markIgnoredSuggestionsForEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): Int = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.suggestedCashflowEntryId eq cashflowEntryId.value.toJavaUuid()) and
                (BankTransactionsTable.status eq BankTransactionStatus.Suggested)
        }) {
            it[status] = BankTransactionStatus.Ignored
            it[updatedAt] = now
        }
    }

    suspend fun listRecentCandidatePool(
        tenantId: TenantId,
        fromDate: LocalDate
    ): List<BankTransactionDto> = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.status inList listOf(
                    BankTransactionStatus.Unmatched,
                    BankTransactionStatus.Suggested
                )) and
                (BankTransactionsTable.transactionDate greaterEq fromDate)
        }.orderBy(
            BankTransactionsTable.transactionDate to SortOrder.DESC,
            BankTransactionsTable.createdAt to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun findByFingerprint(
        tenantId: TenantId,
        fingerprint: String
    ): BankTransactionDto? = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.transactionFingerprint eq fingerprint)
        }.orderBy(BankTransactionsTable.createdAt, SortOrder.DESC).limit(1).singleOrNull()?.toDto()
    }

    suspend fun clearLinkAndSuggestion(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[linkedCashflowEntryId] = null
            it[suggestedCashflowEntryId] = null
            it[suggestedScore] = null
            it[suggestedTier] = null
            it[status] = BankTransactionStatus.Unmatched
            it[updatedAt] = now
        } > 0
    }

    private fun ResultRow.toDto(): BankTransactionDto {
        return BankTransactionDto(
            id = BankTransactionId.parse(this[BankTransactionsTable.id].value.toString()),
            tenantId = TenantId.parse(this[BankTransactionsTable.tenantId].toString()),
            documentId = this[BankTransactionsTable.documentId]?.let { DocumentId.parse(it.toString()) },
            bankConnectionId = this[BankTransactionsTable.bankConnectionId]?.let { BankConnectionId.parse(it.toString()) },
            externalId = this[BankTransactionsTable.externalId],
            source = this[BankTransactionsTable.txSource],
            transactionDate = this[BankTransactionsTable.transactionDate],
            signedAmount = Money.fromDbDecimal(this[BankTransactionsTable.signedAmount]),
            currency = this[BankTransactionsTable.currency],
            isPending = this[BankTransactionsTable.isPending],
            counterpartyName = this[BankTransactionsTable.counterpartyName],
            counterpartyIban = Iban.from(this[BankTransactionsTable.counterpartyIban]),
            structuredCommunicationRaw = this[BankTransactionsTable.structuredCommunicationRaw],
            descriptionRaw = this[BankTransactionsTable.descriptionRaw],
            rowConfidence = this[BankTransactionsTable.rowConfidence]?.toDouble(),
            largeAmountFlag = this[BankTransactionsTable.largeAmountFlag],
            status = this[BankTransactionsTable.status],
            linkedCashflowEntryId = this[BankTransactionsTable.linkedCashflowEntryId]
                ?.let { CashflowEntryId.parse(it.toString()) },
            suggestedCashflowEntryId = this[BankTransactionsTable.suggestedCashflowEntryId]
                ?.let { CashflowEntryId.parse(it.toString()) },
            score = this[BankTransactionsTable.suggestedScore]?.toDouble(),
            tier = this[BankTransactionsTable.suggestedTier],
            createdAt = this[BankTransactionsTable.createdAt],
            updatedAt = this[BankTransactionsTable.updatedAt]
        )
    }
}
