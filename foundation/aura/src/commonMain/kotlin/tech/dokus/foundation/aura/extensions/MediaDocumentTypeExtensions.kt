package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_type_annual_accounts
import tech.dokus.aura.resources.document_type_bank_fee
import tech.dokus.aura.resources.document_type_bank_statement
import tech.dokus.aura.resources.document_type_board_minutes
import tech.dokus.aura.resources.document_type_c4
import tech.dokus.aura.resources.document_type_company_extract
import tech.dokus.aura.resources.document_type_contract
import tech.dokus.aura.resources.document_type_corporate_tax
import tech.dokus.aura.resources.document_type_corporate_tax_advance
import tech.dokus.aura.resources.document_type_credit_note
import tech.dokus.aura.resources.document_type_customs_declaration
import tech.dokus.aura.resources.document_type_delivery_note
import tech.dokus.aura.resources.document_type_depreciation_schedule
import tech.dokus.aura.resources.document_type_dimona
import tech.dokus.aura.resources.document_type_dividend
import tech.dokus.aura.resources.document_type_employment_contract
import tech.dokus.aura.resources.document_type_expense_claim
import tech.dokus.aura.resources.document_type_fine
import tech.dokus.aura.resources.document_type_holiday_pay
import tech.dokus.aura.resources.document_type_ic_listing
import tech.dokus.aura.resources.document_type_insurance
import tech.dokus.aura.resources.document_type_interest_statement
import tech.dokus.aura.resources.document_type_intrastat
import tech.dokus.aura.resources.document_type_inventory
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.document_type_lease
import tech.dokus.aura.resources.document_type_loan
import tech.dokus.aura.resources.document_type_order_confirmation
import tech.dokus.aura.resources.document_type_oss_return
import tech.dokus.aura.resources.document_type_other
import tech.dokus.aura.resources.document_type_payment_confirmation
import tech.dokus.aura.resources.document_type_payroll_summary
import tech.dokus.aura.resources.document_type_permit
import tech.dokus.aura.resources.document_type_personal_tax
import tech.dokus.aura.resources.document_type_pro_forma
import tech.dokus.aura.resources.document_type_purchase_order
import tech.dokus.aura.resources.document_type_quote
import tech.dokus.aura.resources.document_type_receipt
import tech.dokus.aura.resources.document_type_reminder
import tech.dokus.aura.resources.document_type_salary_slip
import tech.dokus.aura.resources.document_type_self_employed_contribution
import tech.dokus.aura.resources.document_type_shareholder_register
import tech.dokus.aura.resources.document_type_social_contribution
import tech.dokus.aura.resources.document_type_social_fund
import tech.dokus.aura.resources.document_type_statement_of_account
import tech.dokus.aura.resources.document_type_subsidy
import tech.dokus.aura.resources.document_type_tax_assessment
import tech.dokus.aura.resources.document_type_unknown
import tech.dokus.aura.resources.document_type_vapz
import tech.dokus.aura.resources.document_type_vat_assessment
import tech.dokus.aura.resources.document_type_vat_listing
import tech.dokus.aura.resources.document_type_vat_return
import tech.dokus.aura.resources.document_type_withholding_tax
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.MediaDocumentType

/**
 * Extension property to get a localized display name for a MediaDocumentType.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentTypeBadge(type: MediaDocumentType) {
 *     Text(text = type.localized)
 * }
 * ```
 */
val MediaDocumentType.localized: String
    @Composable get() = when (this) {
        MediaDocumentType.Invoice -> stringResource(Res.string.document_type_invoice)
        MediaDocumentType.Unknown -> stringResource(Res.string.document_type_unknown)
    }

/**
 * Extension property to get the localized document type name in uppercase for display.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentHeader(type: MediaDocumentType) {
 *     Text(text = type.localizedUppercase) // "INVOICE", "EXPENSE", etc.
 * }
 * ```
 */
val MediaDocumentType.localizedUppercase: String
    @Composable get() = localized.uppercase()

// ============================================================================
// DocumentType Extensions (for document processing)
// ============================================================================

/**
 * Extension property to get a localized display name for a DocumentType.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentTypeBadge(type: DocumentType) {
 *     Text(text = type.localized)
 * }
 * ```
 */
