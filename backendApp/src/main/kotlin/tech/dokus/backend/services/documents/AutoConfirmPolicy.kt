package tech.dokus.backend.services.documents

import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
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
import tech.dokus.domain.model.toDocumentType
import tech.dokus.domain.processing.DocumentProcessingConstants

data class AutoConfirmInput(
    val source: DocumentSource,
    val documentType: DocumentType,
    val draftData: DocumentDraftData,
    val auditPassed: Boolean,
    val confidence: Double,
    val contactId: ContactId?,
    val directionResolvedFromAiHintOnly: Boolean,
)

enum class AutoConfirmRejection {
    ManualSource,
    TypeMismatch,
    MissingRequiredFields,
    DirectionFromAiHintOnly,
    InvalidDirection,
    NonPositiveAmount,
    AuditFailed,
    InsufficientConfidence,
    UnresolvedCounterparty,
}

class AutoConfirmPolicy {

    fun evaluate(input: AutoConfirmInput): AutoConfirmRejection? {
        val (source, documentType, draftData, auditPassed, confidence, contactId, directionFromAiHint) = input

        if (source == DocumentSource.Manual) return AutoConfirmRejection.ManualSource

        val draftType = draftData.toDocumentType()
        if (documentType == DocumentType.Unknown || draftType != documentType) return AutoConfirmRejection.TypeMismatch
        if (draftData is InvoiceDraftData && contactId == null) return AutoConfirmRejection.UnresolvedCounterparty
        if (draftData is CreditNoteDraftData && contactId == null) return AutoConfirmRejection.UnresolvedCounterparty
        if (!hasRequiredFieldsForAutoConfirm(draftData)) return AutoConfirmRejection.MissingRequiredFields
        if (directionFromAiHint) return AutoConfirmRejection.DirectionFromAiHintOnly
        if (!isDirectionValid(draftData)) return AutoConfirmRejection.InvalidDirection
        if (!isAmountPositive(draftData)) return AutoConfirmRejection.NonPositiveAmount
        if (!auditPassed) return AutoConfirmRejection.AuditFailed

        return when (source) {
            DocumentSource.Peppol -> null
            DocumentSource.Upload,
            DocumentSource.Email -> {
                val meetsConfidence = confidence >= DocumentProcessingConstants.AUTO_CONFIRM_CONFIDENCE_THRESHOLD
                if (!meetsConfidence) return AutoConfirmRejection.InsufficientConfidence
                if (contactId == null && draftData !is ReceiptDraftData) {
                    return AutoConfirmRejection.UnresolvedCounterparty
                }
                null
            }
            DocumentSource.Manual -> AutoConfirmRejection.ManualSource
        }
    }

