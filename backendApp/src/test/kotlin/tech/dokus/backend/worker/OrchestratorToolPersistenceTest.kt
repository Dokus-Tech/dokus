package tech.dokus.backend.worker

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.processor.ProcessorIngestionRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentDraftsTable
import tech.dokus.database.tables.documents.DocumentIngestionRunsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.enums.IndexingStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.TenantPlan
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentChunkDto
import tech.dokus.domain.model.DocumentChunkId
import tech.dokus.domain.repository.ChunkRepository
import tech.dokus.domain.repository.ChunkSearchResult
import tech.dokus.domain.repository.ChunkWithEmbedding
import tech.dokus.domain.utils.json
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.models.toDomainType
import tech.dokus.features.ai.models.toExtractedDocumentData
import tech.dokus.features.ai.orchestrator.DocumentOrchestrator
import tech.dokus.features.ai.orchestrator.tools.StoreChunksTool
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionHandler
import tech.dokus.features.ai.orchestrator.tools.StoreExtractionTool
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class OrchestratorToolPersistenceTest {

    private lateinit var database: Database
    private lateinit var ingestionRepository: ProcessorIngestionRepository
    private var tenantId: TenantId = TenantId.generate()
    private var documentId: DocumentId = DocumentId.generate()
    private lateinit var runId: UUID

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_orchestrator_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                UsersTable,
                DocumentsTable,
                DocumentIngestionRunsTable,
                ContactsTable,
                DocumentDraftsTable
            )
        }

        ingestionRepository = ProcessorIngestionRepository()

        val tenantUuid = UUID.randomUUID()
        val documentUuid = UUID.randomUUID()
        runId = UUID.randomUUID()

        tenantId = TenantId(tenantUuid.toKotlinUuid())
        documentId = DocumentId.parse(documentUuid.toString())

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Orchestrator Test Tenant"
                it[displayName] = "Orchestrator Test Tenant"
                it[plan] = TenantPlan.Professional
                it[status] = TenantStatus.Active
                it[language] = Language.En
            }

            DocumentsTable.insert {
                it[id] = documentUuid
                it[tenantId] = tenantUuid
                it[filename] = "invoice.pdf"
                it[contentType] = "application/pdf"
                it[sizeBytes] = 1234
                it[storageKey] = "documents/test/invoice.pdf"
                it[contentHash] = null
                it[documentSource] = DocumentSource.Upload
            }

            DocumentIngestionRunsTable.insert {
                it[id] = runId
                it[documentId] = documentUuid
                it[tenantId] = tenantUuid
                it[status] = IngestionStatus.Processing
                it[provider] = "test"
            }
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                DocumentDraftsTable,
                ContactsTable,
                DocumentIngestionRunsTable,
                DocumentsTable,
                UsersTable,
                TenantTable
            )
        }
    }

    @Test
    fun `store_extraction and store_chunks update ingestion and indexing status`() = runBlocking {
        val storeExtractionHandler = StoreExtractionHandler { payload ->
            val docType = payload.documentType
                ?.trim()
                ?.uppercase()
                ?.let { runCatching { ClassifiedDocumentType.valueOf(it) }.getOrNull() }
                ?.toDomainType() ?: DocumentType.Unknown

            val extractedData = payload.extraction.toExtractedDocumentData(docType) ?: return@StoreExtractionHandler false
            ingestionRepository.markAsSucceeded(
                runId = payload.runId ?: return@StoreExtractionHandler false,
                tenantId = payload.tenantId,
                documentId = payload.documentId,
                documentType = docType,
                extractedData = extractedData,
                confidence = payload.confidence,
                rawText = payload.rawText,
                description = payload.description,
                keywords = payload.keywords,
                meetsThreshold = payload.confidence >= DocumentOrchestrator.AUTO_CONFIRM_THRESHOLD,
                force = false
            )
        }

        val storeExtractionTool = StoreExtractionTool(
            storeFunction = storeExtractionHandler,
            traceSink = null
        )

        val extraction = ExtractedInvoiceData(
            vendorName = "Acme Corp",
            invoiceNumber = "INV-001",
            issueDate = "2026-01-10",
            totalAmount = "10.00",
            currency = "EUR",
            confidence = 0.95,
            extractedText = "Acme Corp invoice"
        )

        val storeResult = storeExtractionTool.execute(
            StoreExtractionTool.Args(
                documentId = documentId.toString(),
                tenantId = tenantId.toString(),
                runId = runId.toString(),
                documentType = "INVOICE",
                extraction = json.encodeToString(extraction),
                description = "Acme Corp — Invoice — €10.00",
                keywords = "acme, invoice",
                confidence = 0.95,
                rawText = "Acme Corp invoice"
            )
        )

        assertTrue(storeResult.startsWith("SUCCESS"), storeResult)

        transaction(database) {
            val runRow = DocumentIngestionRunsTable.selectAll()
                .where { DocumentIngestionRunsTable.id eq runId }
                .single()
            assertEquals(IngestionStatus.Succeeded, runRow[DocumentIngestionRunsTable.status])
        }

        transaction(database) {
            val draftRow = DocumentDraftsTable.selectAll()
                .where { DocumentDraftsTable.documentId eq UUID.fromString(documentId.toString()) }
                .single()
            assertEquals("Acme Corp — Invoice — €10.00", draftRow[DocumentDraftsTable.aiDescription])
            val keywordsJson = draftRow[DocumentDraftsTable.aiKeywords]
            val keywords = keywordsJson?.let { json.decodeFromString<List<String>>(it) }
            assertNotNull(keywords)
            assertEquals(listOf("acme", "invoice"), keywords)
        }

        val chunkRepository = InMemoryChunkRepository()
        val storeChunksTool = StoreChunksTool(
            chunkRepository = chunkRepository,
            indexingUpdater = { id, status, chunksCount, errorMessage ->
                ingestionRepository.updateIndexingStatus(
                    runId = id,
                    status = status,
                    chunksCount = chunksCount,
                    errorMessage = errorMessage
                )
            }
        )

        val chunksJson =
            """[{"content":"Acme Corp invoice","embedding":[0.1,0.2,0.3],"startOffset":0,"endOffset":17,"pageNumber":1}]"""

        val chunksResult = storeChunksTool.execute(
            StoreChunksTool.Args(
                documentId = documentId.toString(),
                tenantId = tenantId.toString(),
                runId = runId.toString(),
                chunks = chunksJson
            )
        )

        assertTrue(chunksResult.startsWith("SUCCESS"), chunksResult)

        transaction(database) {
            val runRow = DocumentIngestionRunsTable.selectAll()
                .where { DocumentIngestionRunsTable.id eq runId }
                .single()
            assertEquals(IndexingStatus.Succeeded, runRow[DocumentIngestionRunsTable.indexingStatus])
            assertEquals(1, runRow[DocumentIngestionRunsTable.chunksCount])
        }
    }

    private class InMemoryChunkRepository : ChunkRepository {
        var storedChunks: List<ChunkWithEmbedding> = emptyList()

        override suspend fun searchSimilarChunks(
            tenantId: TenantId,
            queryEmbedding: List<Float>,
            documentId: DocumentId?,
            topK: Int,
            minSimilarity: Float,
            confirmedOnly: Boolean
        ): ChunkSearchResult = ChunkSearchResult(emptyList(), 0)

        override suspend fun storeChunks(
            tenantId: TenantId,
            documentId: DocumentId,
            contentHash: String,
            chunks: List<ChunkWithEmbedding>
        ) {
            storedChunks = chunks
        }

        override suspend fun getContentHashForDocument(
            tenantId: TenantId,
            documentId: DocumentId
        ): String? = null

        override suspend fun deleteChunksForDocument(
            tenantId: TenantId,
            documentId: DocumentId
        ): Int = 0

        override suspend fun getChunksForDocument(
            tenantId: TenantId,
            documentId: DocumentId
        ): List<DocumentChunkDto> = emptyList()

        override suspend fun getChunkById(
            tenantId: TenantId,
            chunkId: DocumentChunkId
        ): DocumentChunkDto? = null

        override suspend fun countChunksForDocument(
            tenantId: TenantId,
            documentId: DocumentId
        ): Long = 0

        override suspend fun hasChunks(
            tenantId: TenantId,
            documentId: DocumentId
        ): Boolean = storedChunks.isNotEmpty()

        override suspend fun countTotalChunksForTenant(tenantId: TenantId): Long = 0
    }
}
