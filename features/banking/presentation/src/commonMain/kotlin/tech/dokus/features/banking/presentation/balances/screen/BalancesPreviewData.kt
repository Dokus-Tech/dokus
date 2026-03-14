package tech.dokus.features.banking.presentation.balances.screen

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankAccountProvider
import tech.dokus.domain.enums.BankAccountStatus
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.AccountBalanceSeries
import tech.dokus.domain.model.BalanceHistoryPoint
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.features.banking.presentation.balances.mvi.BalancesState
import tech.dokus.foundation.app.state.DokusState

private val PreviewDateTime = LocalDateTime(2026, 3, 9, 15, 0)
private val PreviewTenantId = TenantId.generate()

private val PreviewAccounts = listOf(
    BankAccountDto(
        id = BankAccountId.generate(),
        tenantId = PreviewTenantId,
        iban = Iban("BE68539007547034"),
        name = "KBC Business",
        institutionName = "KBC",
        accountType = BankAccountType.Current,
        currency = Currency.Eur,
        provider = BankAccountProvider.Tink,
        balance = Money(1438042),
        balanceUpdatedAt = null,
        status = BankAccountStatus.Confirmed,
        isActive = true,
        createdAt = LocalDateTime(2026, 1, 1, 0, 0),
    ),
    BankAccountDto(
        id = BankAccountId.generate(),
        tenantId = PreviewTenantId,
        iban = Iban("BE42063012345678"),
        name = "Belfius Savings",
        institutionName = "Belfius",
        accountType = BankAccountType.Savings,
        currency = Currency.Eur,
        provider = BankAccountProvider.Coda,
        balance = Money(340000),
        balanceUpdatedAt = null,
        status = BankAccountStatus.Confirmed,
        isActive = true,
        createdAt = LocalDateTime(2026, 1, 1, 0, 0),
    ),
)

private val PreviewSummary = BankAccountSummary(
    totalBalance = Money(1778042),
    accountCount = 2,
    unmatchedCount = 3,
    totalUnresolvedAmount = Money(842050),
    matchedThisPeriod = 12,
    lastSyncedAt = null,
)

private val PreviewTransactionSummary = BankTransactionSummary(
    unmatchedCount = 3,
    needsReviewCount = 2,
    matchedCount = 12,
    ignoredCount = 5,
    totalCount = 22,
    totalUnresolvedAmount = Money(842050),
)

private val PreviewBalanceHistory = BalanceHistoryResponse(
    series = listOf(
        AccountBalanceSeries(
            accountId = PreviewAccounts[0].id,
            accountName = "KBC Business",
            points = listOf(
                BalanceHistoryPoint(LocalDate(2026, 2, 7), Money(1200000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 11), Money(1230000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 15), Money(1180000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 19), Money(1250000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 23), Money(1320000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 27), Money(1380000)),
                BalanceHistoryPoint(LocalDate(2026, 3, 3), Money(1350000)),
                BalanceHistoryPoint(LocalDate(2026, 3, 7), Money(1438042)),
            ),
        ),
        AccountBalanceSeries(
            accountId = PreviewAccounts[1].id,
            accountName = "Belfius Savings",
            points = listOf(
                BalanceHistoryPoint(LocalDate(2026, 2, 7), Money(340000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 11), Money(340000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 15), Money(340000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 19), Money(340000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 23), Money(340000)),
                BalanceHistoryPoint(LocalDate(2026, 2, 27), Money(340000)),
                BalanceHistoryPoint(LocalDate(2026, 3, 3), Money(340000)),
                BalanceHistoryPoint(LocalDate(2026, 3, 7), Money(340000)),
            ),
        ),
    ),
    totalSeries = listOf(
        BalanceHistoryPoint(LocalDate(2026, 2, 7), Money(1540000)),
        BalanceHistoryPoint(LocalDate(2026, 2, 11), Money(1570000)),
        BalanceHistoryPoint(LocalDate(2026, 2, 15), Money(1520000)),
        BalanceHistoryPoint(LocalDate(2026, 2, 19), Money(1590000)),
        BalanceHistoryPoint(LocalDate(2026, 2, 23), Money(1660000)),
        BalanceHistoryPoint(LocalDate(2026, 2, 27), Money(1720000)),
        BalanceHistoryPoint(LocalDate(2026, 3, 3), Money(1690000)),
        BalanceHistoryPoint(LocalDate(2026, 3, 7), Money(1778042)),
    ),
)

internal fun previewBalancesState() = BalancesState(
    accounts = DokusState.success(PreviewAccounts),
    summary = DokusState.success(PreviewSummary),
    transactionSummary = DokusState.success(PreviewTransactionSummary),
    balanceHistory = DokusState.success(PreviewBalanceHistory),
)
