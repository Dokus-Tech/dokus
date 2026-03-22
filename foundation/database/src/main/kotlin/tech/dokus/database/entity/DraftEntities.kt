@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package tech.dokus.database.entity

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber

// =============================================================================
// Core financial draft entities (nullable fields, mirrors draft tables)
// =============================================================================

data class InvoiceDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val contactId: kotlin.uuid.Uuid? = null,
    val invoiceNumber: String? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val currency: Currency = Currency.Eur,
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null,
    val notes: String? = null,
    val senderIban: Iban? = null,
    val structuredCommunication: StructuredCommunication? = null,
    val items: List<InvoiceDraftItemEntity> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

data class InvoiceDraftItemEntity(
    val id: kotlin.uuid.Uuid,
    val draftId: kotlin.uuid.Uuid,
    val description: String,
    val quantity: Double? = null,
    val unitPrice: Money? = null,
    val vatRate: VatRate? = null,
    val lineTotal: Money? = null,
    val vatAmount: Money? = null,
    val sortOrder: Int = 0,
) {
    companion object
}

data class CreditNoteDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val contactId: kotlin.uuid.Uuid? = null,
    val creditNoteNumber: String? = null,
    val issueDate: LocalDate? = null,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val currency: Currency = Currency.Eur,
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null,
    val originalInvoiceNumber: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    val items: List<CreditNoteDraftItemEntity> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

data class CreditNoteDraftItemEntity(
    val id: kotlin.uuid.Uuid,
    val draftId: kotlin.uuid.Uuid,
    val description: String,
    val quantity: Double? = null,
    val unitPrice: Money? = null,
    val vatRate: VatRate? = null,
    val lineTotal: Money? = null,
    val vatAmount: Money? = null,
    val sortOrder: Int = 0,
) {
    companion object
}

data class ReceiptDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val merchantName: String? = null,
    val merchantVat: VatNumber? = null,
    val date: LocalDate? = null,
    val direction: DocumentDirection = DocumentDirection.Inbound,
    val currency: Currency = Currency.Eur,
    val totalAmount: Money? = null,
    val vatAmount: Money? = null,
    val receiptNumber: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

data class BankStatementDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Neutral,
    val accountIban: Iban? = null,
    val openingBalance: Money? = null,
    val closingBalance: Money? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val notes: String? = null,
    val transactions: List<BankStatementDraftTransactionEntity> = emptyList(),
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object
}

data class BankStatementDraftTransactionEntity(
    val id: kotlin.uuid.Uuid,
    val draftId: kotlin.uuid.Uuid,
    val transactionDate: LocalDate? = null,
    val signedAmount: Money? = null,
    val counterpartyName: String? = null,
    val counterpartyVat: VatNumber? = null,
    val counterpartyIban: Iban? = null,
    val counterpartyBic: String? = null,
    val counterpartyEmail: String? = null,
    val counterpartyCompanyNumber: String? = null,
    val structuredCommunicationRaw: String? = null,
    val freeCommunication: String? = null,
    val descriptionRaw: String? = null,
    val rowConfidence: Double = 0.0,
    val largeAmountFlag: Boolean = false,
    val excluded: Boolean = false,
    val potentialDuplicate: Boolean = false,
    val sortOrder: Int = 0,
) {
    companion object
}

// =============================================================================
// Classified draft/confirmed entities (individual types for all 48 classified types)
// =============================================================================

// ── ProForma ─────────────────────────────────────────────────────────────────

data class ProFormaDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class ProFormaConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Quote ────────────────────────────────────────────────────────────────────

data class QuoteDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class QuoteConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── OrderConfirmation ────────────────────────────────────────────────────────

data class OrderConfirmationDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class OrderConfirmationConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── DeliveryNote ─────────────────────────────────────────────────────────────

data class DeliveryNoteDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class DeliveryNoteConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Reminder ─────────────────────────────────────────────────────────────────

data class ReminderDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class ReminderConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── StatementOfAccount ───────────────────────────────────────────────────────

data class StatementOfAccountDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class StatementOfAccountConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── PurchaseOrder ────────────────────────────────────────────────────────────

data class PurchaseOrderDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class PurchaseOrderConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── ExpenseClaim ─────────────────────────────────────────────────────────────

data class ExpenseClaimDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class ExpenseClaimConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── BankFee ──────────────────────────────────────────────────────────────────

