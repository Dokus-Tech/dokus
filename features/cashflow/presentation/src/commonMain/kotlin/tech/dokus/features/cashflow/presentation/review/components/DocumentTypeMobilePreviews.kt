@file:Suppress("TooManyFunctions", "LongMethod")

package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.review.components.mobile.MobileCanonicalContent
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
private fun MobilePreviewContent(type: DocumentType) {
    MobileCanonicalContent(
        state = previewStateForDocumentType(type),
        isAccountantReadOnly = false,
        onIntent = {},
        onBackClick = {},
        onOpenSource = {},
        modifier = Modifier.fillMaxSize(),
    )
}

// ═══════════════════════════════════════════════════════════════════
// FINANCIAL
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - Invoice", widthDp = 430, heightDp = 900)
@Composable
private fun MobileInvoicePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Invoice) }
}

@Preview(name = "Mobile - CreditNote", widthDp = 430, heightDp = 900)
@Composable
private fun MobileCreditNotePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.CreditNote) }
}

@Preview(name = "Mobile - ProForma", widthDp = 430, heightDp = 900)
@Composable
private fun MobileProFormaPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.ProForma) }
}

@Preview(name = "Mobile - Quote", widthDp = 430, heightDp = 900)
@Composable
private fun MobileQuotePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Quote) }
}

@Preview(name = "Mobile - OrderConfirmation", widthDp = 430, heightDp = 900)
@Composable
private fun MobileOrderConfirmationPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.OrderConfirmation) }
}

@Preview(name = "Mobile - DeliveryNote", widthDp = 430, heightDp = 900)
@Composable
private fun MobileDeliveryNotePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.DeliveryNote) }
}

@Preview(name = "Mobile - Reminder", widthDp = 430, heightDp = 900)
@Composable
private fun MobileReminderPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Reminder) }
}

@Preview(name = "Mobile - StatementOfAccount", widthDp = 430, heightDp = 900)
@Composable
private fun MobileStatementOfAccountPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.StatementOfAccount) }
}

@Preview(name = "Mobile - Receipt", widthDp = 430, heightDp = 900)
@Composable
private fun MobileReceiptPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Receipt) }
}

@Preview(name = "Mobile - PurchaseOrder", widthDp = 430, heightDp = 900)
@Composable
private fun MobilePurchaseOrderPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.PurchaseOrder) }
}

@Preview(name = "Mobile - ExpenseClaim", widthDp = 430, heightDp = 900)
@Composable
private fun MobileExpenseClaimPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.ExpenseClaim) }
}

// ═══════════════════════════════════════════════════════════════════
// BANKING
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - BankStatement", widthDp = 430, heightDp = 900)
@Composable
private fun MobileBankStatementPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.BankStatement) }
}

@Preview(name = "Mobile - BankFee", widthDp = 430, heightDp = 900)
@Composable
private fun MobileBankFeePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.BankFee) }
}

@Preview(name = "Mobile - InterestStatement", widthDp = 430, heightDp = 900)
@Composable
private fun MobileInterestStatementPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.InterestStatement) }
}

@Preview(name = "Mobile - PaymentConfirmation", widthDp = 430, heightDp = 900)
@Composable
private fun MobilePaymentConfirmationPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.PaymentConfirmation) }
}

// ═══════════════════════════════════════════════════════════════════
// VAT (Belgium)
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - VatReturn", widthDp = 430, heightDp = 900)
@Composable
private fun MobileVatReturnPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.VatReturn) }
}

@Preview(name = "Mobile - VatListing", widthDp = 430, heightDp = 900)
@Composable
private fun MobileVatListingPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.VatListing) }
}

@Preview(name = "Mobile - VatAssessment", widthDp = 430, heightDp = 900)
@Composable
private fun MobileVatAssessmentPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.VatAssessment) }
}

@Preview(name = "Mobile - IcListing", widthDp = 430, heightDp = 900)
@Composable
private fun MobileIcListingPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.IcListing) }
}

@Preview(name = "Mobile - OssReturn", widthDp = 430, heightDp = 900)
@Composable
private fun MobileOssReturnPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.OssReturn) }
}

// ═══════════════════════════════════════════════════════════════════
// TAX - CORPORATE
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - CorporateTax", widthDp = 430, heightDp = 900)
@Composable
private fun MobileCorporateTaxPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.CorporateTax) }
}

@Preview(name = "Mobile - CorporateTaxAdvance", widthDp = 430, heightDp = 900)
@Composable
private fun MobileCorporateTaxAdvancePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.CorporateTaxAdvance) }
}

@Preview(name = "Mobile - TaxAssessment", widthDp = 430, heightDp = 900)
@Composable
private fun MobileTaxAssessmentPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.TaxAssessment) }
}

// ═══════════════════════════════════════════════════════════════════
// TAX - PERSONAL
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - PersonalTax", widthDp = 430, heightDp = 900)
@Composable
private fun MobilePersonalTaxPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.PersonalTax) }
}

@Preview(name = "Mobile - WithholdingTax", widthDp = 430, heightDp = 900)
@Composable
private fun MobileWithholdingTaxPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.WithholdingTax) }
}

