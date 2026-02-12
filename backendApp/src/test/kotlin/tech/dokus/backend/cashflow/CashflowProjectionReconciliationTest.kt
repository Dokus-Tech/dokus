package tech.dokus.backend.cashflow

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
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
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
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class CashflowProjectionReconciliationTest {

    private lateinit var database: Database

    private lateinit var tenantUuid: UUID
    private lateinit var contactUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid.toKotlinUuid())
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
                DocumentsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `ensureProjectionIfMissing creates missing invoice projection once`() = runBlocking {
        val (documentId, invoiceEntity) = seedConfirmedInvoice()

        val firstEntryId = reconciliationService
            .ensureProjectionIfMissing(tenantId, documentId, invoiceEntity)
            .getOrThrow()
        val secondEntryId = reconciliationService
            .ensureProjectionIfMissing(tenantId, documentId, invoiceEntity)
            .getOrThrow()

        assertNotNull(firstEntryId)
        assertEquals(firstEntryId, secondEntryId)
        transaction(database) {
            assertEquals(1L, CashflowEntriesTable.selectAll().count())
        }
    }

    @Test
    fun `startup sweep repairs dormant missing projections idempotently`() = runBlocking {
        val (documentId, _) = seedConfirmedInvoice()
        assertEquals(null, cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrThrow())

        reconciliationWorker.runSweepOnce()
        reconciliationWorker.runSweepOnce()

        val repaired = cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrThrow()
        assertNotNull(repaired)
        transaction(database) {
            assertEquals(1L, CashflowEntriesTable.selectAll().count())
        }
    }

    private suspend fun seedConfirmedInvoice(): Pair<DocumentId, FinancialDocumentDto.InvoiceDto> {
        val documentId = documentRepository.create(
            tenantId = tenantId,
            payload = DocumentCreatePayload(
                filename = "invoice.pdf",
                contentType = "application/pdf",
                sizeBytes = 100L,
                storageKey = "test/$tenantUuid/invoice-${UUID.randomUUID()}.pdf",
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
                invoiceNumber = "INV-${UUID.randomUUID().toString().take(8)}",
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

        val invoiceId = UUID.randomUUID()
        val invoiceNumber = "INV-RECON-${UUID.randomUUID().toString().take(8)}"
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
                it[totalAmount] = BigDecimal("121.00")
                it[paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[direction] = DocumentDirection.Outbound
                it[InvoicesTable.documentId] = UUID.fromString(documentId.toString())
            }
        }

        val invoice = invoiceRepository.findByDocumentId(tenantId, documentId)
        assertNotNull(invoice)
        return documentId to invoice
    }
}
