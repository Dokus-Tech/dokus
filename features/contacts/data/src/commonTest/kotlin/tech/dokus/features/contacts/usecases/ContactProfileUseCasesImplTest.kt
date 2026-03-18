package tech.dokus.features.contacts.usecases

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.ContactLinkSource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.toDocDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.ContactStats
import tech.dokus.domain.model.contact.CreateContactNoteRequest
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.UpdateContactNoteRequest
import tech.dokus.domain.model.contact.UpdateContactRequest
import tech.dokus.features.contacts.repository.ContactRemoteDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactProfileUseCasesImplTest {

    @Test
    fun `recent documents prefer purpose rendered summary`() = runBlocking {
        val remoteDataSource = FakeContactRemoteDataSource()
        val documentId = DocumentId.parse("00000000-0000-0000-0000-000000000101")
        remoteDataSource.outboundInvoices = listOf(
            invoice(
                invoiceId = "00000000-0000-0000-0000-000000000201",
                invoiceNumber = "INV-001",
                documentId = documentId,
                notes = "Fallback invoice notes"
            )
        )
        remoteDataSource.documentRecords[documentId] = documentRecord(
            documentId = documentId,
            filename = "invoice-001.pdf",
            purposeRendered = "Google - Cloud credits February 2026",
            purposeBase = "Cloud credits",
            confirmedEntity = confirmedInvoice(
                invoiceId = "00000000-0000-0000-0000-000000000201",
                invoiceNumber = "INV-001",
                itemDescription = "Workspace subscription",
                notes = "Entity notes"
            )
        )

        val snapshot = GetContactInvoiceSnapshotUseCaseImpl(remoteDataSource)
            .invoke(contactId = contactId)
            .getOrThrow()

        assertEquals(
            "Google - Cloud credits February 2026",
            snapshot.recentDocuments.single().summary
        )
        assertEquals("INV-001", snapshot.recentDocuments.single().reference)
    }

    @Test
    fun `recent documents fall back to confirmed entity summary`() = runBlocking {
        val remoteDataSource = FakeContactRemoteDataSource()
        val documentId = DocumentId.parse("00000000-0000-0000-0000-000000000102")
        remoteDataSource.outboundInvoices = listOf(
            invoice(
                invoiceId = "00000000-0000-0000-0000-000000000202",
                invoiceNumber = "INV-002",
                documentId = documentId,
                notes = "Invoice note fallback"
            )
        )
        remoteDataSource.documentRecords[documentId] = documentRecord(
            documentId = documentId,
            filename = "invoice-002.pdf",
            confirmedEntity = confirmedInvoice(
                invoiceId = "00000000-0000-0000-0000-000000000202",
                invoiceNumber = "INV-002",
                itemDescription = "Workspace subscription",
                notes = "Entity notes"
            )
        )

        val snapshot = GetContactInvoiceSnapshotUseCaseImpl(remoteDataSource)
            .invoke(contactId = contactId)
            .getOrThrow()

        assertEquals("Workspace subscription", snapshot.recentDocuments.single().summary)
        assertEquals("INV-002", snapshot.recentDocuments.single().reference)
    }

    @Test
    fun `recent documents fall back to invoice notes and reference when document metadata is missing`() = runBlocking {
        val remoteDataSource = FakeContactRemoteDataSource()
        remoteDataSource.outboundInvoices = listOf(
            invoice(
                invoiceId = "00000000-0000-0000-0000-000000000203",
                invoiceNumber = "INV-003",
                documentId = null,
                notes = "Ad campaign management"
            )
        )

        val snapshot = GetContactInvoiceSnapshotUseCaseImpl(remoteDataSource)
            .invoke(contactId = contactId)
            .getOrThrow()

        assertEquals("Ad campaign management", snapshot.recentDocuments.single().summary)
        assertEquals("INV-003", snapshot.recentDocuments.single().reference)
    }

    @Test
    fun `recent documents only enrich newest five document ids`() = runBlocking {
        val remoteDataSource = FakeContactRemoteDataSource()
        val invoices = (1..6).map { index ->
            val documentId = DocumentId.parse("00000000-0000-0000-0000-00000000010$index")
            remoteDataSource.documentRecords[documentId] = documentRecord(
                documentId = documentId,
                filename = "invoice-$index.pdf"
            )
            invoice(
                invoiceId = "00000000-0000-0000-0000-00000000020$index",
                invoiceNumber = "INV-00$index",
                documentId = documentId,
                issueDate = LocalDate(2026, 2, index)
            )
        }
        remoteDataSource.outboundInvoices = invoices

        val snapshot = GetContactInvoiceSnapshotUseCaseImpl(remoteDataSource)
            .invoke(contactId = contactId)
            .getOrThrow()

        assertEquals(6, snapshot.documentsCount)
        assertEquals(5, snapshot.recentDocuments.size)
        assertEquals(
            setOf(
                "00000000-0000-0000-0000-000000000106",
                "00000000-0000-0000-0000-000000000105",
                "00000000-0000-0000-0000-000000000104",
                "00000000-0000-0000-0000-000000000103",
                "00000000-0000-0000-0000-000000000102",
            ),
            remoteDataSource.requestedDocumentIds.map { it.toString() }.toSet()
        )
    }

    @Test
    fun `summary resolver returns null when no summary source exists`() {
        val invoice = invoice(
            invoiceId = "00000000-0000-0000-0000-000000000204",
            invoiceNumber = "INV-004",
            documentId = null,
            notes = null
        )

        assertNull(resolveRecentDocumentSummary(invoice, documentRecord = null))
        assertEquals("INV-004", resolveRecentDocumentReference(invoice, documentRecord = null))
    }

    private class FakeContactRemoteDataSource : ContactRemoteDataSource {
        var outboundInvoices: List<FinancialDocumentDto.InvoiceDto> = emptyList()
        var inboundInvoices: List<FinancialDocumentDto.InvoiceDto> = emptyList()
        val documentRecords = mutableMapOf<DocumentId, DocumentDetailDto>()
        val requestedDocumentIds = mutableListOf<DocumentId>()

        override suspend fun listContacts(
            isActive: Boolean?,
            limit: Int,
            offset: Int
        ): Result<List<ContactDto>> = error("unused")

        override suspend fun lookupContacts(
            query: String,
            isActive: Boolean?,
            limit: Int,
            offset: Int
        ): Result<List<ContactDto>> = error("unused")

        override suspend fun listCustomers(
            isActive: Boolean,
            limit: Int,
            offset: Int
        ): Result<List<ContactDto>> = error("unused")

        override suspend fun listVendors(
            isActive: Boolean,
            limit: Int,
            offset: Int
        ): Result<List<ContactDto>> = error("unused")

        override suspend fun getContact(contactId: ContactId): Result<ContactDto> = error("unused")

        override suspend fun createContact(request: CreateContactRequest): Result<ContactDto> = error("unused")

        override suspend fun updateContact(
            contactId: ContactId,
            request: UpdateContactRequest
        ): Result<ContactDto> = error("unused")

        override suspend fun deleteContact(contactId: ContactId): Result<Unit> = error("unused")

        override suspend fun getContactPeppolStatus(
            contactId: ContactId,
            refresh: Boolean
        ): Result<PeppolStatusResponse> = error("unused")

        override suspend fun listInvoicesByContact(
            contactId: ContactId,
            direction: DocumentDirection?,
            limit: Int,
            offset: Int
        ): Result<PaginatedResponse<FinancialDocumentDto.InvoiceDto>> {
            val source = when (direction) {
                DocumentDirection.Outbound -> outboundInvoices
                DocumentDirection.Inbound -> inboundInvoices
                else -> outboundInvoices + inboundInvoices
            }
            val items = source.drop(offset).take(limit)
            return Result.success(
                PaginatedResponse(
                    items = items,
                    total = source.size.toLong(),
                    limit = limit,
                    offset = offset
                )
            )
        }

        override suspend fun getDocumentRecord(documentId: DocumentId): Result<DocumentDetailDto> {
            requestedDocumentIds += documentId
            return documentRecords[documentId]
                ?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Missing document record for $documentId"))
        }

        override suspend fun getContactActivity(contactId: ContactId): Result<ContactActivitySummary> = error("unused")

        override suspend fun getContactStats(): Result<ContactStats> = error("unused")

        override suspend fun mergeContacts(
            sourceContactId: ContactId,
            targetContactId: ContactId
        ): Result<ContactMergeResult> = error("unused")

        override suspend fun listNotes(
            contactId: ContactId,
            limit: Int,
            offset: Int
        ): Result<List<ContactNoteDto>> = error("unused")

        override suspend fun createNote(
            contactId: ContactId,
            request: CreateContactNoteRequest
        ): Result<ContactNoteDto> = error("unused")

        override suspend fun updateNote(
            contactId: ContactId,
            noteId: ContactNoteId,
            request: UpdateContactNoteRequest
        ): Result<ContactNoteDto> = error("unused")

        override suspend fun deleteNote(
            contactId: ContactId,
            noteId: ContactNoteId
        ): Result<Unit> = error("unused")

        override fun observeContactChanges(contactId: ContactId) = error("unused")
    }
}

