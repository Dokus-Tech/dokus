package tech.dokus.features.cashflow.presentation.detail.components.bankstatement

import tech.dokus.features.cashflow.presentation.detail.models.BankStatementTransactionUiRow
import tech.dokus.features.cashflow.presentation.detail.models.DocumentUiData

internal fun previewBankStatementTransactions(): List<BankStatementTransactionUiRow> = listOf(
    BankStatementTransactionUiRow(
        index = 0,
        date = "Jan 5",
        description = "SENDING MONEY TO BE86 3632 0614 5450",
        counterpartyName = "SRL Accounting & Tax Solutions",
        counterpartyIban = "BE86 3632 0614 5459",
        communication = "+++091/0044/28176+++",
        displayAmount = "-798.60",
        amountMinor = -79860,
        isExcluded = false,
        isDuplicate = false,
    ),
    BankStatementTransactionUiRow(
        index = 1,
        date = "Jan 13",
        description = "CREDIT TRANSFER FROM BE39 0019 2012 6619",
        counterpartyName = "Coolblue België NV",
        counterpartyIban = "BE39 7350 0001 0000",
        communication = null,
        displayAmount = "-289.00",
        amountMinor = -28900,
        isExcluded = true,
        isDuplicate = true,
    ),
    BankStatementTransactionUiRow(
        index = 2,
        date = "Jan 14",
        description = "EUROPEAN DIRECT DEBIT",
        counterpartyName = "Tesla Belgium BVBA",
        counterpartyIban = "BE72 0000 0000 4372",
        communication = null,
        displayAmount = "-346.97",
        amountMinor = -34697,
        isExcluded = true,
        isDuplicate = true,
    ),
    BankStatementTransactionUiRow(
        index = 3,
        date = "Jan 17",
        description = "CREDIT TRANSFER FROM BE39 0019 2012 6619",
        counterpartyName = "MEDIAHUIS TECHNOLOGY PRODUCT STUDIO",
        counterpartyIban = "BE39 0019 2012 6619",
        communication = "IV-051",
        displayAmount = "13,370.50",
        amountMinor = 1337050,
        isExcluded = false,
        isDuplicate = false,
    ),
    BankStatementTransactionUiRow(
        index = 4,
        date = "Jan 30",
        description = "SENDING MONEY TO BE85 6792 0036 3806",
        counterpartyName = "Donckers Schoten NV",
        counterpartyIban = "BE42 3100 0000 1234",
        communication = null,
        displayAmount = "-1,306.12",
        amountMinor = -130612,
        isExcluded = true,
        isDuplicate = true,
    ),
    BankStatementTransactionUiRow(
        index = 5,
        date = "Feb 4",
        description = "PAYMENT LEASING 0001/0001/BE/2600057216",
        counterpartyName = "KBC Bank NV",
        counterpartyIban = "Internal",
        communication = "Business loan - Feb",
        displayAmount = "-962.52",
        amountMinor = -96252,
        isExcluded = false,
        isDuplicate = false,
    ),
    BankStatementTransactionUiRow(
        index = 6,
        date = "Feb 25",
        description = "SENDING MONEY TO BE85 6792 0036 3806",
        counterpartyName = "Donckers Schoten NV",
        counterpartyIban = "BE42 3100 0000 1234",
        communication = "Fuel, Feb 2026",
        displayAmount = "-487.33",
        amountMinor = -48733,
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

internal fun previewBankStatementEmpty(): DocumentUiData.BankStatement =
    DocumentUiData.BankStatement(
        accountIban = "BE68 5390 0754 7034",
        institutionName = "KBC Bank Statement",
        periodStart = "Jan 1",
        periodEnd = "Mar 1, 2026",
        openingBalance = "14,523.61",
        closingBalance = "14,523.61",
        movement = "0.00",
        transactions = emptyList(),
    )

internal fun previewBankStatementAllExcluded(): DocumentUiData.BankStatement =
    DocumentUiData.BankStatement(
        accountIban = "BE68 5390 0754 7034",
        institutionName = "KBC Bank Statement",
        periodStart = "Jan 1",
        periodEnd = "Mar 1, 2026",
        openingBalance = "14,523.61",
        closingBalance = "12,310.42",
        movement = "-2,213.19",
        transactions = previewBankStatementTransactions().map {
            it.copy(isDuplicate = true, isExcluded = true)
        },
    )

internal fun previewBankStatementManyTransactions(): DocumentUiData.BankStatement {
    val base = previewBankStatementTransactions()
    val many = (0 until 30).map { i ->
        base[i % base.size].copy(
            index = i,
            date = "Mar ${(i % 28) + 1}",
            displayAmount = if (i % 5 == 0) "${(i + 1) * 100}.00" else "-${(i + 1) * 50}.00",
            amountMinor = if (i % 5 == 0) ((i + 1) * 10000).toLong() else -((i + 1) * 5000).toLong(),
            isDuplicate = i % 7 == 0,
            isExcluded = i % 7 == 0,
        )
    }
    return DocumentUiData.BankStatement(
        accountIban = "BE68 5390 0754 7034",
        institutionName = "KBC Bank Statement",
        periodStart = "Mar 1",
        periodEnd = "Mar 31, 2026",
        openingBalance = "14,523.61",
        closingBalance = "8,210.00",
        movement = "-6,313.61",
        transactions = many,
    )
}

internal fun previewBankStatementNoBalances(): DocumentUiData.BankStatement =
    DocumentUiData.BankStatement(
        accountIban = "BE68 5390 0754 7034",
        institutionName = "KBC Bank Statement",
        periodStart = "Jan 1",
        periodEnd = "Mar 1, 2026",
        openingBalance = null,
        closingBalance = null,
        movement = null,
        transactions = previewBankStatementTransactions().map {
            it.copy(isDuplicate = false, isExcluded = false)
        },
    )

internal fun previewBankStatementSingleTransaction(): DocumentUiData.BankStatement =
    DocumentUiData.BankStatement(
        accountIban = "BE68 5390 0754 7034",
        institutionName = "KBC Bank Statement",
        periodStart = "Jan 5",
        periodEnd = "Jan 5, 2026",
        openingBalance = "14,523.61",
        closingBalance = "13,725.01",
        movement = "-798.60",
        transactions = listOf(previewBankStatementTransactions()[0]),
    )
