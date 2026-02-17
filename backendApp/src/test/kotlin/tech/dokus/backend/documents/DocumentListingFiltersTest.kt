package tech.dokus.backend.documents

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.cashflow.CreditNotesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentMatchReviewsTable
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.ContactSource
import tech.dokus.domain.enums.CreditNoteStatus
import tech.dokus.domain.enums.CreditNoteType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DocumentListingFiltersTest {

    private lateinit var database: Database
    private val documentRepository = DocumentRepository()

    private lateinit var tenantUuid: UUID
    private lateinit var contactUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid)

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
                UsersTable,
                DocumentsTable,
                DocumentBlobsTable,
                DocumentSourcesTable,
                DocumentMatchReviewsTable,
                DocumentIngestionRunsTable,
                DocumentDraftsTable,
                ContactsTable,
                InvoicesTable,
                ExpensesTable,
                CreditNotesTable
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
                it[name] = "Acme"
                it[contactSource] = ContactSource.Manual
            }
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                CreditNotesTable,
                ExpensesTable,
                InvoicesTable,
                ContactsTable,
                DocumentMatchReviewsTable,
                DocumentSourcesTable,
                DocumentBlobsTable,
                DocumentDraftsTable,
                DocumentIngestionRunsTable,
                DocumentsTable,
                UsersTable,
                TenantTable
            )
        }
    }

    @Test
    fun `NeedsAttention filter returns canonical workflow set and total matches`() = runBlocking {
        val docQueued = insertDocument("queued.pdf").also { insertIngestion(it, IngestionStatus.Queued) }
        val docProcessing = insertDocument("processing.pdf").also { insertIngestion(it, IngestionStatus.Processing) }
        val docFailed = insertDocument("failed.pdf").also { insertIngestion(it, IngestionStatus.Failed) }
        val docSucceededNoDraft = insertDocument("succeeded-no-draft.pdf").also {
            insertIngestion(it, IngestionStatus.Succeeded)
        }

        val docNeedsReviewNoIngestion = insertDocument("needs-review-no-ingestion.pdf").also {
            insertDraft(it, DocumentStatus.NeedsReview, DocumentType.Invoice)
        }

        val docNeedsReview = insertDocument("needs-review.pdf").also {
            insertIngestion(it, IngestionStatus.Succeeded)
            insertDraft(it, DocumentStatus.NeedsReview, DocumentType.Receipt)
        }

        val docConfirmedWithEntity = insertDocument("confirmed-with-entity.pdf").also {
            insertIngestion(it, IngestionStatus.Succeeded)
            insertDraft(it, DocumentStatus.Confirmed, DocumentType.Invoice)
            insertInvoice(it)
        }

        val docConfirmedNoEntity = insertDocument("confirmed-no-entity.pdf").also {
            insertIngestion(it, IngestionStatus.Succeeded)
            insertDraft(it, DocumentStatus.Confirmed, DocumentType.Invoice)
        }

        val docRejected = insertDocument("rejected.pdf").also {
            insertIngestion(it, IngestionStatus.Succeeded)
            insertDraft(it, DocumentStatus.Rejected, DocumentType.Invoice)
        }

        val (items, total) = documentRepository.listWithDraftsAndIngestion(
            tenantId = tenantId,
            filter = DocumentListFilter.NeedsAttention,
            documentStatus = null,
            documentType = null,
            ingestionStatus = null,
            search = null,
            page = 0,
            limit = 100
        )

        val ids = items.map { it.document.id }.toSet()
        val expected = setOf(
            docQueued,
            docProcessing,
            docFailed,
            docSucceededNoDraft,
            docNeedsReviewNoIngestion,
            docNeedsReview,
            docConfirmedNoEntity
        ).map { DocumentId.parse(it.toString()) }.toSet()

        assertEquals(expected, ids)
        assertEquals(expected.size.toLong(), total)
        assertTrue(DocumentId.parse(docConfirmedWithEntity.toString()) !in ids, "Confirmed+entity must not be in NeedsAttention")
        assertTrue(DocumentId.parse(docRejected.toString()) !in ids, "Rejected must not be in NeedsAttention")
    }

    @Test
    fun `Confirmed filter requires confirmed draft and entity existence`() = runBlocking {
        val docConfirmedWithEntity = insertDocument("confirmed-with-entity.pdf").also {
            insertIngestion(it, IngestionStatus.Succeeded)
            insertDraft(it, DocumentStatus.Confirmed, DocumentType.Invoice)
            insertInvoice(it)
        }

        val docConfirmedNoEntity = insertDocument("confirmed-no-entity.pdf").also {
            insertIngestion(it, IngestionStatus.Succeeded)
            insertDraft(it, DocumentStatus.Confirmed, DocumentType.Invoice)
        }

        val docNoDraftSucceeded = insertDocument("no-draft-succeeded.pdf").also {
            insertIngestion(it, IngestionStatus.Succeeded)
        }

        val (items, total) = documentRepository.listWithDraftsAndIngestion(
            tenantId = tenantId,
            filter = DocumentListFilter.Confirmed,
            documentStatus = null,
            documentType = null,
            ingestionStatus = null,
            search = null,
            page = 0,
            limit = 100
        )

        val ids = items.map { it.document.id }.toSet()
        val expected = setOf(DocumentId.parse(docConfirmedWithEntity.toString()))

        assertEquals(expected, ids)
        assertEquals(1L, total)
        assertTrue(DocumentId.parse(docConfirmedNoEntity.toString()) !in ids)
        assertTrue(DocumentId.parse(docNoDraftSucceeded.toString()) !in ids)
    }

    @Test
    fun `IngestionStatus filter is applied before pagination and totals are correct`() = runBlocking {
        val failedDocs = (1..25).map { index ->
            insertDocument("failed-$index.pdf").also { insertIngestion(it, IngestionStatus.Failed) }
        }
        (1..5).forEach { index ->
            insertDocument("succeeded-$index.pdf").also { insertIngestion(it, IngestionStatus.Succeeded) }
        }

        val (page0, total) = documentRepository.listWithDraftsAndIngestion(
            tenantId = tenantId,
            filter = null,
            documentStatus = null,
            documentType = null,
            ingestionStatus = IngestionStatus.Failed,
            search = null,
            page = 0,
            limit = 10
        )

        assertEquals(25L, total)
        assertEquals(10, page0.size)
        assertTrue(page0.all { it.latestIngestion?.status == IngestionStatus.Failed })

        val (page1, _) = documentRepository.listWithDraftsAndIngestion(
            tenantId = tenantId,
            filter = null,
            documentStatus = null,
            documentType = null,
            ingestionStatus = IngestionStatus.Failed,
            search = null,
            page = 1,
            limit = 10
        )

        assertEquals(10, page1.size)
        assertTrue(page1.all { it.latestIngestion?.status == IngestionStatus.Failed })

        val (page2, _) = documentRepository.listWithDraftsAndIngestion(
            tenantId = tenantId,
            filter = null,
            documentStatus = null,
            documentType = null,
            ingestionStatus = IngestionStatus.Failed,
            search = null,
            page = 2,
            limit = 10
        )

        assertEquals(5, page2.size)
        assertTrue(page2.all { it.latestIngestion?.status == IngestionStatus.Failed })
        assertEquals(
            failedDocs.map { DocumentId.parse(it.toString()) }.toSet(),
            (page0 + page1 + page2).map { it.document.id }.toSet()
        )
    }

    private fun insertDocument(filename: String): UUID {
        val docUuid = Uuid.random()
        transaction(database) {
            DocumentsTable.insert {
                it[id] = docUuid
                it[tenantId] = tenantUuid
                it[DocumentsTable.filename] = filename
                it[contentType] = "application/pdf"
                it[sizeBytes] = 123L
                it[storageKey] = "test/$docUuid/$filename"
                it[contentHash] = null
                it[documentSource] = DocumentSource.Upload
            }
        }
        return docUuid
    }

    private fun insertIngestion(documentUuid: UUID, status: IngestionStatus) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        transaction(database) {
            DocumentIngestionRunsTable.insert {
                it[id] = Uuid.random()
                it[documentId] = documentUuid
                it[tenantId] = tenantUuid
                it[DocumentIngestionRunsTable.status] = status
                it[queuedAt] = now
                when (status) {
                    IngestionStatus.Processing -> it[startedAt] = now
                    IngestionStatus.Succeeded, IngestionStatus.Failed -> {
                        it[startedAt] = now
                        it[finishedAt] = now
                    }
                    IngestionStatus.Queued -> Unit
                }
            }
        }
    }

    private fun insertDraft(documentUuid: UUID, status: DocumentStatus, type: DocumentType) {
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        transaction(database) {
            DocumentDraftsTable.insert {
                it[documentId] = documentUuid
                it[tenantId] = tenantUuid
                it[documentStatus] = status
                it[documentType] = type
                it[aiDraftData] = null
                it[aiDraftSourceRunId] = null
                it[extractedData] = null
                it[lastSuccessfulRunId] = null
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
    }

    private fun insertInvoice(documentUuid: UUID) {
        transaction(database) {
            InvoicesTable.insert {
                it[id] = Uuid.random()
                it[tenantId] = tenantUuid
                it[contactId] = contactUuid
                it[invoiceNumber] = "INV-${documentUuid.toString().take(8)}"
                it[issueDate] = LocalDate(2024, 1, 1)
                it[dueDate] = LocalDate(2024, 2, 1)
                it[subtotalAmount] = BigDecimal("100.00")
                it[vatAmount] = BigDecimal("21.00")
                it[totalAmount] = BigDecimal("121.00")
                it[paidAmount] = BigDecimal.ZERO
                it[status] = InvoiceStatus.Draft
                it[direction] = DocumentDirection.Outbound
                it[currency] = Currency.Eur
                it[documentId] = documentUuid
            }
        }
    }

    @Suppress("unused")
    private fun insertExpense(documentUuid: UUID) {
        transaction(database) {
            ExpensesTable.insert {
                it[id] = Uuid.random()
                it[tenantId] = tenantUuid
                it[date] = LocalDate(2024, 1, 1)
                it[merchant] = "Shop"
                it[amount] = BigDecimal("10.00")
                it[category] = ExpenseCategory.Other
                it[documentId] = documentUuid
                it[contactId] = null
            }
        }
    }

    @Suppress("unused")
    private fun insertCreditNote(documentUuid: UUID) {
        transaction(database) {
            CreditNotesTable.insert {
                it[id] = Uuid.random()
                it[tenantId] = tenantUuid
                it[contactId] = contactUuid
                it[creditNoteType] = CreditNoteType.Sales
                it[creditNoteNumber] = "CN-${documentUuid.toString().take(8)}"
                it[issueDate] = LocalDate(2024, 1, 1)
                it[subtotalAmount] = BigDecimal("50.00")
                it[vatAmount] = BigDecimal("10.50")
                it[totalAmount] = BigDecimal("60.50")
                it[currency] = Currency.Eur
                it[status] = CreditNoteStatus.Confirmed
                it[documentId] = documentUuid
                it[settlementIntent] = tech.dokus.domain.enums.SettlementIntent.Unknown
            }
        }
    }
}
