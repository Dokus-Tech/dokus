package tech.dokus.features.contacts.usecases

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
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
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.EmploymentContractDraftData
import tech.dokus.domain.model.ExpenseClaimDraftData
import tech.dokus.domain.model.FinancialDocumentDto
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
import tech.dokus.features.contacts.repository.ContactRemoteDataSource

private const val InvoicePageSize = 100
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
    private val remoteDataSource: ContactRemoteDataSource
) : GetContactInvoiceSnapshotUseCase {

    override suspend fun invoke(contactId: ContactId): Result<ContactInvoiceSnapshot> {
        return runCatching {
            val outbound = fetchAllInvoices(contactId, DocumentDirection.Outbound)
            val inbound = fetchAllInvoices(contactId, DocumentDirection.Inbound)
            val invoices = (outbound + inbound).distinctBy { it.id }
            buildSnapshot(invoices)
        }
    }

    private suspend fun fetchAllInvoices(
        contactId: ContactId,
        direction: DocumentDirection
    ): List<FinancialDocumentDto.InvoiceDto> {
        val invoices = mutableListOf<FinancialDocumentDto.InvoiceDto>()
        var offset = 0

        while (true) {
            val page = remoteDataSource.listInvoicesByContact(
                contactId = contactId,
                direction = direction,
                limit = InvoicePageSize,
                offset = offset
            ).getOrElse { throw it }

            if (page.items.isEmpty()) break

            invoices += page.items
            if (!page.hasMore) break
            offset += page.items.size
        }

        return invoices
    }

    private suspend fun buildSnapshot(invoices: List<FinancialDocumentDto.InvoiceDto>): ContactInvoiceSnapshot = coroutineScope {
        val totalVolume = invoices.fold(Money.ZERO) { sum, invoice ->
            sum + invoice.totalAmount
        }
        val outstanding = invoices.fold(Money.ZERO) { sum, invoice ->
            sum + invoiceOutstandingAmount(invoice)
        }
        val recentDocuments = invoices
            .sortedWith(
                compareByDescending<FinancialDocumentDto.InvoiceDto> { it.issueDate }
                    .thenByDescending { it.updatedAt }
            )
            .take(RecentDocumentsLimit)
            .map { invoice ->
                async {
                    val documentRecord = invoice.documentId
                        ?.let { documentId -> remoteDataSource.getDocumentRecord(documentId).getOrNull() }
                    invoice.toRecentDocument(documentRecord)
                }
            }
            .awaitAll()

        ContactInvoiceSnapshot(
            documentsCount = invoices.size,
            totalVolume = totalVolume,
            outstanding = outstanding,
            recentDocuments = recentDocuments
        )
    }
}

private fun invoiceOutstandingAmount(
    invoice: FinancialDocumentDto.InvoiceDto,
): Money {
    if (invoice.status !in OutstandingStatuses) return Money.ZERO
    val remainder = invoice.totalAmount - invoice.paidAmount
    return if (remainder.isNegative) Money.ZERO else remainder
}

private fun FinancialDocumentDto.InvoiceDto.toRecentDocument(
    documentRecord: DocumentDetailDto?
): ContactRecentInvoice {
    return ContactRecentInvoice(
        invoiceId = id,
        documentId = documentId,
        issueDate = issueDate,
        updatedAt = updatedAt,
        direction = direction,
        status = status,
        totalAmount = totalAmount,
        outstandingAmount = invoiceOutstandingAmount(this),
        summary = resolveRecentDocumentSummary(this, documentRecord),
        reference = resolveRecentDocumentReference(this, documentRecord)
    )
}

internal fun resolveRecentDocumentSummary(
    invoice: FinancialDocumentDto.InvoiceDto,
    documentRecord: DocumentDetailDto?
): String? {
    return documentRecord?.draft?.purposeRendered.normalizeRecentDocumentText()
        ?: documentRecord?.draft?.purposeBase.normalizeRecentDocumentText()
        ?: documentRecord?.confirmedEntity?.recentDocumentSummary()
        ?: invoice.notes.normalizeRecentDocumentText()
}

internal fun resolveRecentDocumentReference(
    invoice: FinancialDocumentDto.InvoiceDto,
    documentRecord: DocumentDetailDto?
): String? {
    return documentRecord?.draft?.extractedData?.recentDocumentReference()
        ?: documentRecord?.confirmedEntity?.recentDocumentReference()
        ?: invoice.invoiceNumber.toString().normalizeRecentDocumentText()
        ?: documentRecord?.document?.filename.normalizeRecentDocumentText()
}

private fun FinancialDocumentDto.recentDocumentSummary(): String? {
    return when (this) {
        is FinancialDocumentDto.InvoiceDto -> {
            items.firstOrNull()?.description.normalizeRecentDocumentText() ?: notes.normalizeRecentDocumentText()
        }

        is FinancialDocumentDto.CreditNoteDto -> {
            reason.normalizeRecentDocumentText() ?: notes.normalizeRecentDocumentText()
        }

        is FinancialDocumentDto.ExpenseDto -> {
            description.normalizeRecentDocumentText() ?:
                merchant.normalizeRecentDocumentText() ?:
                notes.normalizeRecentDocumentText()
        }

        is FinancialDocumentDto.QuoteDto -> {
            items.firstOrNull().normalizeRecentDocumentText() ?: notes.normalizeRecentDocumentText()
        }

        is FinancialDocumentDto.ProFormaDto -> {
            items.firstOrNull().normalizeRecentDocumentText() ?: notes.normalizeRecentDocumentText()
        }

        is FinancialDocumentDto.PurchaseOrderDto -> {
            items.firstOrNull().normalizeRecentDocumentText() ?: notes.normalizeRecentDocumentText()
        }
    }
}

private fun FinancialDocumentDto.recentDocumentReference(): String? {
    return when (this) {
        is FinancialDocumentDto.InvoiceDto -> invoiceNumber.toString().normalizeRecentDocumentText()
        is FinancialDocumentDto.CreditNoteDto -> creditNoteNumber.normalizeRecentDocumentText()
        is FinancialDocumentDto.ExpenseDto -> null
        is FinancialDocumentDto.QuoteDto -> quoteNumber.normalizeRecentDocumentText()
        is FinancialDocumentDto.ProFormaDto -> proFormaNumber.normalizeRecentDocumentText()
        is FinancialDocumentDto.PurchaseOrderDto -> poNumber.normalizeRecentDocumentText()
    }
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

private fun String?.normalizeRecentDocumentText(): String? {
    return this
        ?.lineSequence()
        ?.firstOrNull { it.isNotBlank() }
        ?.trim()
        ?.replace(WhitespaceRegex, " ")
        ?.takeIf { it.isNotBlank() }
}
