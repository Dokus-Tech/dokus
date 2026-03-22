package tech.dokus.features.banking.presentation.payments.screen

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummaryDto
import tech.dokus.domain.model.TransactionCommunicationDto
import tech.dokus.domain.model.TransactionIgnoreInfoDto
import tech.dokus.domain.model.TransactionMatchInfoDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.CounterpartySnapshotDto
import tech.dokus.features.banking.presentation.payments.mvi.PaymentFilterTab
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsState
import tech.dokus.foundation.app.state.DokusState

private val PreviewDateTime = LocalDateTime(2026, 2, 15, 10, 0)
private val PreviewTenantId = TenantId.generate()
private val PreviewAccountKbc = BankAccountId.generate()
private val PreviewAccountBelfius = BankAccountId.generate()

private val PreviewTransactions: List<BankTransactionDto> = listOf(
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        bankAccountId = PreviewAccountKbc,
        source = BankTransactionSource.PdfStatement,
        transactionDate = LocalDate(2026, 2, 14),
        signedAmount = Money.parseOrThrow("-1250.00"),
        counterparty = CounterpartySnapshotDto(
            name = "Coolblue België NV",
            iban = Iban("BE68539007547034"),
        ),
        communication = TransactionCommunicationDto.Structured(
            raw = "+++090/9337/55493+++",
            normalized = StructuredCommunication("+++090/9337/55493+++"),
        ),
        status = BankTransactionStatus.Unmatched,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        bankAccountId = PreviewAccountKbc,
        source = BankTransactionSource.PdfStatement,
        transactionDate = LocalDate(2026, 2, 13),
        signedAmount = Money.parseOrThrow("3500.00"),
        counterparty = CounterpartySnapshotDto(
            name = "Acme Corp",
            iban = Iban("BE71096123456769"),
        ),
        communication = TransactionCommunicationDto.Structured(
            raw = "+++101/2345/67890+++",
            normalized = StructuredCommunication("+++101/2345/67890+++"),
        ),
        status = BankTransactionStatus.NeedsReview,
        matchInfo = TransactionMatchInfoDto(
            cashflowEntryId = CashflowEntryId.generate(),
            matchedBy = MatchedBy.Auto,
            score = 0.88,
            evidence = listOf("exact_amount", "counterparty_name_match", "within_due_window"),
            matchedAt = PreviewDateTime,
        ),
        statementTrust = StatementTrust.High,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        bankAccountId = PreviewAccountKbc,
        source = BankTransactionSource.PdfStatement,
        transactionDate = LocalDate(2026, 2, 12),
        signedAmount = Money.parseOrThrow("-89.99"),
        counterparty = CounterpartySnapshotDto(name = "DigitalOcean"),
        communication = TransactionCommunicationDto.FreeForm(text = "DO Invoice #12345"),
        status = BankTransactionStatus.Matched,
        resolutionType = ResolutionType.Document,
        matchInfo = TransactionMatchInfoDto(
            cashflowEntryId = CashflowEntryId.generate(),
            matchedBy = MatchedBy.Auto,
            score = 1.0,
            evidence = listOf("exact_amount", "structured_comm_match"),
            matchedAt = PreviewDateTime,
        ),
        statementTrust = StatementTrust.High,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        bankAccountId = PreviewAccountBelfius,
        source = BankTransactionSource.PdfStatement,
        transactionDate = LocalDate(2026, 2, 11),
        signedAmount = Money.parseOrThrow("-42.50"),
        counterparty = CounterpartySnapshotDto(
            name = "Proximus",
            iban = Iban("BE39539007547034"),
        ),
        status = BankTransactionStatus.Ignored,
        ignoreInfo = TransactionIgnoreInfoDto(
            reason = IgnoredReason.BankFee,
            ignoredAt = PreviewDateTime,
            ignoredBy = "user",
        ),
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        bankAccountId = PreviewAccountBelfius,
        source = BankTransactionSource.PdfStatement,
        transactionDate = LocalDate(2026, 2, 10),
        signedAmount = Money.parseOrThrow("7200.00"),
        counterparty = CounterpartySnapshotDto(
            name = "Dokus Tech BVBA",
            iban = Iban("BE62510007547061"),
        ),
        communication = TransactionCommunicationDto.Structured(
            raw = "+++200/0001/00042+++",
            normalized = StructuredCommunication("+++200/0001/00042+++"),
        ),
        status = BankTransactionStatus.Matched,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
    BankTransactionDto(
        id = BankTransactionId.generate(),
        tenantId = PreviewTenantId,
        bankAccountId = PreviewAccountBelfius,
        source = BankTransactionSource.PdfStatement,
        transactionDate = LocalDate(2026, 2, 9),
        signedAmount = Money.parseOrThrow("-320.00"),
        counterparty = CounterpartySnapshotDto(name = "AWS Europe"),
        descriptionRaw = "AWS Monthly Invoice",
        status = BankTransactionStatus.Unmatched,
        currency = Currency.Eur,
        createdAt = PreviewDateTime,
        updatedAt = PreviewDateTime,
    ),
)

internal val PreviewSummary = BankTransactionSummaryDto(
    unmatchedCount = 12,
    needsReviewCount = 3,
    matchedCount = 45,
    ignoredCount = 5,
    totalCount = 65,
    totalUnresolvedAmount = Money.parseOrThrow("8420.50"),
)

private val PreviewAccountNames = mapOf(
    PreviewAccountKbc to "KBC Business",
    PreviewAccountBelfius to "Belfius",
)

internal fun previewPaymentsState(
    filterTab: PaymentFilterTab = PaymentFilterTab.All,
    selectedTransactionId: BankTransactionId? = null,
) = PaymentsState(
    transactions = DokusState.success(
        PaginationState(
            data = PreviewTransactions,
            currentPage = 0,
            pageSize = 50,
            hasMorePages = true,
        )
    ),
    summary = DokusState.success(PreviewSummary),
    accountNames = PreviewAccountNames,
    filterTab = filterTab,
    selectedTransactionId = selectedTransactionId,
)

internal fun previewPaymentsStateWithSelection(): PaymentsState {
    val suggestedTx = PreviewTransactions.first { it.status == BankTransactionStatus.NeedsReview }
    return previewPaymentsState(selectedTransactionId = suggestedTx.id)
}
