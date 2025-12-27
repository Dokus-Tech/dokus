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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

/**
 * Integration tests for concurrent invoice number generation.
 *
 * These tests verify that the invoice number generation system produces
 * gap-less, sequential numbers even under high concurrent load. This is
 * critical for Belgian tax law compliance which mandates gap-less numbering.
 *
 * Tests verify:
 * 1. All generated numbers are unique (no duplicates)
 * 2. Numbers are sequential from 1 to N
 * 3. No gaps in the sequence
 * 4. Thread-safety under concurrent access
 */
@OptIn(ExperimentalUuidApi::class)
class InvoiceNumberConcurrencyTest {

    private lateinit var database: Database
    private lateinit var invoiceNumberRepository: InvoiceNumberRepository
    private lateinit var invoiceNumberGenerator: InvoiceNumberGenerator
    private var testTenantId: TenantId? = null
    private lateinit var testTenantUuid: UUID

    @BeforeEach
    fun setup() {
        // Create H2 in-memory database with PostgreSQL compatibility mode
        // Using DB_CLOSE_DELAY=-1 to keep the database alive for the test duration
        database = Database.connect(
            url = "jdbc:h2:mem:test_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
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
                it[legalName] = "Test Company"
                it[displayName] = "Test Company"
                it[plan] = TenantPlan.Professional
                it[status] = TenantStatus.Active
                it[language] = Language.En
            }

            // Insert tenant settings with invoice configuration
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
     * Test that 100 concurrent requests produce 100 unique sequential numbers.
     * This is the primary concurrency test for Belgian tax law compliance.
     */
    @Test
    fun `100 concurrent requests produce 100 unique sequential numbers`() = runBlocking {
        val numberOfRequests = 100
        val errors = ConcurrentHashMap.newKeySet<Throwable>()
        val currentYear = Year.now().value

        // Launch 100 concurrent coroutines
        val deferreds = (1..numberOfRequests).map {
            async(Dispatchers.IO) {
                try {
                    invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, currentYear).getOrThrow()
                } catch (e: Throwable) {
                    errors.add(e)
                    -1
                }
            }
        }

        // Wait for all to complete
        val sequenceNumbers = deferreds.awaitAll().filter { it > 0 }

        // Verify no errors
        assertTrue(errors.isEmpty(), "Expected no errors but got: ${errors.map { it.message }}")

        // Verify we got 100 numbers
        assertEquals(numberOfRequests, sequenceNumbers.size, "Expected $numberOfRequests sequence numbers")

        // Verify all numbers are unique
        val uniqueNumbers = sequenceNumbers.toSet()
        assertEquals(
            numberOfRequests,
            uniqueNumbers.size,
            "Expected all numbers to be unique. Duplicates found: ${findDuplicates(sequenceNumbers)}"
        )

        // Verify numbers are sequential from 1 to 100
        val sortedNumbers = sequenceNumbers.sorted()
        val expectedNumbers = (1..numberOfRequests).toList()
        assertEquals(
            expectedNumbers,
            sortedNumbers,
            "Expected sequential numbers from 1 to $numberOfRequests"
        )

        // Verify no gaps
        for (i in 1 until sortedNumbers.size) {
            assertEquals(
                sortedNumbers[i - 1] + 1,
                sortedNumbers[i],
                "Gap detected between ${sortedNumbers[i - 1]} and ${sortedNumbers[i]}"
            )
        }
    }