data class BankFeeDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class BankFeeConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── InterestStatement ────────────────────────────────────────────────────────

data class InterestStatementDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class InterestStatementConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── PaymentConfirmation ──────────────────────────────────────────────────────

data class PaymentConfirmationDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class PaymentConfirmationConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── VatReturn ────────────────────────────────────────────────────────────────

data class VatReturnDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class VatReturnConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── VatListing ───────────────────────────────────────────────────────────────

data class VatListingDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class VatListingConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── VatAssessment ────────────────────────────────────────────────────────────

data class VatAssessmentDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class VatAssessmentConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── IcListing ────────────────────────────────────────────────────────────────

data class IcListingDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class IcListingConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── OssReturn ────────────────────────────────────────────────────────────────

data class OssReturnDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class OssReturnConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── CorporateTax ─────────────────────────────────────────────────────────────

data class CorporateTaxDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class CorporateTaxConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── CorporateTaxAdvance ──────────────────────────────────────────────────────

data class CorporateTaxAdvanceDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class CorporateTaxAdvanceConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── TaxAssessment ────────────────────────────────────────────────────────────

data class TaxAssessmentDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class TaxAssessmentConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── PersonalTax ──────────────────────────────────────────────────────────────

data class PersonalTaxDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class PersonalTaxConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── WithholdingTax ───────────────────────────────────────────────────────────

data class WithholdingTaxDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class WithholdingTaxConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── SocialContribution ───────────────────────────────────────────────────────

data class SocialContributionDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class SocialContributionConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── SocialFund ───────────────────────────────────────────────────────────────

data class SocialFundDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class SocialFundConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── SelfEmployedContribution ─────────────────────────────────────────────────

data class SelfEmployedContributionDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class SelfEmployedContributionConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Vapz ─────────────────────────────────────────────────────────────────────

data class VapzDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class VapzConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── SalarySlip ───────────────────────────────────────────────────────────────

data class SalarySlipDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class SalarySlipConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── PayrollSummary ───────────────────────────────────────────────────────────

data class PayrollSummaryDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class PayrollSummaryConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── EmploymentContract ───────────────────────────────────────────────────────

data class EmploymentContractDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class EmploymentContractConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Dimona ───────────────────────────────────────────────────────────────────

data class DimonaDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class DimonaConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── C4 ───────────────────────────────────────────────────────────────────────

data class C4DraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class C4ConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── HolidayPay ───────────────────────────────────────────────────────────────

data class HolidayPayDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class HolidayPayConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Contract ─────────────────────────────────────────────────────────────────

data class ContractDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class ContractConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Lease ────────────────────────────────────────────────────────────────────

data class LeaseDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class LeaseConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Loan ─────────────────────────────────────────────────────────────────────

data class LoanDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class LoanConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Insurance ────────────────────────────────────────────────────────────────

data class InsuranceDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class InsuranceConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Dividend ─────────────────────────────────────────────────────────────────

data class DividendDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class DividendConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── ShareholderRegister ──────────────────────────────────────────────────────

data class ShareholderRegisterDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class ShareholderRegisterConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── CompanyExtract ───────────────────────────────────────────────────────────

data class CompanyExtractDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class CompanyExtractConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── AnnualAccounts ───────────────────────────────────────────────────────────

data class AnnualAccountsDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class AnnualAccountsConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── BoardMinutes ─────────────────────────────────────────────────────────────

data class BoardMinutesDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class BoardMinutesConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Subsidy ──────────────────────────────────────────────────────────────────

data class SubsidyDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class SubsidyConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Fine ─────────────────────────────────────────────────────────────────────

data class FineDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class FineConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Permit ───────────────────────────────────────────────────────────────────

data class PermitDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class PermitConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── CustomsDeclaration ───────────────────────────────────────────────────────

data class CustomsDeclarationDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class CustomsDeclarationConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Intrastat ────────────────────────────────────────────────────────────────

data class IntrastatDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class IntrastatConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── DepreciationSchedule ─────────────────────────────────────────────────────

data class DepreciationScheduleDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class DepreciationScheduleConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Inventory ────────────────────────────────────────────────────────────────

data class InventoryDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class InventoryConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

// ── Other ────────────────────────────────────────────────────────────────────

data class OtherDraftEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }

data class OtherConfirmedEntity(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val documentId: DocumentId,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) { companion object }
