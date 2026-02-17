package tech.dokus.database.repository.processor

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tech.dokus.database.repository.cashflow.DocumentIngestionRunRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProcessorIngestionRepositoryTest {

    private lateinit var database: Database
    private val ingestionRunRepository = DocumentIngestionRunRepository()
    private val processorIngestionRepository = ProcessorIngestionRepository()

    private var tenantId: TenantId = TenantId.generate()
    private var documentId: DocumentId = DocumentId.generate()

    @BeforeTest
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_processor_ingestion_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                DocumentsTable,
                DocumentIngestionRunsTable
            )
        }

        val tenantUuid = Uuid.random()
        val documentUuid = Uuid.random()
        tenantId = TenantId(tenantUuid)
        documentId = DocumentId(documentUuid)

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Test Tenant"
                it[displayName] = "Test Tenant"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }

            DocumentsTable.insert {
                it[id] = documentUuid
                it[tenantId] = tenantUuid
                it[filename] = "lease-invoice.pdf"
                it[contentType] = "application/pdf"
                it[sizeBytes] = 2048L
                it[storageKey] = "documents/$tenantUuid/lease-invoice.pdf"
            }
        }
    }

    @AfterTest
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                DocumentIngestionRunsTable,
                DocumentsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `findPendingForProcessing exposes dpi and maxPages overrides from queued run`() = runBlocking {
        val runId = ingestionRunRepository.createRun(
            documentId = documentId,
            tenantId = tenantId,
            overrideMaxPages = 9,
            overrideDpi = 220
        )

        val pending = processorIngestionRepository.findPendingForProcessing(limit = 10)
        val queued = pending.firstOrNull { it.runId == runId }

        assertNotNull(queued)
        assertEquals(9, queued.overrideMaxPages)
        assertEquals(220, queued.overrideDpi)
    }
}
