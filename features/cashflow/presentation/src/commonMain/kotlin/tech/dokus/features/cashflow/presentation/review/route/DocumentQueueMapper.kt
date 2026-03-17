package tech.dokus.features.cashflow.presentation.review.route

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.FinancialDocumentDto
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
import tech.dokus.domain.model.toCurrency
import tech.dokus.domain.model.toTotalAmount
import tech.dokus.features.cashflow.presentation.documents.components.computeNeedsAttention
import tech.dokus.features.cashflow.presentation.documents.components.resolveCounterparty
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.app.shell.DocQueueStatus
import tech.dokus.foundation.app.shell.DocQueueStatusDetail
import kotlin.time.Clock

internal fun DocumentDetailDto.toDocQueueItem(): DocQueueItem {
    val vendorName = resolveCounterparty(this, "\u2014").ifBlank { "\u2014" }
    val queueAmount = extractQueueAmount(this)
    val status = extractQueueStatus(this)
    val statusDetail = extractQueueStatusDetail(this, status)
    return DocQueueItem(
        id = document.id,
        vendorName = DisplayName(vendorName),
        date = extractQueueDate(this),
        amount = queueAmount?.amount,
        currency = queueAmount?.currency ?: Currency.default,
        status = status,
        statusDetail = statusDetail,
    )
}

internal fun DocumentListItemDto.toDocQueueItem(): DocQueueItem {
    val vendorName = counterpartyDisplayName?.takeIf { it.isNotBlank() } ?: "\u2014"
    val status = extractListItemQueueStatus(this)
    val statusDetail = if (status == DocQueueStatus.Processing) DocQueueStatusDetail.Processing else null
    return DocQueueItem(
        id = documentId,
        vendorName = DisplayName(vendorName),
        date = sortDate,
        amount = totalAmount,
        currency = currency ?: Currency.default,
        status = status,
        statusDetail = statusDetail,
    )
}

private fun extractListItemQueueStatus(item: DocumentListItemDto): DocQueueStatus {
    val ingestion = item.ingestionStatus
    return when {
        ingestion == IngestionStatus.Queued || ingestion == IngestionStatus.Processing ->
            DocQueueStatus.Processing
        ingestion == null -> DocQueueStatus.Processing
        item.documentStatus == DocumentStatus.NeedsReview || item.documentStatus == null ->
            DocQueueStatus.Review
        item.documentStatus == DocumentStatus.Confirmed -> DocQueueStatus.Unpaid
        else -> DocQueueStatus.Review
    }
}

private fun extractQueueStatus(doc: DocumentDetailDto): DocQueueStatus {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val invoiceEntity = doc.confirmedEntity as? FinancialDocumentDto.InvoiceDto
    val draftDueDate = (doc.draft?.extractedData as? InvoiceDraftData)?.dueDate
    val ingestionStatus = doc.latestIngestion?.status

    return when {
        computeNeedsAttention(doc) -> DocQueueStatus.Review
        invoiceEntity != null &&
            (invoiceEntity.paymentInfo != null || invoiceEntity.paidAmount >= invoiceEntity.totalAmount) ->
            DocQueueStatus.Paid
        invoiceEntity != null && invoiceEntity.dueDate < today -> DocQueueStatus.Overdue
        invoiceEntity != null -> DocQueueStatus.Unpaid
        draftDueDate != null && draftDueDate < today -> DocQueueStatus.Overdue
        ingestionStatus == IngestionStatus.Queued || ingestionStatus == IngestionStatus.Processing ->
            DocQueueStatus.Processing
        else -> DocQueueStatus.Unpaid
    }
}

private fun extractQueueStatusDetail(
    doc: DocumentDetailDto,
    status: DocQueueStatus
): DocQueueStatusDetail? {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    return when (status) {
        DocQueueStatus.Paid,
        DocQueueStatus.Unpaid -> null
        DocQueueStatus.Review -> {
            val ingestion = doc.latestIngestion?.status
            if (ingestion == IngestionStatus.Queued || ingestion == IngestionStatus.Processing) {
                DocQueueStatusDetail.Processing
            } else {
                null
            }
        }
        DocQueueStatus.Overdue -> {
            val dueDate = (doc.confirmedEntity as? FinancialDocumentDto.InvoiceDto)?.dueDate
                ?: (doc.draft?.extractedData as? InvoiceDraftData)?.dueDate
            if (dueDate != null && dueDate < today) {
                DocQueueStatusDetail.OverdueDays(dueDate.daysUntil(today))
            } else {
                null
            }
        }
        DocQueueStatus.Processing -> DocQueueStatusDetail.Processing
    }
}

private fun extractQueueDate(doc: DocumentDetailDto): LocalDate {
    val data = doc.draft?.extractedData
    return when (data) {
        is InvoiceDraftData -> data.issueDate
        is ReceiptDraftData -> data.date
        is CreditNoteDraftData -> data.issueDate
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
        is OtherDraftData,
        null -> null
    } ?: doc.document.uploadedAt.date
}

private data class QueueAmount(
    val amount: Money,
    val currency: Currency,
)

private fun extractQueueAmount(doc: DocumentDetailDto): QueueAmount? {
    val data = doc.draft?.extractedData ?: return null
    val amount = data.toTotalAmount() ?: return null
    return QueueAmount(
        amount = amount,
        currency = data.toCurrency(),
    )
}
