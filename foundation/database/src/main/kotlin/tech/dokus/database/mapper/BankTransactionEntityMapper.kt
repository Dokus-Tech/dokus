package tech.dokus.database.mapper

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.BankTransactionEntity
import tech.dokus.database.tables.banking.BankTransactionsTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalUuidApi::class)
fun BankTransactionEntity.Companion.from(row: ResultRow): BankTransactionEntity {
    val evidenceJson = row[BankTransactionsTable.matchEvidence]
    val evidenceList = evidenceJson?.let {
        json.decodeFromString<List<String>>(it)
    } ?: emptyList()

    val currency = row[BankTransactionsTable.currency]
    return BankTransactionEntity(
        id = BankTransactionId.parse(row[BankTransactionsTable.id].value.toString()),
        tenantId = TenantId.parse(row[BankTransactionsTable.tenantId].toString()),
        documentId = row[BankTransactionsTable.documentId]?.let { DocumentId.parse(it.toString()) },
        bankAccountId = row[BankTransactionsTable.bankAccountId]?.let {
            BankAccountId(it.toKotlinUuid())
        },
        source = row[BankTransactionsTable.txSource],
        transactionDate = row[BankTransactionsTable.transactionDate],
        valueDate = row[BankTransactionsTable.valueDate],
        signedAmount = Money.fromDbDecimal(row[BankTransactionsTable.signedAmount], currency),
        currency = currency,
        counterpartyName = row[BankTransactionsTable.counterpartyName],
        counterpartyIban = Iban.from(row[BankTransactionsTable.counterpartyIban]),
        counterpartyBic = row[BankTransactionsTable.counterpartyBic]?.let { Bic(it) },
        structuredCommunicationRaw = row[BankTransactionsTable.structuredCommunicationRaw],
        normalizedStructuredCommunication = row[BankTransactionsTable.normalizedStructuredCommunication]
            ?.let { StructuredCommunication(it) },
        freeCommunication = row[BankTransactionsTable.freeCommunication],
        descriptionRaw = row[BankTransactionsTable.descriptionRaw],
        status = row[BankTransactionsTable.status],
        resolutionType = row[BankTransactionsTable.resolutionType],
        matchedCashflowId = row[BankTransactionsTable.matchedCashflowId]
            ?.let { CashflowEntryId.parse(it.toString()) },
        matchedDocumentId = row[BankTransactionsTable.matchedDocumentId]
            ?.let { DocumentId.parse(it.toString()) },
        matchScore = row[BankTransactionsTable.matchScore]?.toDouble(),
        matchEvidence = evidenceList,
        matchedBy = row[BankTransactionsTable.matchedBy],
        matchedAt = row[BankTransactionsTable.matchedAt],
        ignoredReason = row[BankTransactionsTable.ignoredReason],
        ignoredAt = row[BankTransactionsTable.ignoredAt],
        ignoredBy = row[BankTransactionsTable.ignoredBy],
        statementTrust = row[BankTransactionsTable.statementTrust],
        transferPairId = row[BankTransactionsTable.transferPairId]
            ?.let { BankTransactionId(it.toKotlinUuid()) },
        createdAt = row[BankTransactionsTable.createdAt],
        updatedAt = row[BankTransactionsTable.updatedAt],
    )
}
