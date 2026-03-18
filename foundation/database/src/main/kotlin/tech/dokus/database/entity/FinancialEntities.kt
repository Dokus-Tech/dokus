package tech.dokus.database.entity

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.Percentage
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.enums.InvoiceDueDateMode
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.SettlementIntent
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.CreditNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExpenseId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.StructuredCommunication
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.TenantId

@Serializable
data class InvoiceEntity(
    val id: InvoiceId,
    val tenantId: TenantId,
    val contactId: ContactId,
    val direction: DocumentDirection,
    val invoiceNumber: InvoiceNumber,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val subtotalAmount: Money,
    val vatAmount: Money,
    val totalAmount: Money,
    val paidAmount: Money = Money.ZERO,
    val status: InvoiceStatus,
    val currency: Currency = Currency.Eur,
    val notes: String? = null,
    val paymentTermsDays: Int? = null,
    val dueDateMode: InvoiceDueDateMode = InvoiceDueDateMode.Terms,
    val structuredCommunication: StructuredCommunication? = null,
    val senderIban: Iban? = null,
    val senderBic: Bic? = null,
    val deliveryMethod: InvoiceDeliveryMethod = InvoiceDeliveryMethod.PdfExport,
    val termsAndConditions: String? = null,
    val items: List<InvoiceItemEntity> = emptyList(),
    val peppolId: PeppolId? = null,
    val peppolSentAt: LocalDateTime? = null,
    val peppolStatus: PeppolStatus? = null,
    val documentId: DocumentId? = null,
    val paymentLink: String? = null,
    val paymentLinkExpiresAt: LocalDateTime? = null,
    val paidAt: LocalDateTime? = null,
    val paymentMethod: PaymentMethod? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

@Serializable
data class InvoiceItemEntity(
    val id: String? = null,
    val invoiceId: InvoiceId? = null,
    val description: String,
    val quantity: Double,
    val unitPrice: Money,
    val vatRate: VatRate,
    val lineTotal: Money,
    val vatAmount: Money,
    val sortOrder: Int = 0,
) {
    companion object
}

@Serializable
data class ExpenseEntity(
    val id: ExpenseId,
    val tenantId: TenantId,
    val date: LocalDate,
    val merchant: String,
    val amount: Money,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,
    val category: ExpenseCategory,
    val description: String? = null,
    val documentId: DocumentId? = null,
    val contactId: ContactId? = null,
    val isDeductible: Boolean = true,
    val deductiblePercentage: Percentage = Percentage.FULL,
    val paymentMethod: PaymentMethod? = null,
    val isRecurring: Boolean = false,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

@Serializable
data class CreditNoteEntity(
    val id: CreditNoteId,
    val tenantId: TenantId,
    val contactId: ContactId,
    val creditNoteType: CreditNoteType,
    val creditNoteNumber: String,
    val issueDate: LocalDate,
    val subtotalAmount: Money,
    val vatAmount: Money,
    val totalAmount: Money,
    val status: CreditNoteStatus,
    val settlementIntent: SettlementIntent,
    val documentId: DocumentId? = null,
    val reason: String? = null,
    val currency: Currency = Currency.Eur,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}
