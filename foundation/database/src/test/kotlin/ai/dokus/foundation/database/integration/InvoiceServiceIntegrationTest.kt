package ai.dokus.foundation.database.integration

import ai.dokus.foundation.database.TestDatabaseFactory
import ai.dokus.foundation.database.services.AuditServiceImpl
import ai.dokus.foundation.database.services.InvoiceServiceImpl
import ai.dokus.foundation.database.services.TenantServiceImpl
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.InvoiceItem
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.junit.jupiter.api.*
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Integration tests for InvoiceService
 * Tests critical flows end-to-end
 *
 * Note: These tests focus on the service layer integration.
 * Full domain model requires complex value class setup.
 */
@OptIn(ExperimentalUuidApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvoiceServiceIntegrationTest {
    private lateinit var invoiceService: InvoiceServiceImpl
    private lateinit var tenantService: TenantServiceImpl
    private lateinit var auditService: AuditServiceImpl

    @BeforeAll
    fun setup() {
        TestDatabaseFactory.init()
        auditService = AuditServiceImpl()
        tenantService = TenantServiceImpl()
        invoiceService = InvoiceServiceImpl(tenantService, auditService)
    }

    @BeforeEach
    fun cleanDatabase() {
        TestDatabaseFactory.clean()
    }

    @AfterAll
    fun teardown() {
        TestDatabaseFactory.close()
    }

    /**
     * Helper to create an invoice item with calculated fields
     */
    private fun createInvoiceItem(
        description: String,
        quantity: String,
        unitPrice: String,
        vatRatePercent: String
    ): InvoiceItem {
        val qty = BigDecimal(quantity)
        val price = BigDecimal(unitPrice)
        val vatRate = BigDecimal(vatRatePercent)
        
        val lineTotal = qty * price
        val vatAmount = lineTotal * vatRate / BigDecimal("100")
        
        return InvoiceItem(
            description = description,
            quantity = Quantity(quantity),
            unitPrice = Money(unitPrice),
            vatRate = VatRate(vatRatePercent),
            lineTotal = Money(lineTotal.toString()),
            vatAmount = Money(vatAmount.toString())
        )
    }

    @Test
    fun `create invoice and generate PDF`() = runBlocking {
        // Given - Create tenant
        val tenant = tenantService.createTenant(
            name = "Test Company",
            email = "test@example.com"
        )

        val today = Clock.System.todayIn(TimeZone.UTC)

        // When - Create invoice
        val items = listOf(
            createInvoiceItem("Consulting Services", "40", "100.00", "21"),
            createInvoiceItem("Travel Expenses", "1", "250.00", "21")
        )

        val createRequest = CreateInvoiceRequest(
            tenantId = tenant.id,
            clientId = ClientId(Uuid.random()),
            issueDate = today,
            dueDate = null,
            items = items,
            notes = "Test invoice"
        )

        val invoice = invoiceService.create(createRequest)

        // Then - Verify invoice created
        assertNotNull(invoice)
        assertEquals(2, invoice.items.size)

        // When - Generate PDF
        val pdfBytes = invoiceService.generatePDF(invoice.id)

        // Then - Verify PDF is valid
        assertNotNull(pdfBytes)
        assertTrue(pdfBytes.isNotEmpty())
        
        // Verify PDF signature
        val pdfSignature = "%PDF"
        val fileHeader = String(pdfBytes.take(4).toByteArray())
        assertEquals(pdfSignature, fileHeader, "Generated file should be a valid PDF")
        
        println("✓ Invoice created and PDF generated successfully (${pdfBytes.size} bytes)")
    }

    @Test
    fun `list invoices by tenant works`() = runBlocking {
        // Given
        val tenant = tenantService.createTenant(
            name = "Test Company",
            email = "list@example.com"
        )

        // Create an invoice
        val item = createInvoiceItem("Test Service", "1", "100.00", "21")
        
        invoiceService.create(
            CreateInvoiceRequest(
                tenantId = tenant.id,
                clientId = ClientId(Uuid.random()),
                issueDate = Clock.System.todayIn(TimeZone.UTC),
                dueDate = null,
                items = listOf(item),
                notes = null
            )
        )

        // When - List invoices
        val invoices = invoiceService.listByTenant(tenant.id)

        // Then
        assertEquals(1, invoices.size)
        assertEquals(tenant.id, invoices.first().tenantId)
        
        println("✓ List invoices by tenant works")
    }

    @Test
    fun `find invoice by id works`() = runBlocking {
        // Given
        val tenant = tenantService.createTenant(
            name = "Test Company",
            email = "find@example.com"
        )

        val item = createInvoiceItem("Test Service", "1", "500.00", "21")
        
        val invoice = invoiceService.create(
            CreateInvoiceRequest(
                tenantId = tenant.id,
                clientId = ClientId(Uuid.random()),
                issueDate = Clock.System.todayIn(TimeZone.UTC),
                dueDate = null,
                items = listOf(item),
                notes = null
            )
        )

        // When - Find by ID
        val found = invoiceService.findById(invoice.id)

        // Then
        assertNotNull(found)
        assertEquals(invoice.id, found.id)
        assertEquals("500.00", found.subtotalAmount.value)
        
        println("✓ Find invoice by ID works")
    }
}
