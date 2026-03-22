package tech.dokus.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Email
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.CounterpartySnapshotDto

/**
 * Canonical, normalized draft data shown to users and used for confirmation.
 * This is deliberately AI-agnostic: no prompt/tool metadata, no token usage.
 */
@Serializable
sealed interface DocumentDraftData {
    companion object
}

fun DocumentDraftData.toDirection(): DocumentDirection = when (this) {
    is InvoiceDraftData -> direction
    is CreditNoteDraftData -> direction
    is ReceiptDraftData -> direction
    is BankStatementDraftData -> direction
    is ProFormaDraftData -> direction
    is QuoteDraftData -> direction
    is OrderConfirmationDraftData -> direction
    is DeliveryNoteDraftData -> direction
    is ReminderDraftData -> direction
    is StatementOfAccountDraftData -> direction
    is PurchaseOrderDraftData -> direction
    is ExpenseClaimDraftData -> direction
    is BankFeeDraftData -> direction
    is InterestStatementDraftData -> direction
    is PaymentConfirmationDraftData -> direction
    is VatReturnDraftData -> direction
    is VatListingDraftData -> direction
    is VatAssessmentDraftData -> direction
    is IcListingDraftData -> direction
    is OssReturnDraftData -> direction
    is CorporateTaxDraftData -> direction
    is CorporateTaxAdvanceDraftData -> direction
    is TaxAssessmentDraftData -> direction
    is PersonalTaxDraftData -> direction
    is WithholdingTaxDraftData -> direction
    is SocialContributionDraftData -> direction
    is SocialFundDraftData -> direction
    is SelfEmployedContributionDraftData -> direction
    is VapzDraftData -> direction
    is SalarySlipDraftData -> direction
    is PayrollSummaryDraftData -> direction
    is EmploymentContractDraftData -> direction
    is DimonaDraftData -> direction
    is C4DraftData -> direction
    is HolidayPayDraftData -> direction
    is ContractDraftData -> direction
    is LeaseDraftData -> direction
    is LoanDraftData -> direction
    is InsuranceDraftData -> direction
    is DividendDraftData -> direction
    is ShareholderRegisterDraftData -> direction
    is CompanyExtractDraftData -> direction
    is AnnualAccountsDraftData -> direction
    is BoardMinutesDraftData -> direction
    is SubsidyDraftData -> direction
    is FineDraftData -> direction
    is PermitDraftData -> direction
    is CustomsDeclarationDraftData -> direction
    is IntrastatDraftData -> direction
    is DepreciationScheduleDraftData -> direction
    is InventoryDraftData -> direction
    is OtherDraftData -> direction
}

