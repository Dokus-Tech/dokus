@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package tech.dokus.database.entity

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankAccountProvider
import tech.dokus.domain.enums.BankAccountStatus
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.TenantId

data class BankAccountEntity(
    val id: BankAccountId,
    val tenantId: TenantId,
    val iban: Iban? = null,
    val name: String,
    val institutionName: String,
    val accountType: BankAccountType,
    val currency: Currency = Currency.Eur,
    val provider: BankAccountProvider,
    val balance: Money? = null,
    val balanceUpdatedAt: LocalDateTime? = null,
    val status: BankAccountStatus = BankAccountStatus.Confirmed,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val parentAccountId: BankAccountId? = null,
    val providerAccountId: String? = null,
) {
    companion object
}

data class BankTransactionEntity(
    val id: BankTransactionId,
    val tenantId: TenantId,
    val bankAccountId: BankAccountId? = null,
    val documentId: DocumentId? = null,
    val source: BankTransactionSource = BankTransactionSource.PdfStatement,
    val transactionDate: LocalDate,
    val valueDate: LocalDate? = null,
    val signedAmount: Money,
    val currency: Currency = Currency.Eur,
    val counterpartyName: String? = null,
    val counterpartyIban: Iban? = null,
    val counterpartyBic: Bic? = null,
    val structuredCommunicationRaw: String? = null,
    val normalizedStructuredCommunication: StructuredCommunication? = null,
    val freeCommunication: String? = null,
    val descriptionRaw: String? = null,
    val status: BankTransactionStatus,
    val resolutionType: ResolutionType? = null,
    val matchedCashflowId: CashflowEntryId? = null,
    val matchedDocumentId: DocumentId? = null,
    val matchScore: Double? = null,
    val matchEvidence: List<String> = emptyList(),
    val matchedBy: MatchedBy? = null,
    val matchedAt: LocalDateTime? = null,
    val ignoredReason: IgnoredReason? = null,
    val ignoredAt: LocalDateTime? = null,
    val ignoredBy: String? = null,
    val statementTrust: StatementTrust = StatementTrust.Low,
    val transferPairId: BankTransactionId? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

data class BankStatementEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val bankAccountId: BankAccountId? = null,
    val documentId: DocumentId? = null,
    val source: BankTransactionSource,
    val statementTrust: StatementTrust,
    val fileHash: String? = null,
    val accountIban: Iban? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val openingBalance: Money? = null,
    val closingBalance: Money? = null,
    val transactionCount: Int = 0,
    val createdAt: LocalDateTime,
) {
    companion object
}
