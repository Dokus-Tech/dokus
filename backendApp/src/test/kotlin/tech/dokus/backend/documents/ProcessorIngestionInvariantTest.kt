package tech.dokus.backend.documents

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.cashflow.DocumentDraftRepository
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.ProcessingOutcome
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.InvoiceDraftData
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class ProcessorIngestionInvariantTest {

    private lateinit var database: Database
    private lateinit var tenantUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid.toKotlinUuid())

    private val documentRepository = DocumentRepository()
    private val ingestionRunRepository = DocumentIngestionRunRepository()
    private val draftRepository = DocumentDraftRepository()
    private val processorIngestionRepository = ProcessorIngestionRepository()

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
                ContactsTable,
                DocumentsTable,
                DocumentBlobsTable,
                DocumentSourcesTable,
                DocumentIngestionRunsTable,
                DocumentDraftsTable
            )
        }

        tenantUuid = UUID.randomUUID()
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
                DocumentDraftsTable,
                DocumentIngestionRunsTable,
                DocumentSourcesTable,
                DocumentBlobsTable,
                DocumentsTable,
                ContactsTable,
                UsersTable,
                TenantTable
            )
        }
    }

    @Test
    fun `markAsSucceeded never confirms draft even when AutoConfirmEligible`() = runBlocking {
        val documentId = documentRepository.create(
            tenantId = tenantId,
            payload = tech.dokus.database.repository.cashflow.DocumentCreatePayload(
                filename = "invoice.pdf",
                contentType = "application/pdf",
                sizeBytes = 123L,
                storageKey = "test/$tenantUuid/invoice.pdf",
                contentHash = null,
                source = DocumentSource.Upload
            )
        )

        val runId = ingestionRunRepository.createRun(documentId, tenantId)

        val draftData = InvoiceDraftData(
            direction = DocumentDirection.Inbound,
            invoiceNumber = "INV-001",
            issueDate = LocalDate(2024, 1, 1),
            dueDate = LocalDate(2024, 2, 1),
            currency = Currency.Eur,
            subtotalAmount = Money.from("100.00"),
            vatAmount = Money.from("21.00"),
            totalAmount = Money.from("121.00")
        )

        val marked = processorIngestionRepository.markAsSucceeded(
            runId = runId.toString(),
            tenantId = tenantUuid.toString(),
            documentId = documentId.toString(),
            documentType = DocumentType.Invoice,
            draftData = draftData,
            rawExtractionJson = "{}",
            confidence = 0.99,
            processingOutcome = ProcessingOutcome.AutoConfirmEligible,
            rawText = null,
            description = "Auto-confirm eligible, but still needs review"
        )
        assertTrue(marked)

        val draft = draftRepository.getByDocumentId(documentId, tenantId)
        assertNotNull(draft)
        assertEquals(DocumentStatus.NeedsReview, draft.documentStatus)
    }
}
