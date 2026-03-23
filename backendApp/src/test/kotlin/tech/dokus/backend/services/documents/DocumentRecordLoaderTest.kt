package tech.dokus.backend.services.documents

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.database.entity.BankStatementEntity
import tech.dokus.database.entity.BankTransactionEntity
import tech.dokus.database.repository.banking.BankStatementRepository
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.cashflow.CashflowEntriesRepository
import tech.dokus.database.repository.cashflow.CreditNoteRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.entity.DraftSummaryEntity
import tech.dokus.database.repository.cashflow.ExpenseRepository
import tech.dokus.database.repository.cashflow.InvoiceRepository
import tech.dokus.database.repository.contacts.ContactRepository
import tech.dokus.database.repository.drafts.DraftRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.foundation.backend.storage.DocumentStorageService
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
class DocumentRecordLoaderTest {

    private val documentRepository = mockk<DocumentRepository>()
    private val ingestionRepository = mockk<DocumentIngestionRunRepository>()
    private val invoiceRepository = mockk<InvoiceRepository>()
    private val expenseRepository = mockk<ExpenseRepository>()
    private val creditNoteRepository = mockk<CreditNoteRepository>()
    private val bankStatementRepository = mockk<BankStatementRepository>()
    private val bankTransactionRepository = mockk<BankTransactionRepository>()
    private val cashflowEntriesRepository = mockk<CashflowEntriesRepository>()
    private val contactRepository = mockk<ContactRepository>()
    private val draftRepository = mockk<DraftRepository>()
    private val truthService = mockk<DocumentTruthService>()
    private val documentStorageService = mockk<DocumentStorageService>()

    private val loader = DocumentRecordLoader(
        documentRepository = documentRepository,
        ingestionRepository = ingestionRepository,
        invoiceRepository = invoiceRepository,
        expenseRepository = expenseRepository,
        creditNoteRepository = creditNoteRepository,
        bankStatementRepository = bankStatementRepository,
        bankTransactionRepository = bankTransactionRepository,
        cashflowEntriesRepository = cashflowEntriesRepository,
        contactRepository = contactRepository,
        draftRepository = draftRepository,
        truthService = truthService,
        documentStorageService = documentStorageService,
    )

    private val tenantId = TenantId.parse("11111111-1111-1111-1111-111111111111")
    private val documentId = DocumentId.parse("22222222-2222-2222-2222-222222222222")
    private val now = LocalDateTime(2026, 3, 20, 10, 0, 0)

    @Test
    fun `confirmed bank statement reloads canonical content after draft cleanup`() = runTest {
        val document = DocumentDto(
            id = documentId,
            tenantId = tenantId,
            filename = "statement.pdf",
            uploadedAt = now,
            sortDate = LocalDate(2026, 3, 20),
        )
        val draft = DraftSummaryEntity(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.Confirmed,
            documentType = DocumentType.BankStatement,
            direction = DocumentDirection.Neutral,
            aiDraftSourceRunId = null,
            draftVersion = 1,
            draftEditedAt = null,
            draftEditedBy = null,
            rejectReason = null,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now,
        )
        val statement = BankStatementEntity(
            id = kotlin.uuid.Uuid.parse("33333333-3333-3333-3333-333333333333"),
            tenantId = tenantId,
            documentId = documentId,
            source = BankTransactionSource.PdfStatement,
            statementTrust = StatementTrust.High,
            accountIban = Iban("BE68539007547034"),
            periodStart = LocalDate(2026, 3, 1),
            periodEnd = LocalDate(2026, 3, 31),
            openingBalance = Money.from("1000.00", Currency.Eur),
            closingBalance = Money.from("1200.00", Currency.Eur),
            transactionCount = 1,
            createdAt = now,
        )
        val transaction = BankTransactionEntity(
            id = tech.dokus.domain.ids.BankTransactionId.parse("44444444-4444-4444-4444-444444444444"),
            tenantId = tenantId,
            documentId = documentId,
            source = BankTransactionSource.PdfStatement,
            transactionDate = LocalDate(2026, 3, 15),
            signedAmount = Money.from("200.00", Currency.Eur)!!,
            counterpartyName = "ACME NV",
            descriptionRaw = "SEPA transfer",
            status = BankTransactionStatus.Unmatched,
            statementTrust = StatementTrust.High,
            createdAt = now,
            updatedAt = now,
        )

        coEvery { documentRepository.getById(tenantId, documentId) } returns document
        coEvery { truthService.listSources(tenantId, documentId) } returns emptyList()
        coEvery { documentRepository.getDraftByDocumentId(documentId, tenantId) } returns draft
        coEvery { ingestionRepository.getLatestForDocument(documentId, tenantId) } returns null
        coEvery { truthService.getPendingReviewByDocument(tenantId, documentId) } returns null
        coEvery { bankStatementRepository.findByDocumentId(tenantId, documentId) } returns statement
        coEvery { bankTransactionRepository.listByDocument(tenantId, documentId) } returns listOf(transaction)
        coEvery { cashflowEntriesRepository.getByDocumentId(tenantId, documentId) } returns Result.success(null)

        val record = loader.load(tenantId, documentId)

        val content = assertNotNull(record?.draft?.content)
        val bankStatement = assertIs<DocDto.BankStatement.Draft>(content)
        assertEquals("BE68539007547034", bankStatement.accountIban?.value)
        assertEquals(1, bankStatement.transactions.size)
        assertEquals("ACME NV", bankStatement.transactions.single().counterparty.name)
        coVerify(exactly = 0) { draftRepository.getDraftAsDocDto(any(), any(), any()) }
    }
}
