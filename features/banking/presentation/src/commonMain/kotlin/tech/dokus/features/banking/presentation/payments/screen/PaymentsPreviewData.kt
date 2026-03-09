package tech.dokus.features.banking.presentation.payments.screen

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.banking.presentation.payments.mvi.PaymentFilterTab
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsState
import tech.dokus.foundation.app.state.DokusState

private val PreviewDate = LocalDate(2026, 2, 15)
private val PreviewDateTime = LocalDateTime(2026, 2, 15, 10, 0)
private val PreviewTenantId = TenantId.generate()

internal fun previewTransactions(): List<BankTransactionDto> = listOf(
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        source = BankTransactionSource.BankImport,
        transactionDate = LocalDate(2026, 2, 14),
        signedAmount = Money.parseOrThrow("-1250.00"),
        counterpartyName = "Coolblue België NV",
        counterpartyIban = Iban("BE68539007547034"),
        structuredCommunicationRaw = "+++090/9337/55493+++",
        status = BankTransactionStatus.Unmatched,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        source = BankTransactionSource.BankImport,
        transactionDate = LocalDate(2026, 2, 13),
        signedAmount = Money.parseOrThrow("3500.00"),
        counterpartyName = "Acme Corp",
        counterpartyIban = Iban("BE71096123456769"),
        structuredCommunicationRaw = "+++101/2345/67890+++",
        status = BankTransactionStatus.Suggested,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        source = BankTransactionSource.BankImport,
        transactionDate = LocalDate(2026, 2, 12),
        signedAmount = Money.parseOrThrow("-89.99"),
        counterpartyName = "DigitalOcean",
        descriptionRaw = "DO Invoice #12345",
        status = BankTransactionStatus.Linked,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        source = BankTransactionSource.BankImport,
        transactionDate = LocalDate(2026, 2, 11),
        signedAmount = Money.parseOrThrow("-42.50"),
        counterpartyName = "Proximus",
        counterpartyIban = Iban("BE39539007547034"),
        status = BankTransactionStatus.Ignored,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        source = BankTransactionSource.BankImport,
        transactionDate = LocalDate(2026, 2, 10),
        signedAmount = Money.parseOrThrow("7200.00"),
        counterpartyName = "Dokus Tech BVBA",
        counterpartyIban = Iban("BE62510007547061"),
        structuredCommunicationRaw = "+++200/0001/00042+++",
        status = BankTransactionStatus.Linked,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        source = BankTransactionSource.BankImport,
        transactionDate = LocalDate(2026, 2, 9),
        signedAmount = Money.parseOrThrow("-320.00"),
        counterpartyName = "AWS Europe",
        descriptionRaw = "AWS Monthly Invoice",
        status = BankTransactionStatus.Unmatched,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
)

internal val PreviewSummary = BankTransactionSummary(
    unmatchedCount = 12,
    needsReviewCount = 3,
    matchedCount = 45,
    ignoredCount = 5,
    totalCount = 65,
    totalUnresolvedAmount = Money.parseOrThrow("8420.50"),
)

internal fun previewPaymentsState(
    filterTab: PaymentFilterTab = PaymentFilterTab.All,
    selectedTransactionId: BankTransactionId? = null,
) = PaymentsState(
    transactions = DokusState.success(
        PaginationState(
            data = previewTransactions(),
            currentPage = 0,
            pageSize = 50,
            hasMorePages = true,
        )
    ),
    summary = DokusState.success(PreviewSummary),
    filterTab = filterTab,
    selectedTransactionId = selectedTransactionId,
)