private val tenantId = TenantId.parse("00000000-0000-0000-0000-000000000001")
private val contactId = ContactId.parse("00000000-0000-0000-0000-000000000002")
private val now = LocalDateTime(2026, 2, 1, 10, 0)

private fun invoice(
    invoiceId: String,
    invoiceNumber: String,
    documentId: DocumentId?,
    notes: String? = null,
    issueDate: LocalDate = LocalDate(2026, 2, 1),
): FinancialDocumentDto.InvoiceDto {
    return FinancialDocumentDto.InvoiceDto(
        id = InvoiceId.parse(invoiceId),
        tenantId = tenantId,
        direction = DocumentDirection.Inbound,
        contactId = contactId,
        invoiceNumber = InvoiceNumber(invoiceNumber),
        issueDate = issueDate,
        dueDate = issueDate,
        subtotalAmount = Money(10000),
        vatAmount = Money(2100),
        totalAmount = Money(12100),
        paidAmount = Money.ZERO,
        status = InvoiceStatus.Draft,
        notes = notes,
        documentId = documentId,
        createdAt = now,
        updatedAt = now
    )
}

private fun confirmedInvoice(
    invoiceId: String,
    invoiceNumber: String,
    itemDescription: String? = null,
    notes: String? = null,
): FinancialDocumentDto.InvoiceDto {
    return FinancialDocumentDto.InvoiceDto(
        id = InvoiceId.parse(invoiceId),
        tenantId = tenantId,
        direction = DocumentDirection.Inbound,
        contactId = contactId,
        invoiceNumber = InvoiceNumber(invoiceNumber),
        issueDate = LocalDate(2026, 2, 1),
        dueDate = LocalDate(2026, 2, 1),
        subtotalAmount = Money(10000),
        vatAmount = Money(2100),
        totalAmount = Money(12100),
        paidAmount = Money.ZERO,
        status = InvoiceStatus.Draft,
        notes = notes,
        items = itemDescription?.let {
            listOf(
                InvoiceItemDto(
                    description = it,
                    quantity = 1.0,
                    unitPrice = Money(10000),
                    vatRate = tech.dokus.domain.VatRate.STANDARD_BE,
                    lineTotal = Money(10000),
                    vatAmount = Money(2100)
                )
            )
        } ?: emptyList(),
        createdAt = now,
        updatedAt = now
    )
}

