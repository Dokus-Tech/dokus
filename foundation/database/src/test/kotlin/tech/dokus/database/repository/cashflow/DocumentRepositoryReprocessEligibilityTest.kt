package tech.dokus.database.repository.cashflow

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentBlobsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentSourcesTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.processing.DocumentProcessingConstants
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class DocumentRepositoryReprocessEligibilityTest {

    private lateinit var database: Database
    private val repository = DocumentRepository()
    private val ingestionRunRepository = DocumentIngestionRunRepository()

    private lateinit var tenantUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid.toKotlinUuid())

    @BeforeTest
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_reprocess_eligibility_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                UsersTable,
                ContactsTable,
                DocumentBlobsTable,
                DocumentsTable,
                DocumentSourcesTable,
                DocumentIngestionRunsTable
            )
        }

        tenantUuid = UUID.randomUUID()

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
        }
    }

    @AfterTest
    fun teardown() {
        transaction(database) {
            exec("DROP ALL OBJECTS")
        }
    }

    @Test
    fun `findDocumentsEligibleForReprocess includes null-status documents counted by processing health`() = runBlocking {
        val documentId = repository.create(
            tenantId = tenantId,
            payload = DocumentCreatePayload()
        )

        val stats = repository.getProcessingHealthStats(
            tenantId = tenantId,
            currentProcessingVersion = DocumentProcessingConstants.PROCESSING_VERSION
        )
        val candidates = repository.findDocumentsEligibleForReprocess(
            tenantId = tenantId,
            currentProcessingVersion = DocumentProcessingConstants.PROCESSING_VERSION,
            limit = 500
        )

        assertEquals(1, stats.failedCount)
        assertEquals(1, stats.eligibleForReprocessCount)
        assertEquals(setOf(documentId), candidates.map { it.documentId }.toSet())
    }

    @Test
    fun `findDocumentsEligibleForReprocess excludes confirmed documents and documents with active runs`() = runBlocking {
        val eligibleDocumentId = repository.create(
            tenantId = tenantId,
            payload = DocumentCreatePayload()
        )
        val confirmedDocumentId = repository.create(
            tenantId = tenantId,
            payload = DocumentCreatePayload()
        )
        val activeDocumentId = repository.create(
            tenantId = tenantId,
            payload = DocumentCreatePayload()
        )

        transaction(database) {
            DocumentsTable.update({ DocumentsTable.id eq confirmedDocumentId.value.toJavaUuid() }) {
                it[documentStatus] = DocumentStatus.Confirmed
            }
        }

        ingestionRunRepository.createRun(activeDocumentId, tenantId)

        val candidates = repository.findDocumentsEligibleForReprocess(
            tenantId = tenantId,
            currentProcessingVersion = DocumentProcessingConstants.PROCESSING_VERSION,
            limit = 500
        )

        assertEquals(setOf(eligibleDocumentId), candidates.map { it.documentId }.toSet())
    }
}
