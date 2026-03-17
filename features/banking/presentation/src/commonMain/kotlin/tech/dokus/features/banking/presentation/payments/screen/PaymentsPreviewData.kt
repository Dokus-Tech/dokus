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
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.TransactionCommunication
import tech.dokus.domain.model.TransactionIgnoreInfo
import tech.dokus.domain.model.TransactionMatchInfo
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.CounterpartySnapshot
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
        counterparty = CounterpartySnapshot(
            name = "Coolblue België NV",
            iban = Iban("BE68539007547034"),
        ),
        communication = TransactionCommunication.Structured(
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
        counterparty = CounterpartySnapshot(
            name = "Acme Corp",
            iban = Iban("BE71096123456769"),
        ),
        communication = TransactionCommunication.Structured(
            raw = "+++101/2345/67890+++",
            normalized = StructuredCommunication("+++101/2345/67890+++"),
        ),
        status = BankTransactionStatus.NeedsReview,
        matchedCashflowId = CashflowEntryId.generate(),
        matchScore = 0.88,
        matchEvidence = listOf("exact_amount", "counterparty_name_match", "within_due_window"),
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
        counterparty = CounterpartySnapshot(name = "DigitalOcean"),
        communication = TransactionCommunication.FreeForm(text = "DO Invoice #12345"),
        status = BankTransactionStatus.Matched,
        matchedBy = MatchedBy.Auto,
        resolutionType = ResolutionType.Document,
        matchScore = 1.0,
        matchEvidence = listOf("exact_amount", "structured_comm_match"),
        matchedAt = PreviewDateTime,
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
        counterparty = CounterpartySnapshot(
            name = "Proximus",
            iban = Iban("BE39539007547034"),
        ),
        status = BankTransactionStatus.Ignored,
        ignoredReason = IgnoredReason.BankFee,
        ignoredAt = PreviewDateTime,
        ignoredBy = "user",
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
        counterparty = CounterpartySnapshot(
            name = "Dokus Tech BVBA",
            iban = Iban("BE62510007547061"),
        ),
        communication = TransactionCommunication.Structured(
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
        counterparty = CounterpartySnapshot(name = "AWS Europe"),
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
