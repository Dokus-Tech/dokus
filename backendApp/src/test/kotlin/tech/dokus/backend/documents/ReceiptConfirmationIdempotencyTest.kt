package tech.dokus.backend.documents
import kotlin.uuid.Uuid

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
import tech.dokus.backend.services.documents.confirmation.ReceiptConfirmationService
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.cashflow.CashflowEntriesTable
import tech.dokus.database.tables.cashflow.ExpensesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.ReceiptDraftData
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReceiptConfirmationIdempotencyTest {

    private lateinit var database: Database

    private lateinit var tenantUuid: Uuid
    private val tenantId: TenantId get() = TenantId(tenantUuid)

    private val documentRepository = DocumentRepository()
    private val ingestionRepository = DocumentIngestionRunRepository()
    private val draftRepository = DocumentDraftRepository()
    private val expenseRepository = ExpenseRepository()
    private val cashflowEntriesRepository = CashflowEntriesRepository()
    private val cashflowEntriesService = CashflowEntriesService(cashflowEntriesRepository)
    private val confirmationService = ReceiptConfirmationService(
        expenseRepository = expenseRepository,
        cashflowEntriesService = cashflowEntriesService,
        draftRepository = draftRepository
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
                UsersTable,
                DocumentsTable,
                DocumentBlobsTable,
                DocumentSourcesTable,
                DocumentIngestionRunsTable,
                DocumentDraftsTable,
                ContactsTable,
                ExpensesTable,
                CashflowEntriesTable
            )
        }

        tenantUuid = Uuid.random()

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
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                CashflowEntriesTable,
                ExpensesTable,
                ContactsTable,
                DocumentDraftsTable,
                DocumentIngestionRunsTable,
                DocumentSourcesTable,
                DocumentBlobsTable,
                DocumentsTable,
                UsersTable,
                TenantTable
            )
        }
    }

    @Test
    fun `confirming the same receipt twice is idempotent (no duplicates)`() = runBlocking {
        val (documentId, draftData) = createReceiptDocument(draftAmount = Money.from("18.48")!!)

        val first = confirmationService.confirm(tenantId, documentId, draftData, linkedContactId = null).getOrThrow()
        val second = confirmationService.confirm(tenantId, documentId, draftData, linkedContactId = null).getOrThrow()

        val firstExpenseId = (first.entity as FinancialDocumentDto.ExpenseDto).id
        val secondExpenseId = (second.entity as FinancialDocumentDto.ExpenseDto).id
        assertEquals(firstExpenseId, secondExpenseId)
        assertNotNull(first.cashflowEntryId)
        assertEquals(first.cashflowEntryId, second.cashflowEntryId)

        transaction(database) {
            assertEquals(1L, ExpensesTable.selectAll().count())
            assertEquals(1L, CashflowEntriesTable.selectAll().count())
        }
    }

    @Test
    fun `edit confirmed draft then reconfirm updates expense and cashflow entry`() = runBlocking {
        val (documentId, originalDraft) = createReceiptDocument(draftAmount = Money.from("18.48")!!)

        val confirmed = confirmationService.confirm(tenantId, documentId, originalDraft, linkedContactId = null).getOrThrow()
        val originalEntryId = confirmed.cashflowEntryId
        assertNotNull(originalEntryId)

        val userId = createUser()

        val updatedDraft = originalDraft.copy(
            merchantName = "Updated Merchant",
            totalAmount = Money.from("25.00")!!,
            vatAmount = Money.from("4.34")!!,
            date = LocalDate(2024, 2, 1),
            currency = Currency.Eur
        )

        draftRepository.updateDraft(
            documentId = documentId,
            tenantId = tenantId,
            userId = userId,
            updatedData = updatedDraft
        )

        val reconfirmed = confirmationService.confirm(tenantId, documentId, updatedDraft, linkedContactId = null).getOrThrow()
        val confirmedExpenseId = (confirmed.entity as FinancialDocumentDto.ExpenseDto).id
        val reconfirmedExpenseId = (reconfirmed.entity as FinancialDocumentDto.ExpenseDto).id
        assertEquals(confirmedExpenseId, reconfirmedExpenseId)
        assertEquals(originalEntryId, reconfirmed.cashflowEntryId)

        val updatedExpense = expenseRepository.findByDocumentId(tenantId, documentId)
        assertNotNull(updatedExpense)
        assertEquals("Updated Merchant", updatedExpense.merchant)
        assertEquals(Money.from("25.00")!!, updatedExpense.amount)

        val updatedEntry = cashflowEntriesRepository.getByDocumentId(tenantId, documentId).getOrThrow()
        assertNotNull(updatedEntry)
        assertEquals(Money.from("25.00")!!, updatedEntry.amountGross)
        assertEquals(CashflowEntryStatus.Open, updatedEntry.status)

        val draft = draftRepository.getByDocumentId(documentId, tenantId)
        assertNotNull(draft)
        assertEquals(DocumentStatus.Confirmed, draft.documentStatus)
    }

    private suspend fun createReceiptDocument(draftAmount: Money): Pair<DocumentId, ReceiptDraftData> {
        val documentId = documentRepository.create(
            tenantId = tenantId,
            payload = tech.dokus.database.repository.cashflow.DocumentCreatePayload(
                filename = "receipt.pdf",
                contentType = "application/pdf",
                sizeBytes = 123L,
                storageKey = "test/$tenantUuid/receipt.pdf",
                contentHash = null,
                source = DocumentSource.Upload
            )
        )

        val runId = ingestionRepository.createRun(documentId, tenantId)

        val draftData = ReceiptDraftData(
            merchantName = "Merchant",
            date = LocalDate(2024, 1, 1),
            currency = Currency.Eur,
            totalAmount = draftAmount,
            vatAmount = Money.from("3.20")
        )

        val created = draftRepository.createOrUpdateFromIngestion(
            documentId = documentId,
            tenantId = tenantId,
            runId = runId,
            extractedData = draftData,
            documentType = DocumentType.Receipt,
            force = true
        )
        assertTrue(created)

        return documentId to draftData
    }

    private fun createUser(): UserId {
        val userUuid = Uuid.random()
        val userId = UserId.parse(userUuid.toString())

        transaction(database) {
            UsersTable.insert {
                it[id] = userUuid
                it[email] = "test+$userUuid@example.com"
                it[passwordHash] = "hash"
                it[firstName] = "Test"
                it[lastName] = "User"
            }
        }

        return userId
    }
}
