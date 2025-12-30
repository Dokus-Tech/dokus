package tech.dokus.database.services

import tech.dokus.database.repository.cashflow.InvoiceNumberRepository
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for InvoiceNumberGenerator.
 *
 * Tests cover:
 * 1. formatInvoiceNumber() - various format combinations
 * 2. getCurrentYear() - timezone handling
 *
 * Note: Tests for generateInvoiceNumber() and previewNextInvoiceNumber() that require
 * database interactions are covered in integration tests (InvoiceNumberConcurrencyTest).
 */
class InvoiceNumberGeneratorTest {

    // Create a minimal test instance with a null repository
    // We only test the pure functions that don't require repository access
    private val generator = createTestGenerator()

    private fun createTestGenerator(): InvoiceNumberGenerator {
        // Use reflection to create instance without requiring a real repository
        // for testing pure functions only
        return InvoiceNumberGenerator(
            invoiceNumberRepository = createMockRepository()
        )
    }

    private fun createMockRepository(): InvoiceNumberRepository {
        // Create a real repository instance - we won't use its methods
        // in these unit tests (only pure functions are tested)
        return InvoiceNumberRepository()
    }

    // ========================================
    // Tests for formatInvoiceNumber()
    // ========================================

    @Test
    fun `formatInvoiceNumber with year included`() {
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 1,
            padding = 4,
            includeYear = true
        )

