package tech.dokus.features.cashflow.presentation.review.models

import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData

internal fun DocumentDraftData.toUiData(): DocumentUiData = when (this) {
    is InvoiceDraftData -> DocumentUiData.Invoice(
        direction = direction,
        invoiceNumber = invoiceNumber?.takeIf { it.isNotBlank() },
        issueDate = issueDate?.toString(),
        dueDate = dueDate?.toString(),
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        currencySign = currency.displaySign,
    )

    is CreditNoteDraftData -> DocumentUiData.CreditNote(
        direction = direction,
        creditNoteNumber = creditNoteNumber?.takeIf { it.isNotBlank() },
        issueDate = issueDate?.toString(),
        originalInvoiceNumber = originalInvoiceNumber?.takeIf { it.isNotBlank() },
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        currencySign = currency.displaySign,
    )

    is ReceiptDraftData -> DocumentUiData.Receipt(
        receiptNumber = receiptNumber?.takeIf { it.isNotBlank() },
        date = date?.toString(),
        totalAmount = totalAmount,
        vatAmount = vatAmount,
        currencySign = currency.displaySign,
    )

    is BankStatementDraftData -> DocumentUiData.BankStatement(
        accountIban = accountIban?.value,
        periodStart = periodStart?.toString(),
        periodEnd = periodEnd?.toString(),
        transactionCount = transactions.size,
    )
}
