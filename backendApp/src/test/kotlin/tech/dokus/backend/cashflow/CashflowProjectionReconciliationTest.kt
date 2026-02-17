package tech.dokus.backend.cashflow

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.backend.services.cashflow.CashflowEntriesService
import tech.dokus.backend.services.cashflow.CashflowProjectionReconciliationService
import tech.dokus.backend.worker.CashflowProjectionReconciliationWorker
import tech.dokus.database.repository.auth.TenantRepository
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentCreatePayload
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceNumberRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.services.InvoiceNumberGenerator
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoiceItemsTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.toDbDecimal
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CashflowProjectionReconciliationTest {

    private lateinit var database: Database

    private lateinit var tenantUuid: UUID
    private lateinit var contactUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid)
    private val contactId: ContactId get() = ContactId.parse(contactUuid.toString())

    private val tenantRepository = TenantRepository()
    private val documentRepository = DocumentRepository()
    private val ingestionRepository = DocumentIngestionRunRepository()
    private val draftRepository = DocumentDraftRepository()
    private val invoiceRepository = InvoiceRepository(InvoiceNumberGenerator(InvoiceNumberRepository()))
    private val expenseRepository = ExpenseRepository()
    private val creditNoteRepository = CreditNoteRepository()
    private val cashflowEntriesRepository = CashflowEntriesRepository()
    private val cashflowEntriesService = CashflowEntriesService(cashflowEntriesRepository)
    private val reconciliationService = CashflowProjectionReconciliationService(
        cashflowEntriesRepository = cashflowEntriesRepository,
        cashflowEntriesService = cashflowEntriesService
    )
    private val reconciliationWorker = CashflowProjectionReconciliationWorker(
        tenantRepository = tenantRepository,
        documentRepository = documentRepository,
        invoiceRepository = invoiceRepository,
        expenseRepository = expenseRepository,
        creditNoteRepository = creditNoteRepository,
        cashflowEntriesRepository = cashflowEntriesRepository,
        reconciliationService = reconciliationService
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
                ContactsTable,
                InvoicesTable,
                InvoiceItemsTable,
                ExpensesTable,
                CreditNotesTable,
                CashflowEntriesTable
            )
        }

        tenantUuid = Uuid.random()
        contactUuid = Uuid.random()

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
                it[name] = "Test Counterparty"
                it[contactSource] = ContactSource.Manual
            }
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                CashflowEntriesTable,
                CreditNotesTable,
                ExpensesTable,
                InvoiceItemsTable,
                InvoicesTable,
                ContactsTable,
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
    fun `ensureProjectionIfMissing preserves fully paid invoice state with paidAt and is idempotent`() = runBlocking {
        val paidAt = LocalDateTime(2024, 2, 10, 12, 0, 0)
        val total = Money.from("121.00")!!
        val (documentId, invoiceEntity) = seedConfirmedInvoice(
            totalAmount = total,
            paidAmount = total,
            paidAt = paidAt
        )

        val firstEntryId = reconciliationService
            .ensureProjectionIfMissing(tenantId, documentId, invoiceEntity)
            .getOrThrow()
        val secondEntryId = reconciliationService
            .ensureProjectionIfMissing(tenantId, documentId, invoiceEntity)
            .getOrThrow()

        assertNotNull(firstEntryId)
        assertEquals(firstEntryId, secondEntryId)
        val entry = cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrThrow()
        assertNotNull(entry)
        assertEquals(Money.ZERO, entry.remainingAmount)
        assertEquals(CashflowEntryStatus.Paid, entry.status)
        assertEquals(paidAt, entry.paidAt)
        transaction(database) {
            assertEquals(1L, CashflowEntriesTable.selectAll().count())
        }
    }

    @Test
    fun `ensureProjectionIfMissing preserves partially paid invoice state`() = runBlocking {
        val total = Money.from("121.00")!!
        val paidAmount = Money.from("21.00")!!
        val (documentId, invoiceEntity) = seedConfirmedInvoice(
            totalAmount = total,
            paidAmount = paidAmount,
            paidAt = null
        )

        val entryId = reconciliationService
            .ensureProjectionIfMissing(tenantId, documentId, invoiceEntity)
            .getOrThrow()

        assertNotNull(entryId)
        val entry = cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrThrow()
        assertNotNull(entry)
        assertEquals(Money.from("100.00")!!, entry.remainingAmount)
        assertEquals(CashflowEntryStatus.Open, entry.status)
        assertNull(entry.paidAt)
    }

    @Test
    fun `startup sweep repairs dormant missing projections and counts paid-without-paidAt anomalies`() = runBlocking {
        val total = Money.from("121.00")!!
        val (documentId, _) = seedConfirmedInvoice(
            totalAmount = total,
            paidAmount = total,
            paidAt = null
        )
        assertEquals(null, cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrThrow())

        val firstReport = reconciliationWorker.runSweepOnce()
        val secondReport = reconciliationWorker.runSweepOnce()

        val repaired = cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrThrow()
        assertNotNull(repaired)
        assertEquals(Money.ZERO, repaired.remainingAmount)
        assertEquals(CashflowEntryStatus.Paid, repaired.status)
        assertNull(repaired.paidAt)
        assertEquals(1, firstReport.paidWithoutPaidAtDetected)
        assertEquals(1, secondReport.paidWithoutPaidAtDetected)
        transaction(database) {
            assertEquals(1L, CashflowEntriesTable.selectAll().count())
        }
    }

    private suspend fun seedConfirmedInvoice(
        totalAmount: Money,
        paidAmount: Money,
        paidAt: LocalDateTime?
    ): Pair<DocumentId, FinancialDocumentDto.InvoiceDto> {
        val documentId = documentRepository.create(
            tenantId = tenantId,
            payload = DocumentCreatePayload(
                filename = "invoice.pdf",
                contentType = "application/pdf",
                sizeBytes = 100L,
                storageKey = "test/$tenantUuid/invoice-${Uuid.random()}.pdf",
                contentHash = null,
                source = DocumentSource.Upload
            )
        )

        val runId = ingestionRepository.createRun(documentId, tenantId)
        draftRepository.createOrUpdateFromIngestion(
            documentId = documentId,
            tenantId = tenantId,
            runId = runId,
            extractedData = InvoiceDraftData(
                direction = DocumentDirection.Outbound,
                invoiceNumber = "INV-${Uuid.random().toString().take(8)}",
                issueDate = LocalDate(2024, 1, 1),
                dueDate = LocalDate(2024, 1, 31),
                currency = Currency.Eur,
                subtotalAmount = Money.from("100.00"),
                vatAmount = Money.from("21.00"),
                totalAmount = Money.from("121.00")
            ),
            documentType = DocumentType.Invoice,
            force = true
        )
        draftRepository.updateDocumentStatus(documentId, tenantId, DocumentStatus.Confirmed)

        val invoiceId = Uuid.random()
        val invoiceNumber = "INV-RECON-${Uuid.random().toString().take(8)}"
        val invoiceStatus = when {
            paidAmount.minor <= 0L -> InvoiceStatus.Draft
            paidAmount.minor >= totalAmount.minor -> InvoiceStatus.Paid
            else -> InvoiceStatus.PartiallyPaid
        }
        transaction(database) {
            InvoicesTable.insert {
                it[id] = invoiceId
                it[tenantId] = tenantUuid
                it[contactId] = contactUuid
                it[InvoicesTable.invoiceNumber] = invoiceNumber
                it[issueDate] = LocalDate(2024, 1, 1)
                it[dueDate] = LocalDate(2024, 1, 31)
                it[subtotalAmount] = BigDecimal("100.00")
                it[vatAmount] = BigDecimal("21.00")
                it[InvoicesTable.totalAmount] = totalAmount.toDbDecimal()
                it[InvoicesTable.paidAmount] = paidAmount.toDbDecimal()
                it[status] = invoiceStatus
                it[direction] = DocumentDirection.Outbound
                it[InvoicesTable.documentId] = Uuid.parse(documentId.toString())
                it[InvoicesTable.paidAt] = paidAt
            }
        }

        val invoice = invoiceRepository.findByDocumentId(tenantId, documentId)
        assertNotNull(invoice)
        return documentId to invoice
    }
}
