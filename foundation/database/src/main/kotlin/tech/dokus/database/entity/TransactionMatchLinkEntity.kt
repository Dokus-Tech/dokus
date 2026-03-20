package tech.dokus.database.entity

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PaymentCreatedBy
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import java.util.UUID

data class TransactionMatchLinkEntity(
    val id: UUID,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val cashflowEntryId: CashflowEntryId,
    val importedBankTransactionId: BankTransactionId,
    val documentType: DocumentType?,
    val allocatedAmount: Money?,
    val status: AutoMatchStatus,
    val createdBy: PaymentCreatedBy,
    val confidenceScore: Double?,
    val scoreMargin: Double?,
    val reasonsJson: String?,
    val rulesJson: String?,
    val matchedAt: LocalDateTime,
    val autoPaidAt: LocalDateTime?,
    val reversedAt: LocalDateTime?,
    val reversedByUserId: UUID?,
    val reversalReason: String?,
) {
    companion object
}
