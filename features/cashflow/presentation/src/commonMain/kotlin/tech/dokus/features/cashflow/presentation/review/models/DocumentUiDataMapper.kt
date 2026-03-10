package tech.dokus.features.cashflow.presentation.review.models

import tech.dokus.domain.Money
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData

internal fun DocumentDraftData.toUiData(): DocumentUiData = when (this) {
    is InvoiceDraftData -> {
        val sign = currency.displaySign
        DocumentUiData.Invoice(
            direction = direction,
            invoiceNumber = invoiceNumber?.takeIf { it.isNotBlank() },
            issueDate = issueDate?.toString(),
            dueDate = dueDate?.toString(),
            subtotalAmount = subtotalAmount,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            currencySign = sign,
            lineItems = lineItems.map { it.toLineItemUiData(sign) },
            notes = notes?.takeIf { it.isNotBlank() },
            iban = iban?.value?.takeIf { it.isNotBlank() },
            primaryDescription = notes?.takeIf { it.isNotBlank() } ?: "Invoice",
        )
    }

    is CreditNoteDraftData -> {
        val sign = currency.displaySign
        DocumentUiData.CreditNote(
            direction = direction,
            creditNoteNumber = creditNoteNumber?.takeIf { it.isNotBlank() },
            issueDate = issueDate?.toString(),
            originalInvoiceNumber = originalInvoiceNumber?.takeIf { it.isNotBlank() },
            subtotalAmount = subtotalAmount,
            vatAmount = vatAmount,
            totalAmount = totalAmount,
            currencySign = sign,
            lineItems = lineItems.map { it.toLineItemUiData(sign) },
            reason = reason?.takeIf { it.isNotBlank() },
            notes = notes?.takeIf { it.isNotBlank() },
            primaryDescription = reason?.takeIf { it.isNotBlank() }
                ?: notes?.takeIf { it.isNotBlank() }
                ?: "Credit note",
        )
    }

    is ReceiptDraftData -> DocumentUiData.Receipt(
        receiptNumber = receiptNumber?.takeIf { it.isNotBlank() },
        date = date?.toString(),
        totalAmount = totalAmount,
        vatAmount = vatAmount,
        currencySign = currency.displaySign,
        notes = notes?.takeIf { it.isNotBlank() },
        primaryDescription = "Receipt",
    )

    is BankStatementDraftData -> DocumentUiData.BankStatement(
        accountIban = accountIban?.value,
        periodStart = periodStart?.toString(),
        periodEnd = periodEnd?.toString(),
        transactionCount = transactions.size,
    )
}

private fun FinancialLineItem.toLineItemUiData(currencySign: String): LineItemUiData {
    val net = netAmount ?: unitPrice?.let { unit -> (quantity ?: 1L) * unit }
    val displayAmount = net?.let { "$currencySign${Money(it).toDisplayString()}" } ?: "\u2014"
    return LineItemUiData(
        description = description.ifBlank { "\u2014" },
        displayAmount = displayAmount,
    )
}
