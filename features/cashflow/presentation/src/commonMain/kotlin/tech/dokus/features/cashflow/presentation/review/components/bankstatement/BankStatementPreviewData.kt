package tech.dokus.features.cashflow.presentation.review.components.bankstatement

import tech.dokus.features.cashflow.presentation.review.models.BankStatementTransactionUiRow
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData

internal fun previewBankStatementTransactions(): List<BankStatementTransactionUiRow> = listOf(
    BankStatementTransactionUiRow(
        index = 0,
        date = "Jan 5",
        description = "SENDING MONEY TO BE86 3632 0614 5450",
        counterpartyName = "SRL Accounting & Tax Solutions",
        communication = "+++091/0044/28176+++",
        displayAmount = "-798.60",
        amountMinor = -79860,
        isPositive = false,
        isExcluded = false,
        isDuplicate = false,
    ),
    BankStatementTransactionUiRow(
        index = 1,
        date = "Jan 13",
        description = "CREDIT TRANSFER FROM BE39 0019 2012 6619",
        counterpartyName = "Coolblue België NV",
        communication = null,
        displayAmount = "-289.00",
        amountMinor = -28900,
        isPositive = false,
        isExcluded = true,
        isDuplicate = true,
    ),
    BankStatementTransactionUiRow(
        index = 2,
        date = "Jan 14",
        description = "EUROPEAN DIRECT DEBIT",
        counterpartyName = "Tesla Belgium BVBA",
        communication = null,
        displayAmount = "-346.97",
        amountMinor = -34697,
        isPositive = false,
        isExcluded = true,
        isDuplicate = true,
    ),
    BankStatementTransactionUiRow(
        index = 3,
        date = "Jan 17",
        description = "CREDIT TRANSFER FROM BE39 0019 2012 6619",
        counterpartyName = "MEDIAHUIS TECHNOLOGY PRODUCT STUDIO",
        communication = "IV-051",
        displayAmount = "13,370.50",
        amountMinor = 1337050,
        isPositive = true,
        isExcluded = false,
        isDuplicate = false,
    ),
    BankStatementTransactionUiRow(
        index = 4,
        date = "Jan 30",
        description = "SENDING MONEY TO BE85 6792 0036 3806",
        counterpartyName = "Donckers Schoten NV",
        communication = null,
        displayAmount = "-1,306.12",
        amountMinor = -130612,
        isPositive = false,
        isExcluded = true,
        isDuplicate = true,
    ),
    BankStatementTransactionUiRow(
        index = 5,
        date = "Feb 4",
        description = "PAYMENT LEASING 0001/0001/BE/2600057216",
        counterpartyName = "KBC Bank NV",
        communication = "Business loan - Feb",
        displayAmount = "-962.52",
        amountMinor = -96252,
        isPositive = false,
        isExcluded = false,
        isDuplicate = false,
    ),
    BankStatementTransactionUiRow(
        index = 6,
        date = "Feb 25",
        description = "SENDING MONEY TO BE85 6792 0036 3806",
        counterpartyName = "Donckers Schoten NV",
        communication = "Fuel, Feb 2026",
        displayAmount = "-487.33",
        amountMinor = -48733,
        isPositive = false,
        isExcluded = false,
        isDuplicate = false,
    ),
)

internal fun previewBankStatementUiData(): DocumentUiData.BankStatement =
    DocumentUiData.BankStatement(
        accountIban = "BE68 5390 0754 7034",
        institutionName = "KBC Bank Statement",
        periodStart = "Jan 1",
        periodEnd = "Mar 1, 2026",
        openingBalance = "14,523.61",
        closingBalance = "12,310.42",
        movement = "-2,213.19",
        transactions = previewBankStatementTransactions(),
    )

internal fun previewBankStatementUiDataNoDuplicates(): DocumentUiData.BankStatement =
    DocumentUiData.BankStatement(
        accountIban = "BE68 5390 0754 7034",
        institutionName = "KBC Bank Statement",
        periodStart = "Jan 1",
        periodEnd = "Mar 1, 2026",
        openingBalance = "14,523.61",
        closingBalance = "12,310.42",
        movement = "-2,213.19",
        transactions = previewBankStatementTransactions().map {
            it.copy(isDuplicate = false, isExcluded = false)
        },
    )
