package tech.dokus.features.banking.presentation.balances.screen

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.BankProvider
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.BankConnectionId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankConnectionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.features.banking.presentation.balances.mvi.BalancesState
import tech.dokus.foundation.app.state.DokusState

private val PreviewDateTime = LocalDateTime(2026, 3, 9, 15, 0)
private val PreviewTenantId = TenantId.generate()

private val PreviewConnections = listOf(
    BankConnectionDto(
        id = BankConnectionId.generate(),
        tenantId = PreviewTenantId,
        provider = BankProvider.Tink,
        institutionId = "kbc-001",
        institutionName = "KBC",
        accountId = "acc-001",
        accountName = "KBC Business",
        accountType = BankAccountType.Checking,
        currency = Currency.Eur,
        iban = Iban("BE68539007547034"),
        balance = Money(1438042),
        lastSyncedAt = LocalDateTime(2026, 3, 9, 15, 0),
        isActive = true,
        createdAt = LocalDateTime(2026, 1, 1, 0, 0),
        updatedAt = LocalDateTime(2026, 3, 9, 15, 0),
    ),
    BankConnectionDto(
        id = BankConnectionId.generate(),
        tenantId = PreviewTenantId,
        provider = BankProvider.Manual,
        institutionId = "belfius-001",
        institutionName = "Belfius",
        accountId = "acc-002",
        accountName = "Belfius Savings",
        accountType = BankAccountType.Savings,
        currency = Currency.Eur,
        iban = Iban("BE42063012345678"),
        balance = Money(340000),
        lastSyncedAt = LocalDateTime(2026, 3, 8, 10, 0),
        isActive = true,
        createdAt = LocalDateTime(2026, 1, 1, 0, 0),
        updatedAt = LocalDateTime(2026, 3, 8, 10, 0),
    ),
)

private val PreviewSummary = BankAccountSummary(
    totalBalance = Money(1778042),
    accountCount = 2,
    unmatchedCount = 3,
    totalUnresolvedAmount = Money(842050),
    matchedThisPeriod = 12,
    lastSyncedAt = LocalDateTime(2026, 3, 9, 15, 0),
)

private val PreviewTransactionSummary = BankTransactionSummary(
    unmatchedCount = 3,
    needsReviewCount = 2,
    matchedCount = 12,
    ignoredCount = 5,
    totalCount = 22,
    totalUnresolvedAmount = Money(842050),
)

internal fun previewBalancesState() = BalancesState(
    connections = DokusState.success(PreviewConnections),
    summary = DokusState.success(PreviewSummary),
    transactionSummary = DokusState.success(PreviewTransactionSummary),
    balanceHistory = DokusState.success(
        BalanceHistoryResponse(series = emptyList(), totalSeries = emptyList())
    ),
)
