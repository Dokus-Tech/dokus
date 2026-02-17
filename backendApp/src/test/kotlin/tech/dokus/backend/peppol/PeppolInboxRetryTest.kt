package tech.dokus.backend.peppol
import kotlin.uuid.Uuid

import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.backend.worker.decodePeppolAttachmentBase64
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.database.repository.peppol.PeppolTransmissionRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.database.tables.peppol.PeppolSettingsTable
import tech.dokus.database.tables.peppol.PeppolTransmissionsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.peppol.mapper.PeppolMapper
import tech.dokus.peppol.model.PeppolDocumentList
import tech.dokus.peppol.model.PeppolInboxItem
import tech.dokus.peppol.model.PeppolMonetaryTotals
import tech.dokus.peppol.model.PeppolReceivedDocument
import tech.dokus.peppol.model.PeppolTaxTotal
import tech.dokus.peppol.provider.PeppolCredentials
import tech.dokus.peppol.provider.PeppolProvider
import tech.dokus.peppol.provider.PeppolProviderFactory
import tech.dokus.peppol.provider.client.RecommandCredentials
import tech.dokus.peppol.service.PeppolCredentialResolver
import tech.dokus.peppol.service.PeppolService
import tech.dokus.peppol.validator.PeppolValidator
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PeppolInboxRetryTest {

    private lateinit var database: Database

    private lateinit var tenantUuid: Uuid
    private val tenantId: TenantId get() = TenantId(tenantUuid)

    private lateinit var settingsRepository: PeppolSettingsRepository
    private lateinit var transmissionRepository: PeppolTransmissionRepository

    private lateinit var provider: FakePeppolProvider
    private lateinit var peppolService: PeppolService

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
                ContactsTable,
                InvoicesTable,
                PeppolSettingsTable,
                PeppolTransmissionsTable
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

        settingsRepository = PeppolSettingsRepository()
        transmissionRepository = PeppolTransmissionRepository()

        val providerHttpClient = mockk<io.ktor.client.HttpClient>(relaxed = true)
        val providerFactory = PeppolProviderFactory(providerHttpClient)

        val inboxItem = PeppolInboxItem(
            id = "ext-1",
            documentType = PeppolDocumentType.Invoice,
            senderPeppolId = "0208:BE0000000000",
            receiverPeppolId = "0208:BE0123456789",
            receivedAt = "2024-01-01T00:00:00Z",
            isRead = false
        )
        val receivedDocument = PeppolReceivedDocument(
            id = "ext-1",
            documentType = PeppolDocumentType.Invoice,
            senderPeppolId = "0208:BE0000000000",
            invoiceNumber = "INV-001",
            issueDate = "2024-01-01",
            dueDate = "2024-02-01",
            seller = null,
            buyer = null,
            lineItems = emptyList(),
            totals = PeppolMonetaryTotals(
                lineExtensionAmount = null,
                taxExclusiveAmount = 100.0,
                taxInclusiveAmount = 121.0,
                payableAmount = 121.0
            ),
            taxTotal = PeppolTaxTotal(
                taxAmount = 21.0,
                taxSubtotals = emptyList()
            ),
            note = null,
            currencyCode = "EUR"
        )

        provider = FakePeppolProvider(inboxItem, receivedDocument)
        providerFactory.registerProvider("recommand") { provider }

        val credentialResolver = object : PeppolCredentialResolver {
            override fun isManagedCredentials(): Boolean = true

            override suspend fun resolve(tenantId: TenantId): RecommandCredentials {
                return RecommandCredentials(
                    companyId = "company",
                    apiKey = "key",
                    apiSecret = "secret",
                    peppolId = "0208:BE0123456789",
                    testMode = true
                )
            }
        }

        peppolService = PeppolService(
            settingsRepository = settingsRepository,
            transmissionRepository = transmissionRepository,
            providerFactory = providerFactory,
            mapper = PeppolMapper(),
            validator = PeppolValidator(),
            credentialResolver = credentialResolver
        )

        runBlocking {
            settingsRepository.saveSettings(
                tenantId = tenantId,
                companyId = "company",
                peppolId = "0208:BE0123456789",
                isEnabled = true,
                testMode = true
            ).getOrThrow()
            settingsRepository.updateLastFullSyncAt(tenantId).getOrThrow()
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                PeppolTransmissionsTable,
                PeppolSettingsTable,
                InvoicesTable,
                ContactsTable,
                DocumentsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `pollInbox retries failed transmissions and does not poison imports`() = runBlocking {
        var callbackAttempts = 0

        // First poll: callback fails after transmission is created -> status becomes FAILED, and provider item is NOT marked read.
        peppolService.pollInbox(tenantId) { draftData, _, _, _ ->
            callbackAttempts++
            assertTrue(draftData is InvoiceDraftData)
            Result.failure(IllegalStateException("boom"))
        }.getOrThrow()

        assertEquals(1, callbackAttempts)
        assertEquals(0, provider.markAsReadCount)

        val firstTransmission = transmissionRepository.getByExternalDocumentId(tenantId, "ext-1").getOrThrow()
        assertNotNull(firstTransmission)
        assertEquals(PeppolStatus.Failed, firstTransmission.status)
        assertEquals("boom", firstTransmission.errorMessage)

        // Second poll: retries because status=FAILED, succeeds, marks as DELIVERED and marks provider item as read.
        peppolService.pollInbox(tenantId) { _, _, _, _ ->
            callbackAttempts++
            Result.success(DocumentId.generate())
        }.getOrThrow()

        assertEquals(2, callbackAttempts)
        assertEquals(1, provider.markAsReadCount)

        val deliveredTransmission = transmissionRepository.getByExternalDocumentId(tenantId, "ext-1").getOrThrow()
        assertNotNull(deliveredTransmission)
        assertEquals(PeppolStatus.Delivered, deliveredTransmission.status)

        // Third poll: delivered transmission is skipped (no callback), markAsRead is best-effort.
        peppolService.pollInbox(tenantId) { _, _, _, _ ->
            callbackAttempts++
            Result.success(DocumentId.generate())
        }.getOrThrow()

        assertEquals(2, callbackAttempts)
        assertEquals(2, provider.markAsReadCount)
    }

    @Test
    fun `pollInbox stores decode failure message as retryable transmission error`() = runBlocking {
        peppolService.pollInbox(tenantId) { _, _, _, _ ->
            runCatching {
                decodePeppolAttachmentBase64("%%%not-valid%%%")
                DocumentId.generate()
            }
        }.getOrThrow()

        val transmission = transmissionRepository.getByExternalDocumentId(tenantId, "ext-1").getOrThrow()
        assertNotNull(transmission)
        assertEquals(PeppolStatus.Failed, transmission.status)
        assertTrue(
            transmission.errorMessage?.contains("Invalid PEPPOL attachment base64 payload") == true
        )
    }

    private class FakePeppolProvider(
        private val inboxItem: PeppolInboxItem,
        private val receivedDocument: PeppolReceivedDocument
    ) : PeppolProvider {
        override val providerId: String = "recommand"
        override val providerName: String = "Fake"

        var markAsReadCount: Int = 0

        override fun configure(credentials: PeppolCredentials) = Unit

        override suspend fun sendDocument(request: tech.dokus.peppol.model.PeppolSendRequest): Result<tech.dokus.peppol.model.PeppolSendResponse> =
            Result.failure(NotImplementedError())

        override suspend fun verifyRecipient(peppolId: String): Result<tech.dokus.peppol.model.PeppolVerifyResponse> =
            Result.failure(NotImplementedError())

        override suspend fun getInbox(): Result<List<PeppolInboxItem>> = Result.success(listOf(inboxItem))

        override suspend fun getDocument(documentId: String): Result<PeppolReceivedDocument> =
            Result.success(receivedDocument)

        override suspend fun markAsRead(documentId: String): Result<Unit> {
            markAsReadCount++
            return Result.success(Unit)
        }

        override suspend fun listDocuments(
            direction: PeppolTransmissionDirection?,
            limit: Int,
            offset: Int,
            isUnread: Boolean?
        ): Result<PeppolDocumentList> = Result.success(
            PeppolDocumentList(
                documents = emptyList(),
                total = 0,
                hasMore = false
            )
        )

        override suspend fun testConnection(): Result<Boolean> = Result.success(true)

        override fun serializeRequest(request: tech.dokus.peppol.model.PeppolSendRequest): String = "{}"
    }
}