    /**
     * Test that running the concurrency test multiple times produces consistent results.
     * This ensures the test is deterministic and the implementation is reliable.
     */
    @Test
    fun `concurrent number generation is consistent across multiple runs`() = runBlocking {
        val numberOfRequests = 50
        val runs = 3
        val currentYear = Year.now().value

        repeat(runs) { run ->
            // Reset the sequence for each run by using a different year
            val yearForRun = currentYear + run

            val deferreds = (1..numberOfRequests).map {
                async(Dispatchers.IO) {
                    try {
                        invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, yearForRun).getOrThrow()
                    } catch (e: Throwable) {
                        -1
                    }
                }
            }

            val sequenceNumbers = deferreds.awaitAll().filter { it > 0 }

            // Verify each run produces sequential numbers 1-50
            val sortedNumbers = sequenceNumbers.sorted()
            assertEquals(
                (1..numberOfRequests).toList(),
                sortedNumbers,
                "Run $run: Expected sequential numbers 1-$numberOfRequests"
            )
        }
    }

    /**
     * Test that different tenants get independent sequences.
     * This verifies tenant isolation in concurrent scenarios.
     */
    @Test
    fun `different tenants get independent sequences concurrently`() = runBlocking {
        // Create a second tenant
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

        val numberOfRequests = 50
        val currentYear = Year.now().value

        // Launch concurrent requests for both tenants
        val tenant1Deferreds = (1..numberOfRequests).map {
            async(Dispatchers.IO) {
                invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, currentYear).getOrThrow()
            }
        }

        val tenant2Deferreds = (1..numberOfRequests).map {
            async(Dispatchers.IO) {
                invoiceNumberRepository.getAndIncrementSequence(secondTenantId, currentYear).getOrThrow()
            }
        }

        val tenant1Numbers = tenant1Deferreds.awaitAll().sorted()
        val tenant2Numbers = tenant2Deferreds.awaitAll().sorted()

        // Both tenants should have independent sequences 1-50
        assertEquals(
            (1..numberOfRequests).toList(),
            tenant1Numbers,
            "Tenant 1 should have sequential numbers 1-$numberOfRequests"
        )
        assertEquals(
            (1..numberOfRequests).toList(),
            tenant2Numbers,
            "Tenant 2 should have sequential numbers 1-$numberOfRequests"
        )
    }

    /**
     * Test that the full invoice number generation (with formatting) works concurrently.
     * This tests the complete InvoiceNumberGenerator flow.
     */
    @Test
    fun `full invoice number generation produces unique formatted numbers concurrently`() = runBlocking {
        val numberOfRequests = 50
        val errors = ConcurrentHashMap.newKeySet<Throwable>()

        val deferreds = (1..numberOfRequests).map {
            async(Dispatchers.IO) {
                try {
                    invoiceNumberGenerator.generateInvoiceNumber(testTenantId!!).getOrThrow()
                } catch (e: Throwable) {
                    errors.add(e)
                    null
                }
            }
        }

        val results = deferreds.awaitAll().filterNotNull()

        // Verify no errors
        assertTrue(errors.isEmpty(), "Expected no errors but got: ${errors.map { it.message }}")

        // Verify we got the expected number of results
        assertEquals(numberOfRequests, results.size, "Expected $numberOfRequests invoice numbers")

        // Verify all generated numbers are unique
        val uniqueNumbers = results.toSet()
        assertEquals(
            numberOfRequests,
            uniqueNumbers.size,
            "Expected all invoice numbers to be unique. Duplicates: ${findDuplicates(results)}"
        )

        // Verify format is correct (INV-YYYY-NNNN)
        val currentYear = Year.now().value
        val expectedPrefix = "INV-$currentYear-"
        results.forEach { invoiceNumber ->
            assertTrue(
                invoiceNumber.startsWith(expectedPrefix),
                "Invoice number '$invoiceNumber' should start with '$expectedPrefix'"
            )
        }

        // Extract sequence numbers and verify they are sequential
        val sequenceNumbers = results.map { invoiceNumber ->
            invoiceNumber.substringAfterLast("-").toInt()
        }.sorted()

        assertEquals(
            (1..numberOfRequests).toList(),
            sequenceNumbers,
            "Sequence numbers should be 1 to $numberOfRequests"
        )
    }

    /**
     * Test high concurrency with 100 requests to stress test the locking mechanism.
     */
    @Test
    fun `high concurrency stress test with 100 concurrent requests`() = runBlocking {
        val numberOfRequests = 100
        val errors = ConcurrentHashMap.newKeySet<Throwable>()
        val currentYear = Year.now().value

        // Launch all requests simultaneously
        val deferreds = (1..numberOfRequests).map {
            async(Dispatchers.IO) {
                try {
                    invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, currentYear).getOrThrow()
                } catch (e: Throwable) {
                    errors.add(e)
                    -1
                }
            }
        }

        val results = deferreds.awaitAll().filter { it > 0 }

        // Verify no errors
        assertTrue(
            errors.isEmpty(),
            "Stress test failed with errors: ${errors.map { "${it.javaClass.simpleName}: ${it.message}" }}"
        )

        // Verify all 100 requests succeeded
        assertEquals(numberOfRequests, results.size, "All $numberOfRequests requests should succeed")

        // Verify uniqueness
        assertEquals(numberOfRequests, results.toSet().size, "All numbers should be unique")

        // Verify sequential
        assertEquals((1..numberOfRequests).toList(), results.sorted(), "Numbers should be sequential 1-$numberOfRequests")
    }

    /**
     * Test that yearly reset creates separate sequences for different years.
     */
    @Test
    fun `concurrent requests across different years produce independent sequences`() = runBlocking {
        val year2024 = 2024
        val year2025 = 2025
        val numberOfRequests = 25

        // Launch concurrent requests for both years
        val year2024Deferreds = (1..numberOfRequests).map {
            async(Dispatchers.IO) {
                invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, year2024).getOrThrow()
            }
        }

        val year2025Deferreds = (1..numberOfRequests).map {
            async(Dispatchers.IO) {
                invoiceNumberRepository.getAndIncrementSequence(testTenantId!!, year2025).getOrThrow()
            }
        }

        val year2024Numbers = year2024Deferreds.awaitAll().sorted()
        val year2025Numbers = year2025Deferreds.awaitAll().sorted()

        // Both years should have independent sequences 1-25
        assertEquals(
            (1..numberOfRequests).toList(),
            year2024Numbers,
            "Year 2024 should have sequential numbers 1-$numberOfRequests"
        )
        assertEquals(
            (1..numberOfRequests).toList(),
            year2025Numbers,
            "Year 2025 should have sequential numbers 1-$numberOfRequests"
        )
    }

    /**
     * Helper function to find duplicates in a list.
     */
    private fun <T> findDuplicates(list: List<T>): List<T> {
        val seen = mutableSetOf<T>()
        val duplicates = mutableListOf<T>()
        for (item in list) {
            if (!seen.add(item)) {
                duplicates.add(item)
            }
        }
        return duplicates
    }
}
