package tech.dokus.backend.cashflow

import ai.dokus.foundation.database.repository.cashflow.InvoiceNumberRepository
import ai.dokus.foundation.database.services.InvoiceNumberGenerator
import ai.dokus.foundation.database.tables.auth.TenantSettingsTable
import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.database.tables.cashflow.InvoiceNumberSequencesTable
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantStatus
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.TenantId
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Year
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Integration tests for yearly invoice number rollover.
 *
 * These tests verify that the invoice number generation system correctly handles
 * year boundaries and the yearlyReset configuration option. This is critical for
 * Belgian tax law compliance which requires proper yearly invoice number management.
 *
 * Tests verify:
 * 1. Sequence resets to 1 at year boundary when yearlyReset=true
 * 2. Sequence continues across years when yearlyReset=false
 * 3. Invoice numbers include the correct year in the format
 * 4. Independent sequences are maintained for each year
 * 5. Timezone handling for year calculation
 */
@OptIn(ExperimentalUuidApi::class)
class InvoiceNumberYearRolloverTest {

    private lateinit var database: Database
    private lateinit var invoiceNumberRepository: InvoiceNumberRepository
    private lateinit var invoiceNumberGenerator: InvoiceNumberGenerator
    private var testTenantId: TenantId? = null
    private lateinit var testTenantUuid: UUID

    @BeforeEach
    fun setup() {
        // Create H2 in-memory database with PostgreSQL compatibility mode
        database = Database.connect(
            url = "jdbc:h2:mem:test_rollover_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        // Create tables in a transaction
        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                TenantSettingsTable,
                InvoiceNumberSequencesTable
            )
        }

        // Create test tenant
        testTenantUuid = UUID.randomUUID()
        testTenantId = TenantId(testTenantUuid.toKotlinUuid())

        transaction(database) {
            // Insert tenant
            TenantTable.insert {
                it[id] = testTenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Yearly Rollover Test Company"
                it[displayName] = "Yearly Rollover Test Company"
                it[plan] = TenantPlan.Professional
                it[status] = TenantStatus.Active
                it[language] = Language.En
            }

            // Insert tenant settings with invoice configuration (yearlyReset=true by default)
            TenantSettingsTable.insert {
                it[id] = UUID.randomUUID()
                it[tenantId] = testTenantUuid
                it[invoicePrefix] = "INV"
                it[nextInvoiceNumber] = 1
                it[defaultPaymentTerms] = 30
                it[defaultVatRate] = BigDecimal("21.00")
                it[invoiceYearlyReset] = true
                it[invoicePadding] = 4
                it[invoiceIncludeYear] = true
                it[invoiceTimezone] = "Europe/Brussels"
            }
        }