fun DocumentDraftData.toDocumentType(): DocumentType = when (this) {
    is InvoiceDraftData -> DocumentType.Invoice
    is CreditNoteDraftData -> DocumentType.CreditNote
    is ReceiptDraftData -> DocumentType.Receipt
    is BankStatementDraftData -> DocumentType.BankStatement
    is ProFormaDraftData -> DocumentType.ProForma
    is QuoteDraftData -> DocumentType.Quote
    is OrderConfirmationDraftData -> DocumentType.OrderConfirmation
    is DeliveryNoteDraftData -> DocumentType.DeliveryNote
    is ReminderDraftData -> DocumentType.Reminder
    is StatementOfAccountDraftData -> DocumentType.StatementOfAccount
    is PurchaseOrderDraftData -> DocumentType.PurchaseOrder
    is ExpenseClaimDraftData -> DocumentType.ExpenseClaim
    is BankFeeDraftData -> DocumentType.BankFee
    is InterestStatementDraftData -> DocumentType.InterestStatement
    is PaymentConfirmationDraftData -> DocumentType.PaymentConfirmation
    is VatReturnDraftData -> DocumentType.VatReturn
    is VatListingDraftData -> DocumentType.VatListing
    is VatAssessmentDraftData -> DocumentType.VatAssessment
    is IcListingDraftData -> DocumentType.IcListing
    is OssReturnDraftData -> DocumentType.OssReturn
    is CorporateTaxDraftData -> DocumentType.CorporateTax
    is CorporateTaxAdvanceDraftData -> DocumentType.CorporateTaxAdvance
    is TaxAssessmentDraftData -> DocumentType.TaxAssessment
    is PersonalTaxDraftData -> DocumentType.PersonalTax
    is WithholdingTaxDraftData -> DocumentType.WithholdingTax
    is SocialContributionDraftData -> DocumentType.SocialContribution
    is SocialFundDraftData -> DocumentType.SocialFund
    is SelfEmployedContributionDraftData -> DocumentType.SelfEmployedContribution
    is VapzDraftData -> DocumentType.Vapz
    is SalarySlipDraftData -> DocumentType.SalarySlip
    is PayrollSummaryDraftData -> DocumentType.PayrollSummary
    is EmploymentContractDraftData -> DocumentType.EmploymentContract
    is DimonaDraftData -> DocumentType.Dimona
    is C4DraftData -> DocumentType.C4
    is HolidayPayDraftData -> DocumentType.HolidayPay
    is ContractDraftData -> DocumentType.Contract
    is LeaseDraftData -> DocumentType.Lease
    is LoanDraftData -> DocumentType.Loan
    is InsuranceDraftData -> DocumentType.Insurance
    is DividendDraftData -> DocumentType.Dividend
    is ShareholderRegisterDraftData -> DocumentType.ShareholderRegister
    is CompanyExtractDraftData -> DocumentType.CompanyExtract
    is AnnualAccountsDraftData -> DocumentType.AnnualAccounts
    is BoardMinutesDraftData -> DocumentType.BoardMinutes
    is SubsidyDraftData -> DocumentType.Subsidy
    is FineDraftData -> DocumentType.Fine
    is PermitDraftData -> DocumentType.Permit
    is CustomsDeclarationDraftData -> DocumentType.CustomsDeclaration
    is IntrastatDraftData -> DocumentType.Intrastat
    is DepreciationScheduleDraftData -> DocumentType.DepreciationSchedule
    is InventoryDraftData -> DocumentType.Inventory
    is OtherDraftData -> DocumentType.Other
}

fun DocumentDraftData.toTotalAmount(): Money? = when (this) {
    is InvoiceDraftData -> totalAmount
    is CreditNoteDraftData -> totalAmount
    is ReceiptDraftData -> totalAmount
    is BankStatementDraftData -> null
    is ProFormaDraftData -> null
    is QuoteDraftData -> null
    is OrderConfirmationDraftData -> null
    is DeliveryNoteDraftData -> null
    is ReminderDraftData -> null
    is StatementOfAccountDraftData -> null
    is PurchaseOrderDraftData -> null
    is ExpenseClaimDraftData -> null
    is BankFeeDraftData -> null
    is InterestStatementDraftData -> null
    is PaymentConfirmationDraftData -> null
    is VatReturnDraftData -> null
    is VatListingDraftData -> null
    is VatAssessmentDraftData -> null
    is IcListingDraftData -> null
    is OssReturnDraftData -> null
    is CorporateTaxDraftData -> null
    is CorporateTaxAdvanceDraftData -> null
    is TaxAssessmentDraftData -> null
    is PersonalTaxDraftData -> null
    is WithholdingTaxDraftData -> null
    is SocialContributionDraftData -> null
    is SocialFundDraftData -> null
    is SelfEmployedContributionDraftData -> null
    is VapzDraftData -> null
    is SalarySlipDraftData -> null
    is PayrollSummaryDraftData -> null
    is EmploymentContractDraftData -> null
    is DimonaDraftData -> null
    is C4DraftData -> null
    is HolidayPayDraftData -> null
    is ContractDraftData -> null
    is LeaseDraftData -> null
    is LoanDraftData -> null
    is InsuranceDraftData -> null
    is DividendDraftData -> null
    is ShareholderRegisterDraftData -> null
    is CompanyExtractDraftData -> null
    is AnnualAccountsDraftData -> null
    is BoardMinutesDraftData -> null
    is SubsidyDraftData -> null
    is FineDraftData -> null
    is PermitDraftData -> null
    is CustomsDeclarationDraftData -> null
    is IntrastatDraftData -> null
    is DepreciationScheduleDraftData -> null
    is InventoryDraftData -> null
    is OtherDraftData -> null
}