        assertEquals("INV-2025-0001", result)
    }

    @Test
    fun `formatInvoiceNumber without year`() {
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 1,
            padding = 4,
            includeYear = false
        )

        assertEquals("INV-0001", result)
    }

    @Test
    fun `formatInvoiceNumber with padding of 6`() {
        val result = generator.formatInvoiceNumber(
            prefix = "FACT",
            year = 2025,
            sequence = 42,
            padding = 6,
            includeYear = true
        )
        assertEquals("FACT-2025-000042", result)
    }

    @Test
    fun `formatInvoiceNumber with padding of 2`() {
        val result = generator.formatInvoiceNumber(
            prefix = "REC",
            year = 2025,
            sequence = 5,
            padding = 2,
            includeYear = true
        )
        assertEquals("REC-2025-05", result)
    }

    @Test
    fun `formatInvoiceNumber when number exceeds padding`() {
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 12345,
            padding = 4,
            includeYear = true
        )
        assertEquals("INV-2025-12345", result)
    }

    @Test
    fun `formatInvoiceNumber with custom prefix`() {
        val result = generator.formatInvoiceNumber(
            prefix = "FACTUUR",
            year = 2024,
            sequence = 99,
            padding = 4,
            includeYear = true
        )

        assertEquals("FACTUUR-2024-0099", result)
    }

    @Test
    fun `formatInvoiceNumber with sequence number 1`() {
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 1,
            padding = 4,
            includeYear = true
        )

        assertEquals("INV-2025-0001", result)
    }

    @Test
    fun `formatInvoiceNumber with large sequence number`() {
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 999999,
            padding = 4,
            includeYear = true
        )

        assertEquals("INV-2025-999999", result)
    }

    @Test
    fun `formatInvoiceNumber with minimum padding of 1`() {
        val result = generator.formatInvoiceNumber(
            prefix = "X",
            year = 2025,
            sequence = 7,
            padding = 1,
            includeYear = true
        )

        assertEquals("X-2025-7", result)
    }

    @Test
    fun `formatInvoiceNumber with empty prefix`() {
        val result = generator.formatInvoiceNumber(
            prefix = "",
            year = 2025,
            sequence = 1,
            padding = 4,
            includeYear = true
        )

        assertEquals("-2025-0001", result)
    }

    @Test
    fun `formatInvoiceNumber with empty prefix and no year`() {
        val result = generator.formatInvoiceNumber(
            prefix = "",
            year = 2025,
            sequence = 1,
            padding = 4,
            includeYear = false
        )

        assertEquals("-0001", result)
    }

    @Test
    fun `formatInvoiceNumber handles year 2024`() {
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2024,
            sequence = 1,
            padding = 4,
            includeYear = true
        )
        assertEquals("INV-2024-0001", result)
    }

    @Test
    fun `formatInvoiceNumber handles year 2030`() {
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2030,
            sequence = 1,
            padding = 4,
            includeYear = true
        )
        assertEquals("INV-2030-0001", result)
    }

    @Test
    fun `formatInvoiceNumber handles sequence zero`() {
        // Edge case - sequence 0 should format as 0000
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 0,
            padding = 4,
            includeYear = true
        )
        assertEquals("INV-2025-0000", result)
    }

    @Test
    fun `formatInvoiceNumber with Belgian FACTUUR prefix`() {
        // Belgian-style invoice prefix
        val result = generator.formatInvoiceNumber(
            prefix = "FACT",
            year = 2025,
            sequence = 123,
            padding = 5,
            includeYear = true
        )
        assertEquals("FACT-2025-00123", result)
    }

    // ========================================
    // Tests for getCurrentYear()
    // ========================================

    @Test
    fun `getCurrentYear with valid Brussels timezone`() {
        val year = generator.getCurrentYear("Europe/Brussels")

        // Should return the current year in Brussels timezone
        val expectedYear = ZonedDateTime.now(ZoneId.of("Europe/Brussels")).year
        assertEquals(expectedYear, year)
    }

    @Test
    fun `getCurrentYear with valid UTC timezone`() {
        val year = generator.getCurrentYear("UTC")

        val expectedYear = ZonedDateTime.now(ZoneId.of("UTC")).year
        assertEquals(expectedYear, year)
    }

    @Test
    fun `getCurrentYear with valid Tokyo timezone`() {
        val year = generator.getCurrentYear("Asia/Tokyo")

        val expectedYear = ZonedDateTime.now(ZoneId.of("Asia/Tokyo")).year
        assertEquals(expectedYear, year)
    }

    @Test
    fun `getCurrentYear with valid New York timezone`() {
        val year = generator.getCurrentYear("America/New_York")

        val expectedYear = ZonedDateTime.now(ZoneId.of("America/New_York")).year
        assertEquals(expectedYear, year)
    }

    @Test
    fun `getCurrentYear with invalid timezone falls back to Brussels`() {
        val year = generator.getCurrentYear("Invalid/Timezone")

        // Should fall back to Europe/Brussels
        val expectedYear = ZonedDateTime.now(ZoneId.of("Europe/Brussels")).year
        assertEquals(expectedYear, year)
    }

    @Test
    fun `getCurrentYear with empty string falls back to Brussels`() {
        val year = generator.getCurrentYear("")

        // Should fall back to Europe/Brussels
        val expectedYear = ZonedDateTime.now(ZoneId.of("Europe/Brussels")).year
        assertEquals(expectedYear, year)
    }

    @Test
    fun `getCurrentYear with whitespace falls back to Brussels`() {
        val year = generator.getCurrentYear("   ")

        // Should fall back to Europe/Brussels
        val expectedYear = ZonedDateTime.now(ZoneId.of("Europe/Brussels")).year
        assertEquals(expectedYear, year)
    }

    @Test
    fun `getCurrentYear with null-like string falls back to Brussels`() {
        val year = generator.getCurrentYear("null")

        // Should fall back to Europe/Brussels
        val expectedYear = ZonedDateTime.now(ZoneId.of("Europe/Brussels")).year
        assertEquals(expectedYear, year)
    }

    // ========================================
    // Edge case and validation tests
    // ========================================

    @Test
    fun `formatInvoiceNumber with maximum practical padding`() {
        val result = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 1,
            padding = 10,
            includeYear = true
        )
        assertEquals("INV-2025-0000000001", result)
    }

    @Test
    fun `formatInvoiceNumber sequential numbers are formatted consistently`() {
        val numbers = (1..5).map { seq ->
            generator.formatInvoiceNumber(
                prefix = "SEQ",
                year = 2025,
                sequence = seq,
                padding = 4,
                includeYear = true
            )
        }

        assertEquals("SEQ-2025-0001", numbers[0])
        assertEquals("SEQ-2025-0002", numbers[1])
        assertEquals("SEQ-2025-0003", numbers[2])
        assertEquals("SEQ-2025-0004", numbers[3])
        assertEquals("SEQ-2025-0005", numbers[4])
    }

    @Test
    fun `formatInvoiceNumber without year ignores year parameter`() {
        // Even with different years, the output should be the same when includeYear=false
        val result2024 = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2024,
            sequence = 1,
            padding = 4,
            includeYear = false
        )
        val result2025 = generator.formatInvoiceNumber(
            prefix = "INV",
            year = 2025,
            sequence = 1,
            padding = 4,
            includeYear = false
        )

        assertEquals("INV-0001", result2024)
        assertEquals("INV-0001", result2025)
        assertEquals(result2024, result2025)
    }

    @Test
    fun `formatInvoiceNumber with special characters in prefix`() {
        // Test with prefix containing allowed special characters
        val result = generator.formatInvoiceNumber(
            prefix = "INV_2025",
            year = 2025,
            sequence = 1,
            padding = 4,
            includeYear = false
        )
        assertEquals("INV_2025-0001", result)
    }

    @Test
    fun `getCurrentYear returns consistent value for same timezone`() {
        // Multiple calls should return the same year
        val year1 = generator.getCurrentYear("Europe/Brussels")
        val year2 = generator.getCurrentYear("Europe/Brussels")
        val year3 = generator.getCurrentYear("Europe/Brussels")

        assertEquals(year1, year2)
        assertEquals(year2, year3)
    }

    @Test
    fun `formatInvoiceNumber handles boundary sequence values`() {
        // Test at boundaries
        val seq1 = generator.formatInvoiceNumber("INV", 2025, 1, 4, true)
        val seq9999 = generator.formatInvoiceNumber("INV", 2025, 9999, 4, true)
        val seq10000 = generator.formatInvoiceNumber("INV", 2025, 10000, 4, true)

        assertEquals("INV-2025-0001", seq1)
        assertEquals("INV-2025-9999", seq9999)
        assertEquals("INV-2025-10000", seq10000) // Exceeds padding, still works
    }
}
