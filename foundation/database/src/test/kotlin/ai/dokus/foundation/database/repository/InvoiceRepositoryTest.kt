package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.TestDatabaseFactory
import ai.dokus.foundation.database.TestFixtures
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.InvoiceStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for InvoiceRepository
 * Tests invoice CRUD operations, items management, status updates, and Peppol integration
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvoiceRepositoryTest {
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var clientRepository: ClientRepository

    @BeforeAll
    fun setup() {
        TestDatabaseFactory.init()
        invoiceRepository = InvoiceRepository()
        clientRepository = ClientRepository()
    }

    @BeforeEach
    fun cleanDatabase() {
        TestDatabaseFactory.clean()
    }

    @Test
    fun `create invoice should persist to database`() = runBlocking {
        // Given - Create client first
        val client = TestFixtures.createTestClient()
        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
        }

        val invoice = TestFixtures.createTestInvoice(clientId = client.id)

        // When
        val result = TestDatabaseFactory.dbQuery {
            invoiceRepository.create(invoice)
        }

        // Then
        assertNotNull(result)
        assertEquals(invoice.id, result.id)
        assertEquals(invoice.invoiceNumber, result.invoiceNumber)
        assertEquals(invoice.totalAmount, result.totalAmount)
        assertEquals(InvoiceStatus.Draft, result.status)
    }

    @Test
    fun `findById should return invoice when exists`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(clientId = client.id)

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            invoiceRepository.findById(invoice.id, invoice.tenantId)
        }

        // Then
        assertNotNull(result)
        assertEquals(invoice.id, result.id)
    }

    @Test
    fun `findById should enforce tenant isolation`() = runBlocking {
        // Given - Create invoice for tenant1
        val client = TestFixtures.createTestClient(tenantId = TestFixtures.tenant1Id)
        val invoice = TestFixtures.createTestInvoice(
            tenantId = TestFixtures.tenant1Id,
            clientId = client.id
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
        }

        // When - Try to access with tenant2
        val result = TestDatabaseFactory.dbQuery {
            invoiceRepository.findById(invoice.id, TestFixtures.tenant2Id)
        }

        // Then - Should not find invoice from different tenant
        assertNull(result)
    }

    @Test
    fun `findByTenantId should return all invoices for tenant`() = runBlocking {
        // Given - Create 2 clients and 3 invoices
        val client1 = TestFixtures.createTestClient(
            id = TestFixtures.client1Id,
            tenantId = TestFixtures.tenant1Id
        )
        val client2 = TestFixtures.createTestClient(
            id = TestFixtures.client2Id,
            tenantId = TestFixtures.tenant2Id
        )

        val invoice1 = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice1Id,
            tenantId = TestFixtures.tenant1Id,
            clientId = client1.id,
            invoiceNumber = InvoiceNumber("INV-001")
        )
        val invoice2 = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice2Id,
            tenantId = TestFixtures.tenant1Id,
            clientId = client1.id,
            invoiceNumber = InvoiceNumber("INV-002")
        )
        val invoice3 = TestFixtures.createTestInvoice(
            id = InvoiceId("20000000-0000-0000-0000-000000000003"),
            tenantId = TestFixtures.tenant2Id,
            clientId = client2.id,
            invoiceNumber = InvoiceNumber("INV-003")
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client1)
            clientRepository.create(client2)
            invoiceRepository.create(invoice1)
            invoiceRepository.create(invoice2)
            invoiceRepository.create(invoice3)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            invoiceRepository.findByTenantId(TestFixtures.tenant1Id, limit = 10, offset = 0)
        }

        // Then - Should only return invoices for tenant1
        assertEquals(2, result.size)
        assertTrue(result.all { it.tenantId == TestFixtures.tenant1Id })
    }

    @Test
    fun `findByClientId should return all invoices for client`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        val invoice1 = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice1Id,
            clientId = client.id,
            invoiceNumber = InvoiceNumber("INV-001")
        )
        val invoice2 = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice2Id,
            clientId = client.id,
            invoiceNumber = InvoiceNumber("INV-002")
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice1)
            invoiceRepository.create(invoice2)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            invoiceRepository.findByClientId(client.id, client.tenantId, limit = 10, offset = 0)
        }

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.clientId == client.id })
    }

    @Test
    fun `updateStatus should change invoice status`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(
            clientId = client.id,
            status = InvoiceStatus.Draft
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
        }

        // When
        TestDatabaseFactory.dbQuery {
            invoiceRepository.updateStatus(invoice.id, invoice.tenantId, InvoiceStatus.Sent)
        }

        // Then
        val result = TestDatabaseFactory.dbQuery {
            invoiceRepository.findById(invoice.id, invoice.tenantId)
        }
        assertEquals(InvoiceStatus.Sent, result?.status)
    }

    @Test
    fun `recordPayment should update paid amount and status`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(
            clientId = client.id,
            totalAmount = Money("1000.00"),
            paidAmount = Money.ZERO,
            status = InvoiceStatus.Sent
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
        }

        // When - Record partial payment
        TestDatabaseFactory.dbQuery {
            invoiceRepository.recordPayment(
                invoiceId = invoice.id,
                tenantId = invoice.tenantId,
                amount = Money("500.00")
            )
        }

        // Then - Should be partially paid
        val result1 = TestDatabaseFactory.dbQuery {
            invoiceRepository.findById(invoice.id, invoice.tenantId)
        }
        assertEquals(Money("500.00"), result1?.paidAmount)
        assertEquals(InvoiceStatus.PartiallyPaid, result1?.status)

        // When - Record remaining payment
        TestDatabaseFactory.dbQuery {
            invoiceRepository.recordPayment(
                invoiceId = invoice.id,
                tenantId = invoice.tenantId,
                amount = Money("500.00")
            )
        }

        // Then - Should be fully paid
        val result2 = TestDatabaseFactory.dbQuery {
            invoiceRepository.findById(invoice.id, invoice.tenantId)
        }
        assertEquals(Money("1000.00"), result2?.paidAmount)
        assertEquals(InvoiceStatus.Paid, result2?.status)
    }

    @Test
    fun `findByStatus should filter invoices by status`() = runBlocking {
        // Given - Create invoices with different statuses
        val client = TestFixtures.createTestClient()
        val draftInvoice = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice1Id,
            clientId = client.id,
            invoiceNumber = InvoiceNumber("INV-001"),
            status = InvoiceStatus.Draft
        )
        val sentInvoice = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice2Id,
            clientId = client.id,
            invoiceNumber = InvoiceNumber("INV-002"),
            status = InvoiceStatus.Sent
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(draftInvoice)
            invoiceRepository.create(sentInvoice)
        }

        // When - Find draft invoices
        val drafts = TestDatabaseFactory.dbQuery {
            invoiceRepository.findByStatus(TestFixtures.tenant1Id, InvoiceStatus.Draft, limit = 10)
        }

        // Then
        assertEquals(1, drafts.size)
        assertEquals(InvoiceStatus.Draft, drafts.first().status)
    }

    @Test
    fun `findOverdueInvoices should return invoices past due date`() = runBlocking {
        // Given - Create overdue and current invoices
        val client = TestFixtures.createTestClient()
        val overdueInvoice = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice1Id,
            clientId = client.id,
            invoiceNumber = InvoiceNumber("INV-001"),
            dueDate = TestFixtures.yesterday,
            status = InvoiceStatus.Sent
        )
        val currentInvoice = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice2Id,
            clientId = client.id,
            invoiceNumber = InvoiceNumber("INV-002"),
            dueDate = TestFixtures.tomorrow,
            status = InvoiceStatus.Sent
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(overdueInvoice)
            invoiceRepository.create(currentInvoice)
        }

        // When
        val overdueInvoices = TestDatabaseFactory.dbQuery {
            invoiceRepository.findOverdueInvoices(TestFixtures.tenant1Id, limit = 10)
        }

        // Then - Should only return overdue invoice
        assertEquals(1, overdueInvoices.size)
        assertEquals(overdueInvoice.id, overdueInvoices.first().id)
    }

    @Test
    fun `calculateTotals should sum invoice amounts correctly`() = runBlocking {
        // Given - Create multiple invoices
        val client = TestFixtures.createTestClient()
        val invoice1 = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice1Id,
            clientId = client.id,
            invoiceNumber = InvoiceNumber("INV-001"),
            subtotalAmount = Money("1000.00"),
            vatAmount = Money("210.00"),
            totalAmount = Money("1210.00")
        )
        val invoice2 = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice2Id,
            clientId = client.id,
            invoiceNumber = InvoiceNumber("INV-002"),
            subtotalAmount = Money("2000.00"),
            vatAmount = Money("420.00"),
            totalAmount = Money("2420.00")
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice1)
            invoiceRepository.create(invoice2)
        }

        // When
        val totals = TestDatabaseFactory.dbQuery {
            invoiceRepository.calculateTotals(TestFixtures.tenant1Id)
        }

        // Then
        assertEquals(Money("3000.00"), totals.subtotal)
        assertEquals(Money("630.00"), totals.vatAmount)
        assertEquals(Money("3630.00"), totals.total)
    }

    @Test
    fun `update Peppol status should persist Peppol transmission info`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(clientId = client.id)

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
        }

        // When
        val peppolId = PeppolId("MSG-PEPPOL-12345")
        TestDatabaseFactory.dbQuery {
            invoiceRepository.updatePeppolStatus(
                invoiceId = invoice.id,
                tenantId = invoice.tenantId,
                peppolId = peppolId,
                peppolStatus = PeppolStatus.Sent
            )
        }

        // Then
        val result = TestDatabaseFactory.dbQuery {
            invoiceRepository.findById(invoice.id, invoice.tenantId)
        }
        assertEquals(peppolId, result?.peppolId)
        assertEquals(PeppolStatus.Sent, result?.peppolStatus)
        assertNotNull(result?.peppolSentAt)
    }
}
