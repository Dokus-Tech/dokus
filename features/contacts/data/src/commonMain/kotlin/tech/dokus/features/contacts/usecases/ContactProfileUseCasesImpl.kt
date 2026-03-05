package tech.dokus.features.contacts.usecases

import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.features.contacts.repository.ContactRemoteDataSource

private const val InvoicePageSize = 100
private const val RecentDocumentsLimit = 5

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

    private fun buildSnapshot(invoices: List<FinancialDocumentDto.InvoiceDto>): ContactInvoiceSnapshot {
        val totalVolume = invoices.fold(Money.ZERO) { sum, invoice ->
            sum + invoice.totalAmount
        }
        val outstanding = invoices.fold(Money.ZERO) { sum, invoice ->
            sum + invoiceOutstanding(invoice)
        }
        val recentDocuments = invoices
            .sortedWith(
                compareByDescending<FinancialDocumentDto.InvoiceDto> { it.issueDate }
                    .thenByDescending { it.updatedAt }
            )
            .take(RecentDocumentsLimit)
            .map { invoice ->
                ContactRecentInvoice(
                    invoiceId = invoice.id,
                    issueDate = invoice.issueDate,
                    updatedAt = invoice.updatedAt,
                    direction = invoice.direction,
                    status = invoice.status,
                    totalAmount = invoice.totalAmount,
                    outstandingAmount = invoiceOutstanding(invoice)
                )
            }

        return ContactInvoiceSnapshot(
            documentsCount = invoices.size,
            totalVolume = totalVolume,
            outstanding = outstanding,
            recentDocuments = recentDocuments
        )
    }

    private fun invoiceOutstanding(invoice: FinancialDocumentDto.InvoiceDto): Money {
        if (invoice.status !in OutstandingStatuses) return Money.ZERO
        val remainder = invoice.totalAmount - invoice.paidAmount
        return if (remainder.isNegative) Money.ZERO else remainder
    }
}
