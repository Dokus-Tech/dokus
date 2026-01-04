package tech.dokus.domain.ids

import tech.dokus.domain.exceptions.DokusException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for the PostalCode value class.
 *
 * Tests cover the Validatable contract:
 * 1. isValid returns true for valid postal codes
 * 2. isValid returns false for invalid postal codes
 * 3. validOrThrows returns the PostalCode for valid input
 * 4. validOrThrows throws InvalidPostalCode for invalid input
 * 5. toString() returns the value
 */
class PostalCodeTest {

    // ========================================
    // Tests for isValid property - valid cases
    // ========================================

    @Test
    fun `isValid returns true for valid postal code at lower boundary`() {
        val postalCode = PostalCode("1000")
        assertTrue(postalCode.isValid)
    }

    @Test
    fun `isValid returns true for valid postal code at upper boundary`() {
        val postalCode = PostalCode("9999")
        assertTrue(postalCode.isValid)
    }

    @Test
    fun `isValid returns true for valid postal code in middle range`() {
        val postalCode = PostalCode("5000")
        assertTrue(postalCode.isValid)
    }

    @Test
    fun `isValid returns true for valid Brussels postal code`() {
        val postalCode = PostalCode("1000")
        assertTrue(postalCode.isValid)
    }

    @Test
    fun `isValid returns true for valid Antwerp postal code`() {
        val postalCode = PostalCode("2000")
        assertTrue(postalCode.isValid)
    }

    @Test
    fun `isValid returns true for valid Ghent postal code`() {
        val postalCode = PostalCode("9000")
        assertTrue(postalCode.isValid)
    }

    @Test
    fun `isValid returns true for valid postal code with leading whitespace`() {
        val postalCode = PostalCode("  1000")
        assertTrue(postalCode.isValid)
    }

    @Test
    fun `isValid returns true for valid postal code with trailing whitespace`() {
        val postalCode = PostalCode("1000  ")
        assertTrue(postalCode.isValid)
    }

    // ========================================
    // Tests for isValid property - invalid cases
    // ========================================

    @Test
    fun `isValid returns false for postal code below lower boundary`() {
        val postalCode = PostalCode("999")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for postal code above upper boundary`() {
        val postalCode = PostalCode("10000")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for postal code with leading zero`() {
        val postalCode = PostalCode("0500")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for empty string`() {
        val postalCode = PostalCode("")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for blank string`() {
        val postalCode = PostalCode("    ")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for postal code with letters`() {
        val postalCode = PostalCode("ABCD")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for postal code with mixed letters and numbers`() {
        val postalCode = PostalCode("1A2B")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for postal code with special characters`() {
        val postalCode = PostalCode("10-00")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for postal code too short`() {
        val postalCode = PostalCode("123")
        assertFalse(postalCode.isValid)
    }

    @Test
    fun `isValid returns false for postal code too long`() {
        val postalCode = PostalCode("12345")
        assertFalse(postalCode.isValid)
    }

    // ========================================
    // Tests for validOrThrows property - valid cases
    // ========================================

    @Test
    fun `validOrThrows returns same PostalCode for valid input`() {
        val postalCode = PostalCode("1000")
        val result = postalCode.validOrThrows
        assertEquals(postalCode, result)
    }

    @Test
    fun `validOrThrows returns PostalCode for valid upper boundary`() {
        val postalCode = PostalCode("9999")
        val result = postalCode.validOrThrows
        assertEquals(postalCode, result)
    }

    @Test
    fun `validOrThrows returns PostalCode for valid middle range`() {
        val postalCode = PostalCode("5000")
        val result = postalCode.validOrThrows
        assertEquals(postalCode, result)
    }

    @Test
    fun `validOrThrows returns PostalCode for valid input with whitespace`() {
        val postalCode = PostalCode("  1000  ")
        val result = postalCode.validOrThrows
        assertEquals(postalCode, result)
    }

    // ========================================
    // Tests for validOrThrows property - invalid cases
    // ========================================

    @Test
    fun `validOrThrows throws InvalidPostalCode for postal code below lower boundary`() {
        val postalCode = PostalCode("999")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    @Test
    fun `validOrThrows throws InvalidPostalCode for postal code above upper boundary`() {
        val postalCode = PostalCode("10000")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    @Test
    fun `validOrThrows throws InvalidPostalCode for empty string`() {
        val postalCode = PostalCode("")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    @Test
    fun `validOrThrows throws InvalidPostalCode for blank string`() {
        val postalCode = PostalCode("    ")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    @Test
    fun `validOrThrows throws InvalidPostalCode for postal code with letters`() {
        val postalCode = PostalCode("ABCD")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    @Test
    fun `validOrThrows throws InvalidPostalCode for postal code with leading zero`() {
        val postalCode = PostalCode("0500")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    @Test
    fun `validOrThrows throws InvalidPostalCode for postal code too short`() {
        val postalCode = PostalCode("123")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    @Test
    fun `validOrThrows throws InvalidPostalCode for postal code too long`() {
        val postalCode = PostalCode("12345")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    @Test
    fun `validOrThrows throws InvalidPostalCode for postal code with special characters`() {
        val postalCode = PostalCode("10-00")
        assertFailsWith<DokusException.Validation.InvalidPostalCode> {
            postalCode.validOrThrows
        }
    }

    // ========================================
    // Tests for toString() method
    // ========================================

    @Test
    fun `toString returns the value`() {
        val postalCode = PostalCode("1000")
        assertEquals("1000", postalCode.toString())
    }

    @Test
    fun `toString returns value including whitespace`() {
        val postalCode = PostalCode("  1000  ")
        assertEquals("  1000  ", postalCode.toString())
    }

    @Test
    fun `toString returns empty string for empty input`() {
        val postalCode = PostalCode("")
        assertEquals("", postalCode.toString())
    }

    @Test
    fun `toString returns the exact value passed to constructor`() {
        val value = "5678"
        val postalCode = PostalCode(value)
        assertEquals(value, postalCode.toString())
    }

    // ========================================
    // Tests for value property
    // ========================================

    @Test
    fun `value property returns the underlying string`() {
        val postalCode = PostalCode("1000")
        assertEquals("1000", postalCode.value)
    }

    @Test
    fun `value property returns original value with whitespace`() {
        val postalCode = PostalCode("  1000  ")
        assertEquals("  1000  ", postalCode.value)
    }
}
