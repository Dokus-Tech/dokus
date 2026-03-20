package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.repository.cashflow.TransactionMatchLinkRecord
import tech.dokus.database.tables.documents.TransactionMatchLinksTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

internal fun ResultRow.toTransactionMatchLinkRecord(): TransactionMatchLinkRecord {
    return TransactionMatchLinkRecord(
        id = this[TransactionMatchLinksTable.id].value,
        tenantId = TenantId.parse(this[TransactionMatchLinksTable.tenantId].toString()),
        documentId = DocumentId.parse(this[TransactionMatchLinksTable.documentId].toString()),
        cashflowEntryId = CashflowEntryId.parse(this[TransactionMatchLinksTable.cashflowEntryId].toString()),
        importedBankTransactionId = BankTransactionId.parse(this[TransactionMatchLinksTable.importedBankTransactionId].toString()),
        documentType = this[TransactionMatchLinksTable.documentType],
        allocatedAmount = this[TransactionMatchLinksTable.allocatedAmount]?.let { Money.fromDbDecimal(it) },
        status = this[TransactionMatchLinksTable.status],
        createdBy = this[TransactionMatchLinksTable.createdBy],
        confidenceScore = this[TransactionMatchLinksTable.confidenceScore]?.toDouble(),
        scoreMargin = this[TransactionMatchLinksTable.scoreMargin]?.toDouble(),
        reasonsJson = this[TransactionMatchLinksTable.reasonsJson],
        rulesJson = this[TransactionMatchLinksTable.rulesJson],
        matchedAt = this[TransactionMatchLinksTable.matchedAt],
        autoPaidAt = this[TransactionMatchLinksTable.autoPaidAt],
        reversedAt = this[TransactionMatchLinksTable.reversedAt],
        reversedByUserId = this[TransactionMatchLinksTable.reversedByUserId],
        reversalReason = this[TransactionMatchLinksTable.reversalReason],
    )
}