fun DocumentDraftData.toCurrency(): Currency = when (this) {
    is InvoiceDraftData -> currency
    is CreditNoteDraftData -> currency
    is ReceiptDraftData -> currency
    is BankStatementDraftData -> Currency.default
    is ProFormaDraftData -> Currency.default
    is QuoteDraftData -> Currency.default
    is OrderConfirmationDraftData -> Currency.default
    is DeliveryNoteDraftData -> Currency.default
    is ReminderDraftData -> Currency.default
    is StatementOfAccountDraftData -> Currency.default
    is PurchaseOrderDraftData -> Currency.default
    is ExpenseClaimDraftData -> Currency.default
    is BankFeeDraftData -> Currency.default
    is InterestStatementDraftData -> Currency.default
    is PaymentConfirmationDraftData -> Currency.default
    is VatReturnDraftData -> Currency.default
    is VatListingDraftData -> Currency.default
    is VatAssessmentDraftData -> Currency.default
    is IcListingDraftData -> Currency.default
    is OssReturnDraftData -> Currency.default
    is CorporateTaxDraftData -> Currency.default
    is CorporateTaxAdvanceDraftData -> Currency.default
    is TaxAssessmentDraftData -> Currency.default
    is PersonalTaxDraftData -> Currency.default
    is WithholdingTaxDraftData -> Currency.default
    is SocialContributionDraftData -> Currency.default
    is SocialFundDraftData -> Currency.default
    is SelfEmployedContributionDraftData -> Currency.default
    is VapzDraftData -> Currency.default
    is SalarySlipDraftData -> Currency.default
    is PayrollSummaryDraftData -> Currency.default
    is EmploymentContractDraftData -> Currency.default
    is DimonaDraftData -> Currency.default
    is C4DraftData -> Currency.default
    is HolidayPayDraftData -> Currency.default
    is ContractDraftData -> Currency.default
    is LeaseDraftData -> Currency.default
    is LoanDraftData -> Currency.default
    is InsuranceDraftData -> Currency.default
    is DividendDraftData -> Currency.default
    is ShareholderRegisterDraftData -> Currency.default
    is CompanyExtractDraftData -> Currency.default
    is AnnualAccountsDraftData -> Currency.default
    is BoardMinutesDraftData -> Currency.default
    is SubsidyDraftData -> Currency.default
    is FineDraftData -> Currency.default
    is PermitDraftData -> Currency.default
    is CustomsDeclarationDraftData -> Currency.default
    is IntrastatDraftData -> Currency.default
    is DepreciationScheduleDraftData -> Currency.default
    is InventoryDraftData -> Currency.default
    is OtherDraftData -> Currency.default
}

/**
 * Creates an empty draft data instance for a classified-only document.
 * Used when extraction is skipped for unsupported types.
 */
