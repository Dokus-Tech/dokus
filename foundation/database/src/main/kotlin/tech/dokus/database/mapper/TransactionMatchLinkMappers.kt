package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.TransactionMatchLinkEntity
import tech.dokus.database.tables.documents.TransactionMatchLinksTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId

internal fun TransactionMatchLinkEntity.Companion.from(row: ResultRow): TransactionMatchLinkEntity {
    return TransactionMatchLinkEntity(
        id = row[TransactionMatchLinksTable.id].value,
        tenantId = TenantId.parse(row[TransactionMatchLinksTable.tenantId].toString()),
        documentId = DocumentId.parse(row[TransactionMatchLinksTable.documentId].toString()),
        cashflowEntryId = CashflowEntryId.parse(row[TransactionMatchLinksTable.cashflowEntryId].toString()),
        importedBankTransactionId = BankTransactionId.parse(row[TransactionMatchLinksTable.importedBankTransactionId].toString()),
        documentType = row[TransactionMatchLinksTable.documentType],
        allocatedAmount = row[TransactionMatchLinksTable.allocatedAmount]?.let { Money.fromDbDecimal(it) },
        status = row[TransactionMatchLinksTable.status],
        createdBy = row[TransactionMatchLinksTable.createdBy],
        confidenceScore = row[TransactionMatchLinksTable.confidenceScore]?.toDouble(),
        scoreMargin = row[TransactionMatchLinksTable.scoreMargin]?.toDouble(),
        reasonsJson = row[TransactionMatchLinksTable.reasonsJson],
        rulesJson = row[TransactionMatchLinksTable.rulesJson],
        matchedAt = row[TransactionMatchLinksTable.matchedAt],
        autoPaidAt = row[TransactionMatchLinksTable.autoPaidAt],
        reversedAt = row[TransactionMatchLinksTable.reversedAt],
        reversedByUserId = row[TransactionMatchLinksTable.reversedByUserId],
        reversalReason = row[TransactionMatchLinksTable.reversalReason],
    )
}
