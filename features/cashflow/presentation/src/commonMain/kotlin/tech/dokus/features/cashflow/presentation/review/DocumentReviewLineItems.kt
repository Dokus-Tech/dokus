package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
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
import tech.dokus.foundation.app.state.DokusState

internal class DocumentReviewLineItems {
    suspend fun DocumentReviewCtx.handleAddLineItem() {
        val newItem = FinancialLineItem(
            description = "",
            quantity = 1L,
            unitPrice = null,
            vatRate = null,
            netAmount = null
        )
        updateLineItems(newItem, add = true)
    }

    suspend fun DocumentReviewCtx.handleUpdateLineItem(index: Int, item: FinancialLineItem) {
        updateLineItems(item, index)
    }

    suspend fun DocumentReviewCtx.handleRemoveLineItem(index: Int) {
        updateLineItems(null, index)
    }

    private suspend fun DocumentReviewCtx.updateLineItems(
        item: FinancialLineItem?,
        index: Int? = null,
        add: Boolean = false,
    ) {
        withState {
            val currentData = draftData ?: return@withState
            val docData = documentData ?: return@withState

            val currentItems = when (currentData) {
                is InvoiceDraftData -> currentData.lineItems
                is ReceiptDraftData -> currentData.lineItems
                is CreditNoteDraftData -> currentData.lineItems
                is BankStatementDraftData,
                is ProFormaDraftData,
                is QuoteDraftData,
                is OrderConfirmationDraftData,
                is DeliveryNoteDraftData,
                is ReminderDraftData,
                is StatementOfAccountDraftData,
                is PurchaseOrderDraftData,
                is ExpenseClaimDraftData,
                is BankFeeDraftData,
                is InterestStatementDraftData,
                is PaymentConfirmationDraftData,
                is VatReturnDraftData,
                is VatListingDraftData,
                is VatAssessmentDraftData,
                is IcListingDraftData,
                is OssReturnDraftData,
                is CorporateTaxDraftData,
                is CorporateTaxAdvanceDraftData,
                is TaxAssessmentDraftData,
                is PersonalTaxDraftData,
                is WithholdingTaxDraftData,
                is SocialContributionDraftData,
                is SocialFundDraftData,
                is SelfEmployedContributionDraftData,
                is VapzDraftData,
                is SalarySlipDraftData,
                is PayrollSummaryDraftData,
                is EmploymentContractDraftData,
                is DimonaDraftData,
                is C4DraftData,
                is HolidayPayDraftData,
                is ContractDraftData,
                is LeaseDraftData,
                is LoanDraftData,
                is InsuranceDraftData,
                is DividendDraftData,
                is ShareholderRegisterDraftData,
                is CompanyExtractDraftData,
                is AnnualAccountsDraftData,
                is BoardMinutesDraftData,
                is SubsidyDraftData,
                is FineDraftData,
                is PermitDraftData,
                is CustomsDeclarationDraftData,
                is IntrastatDraftData,
                is DepreciationScheduleDraftData,
                is InventoryDraftData,
                is OtherDraftData -> return@withState
            }

            val updatedItems = currentItems.toMutableList()
            when {
                add -> updatedItems.add(item ?: return@withState)
                item != null && index != null && index in updatedItems.indices -> updatedItems[index] = item
                item == null && index != null && index in updatedItems.indices -> updatedItems.removeAt(index)
                else -> return@withState
            }

            val updatedDraftData = when (currentData) {
                is InvoiceDraftData -> currentData.copy(lineItems = updatedItems)
                is ReceiptDraftData -> currentData.copy(lineItems = updatedItems)
                is CreditNoteDraftData -> currentData.copy(lineItems = updatedItems)
                is BankStatementDraftData,
                is ProFormaDraftData,
                is QuoteDraftData,
                is OrderConfirmationDraftData,
                is DeliveryNoteDraftData,
                is ReminderDraftData,
                is StatementOfAccountDraftData,
                is PurchaseOrderDraftData,
                is ExpenseClaimDraftData,
                is BankFeeDraftData,
                is InterestStatementDraftData,
                is PaymentConfirmationDraftData,
                is VatReturnDraftData,
                is VatListingDraftData,
                is VatAssessmentDraftData,
                is IcListingDraftData,
                is OssReturnDraftData,
                is CorporateTaxDraftData,
                is CorporateTaxAdvanceDraftData,
                is TaxAssessmentDraftData,
                is PersonalTaxDraftData,
                is WithholdingTaxDraftData,
                is SocialContributionDraftData,
                is SocialFundDraftData,
                is SelfEmployedContributionDraftData,
                is VapzDraftData,
                is SalarySlipDraftData,
                is PayrollSummaryDraftData,
                is EmploymentContractDraftData,
                is DimonaDraftData,
                is C4DraftData,
                is HolidayPayDraftData,
                is ContractDraftData,
                is LeaseDraftData,
                is LoanDraftData,
                is InsuranceDraftData,
                is DividendDraftData,
                is ShareholderRegisterDraftData,
                is CompanyExtractDraftData,
                is AnnualAccountsDraftData,
                is BoardMinutesDraftData,
                is SubsidyDraftData,
                is FineDraftData,
                is PermitDraftData,
                is CustomsDeclarationDraftData,
                is IntrastatDraftData,
                is DepreciationScheduleDraftData,
                is InventoryDraftData,
                is OtherDraftData -> return@withState
            }

            updateState {
                copy(
                    document = DokusState.success(docData.copy(draftData = updatedDraftData)),
                    hasUnsavedChanges = true,
                )
            }
        }
    }
}