fun DocumentType.toEmptyDraftData(): DocumentDraftData = when (this) {
    DocumentType.Invoice, DocumentType.CreditNote, DocumentType.Receipt,
    DocumentType.BankStatement, DocumentType.Unknown ->
        error("$this has extraction support and should not create classified-only draft")
    DocumentType.ProForma -> ProFormaDraftData()
    DocumentType.Quote -> QuoteDraftData()
    DocumentType.OrderConfirmation -> OrderConfirmationDraftData()
    DocumentType.DeliveryNote -> DeliveryNoteDraftData()
    DocumentType.Reminder -> ReminderDraftData()
    DocumentType.StatementOfAccount -> StatementOfAccountDraftData()
    DocumentType.PurchaseOrder -> PurchaseOrderDraftData()
    DocumentType.ExpenseClaim -> ExpenseClaimDraftData()
    DocumentType.BankFee -> BankFeeDraftData()
    DocumentType.InterestStatement -> InterestStatementDraftData()
    DocumentType.PaymentConfirmation -> PaymentConfirmationDraftData()
    DocumentType.VatReturn -> VatReturnDraftData()
    DocumentType.VatListing -> VatListingDraftData()
    DocumentType.VatAssessment -> VatAssessmentDraftData()
    DocumentType.IcListing -> IcListingDraftData()
    DocumentType.OssReturn -> OssReturnDraftData()
    DocumentType.CorporateTax -> CorporateTaxDraftData()
    DocumentType.CorporateTaxAdvance -> CorporateTaxAdvanceDraftData()
    DocumentType.TaxAssessment -> TaxAssessmentDraftData()
    DocumentType.PersonalTax -> PersonalTaxDraftData()
    DocumentType.WithholdingTax -> WithholdingTaxDraftData()
    DocumentType.SocialContribution -> SocialContributionDraftData()
    DocumentType.SocialFund -> SocialFundDraftData()
    DocumentType.SelfEmployedContribution -> SelfEmployedContributionDraftData()
    DocumentType.Vapz -> VapzDraftData()
    DocumentType.SalarySlip -> SalarySlipDraftData()
    DocumentType.PayrollSummary -> PayrollSummaryDraftData()
    DocumentType.EmploymentContract -> EmploymentContractDraftData()
    DocumentType.Dimona -> DimonaDraftData()
    DocumentType.C4 -> C4DraftData()
    DocumentType.HolidayPay -> HolidayPayDraftData()
    DocumentType.Contract -> ContractDraftData()
    DocumentType.Lease -> LeaseDraftData()
    DocumentType.Loan -> LoanDraftData()
    DocumentType.Insurance -> InsuranceDraftData()
    DocumentType.Dividend -> DividendDraftData()
    DocumentType.ShareholderRegister -> ShareholderRegisterDraftData()
    DocumentType.CompanyExtract -> CompanyExtractDraftData()
    DocumentType.AnnualAccounts -> AnnualAccountsDraftData()
    DocumentType.BoardMinutes -> BoardMinutesDraftData()
    DocumentType.Subsidy -> SubsidyDraftData()
    DocumentType.Fine -> FineDraftData()
    DocumentType.Permit -> PermitDraftData()
    DocumentType.CustomsDeclaration -> CustomsDeclarationDraftData()
    DocumentType.Intrastat -> IntrastatDraftData()
    DocumentType.DepreciationSchedule -> DepreciationScheduleDraftData()
    DocumentType.Inventory -> InventoryDraftData()
    DocumentType.Other -> OtherDraftData()
}

fun DocumentDraftData.toSortDate(): LocalDate? = when (this) {
    is InvoiceDraftData -> issueDate
    is CreditNoteDraftData -> issueDate
    is ReceiptDraftData -> date
    is BankStatementDraftData -> periodEnd
    else -> null
}

@Serializable
data class PartyDraftDto(
    val name: String? = null,
    val vat: VatNumber? = null,
    val email: Email? = null,
    val iban: Iban? = null,
    val streetLine1: String? = null,
    val streetLine2: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val country: String? = null,
)

@Serializable
@SerialName("invoice_draft")
data class InvoiceDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val invoiceNumber: String? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val currency: Currency = Currency.default,
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null,
    val lineItems: List<FinancialLineItemDto> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntryDto> = emptyList(),
    val iban: Iban? = null,
    val payment: CanonicalPaymentDto? = null,
    val notes: String? = null,
    // Neutral party model used for deterministic direction and counterparty resolution.
    val seller: PartyDraftDto = PartyDraftDto(),
    val buyer: PartyDraftDto = PartyDraftDto(),
) : DocumentDraftData {
}

@Serializable
@SerialName("credit_note_draft")
data class CreditNoteDraftData(
    val creditNoteNumber: String? = null,
    val direction: DocumentDirection = DocumentDirection.Unknown,
    val issueDate: LocalDate? = null,
    val currency: Currency = Currency.default,
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null,
    val lineItems: List<FinancialLineItemDto> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntryDto> = emptyList(),
    val originalInvoiceNumber: String? = null,
    val reason: String? = null,
    val notes: String? = null,
    // Neutral party model used for deterministic direction and counterparty resolution.
    val seller: PartyDraftDto = PartyDraftDto(),
    val buyer: PartyDraftDto = PartyDraftDto(),
) : DocumentDraftData

val CreditNoteDraftData.resolvedCounterpartyName: String?
    get() = when (direction) {
        DocumentDirection.Inbound -> seller.name
        DocumentDirection.Outbound -> buyer.name
        else -> seller.name ?: buyer.name
    }

val CreditNoteDraftData.resolvedCounterpartyVat: VatNumber?
    get() = when (direction) {
        DocumentDirection.Inbound -> seller.vat
        DocumentDirection.Outbound -> buyer.vat
        else -> seller.vat ?: buyer.vat
    }

