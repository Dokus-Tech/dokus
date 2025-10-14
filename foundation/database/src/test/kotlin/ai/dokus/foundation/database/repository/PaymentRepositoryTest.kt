package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.TestDatabaseFactory
import ai.dokus.foundation.database.TestFixtures
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.enums.PaymentMethod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for PaymentRepository
 * Tests payment CRUD operations, invoice linking, and payment aggregations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PaymentRepositoryTest {
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var clientRepository: ClientRepository

    @BeforeAll
    fun setup() {
        TestDatabaseFactory.init()
        paymentRepository = PaymentRepository()
        invoiceRepository = InvoiceRepository()
        clientRepository = ClientRepository()
    }

    @BeforeEach
    fun cleanDatabase() {
        TestDatabaseFactory.clean()
    }

    @Test
    fun `create payment should persist to database`() = runBlocking {
        // Given - Create client and invoice first
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(clientId = client.id)
        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
        }

        val payment = TestFixtures.createTestPayment(invoiceId = invoice.id)

        // When
        val result = TestDatabaseFactory.dbQuery {
            paymentRepository.create(payment)
        }

        // Then
        assertNotNull(result)
        assertEquals(payment.id, result.id)
        assertEquals(payment.invoiceId, result.invoiceId)
        assertEquals(payment.amount, result.amount)
        assertEquals(payment.paymentMethod, result.paymentMethod)
    }

    @Test
    fun `findById should return payment when exists`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(clientId = client.id)
        val payment = TestFixtures.createTestPayment(invoiceId = invoice.id)

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
            paymentRepository.create(payment)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            paymentRepository.findById(payment.id, payment.tenantId)
        }

        // Then
        assertNotNull(result)
        assertEquals(payment.id, result.id)
    }

    @Test
    fun `findById should enforce tenant isolation`() = runBlocking {
        // Given - Create payment for tenant1
        val client = TestFixtures.createTestClient(tenantId = TestFixtures.tenant1Id)
        val invoice = TestFixtures.createTestInvoice(
            tenantId = TestFixtures.tenant1Id,
            clientId = client.id
        )
        val payment = TestFixtures.createTestPayment(
            tenantId = TestFixtures.tenant1Id,
            invoiceId = invoice.id
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
            paymentRepository.create(payment)
        }

        // When - Try to access with tenant2
        val result = TestDatabaseFactory.dbQuery {
            paymentRepository.findById(payment.id, TestFixtures.tenant2Id)
        }

        // Then - Should not find payment from different tenant
        assertNull(result)
    }

    @Test
    fun `findByInvoiceId should return all payments for invoice`() = runBlocking {
        // Given - Create invoice with 2 payments
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(
            clientId = client.id,
            totalAmount = Money("1000.00")
        )

        val payment1 = TestFixtures.createTestPayment(
            id = TestFixtures.payment1Id,
            invoiceId = invoice.id,
            amount = Money("500.00")
        )
        val payment2 = TestFixtures.createTestPayment(
            id = TestFixtures.payment2Id,
            invoiceId = invoice.id,
            amount = Money("500.00")
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
            paymentRepository.create(payment1)
            paymentRepository.create(payment2)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            paymentRepository.findByInvoiceId(invoice.id, invoice.tenantId)
        }

        // Then
        assertEquals(2, result.size)
        assertEquals(Money("1000.00"), result.sumOf { it.amount.value.toBigDecimal() }.let { Money(it.toString()) })
    }

    @Test
    fun `findByTenantId should return all payments for tenant`() = runBlocking {
        // Given - Create 2 payments for tenant1 and 1 for tenant2
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
            clientId = client1.id
        )
        val invoice2 = TestFixtures.createTestInvoice(
            id = TestFixtures.invoice2Id,
            tenantId = TestFixtures.tenant2Id,
            clientId = client2.id
        )

        val payment1 = TestFixtures.createTestPayment(
            id = TestFixtures.payment1Id,
            tenantId = TestFixtures.tenant1Id,
            invoiceId = invoice1.id
        )
        val payment2 = TestFixtures.createTestPayment(
            id = TestFixtures.payment2Id,
            tenantId = TestFixtures.tenant1Id,
            invoiceId = invoice1.id
        )
        val payment3 = TestFixtures.createTestPayment(
            id = PaymentId("40000000-0000-0000-0000-000000000003"),
            tenantId = TestFixtures.tenant2Id,
            invoiceId = invoice2.id
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client1)
            clientRepository.create(client2)
            invoiceRepository.create(invoice1)
            invoiceRepository.create(invoice2)
            paymentRepository.create(payment1)
            paymentRepository.create(payment2)
            paymentRepository.create(payment3)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            paymentRepository.findByTenantId(TestFixtures.tenant1Id, limit = 10, offset = 0)
        }

        // Then - Should only return payments for tenant1
        assertEquals(2, result.size)
    }

    @Test
    fun `findByDateRange should filter payments by date`() = runBlocking {
        // Given - Create payments on different dates
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(clientId = client.id)

        val lastWeek = TestFixtures.today.minus(7, DateTimeUnit.DAY)

        val oldPayment = TestFixtures.createTestPayment(
            id = TestFixtures.payment1Id,
            invoiceId = invoice.id,
            paymentDate = lastWeek
        )
        val recentPayment = TestFixtures.createTestPayment(
            id = TestFixtures.payment2Id,
            invoiceId = invoice.id,
            paymentDate = TestFixtures.today
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
            paymentRepository.create(oldPayment)
            paymentRepository.create(recentPayment)
        }

        // When - Find payments from yesterday to tomorrow
        val result = TestDatabaseFactory.dbQuery {
            paymentRepository.findByDateRange(
                TestFixtures.tenant1Id,
                TestFixtures.yesterday,
                TestFixtures.tomorrow,
                limit = 10
            )
        }

        // Then - Should only return recent payment
        assertEquals(1, result.size)
        assertEquals(recentPayment.id, result.first().id)
    }

    @Test
    fun `calculateTotalByMethod should sum amounts per payment method`() = runBlocking {
        // Given - Create payments with different methods
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(clientId = client.id)

        val bankTransfer1 = TestFixtures.createTestPayment(
            id = TestFixtures.payment1Id,
            invoiceId = invoice.id,
            amount = Money("100.00"),
            paymentMethod = PaymentMethod.BankTransfer
        )
        val bankTransfer2 = TestFixtures.createTestPayment(
            id = TestFixtures.payment2Id,
            invoiceId = invoice.id,
            amount = Money("150.00"),
            paymentMethod = PaymentMethod.BankTransfer
        )
        val creditCard = TestFixtures.createTestPayment(
            id = PaymentId("40000000-0000-0000-0000-000000000003"),
            invoiceId = invoice.id,
            amount = Money("200.00"),
            paymentMethod = PaymentMethod.CreditCard
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
            paymentRepository.create(bankTransfer1)
            paymentRepository.create(bankTransfer2)
            paymentRepository.create(creditCard)
        }

        // When
        val result = TestDatabaseFactory.dbQuery {
            paymentRepository.calculateTotalByMethod(TestFixtures.tenant1Id, PaymentMethod.BankTransfer)
        }

        // Then - Should be 100 + 150 = 250.00
        assertEquals(Money("250.00"), result)
    }

    @Test
    fun `delete should remove payment from database`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(clientId = client.id)
        val payment = TestFixtures.createTestPayment(invoiceId = invoice.id)

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice)
            paymentRepository.create(payment)
        }

        // When
        TestDatabaseFactory.dbQuery {
            paymentRepository.delete(payment.id, payment.tenantId)
        }

        // Then - Payment should not be found
        val result = TestDatabaseFactory.dbQuery {
            paymentRepository.findById(payment.id, payment.tenantId)
        }
        assertNull(result)
    }

    @Test
    fun `count should return correct number of payments`() = runBlocking {
        // Given
        val client = TestFixtures.createTestClient()
        val invoice = TestFixtures.createTestInvoice(clientId = client.id)

        repeat(3) { index ->
            val payment = TestFixtures.createTestPayment(
                id = PaymentId("40000000-0000-0000-0000-00000000000$index"),
                invoiceId = invoice.id
            )
            TestDatabaseFactory.dbQuery {
                if (index == 0) {
                    clientRepository.create(client)
                    invoiceRepository.create(invoice)
                }
                paymentRepository.create(payment)
            }
        }

        // When
        val count = TestDatabaseFactory.dbQuery {
            paymentRepository.count(TestFixtures.tenant1Id)
        }

        // Then
        assertEquals(3, count)
    }

    @Test
    fun `payments should link to correct invoice`() = runBlocking {
        // Given - Create 2 invoices with separate payments
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

        val payment1 = TestFixtures.createTestPayment(
            id = TestFixtures.payment1Id,
            invoiceId = invoice1.id
        )
        val payment2 = TestFixtures.createTestPayment(
            id = TestFixtures.payment2Id,
            invoiceId = invoice2.id
        )

        TestDatabaseFactory.dbQuery {
            clientRepository.create(client)
            invoiceRepository.create(invoice1)
            invoiceRepository.create(invoice2)
            paymentRepository.create(payment1)
            paymentRepository.create(payment2)
        }

        // When
        val invoice1Payments = TestDatabaseFactory.dbQuery {
            paymentRepository.findByInvoiceId(invoice1.id, invoice1.tenantId)
        }
        val invoice2Payments = TestDatabaseFactory.dbQuery {
            paymentRepository.findByInvoiceId(invoice2.id, invoice2.tenantId)
        }

        // Then
        assertEquals(1, invoice1Payments.size)
        assertEquals(1, invoice2Payments.size)
        assertEquals(payment1.id, invoice1Payments.first().id)
        assertEquals(payment2.id, invoice2Payments.first().id)
    }
}