        // Initialize repository and generator
        invoiceNumberRepository = InvoiceNumberRepository()
        invoiceNumberGenerator = InvoiceNumberGenerator(invoiceNumberRepository)
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                InvoiceNumberSequencesTable,
                TenantSettingsTable,
                TenantTable
            )
        }
    }

    /**
     * Test that sequence resets to 1 when year changes (yearlyReset=true).
     * This is the primary yearly rollover test for Belgian tax law compliance.
     */
    @Test
    fun `sequence resets to 1 at year boundary when yearlyReset is enabled`() = runBlocking {
        // Create invoices for year 2024
        val year2024Invoice1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()
        val year2024Invoice2 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()
        val year2024Invoice3 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()

        // Verify 2024 sequence is 1, 2, 3
        assertEquals(1, year2024Invoice1, "First invoice in 2024 should be 1")
        assertEquals(2, year2024Invoice2, "Second invoice in 2024 should be 2")
        assertEquals(3, year2024Invoice3, "Third invoice in 2024 should be 3")

        // Simulate year change to 2025 - create invoices for new year
        val year2025Invoice1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2025).getOrThrow()
        val year2025Invoice2 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2025).getOrThrow()

        // Verify 2025 sequence resets to 1, 2
        assertEquals(1, year2025Invoice1, "First invoice in 2025 should reset to 1")
        assertEquals(2, year2025Invoice2, "Second invoice in 2025 should be 2")

        // Verify 2024 sequence still works independently
        val year2024Invoice4 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()
        assertEquals(4, year2024Invoice4, "Fourth invoice in 2024 should continue at 4")
    }

    /**
     * Test that sequence continues across years when yearlyReset=false.
     * Uses year 0 as a global sequence that doesn't reset.
     */
    @Test
    fun `sequence continues across years when yearlyReset is disabled`() = runBlocking {
        // When yearlyReset=false, we use year 0 as a global sequence
        val globalSequenceYear = 0

        // Create invoices using global sequence (simulating yearlyReset=false)
        val invoice1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalSequenceYear).getOrThrow()
        val invoice2 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalSequenceYear).getOrThrow()
        val invoice3 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalSequenceYear).getOrThrow()
        val invoice4 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalSequenceYear).getOrThrow()

        // All invoices should be sequential regardless of calendar year
        assertEquals(1, invoice1, "First global invoice should be 1")
        assertEquals(2, invoice2, "Second global invoice should be 2")
        assertEquals(3, invoice3, "Third global invoice should be 3")
        assertEquals(4, invoice4, "Fourth global invoice should be 4")

        // Continue to verify the global sequence doesn't reset
        val invoice5 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalSequenceYear).getOrThrow()
        assertEquals(5, invoice5, "Fifth global invoice should continue at 5")
    }

    /**
     * Test that formatted invoice numbers include the correct year.
     */
    @Test
    fun `formatted invoice numbers include correct year`() {
        // Test format with year 2024
        val formatted2024 = invoiceNumberGenerator.formatInvoiceNumber(
            prefix = "INV",
            year = 2024,
            sequence = 1,
            padding = 4,
            includeYear = true
        )
        assertEquals("INV-2024-0001", formatted2024, "2024 invoice should be formatted as INV-2024-0001")

        // Test format with year 2025
        val formatted2025 = invoiceNumberGenerator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 1,
            padding = 4,
            includeYear = true
        )
        assertEquals("INV-2025-0001", formatted2025, "2025 invoice should be formatted as INV-2025-0001")

        // Test that different sequence numbers are formatted correctly
        val formatted2024Seq5 = invoiceNumberGenerator.formatInvoiceNumber(
            prefix = "INV",
            year = 2024,
            sequence = 5,
            padding = 4,
            includeYear = true
        )
        assertEquals("INV-2024-0005", formatted2024Seq5, "2024 invoice 5 should be formatted as INV-2024-0005")
    }

    /**
     * Test that format without year doesn't include year component.
     */
    @Test
    fun `formatted invoice numbers without year do not include year component`() {
        val formattedNoYear = invoiceNumberGenerator.formatInvoiceNumber(
            prefix = "INV",
            year = 2024,
            sequence = 42,
            padding = 4,
            includeYear = false
        )
        assertEquals("INV-0042", formattedNoYear, "Invoice without year should be formatted as INV-0042")
    }

    /**
     * Test that independent sequences are maintained for each year.
     * Multiple years should have their own independent numbering.
     */
    @Test
    fun `independent sequences are maintained for each year`() = runBlocking {
        val years = listOf(2022, 2023, 2024, 2025, 2026)

        // Create 5 invoices for each year
        val invoicesByYear = years.associateWith { year ->
            (1..5).map {
                invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, year).getOrThrow()
            }
        }

        // Verify each year has independent sequence 1-5
        years.forEach { year ->
            val invoices = invoicesByYear[year]!!
            assertEquals(
                listOf(1, 2, 3, 4, 5),
                invoices,
                "Year $year should have independent sequence 1-5"
            )
        }
    }

    /**
     * Test timezone handling for year calculation.
     * Verifies that the getCurrentYear function returns correct year for different timezones.
     */
    @Test
    fun `getCurrentYear returns correct year for timezone`() {
        // Test with Europe/Brussels timezone
        val brusselsYear = invoiceNumberGenerator.getCurrentYear("Europe/Brussels")
        val currentYear = Year.now().value

        // Should return current year (may differ by 1 at year boundary depending on timezone)
        assertTrue(
            brusselsYear in (currentYear - 1)..(currentYear + 1),
            "Brussels year should be near current year"
        )

        // Test with UTC timezone
        val utcYear = invoiceNumberGenerator.getCurrentYear("UTC")
        assertTrue(
            utcYear in (currentYear - 1)..(currentYear + 1),
            "UTC year should be near current year"
        )
    }

    /**
     * Test that invalid timezone falls back to Europe/Brussels.
     */
    @Test
    fun `invalid timezone falls back to Europe Brussels`() {
        // Test with invalid timezone - should fall back to Europe/Brussels
        val fallbackYear = invoiceNumberGenerator.getCurrentYear("Invalid/Timezone")
        val brusselsYear = invoiceNumberGenerator.getCurrentYear("Europe/Brussels")

        assertEquals(brusselsYear, fallbackYear, "Invalid timezone should fall back to Europe/Brussels year")
    }

    /**
     * Test the complete invoice number generation flow with yearly reset enabled.
     * Simulates generating invoice numbers across year boundaries.
     */
    @Test
    fun `complete flow with yearly reset generates correct numbers across years`() = runBlocking {
        // Generate invoices for 2024
        val num2024_1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()
        val num2024_2 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()

        val formatted2024_1 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2024, num2024_1, 4, true)
        val formatted2024_2 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2024, num2024_2, 4, true)

        assertEquals("INV-2024-0001", formatted2024_1)
        assertEquals("INV-2024-0002", formatted2024_2)

        // Year changes to 2025 - sequence resets
        val num2025_1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2025).getOrThrow()
        val num2025_2 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2025).getOrThrow()

        val formatted2025_1 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2025, num2025_1, 4, true)
        val formatted2025_2 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2025, num2025_2, 4, true)

        assertEquals("INV-2025-0001", formatted2025_1, "First 2025 invoice should reset to 0001")
        assertEquals("INV-2025-0002", formatted2025_2, "Second 2025 invoice should be 0002")
    }

    /**
     * Test the complete flow with yearly reset disabled.
     * Numbers should continue regardless of year.
     */
    @Test
    fun `complete flow without yearly reset continues sequence across years`() = runBlocking {
        // When yearlyReset=false, use year 0 for global sequence
        val globalYear = 0

        // Generate invoices
        val num1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalYear).getOrThrow()
        val num2 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalYear).getOrThrow()
        val num3 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalYear).getOrThrow()

        // Format with different years (but sequence continues)
        val formatted2024 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2024, num1, 4, true)
        val formatted2025_1 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2025, num2, 4, true)
        val formatted2025_2 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2025, num3, 4, true)

        // Sequence numbers continue (1, 2, 3) but year in format changes
        assertEquals("INV-2024-0001", formatted2024)
        assertEquals("INV-2025-0002", formatted2025_1, "Sequence continues to 0002 in 2025")
        assertEquals("INV-2025-0003", formatted2025_2, "Sequence continues to 0003 in 2025")
    }

    /**
     * Test that formatting without year works with continuous numbering.
     */
    @Test
    fun `continuous numbering without year in format`() = runBlocking {
        val globalYear = 0

        val num1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalYear).getOrThrow()
        val num2 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, globalYear).getOrThrow()

        // Format without year
        val formatted1 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2024, num1, 4, false)
        val formatted2 = invoiceNumberGenerator.formatInvoiceNumber("INV", 2025, num2, 4, false)

        // Both should have continuous numbers, no year in format
        assertEquals("INV-0001", formatted1)
        assertEquals("INV-0002", formatted2)
    }

    /**
     * Test various padding configurations with year rollover.
     */
    @Test
    fun `year rollover works with different padding configurations`() = runBlocking {
        // Test with 6-digit padding
        val num2024 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()
        val num2025 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2025).getOrThrow()

        val formatted2024 = invoiceNumberGenerator.formatInvoiceNumber("FACT", 2024, num2024, 6, true)
        val formatted2025 = invoiceNumberGenerator.formatInvoiceNumber("FACT", 2025, num2025, 6, true)

        assertEquals("FACT-2024-000001", formatted2024)
        assertEquals("FACT-2025-000001", formatted2025, "2025 resets to 000001")
    }

    /**
     * Test that multiple tenants have independent year sequences.
     */
    @Test
    fun `multiple tenants have independent year sequences`() = runBlocking {
        // Create second tenant
        val secondTenantUuid = UUID.randomUUID()
        val secondTenantId = TenantId(secondTenantUuid.toKotlinUuid())

        transaction(database) {
            TenantTable.insert {
                it[id] = secondTenantUuid
                it[type] = TenantType.Freelancer
                it[legalName] = "Second Tenant"
                it[displayName] = "Second Tenant"
                it[plan] = TenantPlan.Starter
                it[status] = TenantStatus.Active
                it[language] = Language.En
            }

            TenantSettingsTable.insert {
                it[id] = UUID.randomUUID()
                it[tenantId] = secondTenantUuid
                it[invoicePrefix] = "SEC"
                it[nextInvoiceNumber] = 1
                it[defaultPaymentTerms] = 14
                it[defaultVatRate] = BigDecimal("21.00")
                it[invoiceYearlyReset] = true
                it[invoicePadding] = 4
                it[invoiceIncludeYear] = true
                it[invoiceTimezone] = "Europe/Brussels"
            }
        }

        // Generate invoices for both tenants in 2024
        val tenant1_2024_1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()
        val tenant1_2024_2 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2024).getOrThrow()
        val tenant2_2024_1 = invoiceNumberRepository.getAndIncrementSequence(secondTenantId, 2024).getOrThrow()

        assertEquals(1, tenant1_2024_1, "Tenant 1 first invoice in 2024 should be 1")
        assertEquals(2, tenant1_2024_2, "Tenant 1 second invoice in 2024 should be 2")
        assertEquals(1, tenant2_2024_1, "Tenant 2 first invoice in 2024 should be 1 (independent)")

        // Year rollover affects each tenant independently
        val tenant1_2025_1 = invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, 2025).getOrThrow()
        val tenant2_2025_1 = invoiceNumberRepository.getAndIncrementSequence(secondTenantId, 2025).getOrThrow()

        assertEquals(1, tenant1_2025_1, "Tenant 1 first invoice in 2025 should reset to 1")
        assertEquals(1, tenant2_2025_1, "Tenant 2 first invoice in 2025 should reset to 1")
    }
}