// ═══════════════════════════════════════════════════════════════════
// SOCIAL CONTRIBUTIONS
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - SocialContribution", widthDp = 430, heightDp = 900)
@Composable
private fun MobileSocialContributionPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.SocialContribution) }
}

@Preview(name = "Mobile - SocialFund", widthDp = 430, heightDp = 900)
@Composable
private fun MobileSocialFundPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.SocialFund) }
}

@Preview(name = "Mobile - SelfEmployedContribution", widthDp = 430, heightDp = 900)
@Composable
private fun MobileSelfEmployedContributionPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.SelfEmployedContribution) }
}

@Preview(name = "Mobile - Vapz", widthDp = 430, heightDp = 900)
@Composable
private fun MobileVapzPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Vapz) }
}

// ═══════════════════════════════════════════════════════════════════
// PAYROLL / HR
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - SalarySlip", widthDp = 430, heightDp = 900)
@Composable
private fun MobileSalarySlipPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.SalarySlip) }
}

@Preview(name = "Mobile - PayrollSummary", widthDp = 430, heightDp = 900)
@Composable
private fun MobilePayrollSummaryPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.PayrollSummary) }
}

@Preview(name = "Mobile - EmploymentContract", widthDp = 430, heightDp = 900)
@Composable
private fun MobileEmploymentContractPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.EmploymentContract) }
}

@Preview(name = "Mobile - Dimona", widthDp = 430, heightDp = 900)
@Composable
private fun MobileDimonaPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Dimona) }
}

@Preview(name = "Mobile - C4", widthDp = 430, heightDp = 900)
@Composable
private fun MobileC4Preview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.C4) }
}

@Preview(name = "Mobile - HolidayPay", widthDp = 430, heightDp = 900)
@Composable
private fun MobileHolidayPayPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.HolidayPay) }
}

// ═══════════════════════════════════════════════════════════════════
// LEGAL / CONTRACTS
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - Contract", widthDp = 430, heightDp = 900)
@Composable
private fun MobileContractPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Contract) }
}

@Preview(name = "Mobile - Lease", widthDp = 430, heightDp = 900)
@Composable
private fun MobileLeasePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Lease) }
}

@Preview(name = "Mobile - Loan", widthDp = 430, heightDp = 900)
@Composable
private fun MobileLoanPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Loan) }
}

@Preview(name = "Mobile - Insurance", widthDp = 430, heightDp = 900)
@Composable
private fun MobileInsurancePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Insurance) }
}

// ═══════════════════════════════════════════════════════════════════
// CORPORATE DOCUMENTS
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - Dividend", widthDp = 430, heightDp = 900)
@Composable
private fun MobileDividendPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Dividend) }
}

@Preview(name = "Mobile - ShareholderRegister", widthDp = 430, heightDp = 900)
@Composable
private fun MobileShareholderRegisterPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.ShareholderRegister) }
}

@Preview(name = "Mobile - CompanyExtract", widthDp = 430, heightDp = 900)
@Composable
private fun MobileCompanyExtractPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.CompanyExtract) }
}

@Preview(name = "Mobile - AnnualAccounts", widthDp = 430, heightDp = 900)
@Composable
private fun MobileAnnualAccountsPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.AnnualAccounts) }
}

@Preview(name = "Mobile - BoardMinutes", widthDp = 430, heightDp = 900)
@Composable
private fun MobileBoardMinutesPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.BoardMinutes) }
}

// ═══════════════════════════════════════════════════════════════════
// GOVERNMENT / REGULATORY
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - Subsidy", widthDp = 430, heightDp = 900)
@Composable
private fun MobileSubsidyPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Subsidy) }
}

@Preview(name = "Mobile - Fine", widthDp = 430, heightDp = 900)
@Composable
private fun MobileFinePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Fine) }
}

@Preview(name = "Mobile - Permit", widthDp = 430, heightDp = 900)
@Composable
private fun MobilePermitPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Permit) }
}

// ═══════════════════════════════════════════════════════════════════
// INTERNATIONAL TRADE
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - CustomsDeclaration", widthDp = 430, heightDp = 900)
@Composable
private fun MobileCustomsDeclarationPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.CustomsDeclaration) }
}

@Preview(name = "Mobile - Intrastat", widthDp = 430, heightDp = 900)
@Composable
private fun MobileIntrastatPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Intrastat) }
}

// ═══════════════════════════════════════════════════════════════════
// ASSETS
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - DepreciationSchedule", widthDp = 430, heightDp = 900)
@Composable
private fun MobileDepreciationSchedulePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.DepreciationSchedule) }
}

@Preview(name = "Mobile - Inventory", widthDp = 430, heightDp = 900)
@Composable
private fun MobileInventoryPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Inventory) }
}

// ═══════════════════════════════════════════════════════════════════
// CATCH-ALL
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Mobile - Other", widthDp = 430, heightDp = 900)
@Composable
private fun MobileOtherPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Other) }
}

@Preview(name = "Mobile - Unknown", widthDp = 430, heightDp = 900)
@Composable
private fun MobileUnknownPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { MobilePreviewContent(DocumentType.Unknown) }
}