    private fun isDirectionValid(draftData: DocumentDraftData): Boolean {
        return when (draftData) {
            is InvoiceDraftData -> draftData.direction != DocumentDirection.Unknown
            is ReceiptDraftData -> draftData.direction != DocumentDirection.Unknown
            is CreditNoteDraftData -> draftData.direction != DocumentDirection.Unknown
            is BankStatementDraftData -> draftData.direction == DocumentDirection.Neutral
            is ProFormaDraftData -> draftData.direction != DocumentDirection.Unknown
            is QuoteDraftData -> draftData.direction != DocumentDirection.Unknown
            is OrderConfirmationDraftData -> draftData.direction != DocumentDirection.Unknown
            is DeliveryNoteDraftData -> draftData.direction != DocumentDirection.Unknown
            is ReminderDraftData -> draftData.direction != DocumentDirection.Unknown
            is StatementOfAccountDraftData -> draftData.direction != DocumentDirection.Unknown
            is PurchaseOrderDraftData -> draftData.direction != DocumentDirection.Unknown
            is ExpenseClaimDraftData -> draftData.direction != DocumentDirection.Unknown
            is BankFeeDraftData -> draftData.direction != DocumentDirection.Unknown
            is InterestStatementDraftData -> draftData.direction != DocumentDirection.Unknown
            is PaymentConfirmationDraftData -> draftData.direction != DocumentDirection.Unknown
            is VatReturnDraftData -> draftData.direction != DocumentDirection.Unknown
            is VatListingDraftData -> draftData.direction != DocumentDirection.Unknown
            is VatAssessmentDraftData -> draftData.direction != DocumentDirection.Unknown
            is IcListingDraftData -> draftData.direction != DocumentDirection.Unknown
            is OssReturnDraftData -> draftData.direction != DocumentDirection.Unknown
            is CorporateTaxDraftData -> draftData.direction != DocumentDirection.Unknown
            is CorporateTaxAdvanceDraftData -> draftData.direction != DocumentDirection.Unknown
            is TaxAssessmentDraftData -> draftData.direction != DocumentDirection.Unknown
            is PersonalTaxDraftData -> draftData.direction != DocumentDirection.Unknown
            is WithholdingTaxDraftData -> draftData.direction != DocumentDirection.Unknown
            is SocialContributionDraftData -> draftData.direction != DocumentDirection.Unknown
            is SocialFundDraftData -> draftData.direction != DocumentDirection.Unknown
            is SelfEmployedContributionDraftData -> draftData.direction != DocumentDirection.Unknown
            is VapzDraftData -> draftData.direction != DocumentDirection.Unknown
            is SalarySlipDraftData -> draftData.direction != DocumentDirection.Unknown
            is PayrollSummaryDraftData -> draftData.direction != DocumentDirection.Unknown
            is EmploymentContractDraftData -> draftData.direction != DocumentDirection.Unknown
            is DimonaDraftData -> draftData.direction != DocumentDirection.Unknown
            is C4DraftData -> draftData.direction != DocumentDirection.Unknown
            is HolidayPayDraftData -> draftData.direction != DocumentDirection.Unknown
            is ContractDraftData -> draftData.direction != DocumentDirection.Unknown
            is LeaseDraftData -> draftData.direction != DocumentDirection.Unknown
            is LoanDraftData -> draftData.direction != DocumentDirection.Unknown
            is InsuranceDraftData -> draftData.direction != DocumentDirection.Unknown
            is DividendDraftData -> draftData.direction != DocumentDirection.Unknown
            is ShareholderRegisterDraftData -> draftData.direction != DocumentDirection.Unknown
            is CompanyExtractDraftData -> draftData.direction != DocumentDirection.Unknown
            is AnnualAccountsDraftData -> draftData.direction != DocumentDirection.Unknown
            is BoardMinutesDraftData -> draftData.direction != DocumentDirection.Unknown
            is SubsidyDraftData -> draftData.direction != DocumentDirection.Unknown
            is FineDraftData -> draftData.direction != DocumentDirection.Unknown
            is PermitDraftData -> draftData.direction != DocumentDirection.Unknown
            is CustomsDeclarationDraftData -> draftData.direction != DocumentDirection.Unknown
            is IntrastatDraftData -> draftData.direction != DocumentDirection.Unknown
            is DepreciationScheduleDraftData -> draftData.direction != DocumentDirection.Unknown
            is InventoryDraftData -> draftData.direction != DocumentDirection.Unknown
            is OtherDraftData -> draftData.direction != DocumentDirection.Unknown
        }
    }

    private fun isAmountPositive(draftData: DocumentDraftData): Boolean {
        return when (draftData) {
            is InvoiceDraftData -> draftData.totalAmount?.isPositive == true
            is ReceiptDraftData -> draftData.totalAmount?.isPositive == true
            is CreditNoteDraftData -> draftData.totalAmount?.isPositive == true
            is BankStatementDraftData -> false
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
            is OtherDraftData -> false
        }
    }

    private fun hasRequiredFieldsForAutoConfirm(draftData: DocumentDraftData): Boolean {
        return when (draftData) {
            is InvoiceDraftData -> true
            is ReceiptDraftData -> {
                draftData.date != null &&
                    !draftData.merchantName.isNullOrBlank() &&
                    draftData.totalAmount != null
            }
            is CreditNoteDraftData -> {
                !draftData.creditNoteNumber.isNullOrBlank() &&
                    draftData.issueDate != null &&
                    draftData.subtotalAmount != null &&
                    draftData.vatAmount != null &&
                    draftData.totalAmount != null
            }
            is BankStatementDraftData -> draftData.transactions.isNotEmpty()
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
            is OtherDraftData -> true
        }
    }
}
