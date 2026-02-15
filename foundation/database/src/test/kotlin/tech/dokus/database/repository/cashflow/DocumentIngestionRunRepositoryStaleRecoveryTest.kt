package tech.dokus.database.repository.cashflow

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.IngestionRunId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.processing.DocumentProcessingConstants
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class DocumentIngestionRunRepositoryStaleRecoveryTest {

    private lateinit var database: Database
    private val repository = DocumentIngestionRunRepository()

    private var tenantId: TenantId = TenantId.generate()
    private var documentId: DocumentId = DocumentId.generate()
    private lateinit var tenantUuid: UUID
    private lateinit var documentUuid: UUID

    @BeforeTest
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_ingestion_stale_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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

        tenantUuid = UUID.randomUUID()
        documentUuid = UUID.randomUUID()
        tenantId = TenantId(tenantUuid.toKotlinUuid())
        documentId = DocumentId(documentUuid.toKotlinUuid())

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
                it[filename] = "stale-run.pdf"
                it[contentType] = "application/pdf"
                it[sizeBytes] = 123L
                it[storageKey] = "docs/$tenantUuid/stale-run.pdf"
                it[documentSource] = DocumentSource.Upload
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
    fun `getLatestForDocument recovers stale processing run to failed`() = runBlocking {
        insertProcessingRun(stale = true)

        val latest = repository.getLatestForDocument(documentId, tenantId)

        assertNotNull(latest)
        assertEquals(IngestionStatus.Failed, latest.status)
        assertEquals(DocumentProcessingConstants.ingestionTimeoutErrorMessage(), latest.errorMessage)
    }

    @Test
    fun `findActiveRun returns null after stale processing auto recovery`() = runBlocking {
        insertProcessingRun(stale = true)

        val active = repository.findActiveRun(documentId, tenantId)

        assertNull(active)
    }

    @Test
    fun `findActiveRun returns null after null startedAt processing auto recovery`() = runBlocking {
        insertProcessingRunWithNullStartedAt()

        val active = repository.findActiveRun(documentId, tenantId)
        val latest = repository.getLatestForDocument(documentId, tenantId)

        assertNull(active)
        assertNotNull(latest)
        assertEquals(IngestionStatus.Failed, latest.status)
        assertEquals(DocumentProcessingConstants.ingestionTimeoutErrorMessage(), latest.errorMessage)
    }

    @Test
    fun `listByDocument exposes stale processing as failed after recovery`() = runBlocking {
        insertProcessingRun(stale = true)

        val runs = repository.listByDocument(documentId, tenantId)

        assertEquals(1, runs.size)
        assertEquals(IngestionStatus.Failed, runs.first().status)
        assertEquals(DocumentProcessingConstants.ingestionTimeoutErrorMessage(), runs.first().errorMessage)
    }

    private fun insertProcessingRun(stale: Boolean): IngestionRunId {
        val runId = IngestionRunId.generate()
        val now = Clock.System.now()
        val staleStartedAt = now -
            (DocumentProcessingConstants.INGESTION_RUN_TIMEOUT + 1.minutes)
        val startedAt = if (stale) staleStartedAt else now
        val queuedAt = startedAt.toLocalDateTime(TimeZone.UTC)

        transaction(database) {
            DocumentIngestionRunsTable.insert {
                it[DocumentIngestionRunsTable.id] = runId.value.toJavaUuid()
                it[DocumentIngestionRunsTable.documentId] = documentUuid
                it[DocumentIngestionRunsTable.tenantId] = tenantUuid
                it[DocumentIngestionRunsTable.status] = IngestionStatus.Processing
                it[DocumentIngestionRunsTable.queuedAt] = queuedAt
                it[DocumentIngestionRunsTable.startedAt] = queuedAt
            }
        }

        return runId
    }

    private fun insertProcessingRunWithNullStartedAt(): IngestionRunId {
        val runId = IngestionRunId.generate()
        val queuedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        transaction(database) {
            DocumentIngestionRunsTable.insert {
                it[DocumentIngestionRunsTable.id] = runId.value.toJavaUuid()
                it[DocumentIngestionRunsTable.documentId] = documentUuid
                it[DocumentIngestionRunsTable.tenantId] = tenantUuid
                it[DocumentIngestionRunsTable.status] = IngestionStatus.Processing
                it[DocumentIngestionRunsTable.queuedAt] = queuedAt
                it[DocumentIngestionRunsTable.startedAt] = null
            }
        }

        return runId
    }
}