@Serializable
@SerialName("receipt_draft")
data class ReceiptDraftData(
    val direction: DocumentDirection = DocumentDirection.Inbound,
    val merchantName: String? = null,
    val merchantVat: VatNumber? = null,
    val date: LocalDate? = null,
    val currency: Currency = Currency.default,
    val totalAmount: Money? = null,
    val vatAmount: Money? = null,
    val lineItems: List<FinancialLineItemDto> = emptyList(),
    val vatBreakdown: List<VatBreakdownEntryDto> = emptyList(),
    val receiptNumber: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val notes: String? = null
) : DocumentDraftData {
}

@Serializable
data class BankStatementTransactionDraftRowDto(
    val transactionDate: LocalDate? = null,
    val signedAmount: Money? = null,
    val counterparty: CounterpartySnapshotDto = CounterpartySnapshotDto(),
    val communication: TransactionCommunicationDto? = null,
    val descriptionRaw: String? = null,
    val rowConfidence: Double = 0.0,
    val largeAmountFlag: Boolean = false,
    val excluded: Boolean = false,
    val potentialDuplicate: Boolean = false,
) {
    companion object
}

@Serializable
@SerialName("bank_statement_draft")
data class BankStatementDraftData(
    val direction: DocumentDirection = DocumentDirection.Neutral,
    val transactions: List<BankStatementTransactionDraftRowDto> = emptyList(),
    val accountIban: Iban? = null,
    val openingBalance: Money? = null,
    val closingBalance: Money? = null,
    val periodStart: LocalDate? = null,
    val periodEnd: LocalDate? = null,
    val institution: PartyDraftDto = PartyDraftDto(),
    val notes: String? = null,
) : DocumentDraftData

// --- Classified-only draft data types (unsupported for extraction) ---

@Serializable
@SerialName("pro_forma_draft")
data class ProFormaDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("quote_draft")
data class QuoteDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("order_confirmation_draft")
data class OrderConfirmationDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("delivery_note_draft")
data class DeliveryNoteDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("reminder_draft")
data class ReminderDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("statement_of_account_draft")
data class StatementOfAccountDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("purchase_order_draft")
data class PurchaseOrderDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("expense_claim_draft")
data class ExpenseClaimDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("bank_fee_draft")
data class BankFeeDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("interest_statement_draft")
data class InterestStatementDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("payment_confirmation_draft")
data class PaymentConfirmationDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("vat_return_draft")
data class VatReturnDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("vat_listing_draft")
data class VatListingDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("vat_assessment_draft")
data class VatAssessmentDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("ic_listing_draft")
data class IcListingDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("oss_return_draft")
data class OssReturnDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("corporate_tax_draft")
data class CorporateTaxDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("corporate_tax_advance_draft")
data class CorporateTaxAdvanceDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("tax_assessment_draft")
data class TaxAssessmentDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("personal_tax_draft")
data class PersonalTaxDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("withholding_tax_draft")
data class WithholdingTaxDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("social_contribution_draft")
data class SocialContributionDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("social_fund_draft")
data class SocialFundDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("self_employed_contribution_draft")
data class SelfEmployedContributionDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("vapz_draft")
data class VapzDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("salary_slip_draft")
data class SalarySlipDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("payroll_summary_draft")
data class PayrollSummaryDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("employment_contract_draft")
data class EmploymentContractDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("dimona_draft")
data class DimonaDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("c4_draft")
data class C4DraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("holiday_pay_draft")
data class HolidayPayDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("contract_draft")
data class ContractDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("lease_draft")
data class LeaseDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("loan_draft")
data class LoanDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("insurance_draft")
data class InsuranceDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("dividend_draft")
data class DividendDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("shareholder_register_draft")
data class ShareholderRegisterDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("company_extract_draft")
data class CompanyExtractDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("annual_accounts_draft")
data class AnnualAccountsDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("board_minutes_draft")
data class BoardMinutesDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("subsidy_draft")
data class SubsidyDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("fine_draft")
data class FineDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("permit_draft")
data class PermitDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("customs_declaration_draft")
data class CustomsDeclarationDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("intrastat_draft")
data class IntrastatDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("depreciation_schedule_draft")
data class DepreciationScheduleDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("inventory_draft")
data class InventoryDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData

@Serializable
@SerialName("other_draft")
data class OtherDraftData(
    val direction: DocumentDirection = DocumentDirection.Unknown,
) : DocumentDraftData
