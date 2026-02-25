package tech.dokus.backend.peppol

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.peppol.PeppolTransmissionRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.cashflow.InvoicesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.database.tables.peppol.PeppolTransmissionsTable
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.TenantId
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class PeppolTransmissionRepositoryIdempotencyTest {

    private lateinit var database: Database
    private lateinit var repository: PeppolTransmissionRepository

    private var tenantA: TenantId = TenantId("00000000-0000-0000-0000-000000000001")
    private var invoiceA: InvoiceId = InvoiceId.parse("00000000-0000-0000-0000-000000000011")

    private var tenantB: TenantId = TenantId("00000000-0000-0000-0000-000000000002")
    private var invoiceB: InvoiceId = InvoiceId.parse("00000000-0000-0000-0000-000000000022")

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_peppol_idempotency_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                ContactsTable,
                DocumentsTable,
                InvoicesTable,
                PeppolTransmissionsTable
            )
        }

        val tenantAUuid = UUID.randomUUID()
        val tenantBUuid = UUID.randomUUID()
        val contactAUuid = UUID.randomUUID()
        val contactBUuid = UUID.randomUUID()
        val invoiceAUuid = UUID.randomUUID()
        val invoiceBUuid = UUID.randomUUID()

        tenantA = TenantId(tenantAUuid.toKotlinUuid())
        tenantB = TenantId(tenantBUuid.toKotlinUuid())
        invoiceA = InvoiceId(invoiceAUuid.toKotlinUuid())
        invoiceB = InvoiceId(invoiceBUuid.toKotlinUuid())

        transaction(database) {
            insertTenant(tenantAUuid, "Tenant A", "BE0123456789")
            insertTenant(tenantBUuid, "Tenant B", "BE9876543210")

            insertContact(contactAUuid, tenantAUuid, "Customer A")
            insertContact(contactBUuid, tenantBUuid, "Customer B")

            insertInvoice(invoiceAUuid, tenantAUuid, contactAUuid, "INV-A-001")
            insertInvoice(invoiceBUuid, tenantBUuid, contactBUuid, "INV-B-001")
        }

        repository = PeppolTransmissionRepository()
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                PeppolTransmissionsTable,
                InvoicesTable,
                DocumentsTable,
                ContactsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `concurrent upsert with same tenant and idempotency key yields one transmission`() = runBlocking {
        val idempotencyKey = "tenant-a-key"

        val ids = coroutineScope {
            (1..10).map {
                async(Dispatchers.Default) {
                    repository.upsertOutboundQueued(
                        tenantId = tenantA,
                        documentType = PeppolDocumentType.Invoice,
                        invoiceId = invoiceA,
                        recipientPeppolId = PeppolId("0208:BE0000000001"),
                        idempotencyKey = idempotencyKey,
                        rawRequest = "{\"invoice\":\"A\"}"
                    ).getOrThrow().id
                }
            }.awaitAll()
        }

        assertEquals(1, ids.distinct().size)

        val transmissions = repository.listTransmissions(
            tenantId = tenantA,
            direction = PeppolTransmissionDirection.Outbound,
            status = null,
            limit = 10,
            offset = 0
        ).getOrThrow()

        assertEquals(1, transmissions.size)
        assertEquals(PeppolStatus.Queued, transmissions.single().status)
    }

    @Test
    fun `same idempotency key across tenants creates isolated rows`() = runBlocking {
        val sharedKey = "shared-key"

        val first = repository.upsertOutboundQueued(
            tenantId = tenantA,
            documentType = PeppolDocumentType.Invoice,
            invoiceId = invoiceA,
            recipientPeppolId = PeppolId("0208:BE0000000001"),
            idempotencyKey = sharedKey,
            rawRequest = "{\"invoice\":\"A\"}"
        ).getOrThrow()

        val second = repository.upsertOutboundQueued(
            tenantId = tenantB,
            documentType = PeppolDocumentType.Invoice,
            invoiceId = invoiceB,
            recipientPeppolId = PeppolId("0208:BE0000000002"),
            idempotencyKey = sharedKey,
            rawRequest = "{\"invoice\":\"B\"}"
        ).getOrThrow()

        assertNotEquals(first.id, second.id)
        assertEquals(1, repository.listTransmissions(tenantA, PeppolTransmissionDirection.Outbound).getOrThrow().size)
        assertEquals(1, repository.listTransmissions(tenantB, PeppolTransmissionDirection.Outbound).getOrThrow().size)
    }

    @Test
    fun `re-enqueue with same key returns existing transmission without duplicates`() = runBlocking {
        val idempotencyKey = "stable-key"

        val first = repository.upsertOutboundQueued(
            tenantId = tenantA,
            documentType = PeppolDocumentType.Invoice,
            invoiceId = invoiceA,
            recipientPeppolId = PeppolId("0208:BE0000000001"),
            idempotencyKey = idempotencyKey,
            rawRequest = "{\"invoice\":\"A-v1\"}"
        ).getOrThrow()

        val second = repository.upsertOutboundQueued(
            tenantId = tenantA,
            documentType = PeppolDocumentType.Invoice,
            invoiceId = invoiceA,
            recipientPeppolId = PeppolId("0208:BE0000000001"),
            idempotencyKey = idempotencyKey,
            rawRequest = "{\"invoice\":\"A-v2\"}"
        ).getOrThrow()

        assertEquals(first.id, second.id)
        assertEquals(PeppolStatus.Queued, second.status)

        val transmissions = repository.listTransmissions(tenantA, PeppolTransmissionDirection.Outbound).getOrThrow()
        assertEquals(1, transmissions.size)
    }

    private fun insertTenant(id: UUID, name: String, vatNumber: String) {
        TenantTable.insert {
            it[TenantTable.id] = id
            it[TenantTable.type] = TenantType.Company
            it[TenantTable.legalName] = name
            it[TenantTable.displayName] = name
            it[TenantTable.plan] = SubscriptionTier.Core
            it[TenantTable.status] = TenantStatus.Active
            it[TenantTable.language] = Language.En
            it[TenantTable.vatNumber] = vatNumber
        }
    }

    private fun insertContact(id: UUID, tenantId: UUID, name: String) {
        ContactsTable.insert {
            it[ContactsTable.id] = id
            it[ContactsTable.tenantId] = tenantId
            it[ContactsTable.name] = name
            it[ContactsTable.businessType] = ClientType.Business
            it[ContactsTable.defaultPaymentTerms] = 30
            it[ContactsTable.isActive] = true
        }
    }

    private fun insertInvoice(id: UUID, tenantId: UUID, contactId: UUID, invoiceNumber: String) {
        InvoicesTable.insert {
            it[InvoicesTable.id] = id
            it[InvoicesTable.tenantId] = tenantId
            it[InvoicesTable.contactId] = contactId
            it[InvoicesTable.invoiceNumber] = invoiceNumber
            it[InvoicesTable.issueDate] = LocalDate(2024, 1, 1)
            it[InvoicesTable.dueDate] = LocalDate(2024, 1, 31)
            it[InvoicesTable.subtotalAmount] = BigDecimal("100.00")
            it[InvoicesTable.vatAmount] = BigDecimal("21.00")
            it[InvoicesTable.totalAmount] = BigDecimal("121.00")
            it[InvoicesTable.paidAmount] = BigDecimal.ZERO
            it[InvoicesTable.status] = InvoiceStatus.Sent
            it[InvoicesTable.direction] = DocumentDirection.Outbound
            it[InvoicesTable.currency] = Currency.Eur
        }
    }
}
