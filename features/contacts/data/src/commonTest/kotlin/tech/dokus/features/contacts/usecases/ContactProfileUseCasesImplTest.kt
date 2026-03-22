package tech.dokus.features.contacts.usecases

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocLineItem
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.IngestionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactProfileUseCasesImplTest {

    @Test
    fun `recent documents prefer purpose rendered summary`() = runBlocking {
        val documentId = DocumentId.parse("00000000-0000-0000-0000-000000000101")
        val fakeLoadDocs = FakeLoadDocumentRecordsUseCase()
        val fakeGetDoc = FakeGetDocumentRecordUseCase()

        fakeLoadDocs.items = listOf(
            listItem(documentId = documentId)
        )
        fakeGetDoc.records[documentId] = documentRecord(
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

        val snapshot = GetContactInvoiceSnapshotUseCaseImpl(fakeLoadDocs, fakeGetDoc)
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
        val documentId = DocumentId.parse("00000000-0000-0000-0000-000000000102")
        val fakeLoadDocs = FakeLoadDocumentRecordsUseCase()
        val fakeGetDoc = FakeGetDocumentRecordUseCase()

        fakeLoadDocs.items = listOf(
            listItem(documentId = documentId)
        )
        fakeGetDoc.records[documentId] = documentRecord(
            documentId = documentId,
            filename = "invoice-002.pdf",
            confirmedEntity = confirmedInvoice(
                invoiceId = "00000000-0000-0000-0000-000000000202",
                invoiceNumber = "INV-002",
                itemDescription = "Workspace subscription",
                notes = "Entity notes"
            )
        )

        val snapshot = GetContactInvoiceSnapshotUseCaseImpl(fakeLoadDocs, fakeGetDoc)
            .invoke(contactId = contactId)
            .getOrThrow()

        assertEquals("Workspace subscription", snapshot.recentDocuments.single().summary)
        assertEquals("INV-002", snapshot.recentDocuments.single().reference)
    }

    @Test
    fun `recent documents fall back to counterparty name when document metadata is missing`() = runBlocking {
        val documentId = DocumentId.parse("00000000-0000-0000-0000-000000000103")
        val fakeLoadDocs = FakeLoadDocumentRecordsUseCase()
        val fakeGetDoc = FakeGetDocumentRecordUseCase()

        fakeLoadDocs.items = listOf(
            listItem(
                documentId = documentId,
                counterpartyDisplayName = "Acme Corp"
            )
        )
        // No document record available — getDocumentRecord returns failure
        // so reference should fall back to counterpartyDisplayName

        val snapshot = GetContactInvoiceSnapshotUseCaseImpl(fakeLoadDocs, fakeGetDoc)
            .invoke(contactId = contactId)
            .getOrThrow()

        assertEquals("Acme Corp", snapshot.recentDocuments.single().reference)
    }

    @Test
    fun `recent documents only enrich newest five document ids`() = runBlocking {
        val fakeLoadDocs = FakeLoadDocumentRecordsUseCase()
        val fakeGetDoc = FakeGetDocumentRecordUseCase()

        val items = (1..6).map { index ->
            val documentId = DocumentId.parse("00000000-0000-0000-0000-00000000010$index")
            fakeGetDoc.records[documentId] = documentRecord(
                documentId = documentId,
                filename = "invoice-$index.pdf"
            )
            listItem(
                documentId = documentId,
                sortDate = LocalDate(2026, 2, index),
            )
        }
        fakeLoadDocs.items = items

        val snapshot = GetContactInvoiceSnapshotUseCaseImpl(fakeLoadDocs, fakeGetDoc)
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
            fakeGetDoc.requestedDocumentIds.map { it.toString() }.toSet()
        )
    }

    @Test
    fun `summary resolver returns null when no summary source exists`() {
        assertNull(resolveRecentDocumentSummary(documentRecord = null))
    }

    private class FakeLoadDocumentRecordsUseCase : LoadDocumentRecordsUseCase {
        var items: List<DocumentListItemDto> = emptyList()

        override suspend fun invoke(
            page: Int,
            pageSize: Int,
            filter: DocumentListFilter?,
            documentStatus: DocumentStatus?,
            ingestionStatus: IngestionStatus?,
            sortBy: String?,
            contactId: String?,
        ): Result<PaginatedResponse<DocumentListItemDto>> {
            return Result.success(
                PaginatedResponse(
                    items = items,
                    total = items.size.toLong(),
                    limit = pageSize,
                    offset = page * pageSize,
                )
            )
        }
    }

    private class FakeGetDocumentRecordUseCase : GetDocumentRecordUseCase {
        val records = mutableMapOf<DocumentId, DocumentDetailDto>()
        val requestedDocumentIds = mutableListOf<DocumentId>()

        override suspend fun invoke(documentId: DocumentId): Result<DocumentDetailDto> {
            requestedDocumentIds += documentId
            return records[documentId]
                ?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Missing document record for $documentId"))
        }
    }
}

private val tenantId = TenantId.parse("00000000-0000-0000-0000-000000000001")
private val contactId = ContactId.parse("00000000-0000-0000-0000-000000000002")
private val now = LocalDateTime(2026, 2, 1, 10, 0)

private fun listItem(
    documentId: DocumentId,
    sortDate: LocalDate = LocalDate(2026, 2, 1),
    counterpartyDisplayName: String? = null,
): DocumentListItemDto {
    return DocumentListItemDto(
        documentId = documentId,
        tenantId = tenantId,
        filename = "document.pdf",
        documentType = DocumentType.Invoice,
        direction = DocumentDirection.Inbound,
        documentStatus = DocumentStatus.Confirmed,
        ingestionStatus = null,
        effectiveOrigin = DocumentSource.Upload,
        uploadedAt = now,
        counterpartyDisplayName = counterpartyDisplayName,
        purposeRendered = null,
        totalAmount = Money(12100),
        currency = null,
        sortDate = sortDate,
    )
}

private fun confirmedInvoice(
    invoiceId: String,
    invoiceNumber: String,
    itemDescription: String? = null,
    notes: String? = null,
): DocDto.Invoice.Confirmed {
    return DocDto.Invoice.Confirmed(
        id = InvoiceId.parse(invoiceId),
        tenantId = tenantId,
        direction = DocumentDirection.Inbound,
        contactId = contactId,
        invoiceNumber = invoiceNumber,
        issueDate = LocalDate(2026, 2, 1),
        dueDate = LocalDate(2026, 2, 1),
        subtotalAmount = Money(10000),
        vatAmount = Money(2100),
        totalAmount = Money(12100),
        paidAmount = Money.ZERO,
        status = InvoiceStatus.Draft,
        notes = notes,
        lineItems = itemDescription?.let {
            listOf(
                DocLineItem(
                    description = it,
                    quantity = tech.dokus.domain.Quantity(1.0),
                    unitPrice = Money(10000),
                    vatRate = tech.dokus.domain.VatRate.STANDARD_BE,
                    netAmount = Money(10000),
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
    confirmedEntity: DocDto? = null,
): DocumentDetailDto {
    val content = confirmedEntity
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
            resolvedContact = ResolvedContact.Linked(
                contactId = contactId,
                name = "",
                vatNumber = null,
                email = null,
                avatarPath = null
            ),
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
