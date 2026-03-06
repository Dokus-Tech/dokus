package tech.dokus.database.repository.cashflow

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import tech.dokus.database.tables.documents.ImportedBankTransactionsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.ImportedBankTransactionStatus
import tech.dokus.domain.enums.PaymentCandidateTier
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.ImportedBankTransactionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.ImportedBankTransactionDto
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.toDbDecimal
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

data class ImportedBankTransactionCreate(
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
class ImportedBankTransactionRepository {
    suspend fun replaceForDocument(
        tenantId: TenantId,
        documentId: DocumentId,
        rows: List<ImportedBankTransactionCreate>
    ): List<ImportedBankTransactionDto> = newSuspendedTransaction {
        val tenantUuid = tenantId.value.toJavaUuid()
        val documentUuid = documentId.value.toJavaUuid()
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        ImportedBankTransactionsTable.deleteWhere {
            (ImportedBankTransactionsTable.tenantId eq tenantUuid) and
                (ImportedBankTransactionsTable.documentId eq documentUuid) and
                (ImportedBankTransactionsTable.status neq ImportedBankTransactionStatus.Linked)
        }

        val existingRowHashes = ImportedBankTransactionsTable.select(ImportedBankTransactionsTable.rowHash).where {
            (ImportedBankTransactionsTable.tenantId eq tenantUuid) and
                (ImportedBankTransactionsTable.documentId eq documentUuid)
        }.map { it[ImportedBankTransactionsTable.rowHash] }
            .toHashSet()

        val rowsToInsert = rows
            .distinctBy { it.rowHash }
            .filterNot { it.rowHash in existingRowHashes }

        if (rowsToInsert.isNotEmpty()) {
            ImportedBankTransactionsTable.batchInsert(rowsToInsert) { row ->
                this[ImportedBankTransactionsTable.id] = UUID.randomUUID()
                this[ImportedBankTransactionsTable.tenantId] = tenantUuid
                this[ImportedBankTransactionsTable.documentId] = documentUuid
                this[ImportedBankTransactionsTable.rowHash] = row.rowHash
                this[ImportedBankTransactionsTable.transactionFingerprint] = row.transactionFingerprint
                this[ImportedBankTransactionsTable.transactionDate] = row.transactionDate
                this[ImportedBankTransactionsTable.signedAmount] = row.signedAmount.toDbDecimal()
                this[ImportedBankTransactionsTable.counterpartyName] = row.counterpartyName
                this[ImportedBankTransactionsTable.counterpartyIban] = row.counterpartyIban
                this[ImportedBankTransactionsTable.structuredCommunicationRaw] = row.structuredCommunicationRaw
                this[ImportedBankTransactionsTable.normalizedStructuredCommunication] = row.normalizedStructuredCommunication
                this[ImportedBankTransactionsTable.descriptionRaw] = row.descriptionRaw
                this[ImportedBankTransactionsTable.rowConfidence] = row.rowConfidence?.toBigDecimal()
                this[ImportedBankTransactionsTable.largeAmountFlag] = row.largeAmountFlag
                this[ImportedBankTransactionsTable.status] = ImportedBankTransactionStatus.Unmatched
                this[ImportedBankTransactionsTable.createdAt] = now
                this[ImportedBankTransactionsTable.updatedAt] = now
            }
        }

        ImportedBankTransactionsTable.selectAll().where {
            (ImportedBankTransactionsTable.tenantId eq tenantUuid) and
                (ImportedBankTransactionsTable.documentId eq documentUuid)
        }.orderBy(ImportedBankTransactionsTable.transactionDate, SortOrder.DESC).map { it.toDto() }
    }

    suspend fun listSelectable(
        tenantId: TenantId,
        statuses: List<ImportedBankTransactionStatus> = listOf(
            ImportedBankTransactionStatus.Unmatched,
            ImportedBankTransactionStatus.Suggested
        )
    ): List<ImportedBankTransactionDto> = newSuspendedTransaction {
        ImportedBankTransactionsTable.selectAll().where {
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.status inList statuses)
        }.orderBy(
            ImportedBankTransactionsTable.transactionDate to SortOrder.DESC,
            ImportedBankTransactionsTable.createdAt to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun listByDocument(
        tenantId: TenantId,
        documentId: DocumentId
    ): List<ImportedBankTransactionDto> = newSuspendedTransaction {
        ImportedBankTransactionsTable.selectAll().where {
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.documentId eq documentId.value.toJavaUuid())
        }.orderBy(
            ImportedBankTransactionsTable.transactionDate to SortOrder.DESC,
            ImportedBankTransactionsTable.createdAt to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun findById(
        tenantId: TenantId,
        transactionId: ImportedBankTransactionId
    ): ImportedBankTransactionDto? = newSuspendedTransaction {
        ImportedBankTransactionsTable.selectAll().where {
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }.singleOrNull()?.toDto()
    }

    suspend fun listSuggestionsForEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): List<ImportedBankTransactionDto> = newSuspendedTransaction {
        ImportedBankTransactionsTable.selectAll().where {
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.suggestedCashflowEntryId eq cashflowEntryId.value.toJavaUuid())
        }.orderBy(
            ImportedBankTransactionsTable.suggestedScore to SortOrder.DESC,
            ImportedBankTransactionsTable.transactionDate to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun clearSuggestionsForEntry(
        tenantId: TenantId,
        cashflowEntryId: CashflowEntryId
    ): Int = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        ImportedBankTransactionsTable.update({
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.suggestedCashflowEntryId eq cashflowEntryId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.status neq ImportedBankTransactionStatus.Linked)
        }) {
            it[suggestedCashflowEntryId] = null
            it[suggestedScore] = null
            it[suggestedTier] = null
            it[status] = ImportedBankTransactionStatus.Unmatched
            it[updatedAt] = now
        }
    }

    suspend fun setSuggestion(
        tenantId: TenantId,
        transactionId: ImportedBankTransactionId,
        cashflowEntryId: CashflowEntryId,
        score: Double,
        tier: PaymentCandidateTier
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        ImportedBankTransactionsTable.update({
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[suggestedCashflowEntryId] = cashflowEntryId.value.toJavaUuid()
            it[suggestedScore] = score.toBigDecimal()
            it[suggestedTier] = tier
            it[status] = if (tier == PaymentCandidateTier.Strong) {
                ImportedBankTransactionStatus.Suggested
            } else {
                ImportedBankTransactionStatus.Unmatched
            }
            it[updatedAt] = now
        } > 0
    }

    suspend fun markLinked(
        tenantId: TenantId,
        transactionId: ImportedBankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        ImportedBankTransactionsTable.update({
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[status] = ImportedBankTransactionStatus.Linked
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
        ImportedBankTransactionsTable.update({
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.suggestedCashflowEntryId eq cashflowEntryId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.status eq ImportedBankTransactionStatus.Suggested)
        }) {
            it[status] = ImportedBankTransactionStatus.Ignored
            it[updatedAt] = now
        }
    }

    suspend fun listRecentCandidatePool(
        tenantId: TenantId,
        fromDate: LocalDate
    ): List<ImportedBankTransactionDto> = newSuspendedTransaction {
        ImportedBankTransactionsTable.selectAll().where {
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.status inList listOf(
                    ImportedBankTransactionStatus.Unmatched,
                    ImportedBankTransactionStatus.Suggested
                )) and
                (ImportedBankTransactionsTable.transactionDate greaterEq fromDate)
        }.orderBy(
            ImportedBankTransactionsTable.transactionDate to SortOrder.DESC,
            ImportedBankTransactionsTable.createdAt to SortOrder.DESC
        ).map { it.toDto() }
    }

    suspend fun findByFingerprint(
        tenantId: TenantId,
        fingerprint: String
    ): ImportedBankTransactionDto? = newSuspendedTransaction {
        ImportedBankTransactionsTable.selectAll().where {
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.transactionFingerprint eq fingerprint)
        }.orderBy(ImportedBankTransactionsTable.createdAt, SortOrder.DESC).limit(1).singleOrNull()?.toDto()
    }

    suspend fun clearLinkAndSuggestion(
        tenantId: TenantId,
        transactionId: ImportedBankTransactionId
    ): Boolean = newSuspendedTransaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        ImportedBankTransactionsTable.update({
            (ImportedBankTransactionsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (ImportedBankTransactionsTable.id eq transactionId.value.toJavaUuid())
        }) {
            it[linkedCashflowEntryId] = null
            it[suggestedCashflowEntryId] = null
            it[suggestedScore] = null
            it[suggestedTier] = null
            it[status] = ImportedBankTransactionStatus.Unmatched
            it[updatedAt] = now
        } > 0
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toDto(): ImportedBankTransactionDto {
        return ImportedBankTransactionDto(
            id = ImportedBankTransactionId.parse(this[ImportedBankTransactionsTable.id].value.toString()),
            tenantId = TenantId.parse(this[ImportedBankTransactionsTable.tenantId].toString()),
            documentId = DocumentId.parse(this[ImportedBankTransactionsTable.documentId].toString()),
            transactionDate = this[ImportedBankTransactionsTable.transactionDate],
            signedAmount = Money.fromDbDecimal(this[ImportedBankTransactionsTable.signedAmount]),
            counterpartyName = this[ImportedBankTransactionsTable.counterpartyName],
            counterpartyIban = Iban.from(this[ImportedBankTransactionsTable.counterpartyIban]),
            structuredCommunicationRaw = this[ImportedBankTransactionsTable.structuredCommunicationRaw],
            descriptionRaw = this[ImportedBankTransactionsTable.descriptionRaw],
            rowConfidence = this[ImportedBankTransactionsTable.rowConfidence]?.toDouble(),
            largeAmountFlag = this[ImportedBankTransactionsTable.largeAmountFlag],
            status = this[ImportedBankTransactionsTable.status],
            linkedCashflowEntryId = this[ImportedBankTransactionsTable.linkedCashflowEntryId]
                ?.let { CashflowEntryId.parse(it.toString()) },
            suggestedCashflowEntryId = this[ImportedBankTransactionsTable.suggestedCashflowEntryId]
                ?.let { CashflowEntryId.parse(it.toString()) },
            score = this[ImportedBankTransactionsTable.suggestedScore]?.toDouble(),
            tier = this[ImportedBankTransactionsTable.suggestedTier],
            createdAt = this[ImportedBankTransactionsTable.createdAt],
            updatedAt = this[ImportedBankTransactionsTable.updatedAt]
        )
    }
}
