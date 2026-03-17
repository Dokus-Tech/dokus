package tech.dokus.database.repository.banking

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.banking.BankTransactionsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.TransactionCommunication
import tech.dokus.domain.model.TransactionIgnoreInfo
import tech.dokus.domain.model.TransactionMatchInfo
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.domain.toDbDecimal
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

data class BankTransactionCreate(
    val dedupHash: String,
    val bankAccountId: BankAccountId? = null,
    val transactionDate: LocalDate,
    val valueDate: LocalDate? = null,
    val signedAmount: Money,
    val counterpartyName: String? = null,
    val counterpartyIban: String? = null,
    val counterpartyBic: String? = null,
    val structuredCommunicationRaw: String? = null,
    val normalizedStructuredCommunication: String? = null,
    val freeCommunication: String? = null,
    val descriptionRaw: String? = null,
    val statementTrust: StatementTrust = StatementTrust.Low,
)

@OptIn(ExperimentalUuidApi::class)
class BankTransactionRepository {

    private val json = Json { ignoreUnknownKeys = true }

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
                    (BankTransactionsTable.status neq BankTransactionStatus.Matched)
        }

        val existingDedupHashes = BankTransactionsTable
            .select(BankTransactionsTable.dedupHash)
            .where {
                (BankTransactionsTable.tenantId eq tenantUuid) and
                        (BankTransactionsTable.documentId eq documentUuid)
            }.map { it[BankTransactionsTable.dedupHash] }
            .toHashSet()

        val rowsToInsert = rows
            .distinctBy { it.dedupHash }
            .filterNot { it.dedupHash in existingDedupHashes }

        if (rowsToInsert.isNotEmpty()) {
            BankTransactionsTable.batchInsert(rowsToInsert) { row ->
                this[BankTransactionsTable.id] = UUID.randomUUID()
                this[BankTransactionsTable.tenantId] = tenantUuid
                this[BankTransactionsTable.txSource] = BankTransactionSource.PdfStatement
                this[BankTransactionsTable.documentId] = documentUuid
                this[BankTransactionsTable.bankAccountId] = row.bankAccountId?.value?.toJavaUuid()
                this[BankTransactionsTable.dedupHash] = row.dedupHash
                this[BankTransactionsTable.transactionDate] = row.transactionDate
                this[BankTransactionsTable.valueDate] = row.valueDate
                this[BankTransactionsTable.signedAmount] = row.signedAmount.toDbDecimal()
                this[BankTransactionsTable.counterpartyName] = row.counterpartyName
                this[BankTransactionsTable.counterpartyIban] = row.counterpartyIban
                this[BankTransactionsTable.counterpartyBic] = row.counterpartyBic
                this[BankTransactionsTable.structuredCommunicationRaw] =
                    row.structuredCommunicationRaw
                this[BankTransactionsTable.normalizedStructuredCommunication] =
                    row.normalizedStructuredCommunication
                this[BankTransactionsTable.freeCommunication] = row.freeCommunication
                this[BankTransactionsTable.descriptionRaw] = row.descriptionRaw
                this[BankTransactionsTable.statementTrust] = row.statementTrust
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
            BankTransactionStatus.NeedsReview
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

    suspend fun listCandidatesForEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): List<BankTransactionDto> = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.matchedCashflowId eq cashflowEntryId.value.toJavaUuid()) and
                    (BankTransactionsTable.status eq BankTransactionStatus.NeedsReview)
        }.orderBy(
            BankTransactionsTable.matchScore to SortOrder.DESC,
            BankTransactionsTable.transactionDate to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun clearCandidatesForEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): Int = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.matchedCashflowId eq cashflowEntryId.value.toJavaUuid()) and
                    (BankTransactionsTable.status neq BankTransactionStatus.Matched)
        }) {
            it[matchedCashflowId] = null
            it[matchScore] = null
            it[status] = BankTransactionStatus.Unmatched
            it[updatedAt] = now
        }
    }

    suspend fun setMatchCandidate(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId,
        score: Double,
        evidence: List<String> = emptyList(),
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[matchedCashflowId] = cashflowEntryId.value.toJavaUuid()
            it[matchScore] = score.toBigDecimal()
            it[status] = BankTransactionStatus.NeedsReview
            if (evidence.isNotEmpty()) {
                it[matchEvidence] = json.encodeToString(evidence)
            }
            it[updatedAt] = now
        } > 0
    }

    suspend fun suggestTransfer(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        evidence: List<String> = emptyList(),
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[status] = BankTransactionStatus.NeedsReview
            if (evidence.isNotEmpty()) {
                it[matchEvidence] = json.encodeToString(evidence)
            }
            it[updatedAt] = now
        } > 0
    }

    suspend fun markMatched(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId,
        matchedBy: MatchedBy,
        resolutionType: ResolutionType,
        score: Double? = null,
        evidence: List<String> = emptyList(),
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[status] = BankTransactionStatus.Matched
            it[matchedCashflowId] = cashflowEntryId.value.toJavaUuid()
            it[BankTransactionsTable.matchedBy] = matchedBy
            it[BankTransactionsTable.resolutionType] = resolutionType
            if (score != null) {
                it[matchScore] = score.toBigDecimal()
            }
            if (evidence.isNotEmpty()) {
                it[matchEvidence] = json.encodeToString(evidence)
            }
            it[matchedAt] = now
            it[updatedAt] = now
        } > 0
    }

    suspend fun listRecentCandidatePool(
        tenantId: TenantId,
        fromDate: LocalDate
    ): List<BankTransactionDto> = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.status inList listOf(
                        BankTransactionStatus.Unmatched,
                        BankTransactionStatus.NeedsReview
                    )) and
                    (BankTransactionsTable.transactionDate greaterEq fromDate)
        }.orderBy(
            BankTransactionsTable.transactionDate to SortOrder.DESC,
            BankTransactionsTable.createdAt to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun listAll(
        tenantId: TenantId,
        status: BankTransactionStatus? = null,
        source: BankTransactionSource? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int? = null,
        offset: Long? = null,
    ): List<BankTransactionDto> = newSuspendedTransaction {
        val condition = buildCondition(tenantId, status, source, fromDate, toDate)
        BankTransactionsTable.selectAll().where { condition }
            .orderBy(
                BankTransactionsTable.transactionDate to SortOrder.DESC,
                BankTransactionsTable.createdAt to SortOrder.DESC
            )
            .let { query ->
                if (limit != null) query.limit(limit).offset(offset ?: 0) else query
            }
            .map { it.toDto() }
    }

    suspend fun countAll(
        tenantId: TenantId,
        status: BankTransactionStatus? = null,
        source: BankTransactionSource? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
    ): Long = newSuspendedTransaction {
        val condition = buildCondition(tenantId, status, source, fromDate, toDate)
        BankTransactionsTable.selectAll().where { condition }.count()
    }

    private fun buildCondition(
        tenantId: TenantId,
        status: BankTransactionStatus?,
        source: BankTransactionSource?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
    ): org.jetbrains.exposed.v1.core.Op<Boolean> {
        val tenantUuid = tenantId.value.toJavaUuid()
        var condition: org.jetbrains.exposed.v1.core.Op<Boolean> =
            BankTransactionsTable.tenantId eq tenantUuid
        if (status != null) {
            condition = condition and (BankTransactionsTable.status eq status)
        }
        if (source != null) {
            condition = condition and (BankTransactionsTable.txSource eq source)
        }
        if (fromDate != null) {
            condition = condition and (BankTransactionsTable.transactionDate greaterEq fromDate)
        }
        if (toDate != null) {
            condition = condition and (BankTransactionsTable.transactionDate lessEq toDate)
        }
        return condition
    }

    suspend fun countByStatus(
        tenantId: TenantId
    ): Map<BankTransactionStatus, Long> = newSuspendedTransaction {
        val tenantUuid = tenantId.value.toJavaUuid()
        BankTransactionsTable
            .select(BankTransactionsTable.status)
            .where { BankTransactionsTable.tenantId eq tenantUuid }
            .map { it[BankTransactionsTable.status] }
            .groupingBy { it }
            .eachCount()
            .mapValues { it.value.toLong() }
    }

    suspend fun sumUnresolved(
        tenantId: TenantId
    ): Long = newSuspendedTransaction {
        val tenantUuid = tenantId.value.toJavaUuid()
        BankTransactionsTable
            .select(BankTransactionsTable.signedAmount)
            .where {
                (BankTransactionsTable.tenantId eq tenantUuid) and
                        (BankTransactionsTable.status inList listOf(
                            BankTransactionStatus.Unmatched,
                            BankTransactionStatus.NeedsReview
                        ))
            }
            .sumOf { it[BankTransactionsTable.signedAmount].toLong() }
    }

    suspend fun markIgnored(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        ignoredReason: IgnoredReason,
        ignoredBy: String
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[status] = BankTransactionStatus.Ignored
            it[BankTransactionsTable.ignoredReason] = ignoredReason
            it[BankTransactionsTable.ignoredBy] = ignoredBy
            it[ignoredAt] = now
            it[updatedAt] = now
        } > 0
    }

    suspend fun markTransfer(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        transferPairId: kotlin.uuid.Uuid,
        matchedBy: MatchedBy,
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[status] = BankTransactionStatus.Matched
            it[resolutionType] = ResolutionType.Transfer
            it[BankTransactionsTable.transferPairId] = transferPairId.toJavaUuid()
            it[BankTransactionsTable.matchedBy] = matchedBy
            it[matchedAt] = now
            it[updatedAt] = now
        } > 0
    }

    suspend fun undoTransfer(
        tenantId: TenantId,
        transactionId: BankTransactionId,
    ): kotlin.uuid.Uuid? = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        // Get the transferPairId before clearing so we can undo the counterpart too
        val pairId = BankTransactionsTable.select(BankTransactionsTable.transferPairId)
            .where {
                (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                        (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
            }.singleOrNull()?.get(BankTransactionsTable.transferPairId)?.toKotlinUuid()

        // Clear this transaction
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[status] = BankTransactionStatus.Unmatched
            it[resolutionType] = null
            it[transferPairId] = null
            it[matchedBy] = null
            it[matchedAt] = null
            it[updatedAt] = now
        }

        // Also revert the counterpart if it exists
        if (pairId != null) {
            BankTransactionsTable.update({
                (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                        (BankTransactionsTable.transferPairId eq pairId.toJavaUuid()) and
                        (BankTransactionsTable.id neq transactionId.value.toJavaUuid())
            }) {
                it[status] = BankTransactionStatus.Unmatched
                it[resolutionType] = null
                it[transferPairId] = null
                it[matchedBy] = null
                it[matchedAt] = null
                it[updatedAt] = now
            }
        }
        pairId
    }

    suspend fun findUnmatchedByAccountAndDateRange(
        tenantId: TenantId,
        accountId: BankAccountId,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<BankTransactionDto> = newSuspendedTransaction {
        BankTransactionsTable.selectAll().where {
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.bankAccountId eq accountId.value.toJavaUuid()) and
                    (BankTransactionsTable.transactionDate greaterEq startDate) and
                    (BankTransactionsTable.transactionDate lessEq endDate) and
                    (BankTransactionsTable.status inList listOf(
                        BankTransactionStatus.Unmatched,
                        BankTransactionStatus.NeedsReview,
                    ))
        }.map { it.toDto() }
    }

    suspend fun clearMatch(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        BankTransactionsTable.update({
            (BankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[matchedCashflowId] = null
            it[matchedDocumentId] = null
            it[matchScore] = null
            it[matchEvidence] = null
            it[matchedBy] = null
            it[matchedAt] = null
            it[resolutionType] = null
            it[status] = BankTransactionStatus.Unmatched
            it[updatedAt] = now
        } > 0
    }

    private fun ResultRow.toDto(): BankTransactionDto {
        val evidenceJson = this[BankTransactionsTable.matchEvidence]
        val evidenceList = evidenceJson?.let {
            json.decodeFromString<List<String>>(it)
        }

        return BankTransactionDto(
            id = BankTransactionId.parse(this[BankTransactionsTable.id].value.toString()),
            tenantId = TenantId.parse(this[BankTransactionsTable.tenantId].toString()),
            documentId = this[BankTransactionsTable.documentId]?.let { DocumentId.parse(it.toString()) },
            bankAccountId = this[BankTransactionsTable.bankAccountId]?.let {
                BankAccountId(it.toKotlinUuid())
            },
            source = this[BankTransactionsTable.txSource],
            transactionDate = this[BankTransactionsTable.transactionDate],
            valueDate = this[BankTransactionsTable.valueDate],
            signedAmount = Money.fromDbDecimal(this[BankTransactionsTable.signedAmount]),
            currency = this[BankTransactionsTable.currency],
            counterparty = CounterpartySnapshot(
                name = this[BankTransactionsTable.counterpartyName],
                iban = Iban.from(this[BankTransactionsTable.counterpartyIban]),
                bic = this[BankTransactionsTable.counterpartyBic]?.let { Bic(it) },
            ),
            communication = TransactionCommunication.from(
                structuredCommunicationRaw = this[BankTransactionsTable.structuredCommunicationRaw],
                freeCommunication = this[BankTransactionsTable.freeCommunication],
            ),
            descriptionRaw = this[BankTransactionsTable.descriptionRaw],
            status = this[BankTransactionsTable.status],
            resolutionType = this[BankTransactionsTable.resolutionType],
            matchInfo = this[BankTransactionsTable.matchedCashflowId]?.let { cashflowId ->
                TransactionMatchInfo(
                    cashflowEntryId = CashflowEntryId.parse(cashflowId.toString()),
                    documentId = this[BankTransactionsTable.matchedDocumentId]
                        ?.let { DocumentId.parse(it.toString()) },
                    score = this[BankTransactionsTable.matchScore]?.toDouble() ?: 0.0,
                    evidence = evidenceList ?: emptyList(),
                    matchedBy = this[BankTransactionsTable.matchedBy] ?: MatchedBy.Auto,
                    matchedAt = this[BankTransactionsTable.matchedAt]
                        ?: this[BankTransactionsTable.updatedAt],
                )
            },
            ignoreInfo = this[BankTransactionsTable.ignoredReason]?.let { reason ->
                TransactionIgnoreInfo(
                    reason = reason,
                    ignoredAt = this[BankTransactionsTable.ignoredAt]
                        ?: this[BankTransactionsTable.updatedAt],
                    ignoredBy = this[BankTransactionsTable.ignoredBy],
                )
            },
            statementTrust = this[BankTransactionsTable.statementTrust],
            transferPairId = this[BankTransactionsTable.transferPairId]
                ?.let { BankTransactionId(it.toKotlinUuid()) },
            createdAt = this[BankTransactionsTable.createdAt],
            updatedAt = this[BankTransactionsTable.updatedAt]
        )
    }
}
