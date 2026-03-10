package tech.dokus.features.cashflow.presentation.review.models

import androidx.compose.runtime.Immutable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection

@Immutable
sealed interface DocumentUiData {

    @Immutable
    data class Invoice(
        val direction: DocumentDirection,
        val invoiceNumber: String?,
        val issueDate: String?,
        val dueDate: String?,
        val subtotalAmount: Money?,
        val vatAmount: Money?,
        val totalAmount: Money?,
        val currencySign: String,
    ) : DocumentUiData

    @Immutable
    data class CreditNote(
        val direction: DocumentDirection,
        val creditNoteNumber: String?,
        val issueDate: String?,
        val originalInvoiceNumber: String?,
        val subtotalAmount: Money?,
        val vatAmount: Money?,
        val totalAmount: Money?,
        val currencySign: String,
    ) : DocumentUiData

    @Immutable
    data class Receipt(
        val receiptNumber: String?,
        val date: String?,
        val totalAmount: Money?,
        val vatAmount: Money?,
        val currencySign: String,
    ) : DocumentUiData

    @Immutable
    data class BankStatement(
        val accountIban: String?,
        val periodStart: String?,
        val periodEnd: String?,
        val transactionCount: Int,
    ) : DocumentUiData
}
