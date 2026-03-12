package tech.dokus.features.cashflow.presentation.review.models

import tech.dokus.domain.Money
import tech.dokus.domain.model.AnnualAccountsDraftData
import tech.dokus.domain.model.BankFeeDraftData
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.BoardMinutesDraftData
import tech.dokus.domain.model.C4DraftData
import tech.dokus.domain.model.CompanyExtractDraftData
import tech.dokus.domain.model.ContractDraftData
import tech.dokus.domain.model.CorporateTaxAdvanceDraftData
import tech.dokus.domain.model.CorporateTaxDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.CustomsDeclarationDraftData
import tech.dokus.domain.model.DeliveryNoteDraftData
import tech.dokus.domain.model.DepreciationScheduleDraftData
import tech.dokus.domain.model.DimonaDraftData
import tech.dokus.domain.model.DividendDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.FineDraftData
import tech.dokus.domain.model.HolidayPayDraftData
import tech.dokus.domain.model.IcListingDraftData
import tech.dokus.domain.model.InsuranceDraftData
import tech.dokus.domain.model.InterestStatementDraftData
import tech.dokus.domain.model.IntrastatDraftData
import tech.dokus.domain.model.InventoryDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.LeaseDraftData
import tech.dokus.domain.model.LoanDraftData
import tech.dokus.domain.model.OrderConfirmationDraftData
import tech.dokus.domain.model.OssReturnDraftData
import tech.dokus.domain.model.OtherDraftData
import tech.dokus.domain.model.PaymentConfirmationDraftData
import tech.dokus.domain.model.PayrollSummaryDraftData
import tech.dokus.domain.model.PermitDraftData
import tech.dokus.domain.model.PersonalTaxDraftData
import tech.dokus.domain.model.ProFormaDraftData
import tech.dokus.domain.model.PurchaseOrderDraftData
import tech.dokus.domain.model.QuoteDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.ReminderDraftData
import tech.dokus.domain.model.SalarySlipDraftData
import tech.dokus.domain.model.SelfEmployedContributionDraftData
import tech.dokus.domain.model.ShareholderRegisterDraftData
import tech.dokus.domain.model.SocialContributionDraftData
import tech.dokus.domain.model.SocialFundDraftData
import tech.dokus.domain.model.StatementOfAccountDraftData
import tech.dokus.domain.model.SubsidyDraftData
import tech.dokus.domain.model.TaxAssessmentDraftData
import tech.dokus.domain.model.VapzDraftData
import tech.dokus.domain.model.VatAssessmentDraftData
import tech.dokus.domain.model.VatListingDraftData
import tech.dokus.domain.model.VatReturnDraftData
import tech.dokus.domain.model.WithholdingTaxDraftData

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

    is ProFormaDraftData -> DocumentUiData.ProForma()
    is QuoteDraftData -> DocumentUiData.Quote()
    is OrderConfirmationDraftData -> DocumentUiData.OrderConfirmation()
    is DeliveryNoteDraftData -> DocumentUiData.DeliveryNote()
    is ReminderDraftData -> DocumentUiData.Reminder()
    is StatementOfAccountDraftData -> DocumentUiData.StatementOfAccount()
    is PurchaseOrderDraftData -> DocumentUiData.PurchaseOrder()
    is ExpenseClaimDraftData -> DocumentUiData.ExpenseClaim()
    is BankFeeDraftData -> DocumentUiData.BankFee()
    is InterestStatementDraftData -> DocumentUiData.InterestStatement()
    is PaymentConfirmationDraftData -> DocumentUiData.PaymentConfirmation()
    is VatReturnDraftData -> DocumentUiData.VatReturn()
    is VatListingDraftData -> DocumentUiData.VatListing()
    is VatAssessmentDraftData -> DocumentUiData.VatAssessment()
    is IcListingDraftData -> DocumentUiData.IcListing()
    is OssReturnDraftData -> DocumentUiData.OssReturn()
    is CorporateTaxDraftData -> DocumentUiData.CorporateTax()
    is CorporateTaxAdvanceDraftData -> DocumentUiData.CorporateTaxAdvance()
    is TaxAssessmentDraftData -> DocumentUiData.TaxAssessment()
    is PersonalTaxDraftData -> DocumentUiData.PersonalTax()
    is WithholdingTaxDraftData -> DocumentUiData.WithholdingTax()
    is SocialContributionDraftData -> DocumentUiData.SocialContribution()
    is SocialFundDraftData -> DocumentUiData.SocialFund()
    is SelfEmployedContributionDraftData -> DocumentUiData.SelfEmployedContribution()
    is VapzDraftData -> DocumentUiData.Vapz()
    is SalarySlipDraftData -> DocumentUiData.SalarySlip()
    is PayrollSummaryDraftData -> DocumentUiData.PayrollSummary()
    is EmploymentContractDraftData -> DocumentUiData.EmploymentContract()
    is DimonaDraftData -> DocumentUiData.Dimona()
    is C4DraftData -> DocumentUiData.C4()
    is HolidayPayDraftData -> DocumentUiData.HolidayPay()
    is ContractDraftData -> DocumentUiData.Contract()
    is LeaseDraftData -> DocumentUiData.Lease()
    is LoanDraftData -> DocumentUiData.Loan()
    is InsuranceDraftData -> DocumentUiData.Insurance()
    is DividendDraftData -> DocumentUiData.Dividend()
    is ShareholderRegisterDraftData -> DocumentUiData.ShareholderRegister()
    is CompanyExtractDraftData -> DocumentUiData.CompanyExtract()
    is AnnualAccountsDraftData -> DocumentUiData.AnnualAccounts()
    is BoardMinutesDraftData -> DocumentUiData.BoardMinutes()
    is SubsidyDraftData -> DocumentUiData.Subsidy()
    is FineDraftData -> DocumentUiData.Fine()
    is PermitDraftData -> DocumentUiData.Permit()
    is CustomsDeclarationDraftData -> DocumentUiData.CustomsDeclaration()
    is IntrastatDraftData -> DocumentUiData.Intrastat()
    is DepreciationScheduleDraftData -> DocumentUiData.DepreciationSchedule()
    is InventoryDraftData -> DocumentUiData.Inventory()
    is OtherDraftData -> DocumentUiData.Other()
}

private fun FinancialLineItem.toLineItemUiData(currencySign: String): LineItemUiData {
    val net = netAmount ?: unitPrice?.let { unit -> (quantity ?: 1L) * unit }
    val displayAmount = net?.let { "$currencySign${Money(it).toDisplayString()}" } ?: "\u2014"
    return LineItemUiData(
        description = description.ifBlank { "\u2014" },
        displayAmount = displayAmount,
    )
}
