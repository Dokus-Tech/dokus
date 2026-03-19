package tech.dokus.features.cashflow.presentation.review.models

import androidx.compose.runtime.Immutable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType

@Immutable
data class LineItemUiData(
    val description: String,
    val displayAmount: String,
)

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
        val lineItems: List<LineItemUiData>,
        val notes: String?,
        val iban: String?,
        val primaryDescription: String,
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
        val lineItems: List<LineItemUiData>,
        val reason: String?,
        val notes: String?,
        val primaryDescription: String,
    ) : DocumentUiData

    @Immutable
    data class Receipt(
        val receiptNumber: String?,
        val date: String?,
        val totalAmount: Money?,
        val vatAmount: Money?,
        val currencySign: String,
        val notes: String?,
        val primaryDescription: String,
    ) : DocumentUiData

    @Immutable
    data class BankStatement(
        val accountIban: String?,
        val institutionName: String?,
        val periodStart: String?,
        val periodEnd: String?,
        val openingBalance: String?,
        val closingBalance: String?,
        val movement: String?,
        val transactions: List<BankStatementTransactionUiRow> = emptyList(),
    ) : DocumentUiData {
        val includedCount: Int get() = transactions.count { !it.isExcluded }
        val excludedCount: Int get() = transactions.count { it.isExcluded }
        val duplicateCount: Int get() = transactions.count { it.isDuplicate }
        val hasDuplicates: Boolean get() = duplicateCount > 0
    }

    // --- Classified-only document types ---

    @Immutable
    data class ProForma(
        val documentType: DocumentType = DocumentType.ProForma,
    ) : DocumentUiData

    @Immutable
    data class Quote(
        val documentType: DocumentType = DocumentType.Quote,
    ) : DocumentUiData

    @Immutable
    data class OrderConfirmation(
        val documentType: DocumentType = DocumentType.OrderConfirmation,
    ) : DocumentUiData

    @Immutable
    data class DeliveryNote(
        val documentType: DocumentType = DocumentType.DeliveryNote,
    ) : DocumentUiData

    @Immutable
    data class Reminder(
        val documentType: DocumentType = DocumentType.Reminder,
    ) : DocumentUiData

    @Immutable
    data class StatementOfAccount(
        val documentType: DocumentType = DocumentType.StatementOfAccount,
    ) : DocumentUiData

    @Immutable
    data class PurchaseOrder(
        val documentType: DocumentType = DocumentType.PurchaseOrder,
    ) : DocumentUiData

    @Immutable
    data class ExpenseClaim(
        val documentType: DocumentType = DocumentType.ExpenseClaim,
    ) : DocumentUiData

    @Immutable
    data class BankFee(
        val documentType: DocumentType = DocumentType.BankFee,
    ) : DocumentUiData

    @Immutable
    data class InterestStatement(
        val documentType: DocumentType = DocumentType.InterestStatement,
    ) : DocumentUiData

    @Immutable
    data class PaymentConfirmation(
        val documentType: DocumentType = DocumentType.PaymentConfirmation,
    ) : DocumentUiData

    @Immutable
    data class VatReturn(
        val documentType: DocumentType = DocumentType.VatReturn,
    ) : DocumentUiData

    @Immutable
    data class VatListing(
        val documentType: DocumentType = DocumentType.VatListing,
    ) : DocumentUiData

    @Immutable
    data class VatAssessment(
        val documentType: DocumentType = DocumentType.VatAssessment,
    ) : DocumentUiData

    @Immutable
    data class IcListing(
        val documentType: DocumentType = DocumentType.IcListing,
    ) : DocumentUiData

    @Immutable
    data class OssReturn(
        val documentType: DocumentType = DocumentType.OssReturn,
    ) : DocumentUiData

    @Immutable
    data class CorporateTax(
        val documentType: DocumentType = DocumentType.CorporateTax,
    ) : DocumentUiData

    @Immutable
    data class CorporateTaxAdvance(
        val documentType: DocumentType = DocumentType.CorporateTaxAdvance,
    ) : DocumentUiData

    @Immutable
    data class TaxAssessment(
        val documentType: DocumentType = DocumentType.TaxAssessment,
    ) : DocumentUiData

    @Immutable
    data class PersonalTax(
        val documentType: DocumentType = DocumentType.PersonalTax,
    ) : DocumentUiData

    @Immutable
    data class WithholdingTax(
        val documentType: DocumentType = DocumentType.WithholdingTax,
    ) : DocumentUiData

    @Immutable
    data class SocialContribution(
        val documentType: DocumentType = DocumentType.SocialContribution,
    ) : DocumentUiData

    @Immutable
    data class SocialFund(
        val documentType: DocumentType = DocumentType.SocialFund,
    ) : DocumentUiData

    @Immutable
    data class SelfEmployedContribution(
        val documentType: DocumentType = DocumentType.SelfEmployedContribution,
    ) : DocumentUiData

    @Immutable
    data class Vapz(
        val documentType: DocumentType = DocumentType.Vapz,
    ) : DocumentUiData

    @Immutable
    data class SalarySlip(
        val documentType: DocumentType = DocumentType.SalarySlip,
    ) : DocumentUiData

    @Immutable
    data class PayrollSummary(
        val documentType: DocumentType = DocumentType.PayrollSummary,
    ) : DocumentUiData

    @Immutable
    data class EmploymentContract(
        val documentType: DocumentType = DocumentType.EmploymentContract,
    ) : DocumentUiData

    @Immutable
    data class Dimona(
        val documentType: DocumentType = DocumentType.Dimona,
    ) : DocumentUiData

    @Immutable
    data class C4(
        val documentType: DocumentType = DocumentType.C4,
    ) : DocumentUiData

    @Immutable
    data class HolidayPay(
        val documentType: DocumentType = DocumentType.HolidayPay,
    ) : DocumentUiData

    @Immutable
    data class Contract(
        val documentType: DocumentType = DocumentType.Contract,
    ) : DocumentUiData

    @Immutable
    data class Lease(
        val documentType: DocumentType = DocumentType.Lease,
    ) : DocumentUiData

    @Immutable
    data class Loan(
        val documentType: DocumentType = DocumentType.Loan,
    ) : DocumentUiData

    @Immutable
    data class Insurance(
        val documentType: DocumentType = DocumentType.Insurance,
    ) : DocumentUiData

    @Immutable
    data class Dividend(
        val documentType: DocumentType = DocumentType.Dividend,
    ) : DocumentUiData

    @Immutable
    data class ShareholderRegister(
        val documentType: DocumentType = DocumentType.ShareholderRegister,
    ) : DocumentUiData

    @Immutable
    data class CompanyExtract(
        val documentType: DocumentType = DocumentType.CompanyExtract,
    ) : DocumentUiData

    @Immutable
    data class AnnualAccounts(
        val documentType: DocumentType = DocumentType.AnnualAccounts,
    ) : DocumentUiData

    @Immutable
    data class BoardMinutes(
        val documentType: DocumentType = DocumentType.BoardMinutes,
    ) : DocumentUiData

    @Immutable
    data class Subsidy(
        val documentType: DocumentType = DocumentType.Subsidy,
    ) : DocumentUiData

    @Immutable
    data class Fine(
        val documentType: DocumentType = DocumentType.Fine,
    ) : DocumentUiData

    @Immutable
    data class Permit(
        val documentType: DocumentType = DocumentType.Permit,
    ) : DocumentUiData

    @Immutable
    data class CustomsDeclaration(
        val documentType: DocumentType = DocumentType.CustomsDeclaration,
    ) : DocumentUiData

    @Immutable
    data class Intrastat(
        val documentType: DocumentType = DocumentType.Intrastat,
    ) : DocumentUiData

    @Immutable
    data class DepreciationSchedule(
        val documentType: DocumentType = DocumentType.DepreciationSchedule,
    ) : DocumentUiData

    @Immutable
    data class Inventory(
        val documentType: DocumentType = DocumentType.Inventory,
    ) : DocumentUiData

    @Immutable
    data class Other(
        val documentType: DocumentType = DocumentType.Other,
    ) : DocumentUiData
}