val DocumentType.localized: String
    @Composable get() = when (this) {
        // Financial
        DocumentType.Invoice -> stringResource(Res.string.document_type_invoice)
        DocumentType.CreditNote -> stringResource(Res.string.document_type_credit_note)
        DocumentType.ProForma -> stringResource(Res.string.document_type_pro_forma)
        DocumentType.Quote -> stringResource(Res.string.document_type_quote)
        DocumentType.OrderConfirmation -> stringResource(Res.string.document_type_order_confirmation)
        DocumentType.DeliveryNote -> stringResource(Res.string.document_type_delivery_note)
        DocumentType.Reminder -> stringResource(Res.string.document_type_reminder)
        DocumentType.StatementOfAccount -> stringResource(Res.string.document_type_statement_of_account)
        DocumentType.Receipt -> stringResource(Res.string.document_type_receipt)
        DocumentType.PurchaseOrder -> stringResource(Res.string.document_type_purchase_order)
        DocumentType.ExpenseClaim -> stringResource(Res.string.document_type_expense_claim)
        // Banking
        DocumentType.BankStatement -> stringResource(Res.string.document_type_bank_statement)
        DocumentType.BankFee -> stringResource(Res.string.document_type_bank_fee)
        DocumentType.InterestStatement -> stringResource(Res.string.document_type_interest_statement)
        DocumentType.PaymentConfirmation -> stringResource(Res.string.document_type_payment_confirmation)
        // VAT (Belgium)
        DocumentType.VatReturn -> stringResource(Res.string.document_type_vat_return)
        DocumentType.VatListing -> stringResource(Res.string.document_type_vat_listing)
        DocumentType.VatAssessment -> stringResource(Res.string.document_type_vat_assessment)
        DocumentType.IcListing -> stringResource(Res.string.document_type_ic_listing)
        DocumentType.OssReturn -> stringResource(Res.string.document_type_oss_return)
        // Tax - Corporate
        DocumentType.CorporateTax -> stringResource(Res.string.document_type_corporate_tax)
        DocumentType.CorporateTaxAdvance -> stringResource(Res.string.document_type_corporate_tax_advance)
        DocumentType.TaxAssessment -> stringResource(Res.string.document_type_tax_assessment)
        // Tax - Personal
        DocumentType.PersonalTax -> stringResource(Res.string.document_type_personal_tax)
        DocumentType.WithholdingTax -> stringResource(Res.string.document_type_withholding_tax)
        // Social Contributions (Belgium)
        DocumentType.SocialContribution -> stringResource(Res.string.document_type_social_contribution)
        DocumentType.SocialFund -> stringResource(Res.string.document_type_social_fund)
        DocumentType.SelfEmployedContribution -> stringResource(Res.string.document_type_self_employed_contribution)
        DocumentType.Vapz -> stringResource(Res.string.document_type_vapz)
        // Payroll / HR
        DocumentType.SalarySlip -> stringResource(Res.string.document_type_salary_slip)
        DocumentType.PayrollSummary -> stringResource(Res.string.document_type_payroll_summary)
        DocumentType.EmploymentContract -> stringResource(Res.string.document_type_employment_contract)
        DocumentType.Dimona -> stringResource(Res.string.document_type_dimona)
        DocumentType.C4 -> stringResource(Res.string.document_type_c4)
        DocumentType.HolidayPay -> stringResource(Res.string.document_type_holiday_pay)
        // Legal / Contracts
        DocumentType.Contract -> stringResource(Res.string.document_type_contract)
        DocumentType.Lease -> stringResource(Res.string.document_type_lease)
        DocumentType.Loan -> stringResource(Res.string.document_type_loan)
        DocumentType.Insurance -> stringResource(Res.string.document_type_insurance)
        // Corporate Documents
        DocumentType.Dividend -> stringResource(Res.string.document_type_dividend)
        DocumentType.ShareholderRegister -> stringResource(Res.string.document_type_shareholder_register)
        DocumentType.CompanyExtract -> stringResource(Res.string.document_type_company_extract)
        DocumentType.AnnualAccounts -> stringResource(Res.string.document_type_annual_accounts)
        DocumentType.BoardMinutes -> stringResource(Res.string.document_type_board_minutes)
        // Government / Regulatory
        DocumentType.Subsidy -> stringResource(Res.string.document_type_subsidy)
        DocumentType.Fine -> stringResource(Res.string.document_type_fine)
        DocumentType.Permit -> stringResource(Res.string.document_type_permit)
        // International Trade
        DocumentType.CustomsDeclaration -> stringResource(Res.string.document_type_customs_declaration)
        DocumentType.Intrastat -> stringResource(Res.string.document_type_intrastat)
        // Assets
        DocumentType.DepreciationSchedule -> stringResource(Res.string.document_type_depreciation_schedule)
        DocumentType.Inventory -> stringResource(Res.string.document_type_inventory)
        // Catch-all
        DocumentType.Other -> stringResource(Res.string.document_type_other)
        DocumentType.Unknown -> stringResource(Res.string.document_type_unknown)
    }

/**
 * Extension property to get the localized document type name in uppercase for display.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentHeader(type: DocumentType) {
 *     Text(text = type.localizedUppercase) // "INVOICE", "EXPENSE", etc.
 * }
 * ```
 */
val DocumentType.localizedUppercase: String
    @Composable get() = localized.uppercase()