private fun documentRecord(
    documentId: DocumentId,
    filename: String,
    purposeRendered: String? = null,
    purposeBase: String? = null,
    confirmedEntity: FinancialDocumentDto? = null,
): DocumentDetailDto {
    val content = confirmedEntity?.toDocDto()
        ?: DocDto.Invoice.Draft(invoiceNumber = null)

    return DocumentDetailDto(
        document = DocumentDto(
            id = documentId,
            tenantId = tenantId,
            filename = filename,
            uploadedAt = now,
            sortDate = now.date,
        ),
        draft = DocumentDraftDto(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.Confirmed,
            documentType = DocumentType.Invoice,
            content = content,
            purposeBase = purposeBase,
            purposeRendered = purposeRendered,
            resolvedContact = if (contactId != null) {
                ResolvedContact.Linked(contactId = contactId, name = "", vatNumber = null, email = null, avatarPath = null)
            } else {
                ResolvedContact.Unknown
            },
            aiDraftSourceRunId = null,
            draftVersion = 0,
            draftEditedAt = null,
            draftEditedBy = null,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        ),
        latestIngestion = null,
    )
}

private fun confirmedEntityReference(confirmedEntity: FinancialDocumentDto?): String? {
    return (confirmedEntity as? FinancialDocumentDto.InvoiceDto)?.invoiceNumber?.toString()
}
