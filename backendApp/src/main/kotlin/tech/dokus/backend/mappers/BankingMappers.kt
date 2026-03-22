package tech.dokus.backend.mappers

import tech.dokus.database.entity.BankAccountEntity
import tech.dokus.database.entity.BankTransactionEntity
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.TransactionIgnoreInfoDto
import tech.dokus.domain.model.TransactionMatchInfoDto
import tech.dokus.domain.model.TransactionCommunicationDto
import tech.dokus.domain.model.contact.CounterpartySnapshotDto

fun BankAccountDto.Companion.from(entity: BankAccountEntity) = BankAccountDto(
    id = entity.id,
    tenantId = entity.tenantId,
    iban = entity.iban,
    name = entity.name,
    institutionName = entity.institutionName,
    accountType = entity.accountType,
    currency = entity.currency,
    provider = entity.provider,
    balance = entity.balance,
    balanceUpdatedAt = entity.balanceUpdatedAt,
    status = entity.status,
    isActive = entity.isActive,
    createdAt = entity.createdAt,
    parentAccountId = entity.parentAccountId,
    providerAccountId = entity.providerAccountId,
)

fun BankTransactionDto.Companion.from(entity: BankTransactionEntity) = BankTransactionDto(
    id = entity.id,
    tenantId = entity.tenantId,
    bankAccountId = entity.bankAccountId,
    documentId = entity.documentId,
    source = entity.source,
    transactionDate = entity.transactionDate,
    valueDate = entity.valueDate,
    signedAmount = entity.signedAmount,
    currency = entity.currency,
    counterparty = CounterpartySnapshotDto(
        name = entity.counterpartyName,
        iban = entity.counterpartyIban,
        bic = entity.counterpartyBic,
    ),
    communication = TransactionCommunicationDto.from(
        structuredCommunicationRaw = entity.structuredCommunicationRaw,
        freeCommunication = entity.freeCommunication,
    ),
    descriptionRaw = entity.descriptionRaw,
    status = entity.status,
    resolutionType = entity.resolutionType,
    matchInfo = entity.matchedCashflowId?.let { cashflowId ->
        TransactionMatchInfoDto(
            cashflowEntryId = cashflowId,
            documentId = entity.matchedDocumentId,
            score = entity.matchScore ?: 0.0,
            evidence = entity.matchEvidence,
            matchedBy = entity.matchedBy ?: return@let null,
            matchedAt = entity.matchedAt ?: return@let null,
        )
    },
    ignoreInfo = entity.ignoredReason?.let { reason ->
        TransactionIgnoreInfoDto(
            reason = reason,
            ignoredAt = entity.ignoredAt ?: return@let null,
            ignoredBy = entity.ignoredBy,
        )
    },
    statementTrust = entity.statementTrust,
    transferPairId = entity.transferPairId,
    createdAt = entity.createdAt,
    updatedAt = entity.updatedAt,
)
