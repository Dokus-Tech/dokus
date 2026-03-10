package tech.dokus.features.contacts.usecases

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.BankStatementDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
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
    documentRecord: DocumentRecordDto?
): ContactRecentInvoice {
    return ContactRecentInvoice(
        invoiceId = id,
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
    documentRecord: DocumentRecordDto?
): String? {
    return documentRecord?.draft?.purposeRendered.normalizeRecentDocumentText()
        ?: documentRecord?.draft?.purposeBase.normalizeRecentDocumentText()
        ?: documentRecord?.confirmedEntity?.recentDocumentSummary()
        ?: invoice.notes.normalizeRecentDocumentText()
}

internal fun resolveRecentDocumentReference(
    invoice: FinancialDocumentDto.InvoiceDto,
    documentRecord: DocumentRecordDto?
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
        is BankStatementDraftData -> null
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
