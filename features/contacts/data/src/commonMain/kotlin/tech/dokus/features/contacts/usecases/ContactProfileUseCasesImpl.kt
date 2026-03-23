package tech.dokus.features.contacts.usecases

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.InvoiceId
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
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentListItemDto
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
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.features.contacts.repository.ContactRemoteDataSource

private const val DocumentPageSize = 100
private const val RecentDocumentsLimit = 5
private val WhitespaceRegex = Regex("\\s+")

private val OutstandingStatuses = setOf(
    InvoiceStatus.Draft,
    InvoiceStatus.Sent,
    InvoiceStatus.Viewed,
    InvoiceStatus.PartiallyPaid,
    InvoiceStatus.Overdue,
)

internal class GetContactPeppolStatusUseCaseImpl(
    private val remoteDataSource: ContactRemoteDataSource
) : GetContactPeppolStatusUseCase {
    override suspend fun invoke(
        contactId: ContactId,
        refresh: Boolean
    ) = remoteDataSource.getContactPeppolStatus(contactId, refresh)
}

internal class GetContactInvoiceSnapshotUseCaseImpl(
    private val loadDocumentRecords: LoadDocumentRecordsUseCase,
    private val getDocumentRecord: GetDocumentRecordUseCase,
) : GetContactInvoiceSnapshotUseCase {

    override suspend fun invoke(contactId: ContactId): Result<ContactInvoiceSnapshot> {
        return runCatching {
            val page = loadDocumentRecords(
                page = 0,
                pageSize = DocumentPageSize,
                contactId = contactId,
            ).getOrElse { throw it }

            buildSnapshot(page.items)
        }
    }

    private suspend fun buildSnapshot(documents: List<DocumentListItemDto>): ContactInvoiceSnapshot = coroutineScope {
        val totalVolume = documents.fold(Money.zero(Currency.Eur)) { sum, doc ->
            sum + (doc.totalAmount ?: Money.zero(Currency.Eur))
        }

        val confirmedDocs = documents.filter { it.documentStatus == DocumentStatus.Confirmed }

        val recentDocuments = confirmedDocs
            .sortedByDescending { it.sortDate }
            .take(RecentDocumentsLimit)
            .map { doc ->
                async {
                    val detail = getDocumentRecord(doc.documentId).getOrNull()
                    doc.toRecentDocument(detail)
                }
            }
            .awaitAll()

        ContactInvoiceSnapshot(
            documentsCount = documents.size,
            totalVolume = totalVolume,
            outstanding = Money.zero(Currency.Eur),
            recentDocuments = recentDocuments
        )
    }
}

private fun DocumentListItemDto.toRecentDocument(
    detail: DocumentDetailDto?,
): ContactRecentInvoice {
    val invoiceContent = detail?.draft?.content as? DocDto.Invoice.Confirmed
    val outstanding = if (invoiceContent != null) invoiceOutstandingAmount(invoiceContent) else Money.zero(Currency.Eur)

    return ContactRecentInvoice(
        invoiceId = invoiceContent?.id ?: InvoiceId.parse("00000000-0000-0000-0000-000000000000"),
        documentId = documentId,
        issueDate = sortDate,
        updatedAt = uploadedAt,
        direction = direction ?: DocumentDirection.Unknown,
        status = invoiceContent?.status ?: InvoiceStatus.Draft,
        totalAmount = totalAmount ?: Money.zero(Currency.Eur),
        outstandingAmount = outstanding,
        summary = resolveRecentDocumentSummary(detail),
        reference = resolveRecentDocumentReference(detail, this),
    )
}

private fun invoiceOutstandingAmount(
    invoice: DocDto.Invoice.Confirmed,
): Money {
    if (invoice.status !in OutstandingStatuses) return Money.zero(Currency.Eur)
    val total = invoice.totalAmount ?: return Money.zero(Currency.Eur)
    val remainder = total - invoice.paidAmount
    return if (remainder.isNegative) Money.zero(Currency.Eur) else remainder
}

internal fun resolveRecentDocumentSummary(
    documentRecord: DocumentDetailDto?
): String? {
    return documentRecord?.draft?.purposeRendered.normalizeRecentDocumentText()
        ?: documentRecord?.draft?.purposeBase.normalizeRecentDocumentText()
        ?: documentRecord?.draft?.content?.recentDocumentSummary()
}

internal fun resolveRecentDocumentReference(
    documentRecord: DocumentDetailDto?,
    listItem: DocumentListItemDto? = null,
): String? {
    return documentRecord?.draft?.content?.recentDocumentReference()
        ?: listItem?.counterpartyDisplayName.normalizeRecentDocumentText()
        ?: documentRecord?.document?.filename.normalizeRecentDocumentText()
}

private fun DocumentDraftData.recentDocumentReference(): String? {
    return when (this) {
        is InvoiceDraftData -> invoiceNumber.normalizeRecentDocumentText()
        is CreditNoteDraftData -> creditNoteNumber.normalizeRecentDocumentText()
        is ReceiptDraftData -> receiptNumber.normalizeRecentDocumentText()
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
        is OtherDraftData -> null
    }
}

private fun DocDto.recentDocumentSummary(): String? = when (this) {
    is DocDto.Invoice -> lineItems.firstOrNull()?.description.normalizeRecentDocumentText()
        ?: notes.normalizeRecentDocumentText()
    is DocDto.CreditNote -> reason.normalizeRecentDocumentText()
        ?: notes.normalizeRecentDocumentText()
    is DocDto.Receipt -> merchantName.normalizeRecentDocumentText()
        ?: notes.normalizeRecentDocumentText()
    is DocDto.BankStatement -> notes.normalizeRecentDocumentText()
    is DocDto.ClassifiedDoc -> null
}

private fun DocDto.recentDocumentReference(): String? = when (this) {
    is DocDto.Invoice -> invoiceNumber.normalizeRecentDocumentText()
    is DocDto.CreditNote -> creditNoteNumber.normalizeRecentDocumentText()
    is DocDto.Receipt -> receiptNumber.normalizeRecentDocumentText()
    is DocDto.BankStatement,
    is DocDto.ClassifiedDoc -> null
}

private fun String?.normalizeRecentDocumentText(): String? {
    return this
        ?.lineSequence()
        ?.firstOrNull { it.isNotBlank() }
        ?.trim()
        ?.replace(WhitespaceRegex, " ")
        ?.takeIf { it.isNotBlank() }
}
