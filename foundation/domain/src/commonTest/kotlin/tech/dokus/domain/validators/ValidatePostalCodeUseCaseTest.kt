package tech.dokus.domain.validators

import tech.dokus.domain.ids.PostalCode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ValidatePostalCodeUseCase.
 *
 * Tests cover:
 * 1. Valid Belgian postal codes (1000-9999)
 * 2. Invalid postal codes (out of range, wrong format)
 * 3. Blank and whitespace input
 * 4. Non-numeric input
 * 5. Leading/trailing whitespace handling
 */
class ValidatePostalCodeUseCaseTest {

    // ========================================
    // Tests for valid Belgian postal codes
    // ========================================

    @Test
    fun `valid postal code at lower boundary`() {
        val postalCode = PostalCode("1000")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code at upper boundary`() {
        val postalCode = PostalCode("9999")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code in middle range`() {
        val postalCode = PostalCode("5000")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid Brussels postal code`() {
        val postalCode = PostalCode("1000")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid Antwerp postal code`() {
        val postalCode = PostalCode("2000")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid Ghent postal code`() {
        val postalCode = PostalCode("9000")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid Liege postal code`() {
        val postalCode = PostalCode("4000")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code with mixed digits`() {
        val postalCode = PostalCode("3527")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    // ========================================
    // Tests for invalid postal codes - out of range
    // ========================================

    @Test
    fun `invalid postal code below lower boundary`() {
        val postalCode = PostalCode("999")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code just below lower boundary`() {
        val postalCode = PostalCode("0999")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code zero`() {
        val postalCode = PostalCode("0000")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code above upper boundary`() {
        val postalCode = PostalCode("10000")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with leading zero`() {
        // 0500 is only 4 digits but starts with 0, making it 500 which is out of range
        val postalCode = PostalCode("0500")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with all zeros`() {
        val postalCode = PostalCode("0000")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    // ========================================
    // Tests for invalid postal codes - wrong format
    // ========================================

    @Test
    fun `invalid postal code too short`() {
        val postalCode = PostalCode("123")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code too long`() {
        val postalCode = PostalCode("12345")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code single digit`() {
        val postalCode = PostalCode("1")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code two digits`() {
        val postalCode = PostalCode("12")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    // ========================================
    // Tests for blank and whitespace input
    // ========================================

    @Test
    fun `invalid postal code empty string`() {
        val postalCode = PostalCode("")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code blank with spaces`() {
        val postalCode = PostalCode("    ")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code blank with tabs`() {
        val postalCode = PostalCode("\t\t")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code blank with newlines`() {
        val postalCode = PostalCode("\n\n")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code blank with mixed whitespace`() {
        val postalCode = PostalCode(" \t\n ")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    // ========================================
    // Tests for non-numeric input
    // ========================================

    @Test
    fun `invalid postal code with letters`() {
        val postalCode = PostalCode("ABCD")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with mixed letters and numbers`() {
        val postalCode = PostalCode("1A2B")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with special characters`() {
        val postalCode = PostalCode("10-00")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with decimal point`() {
        val postalCode = PostalCode("10.00")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with plus sign`() {
        val postalCode = PostalCode("+1000")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with minus sign`() {
        val postalCode = PostalCode("-1000")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with spaces between digits`() {
        val postalCode = PostalCode("10 00")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    // ========================================
    // Tests for leading/trailing whitespace handling
    // ========================================

    @Test
    fun `valid postal code with leading whitespace`() {
        val postalCode = PostalCode("  1000")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code with trailing whitespace`() {
        val postalCode = PostalCode("1000  ")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code with leading and trailing whitespace`() {
        val postalCode = PostalCode("  1000  ")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code with leading tabs`() {
        val postalCode = PostalCode("\t5000")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code with trailing tabs`() {
        val postalCode = PostalCode("5000\t")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code with mixed leading and trailing whitespace`() {
        val postalCode = PostalCode("\t 9999 \t")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    // ========================================
    // Edge cases and boundary value tests
    // ========================================

    @Test
    fun `valid postal code at boundary 1001`() {
        val postalCode = PostalCode("1001")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code at boundary 9998`() {
        val postalCode = PostalCode("9998")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code 1111`() {
        val postalCode = PostalCode("1111")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `valid postal code 8888`() {
        val postalCode = PostalCode("8888")
        assertTrue(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code negative number string`() {
        val postalCode = PostalCode("-5000")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with unicode digits`() {
        // Arabic-Indic digits (should be invalid)
        val postalCode = PostalCode("\u0661\u0660\u0660\u0660")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }

    @Test
    fun `invalid postal code with full-width digits`() {
        // Full-width digits (should be invalid)
        val postalCode = PostalCode("\uFF11\uFF10\uFF10\uFF10")
        assertFalse(ValidatePostalCodeUseCase(postalCode))
    }
}
