@file:Suppress("TooManyFunctions", "LongMethod")

package tech.dokus.features.cashflow.presentation.detail.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.enums.DocumentType
import tech.dokus.features.cashflow.presentation.detail.screen.DocumentDetailScreen
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
private fun DesktopPreviewContent(type: DocumentType) {
    DocumentDetailScreen(
        state = previewStateForDocumentType(type),
        isLargeScreen = true,
        isAccountantReadOnly = false,
        onIntent = {},
        onBackClick = {},
        onOpenSource = {},
        onCorrectContact = {},
        onCreateContact = {},
    )
}

// ═══════════════════════════════════════════════════════════════════
// FINANCIAL
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - Invoice", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopInvoicePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Invoice) }
}

@Preview(name = "Desktop - CreditNote", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopCreditNotePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.CreditNote) }
}

@Preview(name = "Desktop - ProForma", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopProFormaPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.ProForma) }
}

@Preview(name = "Desktop - Quote", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopQuotePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Quote) }
}

@Preview(name = "Desktop - OrderConfirmation", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopOrderConfirmationPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.OrderConfirmation) }
}

@Preview(name = "Desktop - DeliveryNote", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopDeliveryNotePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.DeliveryNote) }
}

@Preview(name = "Desktop - Reminder", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopReminderPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Reminder) }
}

@Preview(name = "Desktop - StatementOfAccount", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopStatementOfAccountPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.StatementOfAccount) }
}

@Preview(name = "Desktop - Receipt", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopReceiptPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Receipt) }
}

@Preview(name = "Desktop - PurchaseOrder", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopPurchaseOrderPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.PurchaseOrder) }
}

@Preview(name = "Desktop - ExpenseClaim", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopExpenseClaimPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.ExpenseClaim) }
}

// ═══════════════════════════════════════════════════════════════════
// BANKING
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - BankStatement", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopBankStatementPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.BankStatement) }
}

@Preview(name = "Desktop - BankFee", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopBankFeePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.BankFee) }
}

@Preview(name = "Desktop - InterestStatement", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopInterestStatementPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.InterestStatement) }
}

@Preview(name = "Desktop - PaymentConfirmation", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopPaymentConfirmationPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.PaymentConfirmation) }
}

// ═══════════════════════════════════════════════════════════════════
// VAT (Belgium)
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - VatReturn", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopVatReturnPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.VatReturn) }
}

@Preview(name = "Desktop - VatListing", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopVatListingPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.VatListing) }
}

@Preview(name = "Desktop - VatAssessment", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopVatAssessmentPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.VatAssessment) }
}

@Preview(name = "Desktop - IcListing", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopIcListingPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.IcListing) }
}

@Preview(name = "Desktop - OssReturn", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopOssReturnPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.OssReturn) }
}

// ═══════════════════════════════════════════════════════════════════
// TAX - CORPORATE
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - CorporateTax", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopCorporateTaxPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.CorporateTax) }
}

@Preview(name = "Desktop - CorporateTaxAdvance", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopCorporateTaxAdvancePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.CorporateTaxAdvance) }
}

@Preview(name = "Desktop - TaxAssessment", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopTaxAssessmentPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.TaxAssessment) }
}

// ═══════════════════════════════════════════════════════════════════
// TAX - PERSONAL
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - PersonalTax", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopPersonalTaxPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.PersonalTax) }
}

@Preview(name = "Desktop - WithholdingTax", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopWithholdingTaxPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.WithholdingTax) }
}

// ═══════════════════════════════════════════════════════════════════
// SOCIAL CONTRIBUTIONS
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - SocialContribution", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopSocialContributionPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.SocialContribution) }
}

@Preview(name = "Desktop - SocialFund", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopSocialFundPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.SocialFund) }
}

@Preview(name = "Desktop - SelfEmployedContribution", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopSelfEmployedContributionPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.SelfEmployedContribution) }
}

@Preview(name = "Desktop - Vapz", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopVapzPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Vapz) }
}

// ═══════════════════════════════════════════════════════════════════
// PAYROLL / HR
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - SalarySlip", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopSalarySlipPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.SalarySlip) }
}

@Preview(name = "Desktop - PayrollSummary", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopPayrollSummaryPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.PayrollSummary) }
}

@Preview(name = "Desktop - EmploymentContract", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopEmploymentContractPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.EmploymentContract) }
}

@Preview(name = "Desktop - Dimona", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopDimonaPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Dimona) }
}

@Preview(name = "Desktop - C4", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopC4Preview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.C4) }
}

@Preview(name = "Desktop - HolidayPay", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopHolidayPayPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.HolidayPay) }
}

// ═══════════════════════════════════════════════════════════════════
// LEGAL / CONTRACTS
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - Contract", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopContractPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Contract) }
}

@Preview(name = "Desktop - Lease", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopLeasePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Lease) }
}

@Preview(name = "Desktop - Loan", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopLoanPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Loan) }
}

@Preview(name = "Desktop - Insurance", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopInsurancePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Insurance) }
}

// ═══════════════════════════════════════════════════════════════════
// CORPORATE DOCUMENTS
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - Dividend", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopDividendPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Dividend) }
}

@Preview(name = "Desktop - ShareholderRegister", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopShareholderRegisterPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.ShareholderRegister) }
}

@Preview(name = "Desktop - CompanyExtract", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopCompanyExtractPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.CompanyExtract) }
}

@Preview(name = "Desktop - AnnualAccounts", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopAnnualAccountsPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.AnnualAccounts) }
}

@Preview(name = "Desktop - BoardMinutes", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopBoardMinutesPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.BoardMinutes) }
}

// ═══════════════════════════════════════════════════════════════════
// GOVERNMENT / REGULATORY
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - Subsidy", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopSubsidyPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Subsidy) }
}

@Preview(name = "Desktop - Fine", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopFinePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Fine) }
}

@Preview(name = "Desktop - Permit", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopPermitPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Permit) }
}

// ═══════════════════════════════════════════════════════════════════
// INTERNATIONAL TRADE
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - CustomsDeclaration", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopCustomsDeclarationPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.CustomsDeclaration) }
}

@Preview(name = "Desktop - Intrastat", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopIntrastatPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Intrastat) }
}

// ═══════════════════════════════════════════════════════════════════
// ASSETS
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - DepreciationSchedule", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopDepreciationSchedulePreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.DepreciationSchedule) }
}

@Preview(name = "Desktop - Inventory", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopInventoryPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Inventory) }
}

// ═══════════════════════════════════════════════════════════════════
// CATCH-ALL
// ═══════════════════════════════════════════════════════════════════

@Preview(name = "Desktop - Other", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopOtherPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Other) }
}

@Preview(name = "Desktop - Unknown", widthDp = 1366, heightDp = 900)
@Composable
private fun DesktopUnknownPreview(@PreviewParameter(PreviewParametersProvider::class) p: PreviewParameters) {
    TestWrapper(p) { DesktopPreviewContent(DocumentType.Unknown) }
}
