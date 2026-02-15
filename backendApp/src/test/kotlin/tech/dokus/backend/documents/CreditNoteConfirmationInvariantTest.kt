package tech.dokus.backend.documents

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.backend.services.cashflow.CreditNoteService
import tech.dokus.backend.services.documents.confirmation.CreditNoteConfirmationService
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.DocumentCreatePayload
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.InvoiceNumberRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.RefundClaimRepository
import tech.dokus.database.repository.documents.DocumentLinkRepository
import tech.dokus.database.services.InvoiceNumberGenerator
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.database.tables.cashflow.InvoiceItemsTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.cashflow.RefundClaimsTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentLinksTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.CashflowSourceType
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentLinkType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreditNoteDraftData
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class CreditNoteConfirmationInvariantTest {

    private lateinit var database: Database

    private lateinit var tenantUuid: UUID
    private lateinit var contactUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid.toKotlinUuid())
    private val contactId: ContactId get() = ContactId.parse(contactUuid.toString())

    private val documentRepository = DocumentRepository()
    private val ingestionRunRepository = DocumentIngestionRunRepository()
    private val draftRepository = DocumentDraftRepository()
    private val cashflowEntriesRepository = CashflowEntriesRepository()
    private val documentLinkRepository = DocumentLinkRepository()
    private val invoiceRepository = InvoiceRepository(InvoiceNumberGenerator(InvoiceNumberRepository()))
    private val creditNoteRepository = CreditNoteRepository()
    private val refundClaimRepository = RefundClaimRepository()
    private val cashflowEntriesService = CashflowEntriesService(cashflowEntriesRepository)
    private val creditNoteService = CreditNoteService(
        creditNoteRepository = creditNoteRepository,
        refundClaimRepository = refundClaimRepository,
        cashflowEntriesService = cashflowEntriesService
    )
    private val confirmationService = CreditNoteConfirmationService(
        creditNoteService = creditNoteService,
        draftRepository = draftRepository,
        documentLinkRepository = documentLinkRepository,
        invoiceRepository = invoiceRepository
    )

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                DocumentsTable,
                DocumentBlobsTable,
                DocumentSourcesTable,
                DocumentIngestionRunsTable,
                DocumentDraftsTable,
                DocumentLinksTable,
                ContactsTable,
                InvoicesTable,
                InvoiceItemsTable,
                CreditNotesTable,
                RefundClaimsTable,
                CashflowEntriesTable
            )
        }

        tenantUuid = UUID.randomUUID()
        contactUuid = UUID.randomUUID()

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Test Company"
                it[displayName] = "Test Company"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }

            ContactsTable.insert {
                it[id] = contactUuid
                it[tenantId] = tenantUuid
                it[name] = "Coolblue"
                it[contactSource] = ContactSource.Manual
            }
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                CashflowEntriesTable,
                RefundClaimsTable,
                CreditNotesTable,
                InvoiceItemsTable,
                InvoicesTable,
                ContactsTable,
                DocumentLinksTable,
                DocumentDraftsTable,
                DocumentIngestionRunsTable,
                DocumentSourcesTable,
                DocumentBlobsTable,
                DocumentsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `credit note confirmation does not create or mutate cashflow and keeps one internal audit link`() = runBlocking {
        val (invoiceDocumentId, invoiceId, invoiceNumber) = seedInvoiceDocument()
        val originalEntry = cashflowEntriesRepository.createEntry(
            tenantId = tenantId,
            sourceType = CashflowSourceType.Invoice,
            sourceId = invoiceId,
            documentId = invoiceDocumentId,
            direction = CashflowDirection.Out,
            eventDate = LocalDate(2024, 2, 10),
            amountGross = Money.from("1000.00")!!,
            amountVat = Money.from("210.00")!!,
            contactId = contactId
        ).getOrThrow()

        val draftData = CreditNoteDraftData(
            creditNoteNumber = "CN-100",
            direction = DocumentDirection.Inbound,
            issueDate = LocalDate(2024, 2, 2),
            currency = Currency.Eur,
            subtotalAmount = Money.from("300.00"),
            vatAmount = Money.from("63.00"),
            totalAmount = Money.from("363.00"),
            originalInvoiceNumber = invoiceNumber
        )
        val creditNoteDocumentId = seedCreditNoteDraft(draftData)

        val first = confirmationService.confirm(tenantId, creditNoteDocumentId, draftData, contactId).getOrThrow()
        val second = confirmationService.confirm(tenantId, creditNoteDocumentId, draftData, contactId).getOrThrow()

        assertNull(first.cashflowEntryId)
        assertNull(second.cashflowEntryId)
        assertNull(cashflowEntriesRepository.getByDocumentId(tenantId, creditNoteDocumentId).getOrThrow())

        val entryAfter = cashflowEntriesRepository.getEntry(originalEntry.id, tenantId).getOrThrow()
        assertEquals(Money.from("1000.00")!!, entryAfter?.amountGross)
        assertEquals(Money.from("1000.00")!!, entryAfter?.remainingAmount)
        assertEquals(CashflowEntryStatus.Open, entryAfter?.status)

        val links = documentLinkRepository.getBySourceAndType(
            tenantId = tenantId,
            sourceDocumentId = creditNoteDocumentId,
            linkType = DocumentLinkType.OriginalDocument
        )
        assertEquals(1, links.size)
        assertEquals(invoiceDocumentId, links.single().targetDocumentId)
        assertNull(links.single().externalReference)
    }

    @Test
    fun `credit note confirmation stores external original reference when internal target is not found`() = runBlocking {
        val originalInvoiceNumber = "INV-NOT-IN-SYSTEM"
        val draftData = CreditNoteDraftData(
            creditNoteNumber = "CN-200",
            direction = DocumentDirection.Inbound,
            issueDate = LocalDate(2024, 2, 3),
            currency = Currency.Eur,
            subtotalAmount = Money.from("100.00"),
            vatAmount = Money.from("21.00"),
            totalAmount = Money.from("121.00"),
            originalInvoiceNumber = originalInvoiceNumber
        )
        val creditNoteDocumentId = seedCreditNoteDraft(draftData)

        confirmationService.confirm(tenantId, creditNoteDocumentId, draftData, contactId).getOrThrow()

        val links = documentLinkRepository.getBySourceAndType(
            tenantId = tenantId,
            sourceDocumentId = creditNoteDocumentId,
            linkType = DocumentLinkType.OriginalDocument
        )
        assertEquals(1, links.size)
        assertNull(links.single().targetDocumentId)
        assertEquals(originalInvoiceNumber, links.single().externalReference)
        assertTrue(cashflowEntriesRepository.listEntries(tenantId).getOrThrow().isEmpty())
    }

    private suspend fun seedInvoiceDocument(): Triple<DocumentId, UUID, String> {
        val documentId = documentRepository.create(
            tenantId = tenantId,
            payload = DocumentCreatePayload(
                filename = "invoice.pdf",
                contentType = "application/pdf",
                sizeBytes = 123L,
                storageKey = "test/$tenantUuid/invoice.pdf",
                contentHash = null,
                source = DocumentSource.Upload
            )
        )
        val invoiceId = UUID.randomUUID()
        val invoiceNumber = "INV-REF-100"

        transaction(database) {
            InvoicesTable.insert {
                it[id] = invoiceId
                it[tenantId] = tenantUuid
                it[contactId] = contactUuid
                it[InvoicesTable.invoiceNumber] = invoiceNumber
                it[issueDate] = LocalDate(2024, 1, 1)
                it[dueDate] = LocalDate(2024, 2, 10)
                it[subtotalAmount] = BigDecimal("826.45")
                it[vatAmount] = BigDecimal("173.55")
                it[totalAmount] = BigDecimal("1000.00")
                it[paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[direction] = DocumentDirection.Outbound
                it[InvoicesTable.documentId] = UUID.fromString(documentId.toString())
            }
        }

        return Triple(documentId, invoiceId, invoiceNumber)
    }

    private suspend fun seedCreditNoteDraft(draftData: CreditNoteDraftData): DocumentId {
        val documentId = documentRepository.create(
            tenantId = tenantId,
            payload = DocumentCreatePayload(
                filename = "credit-note.pdf",
                contentType = "application/pdf",
                sizeBytes = 123L,
                storageKey = "test/$tenantUuid/credit-note-${UUID.randomUUID()}.pdf",
                contentHash = null,
                source = DocumentSource.Upload
            )
        )

        val runId = ingestionRunRepository.createRun(documentId, tenantId)
        draftRepository.createOrUpdateFromIngestion(
            documentId = documentId,
            tenantId = tenantId,
            runId = runId,
            extractedData = draftData,
            documentType = DocumentType.CreditNote,
            force = true
        )
        return documentId
    }
}
