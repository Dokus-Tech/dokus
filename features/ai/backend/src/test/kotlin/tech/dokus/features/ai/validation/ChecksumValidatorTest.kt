package tech.dokus.features.ai.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChecksumValidatorTest {

    // =========================================================================
    // auditOgm tests
    // =========================================================================

    @Test
    fun `auditOgm returns incomplete for null input`() {
        val result = ChecksumValidator.auditOgm(null)

        assertTrue(result.passed)
        assertEquals(Severity.INFO, result.severity)
        assertEquals(CheckType.CHECKSUM_OGM, result.type)
    }

    @Test
    fun `auditOgm returns incomplete for blank input`() {
        val result = ChecksumValidator.auditOgm("   ")

        assertTrue(result.passed)
        assertEquals(Severity.INFO, result.severity)
    }

    @Test
    fun `auditOgm returns incomplete for non-OGM format`() {
        val result = ChecksumValidator.auditOgm("RF12345678")

        assertTrue(result.passed) // Not an OGM, so not a failure
        assertTrue(result.message.contains("not in OGM format"))
    }

    @Test
    fun `auditOgm passes for valid OGM`() {
        // Base: 0123456789, Check: 0123456789 % 97 = 39
        val result = ChecksumValidator.auditOgm("+++012/3456/78939+++")

        assertTrue(result.passed)
        assertEquals(Severity.INFO, result.severity)
        assertTrue(result.message.contains("verified"))
    }

    @Test
    fun `auditOgm passes for OCR-corrected OGM`() {
        // O instead of 0, Base: 0123456789, Check: 39
        val result = ChecksumValidator.auditOgm("+++O12/3456/78939+++")

        assertTrue(result.passed)
        assertEquals(Severity.INFO, result.severity)
        assertTrue(result.message.contains("OCR correction"))
    }

    @Test
    fun `auditOgm fails critical for invalid checksum`() {
        // Wrong check digits (99 instead of 06)
        val result = ChecksumValidator.auditOgm("+++012/3456/78999+++")

        assertFalse(result.passed)
        assertEquals(Severity.CRITICAL, result.severity)
        assertTrue(result.hint?.contains("Re-read") == true)
        assertTrue(result.hint?.contains("OCR") == true)
    }

    @Test
    fun `auditOgm warns for invalid format that looks like OGM`() {
        // Has +++ but wrong format
        val result = ChecksumValidator.auditOgm("+++12345+++")

        assertFalse(result.passed)
        assertEquals(Severity.WARNING, result.severity)
        assertTrue(result.message.contains("Invalid OGM format"))
    }

    // =========================================================================
    // auditIban tests
    // =========================================================================

    @Test
    fun `auditIban returns incomplete for null input`() {
        val result = ChecksumValidator.auditIban(null)

        assertTrue(result.passed)
        assertEquals(Severity.INFO, result.severity)
        assertEquals(CheckType.CHECKSUM_IBAN, result.type)
    }

    @Test
    fun `auditIban returns incomplete for blank input`() {
        val result = ChecksumValidator.auditIban("   ")

        assertTrue(result.passed)
        assertEquals(Severity.INFO, result.severity)
    }

    @Test
    fun `auditIban passes for valid Belgian IBAN`() {
        // BE68 5390 0754 7034 is a valid Belgian IBAN
        val result = ChecksumValidator.auditIban("BE68539007547034")

        assertTrue(result.passed)
        assertEquals(Severity.INFO, result.severity)
        assertTrue(result.message.contains("verified"))
    }

    @Test
    fun `auditIban passes for valid IBAN with spaces`() {
        val result = ChecksumValidator.auditIban("BE68 5390 0754 7034")

        assertTrue(result.passed)
    }

    @Test
    fun `auditIban passes for valid IBAN with lowercase`() {
        val result = ChecksumValidator.auditIban("be68539007547034")

        assertTrue(result.passed)
    }

    @Test
    fun `auditIban fails critical for invalid checksum`() {
        // Changed last digit to make checksum fail
        val result = ChecksumValidator.auditIban("BE68539007547035")

        assertFalse(result.passed)
        assertEquals(Severity.CRITICAL, result.severity)
        assertTrue(result.hint?.contains("Re-read") == true)
        assertTrue(result.hint?.contains("BANK DETAILS") == true)
    }

    @Test
    fun `auditIban hint mentions Belgian format for BE IBAN`() {
        // Invalid Belgian IBAN (wrong length)
        val result = ChecksumValidator.auditIban("BE6853900754")

        assertFalse(result.passed)
        assertTrue(result.hint?.contains("Belgian") == true)
        assertTrue(result.hint?.contains("16 characters") == true)
    }
}
