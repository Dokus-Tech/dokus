package tech.dokus.features.ai.tools

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ValidateIbanToolTest {

    @Test
    fun `valid Belgian IBAN returns VALID`() = runTest {
        // Known valid Belgian IBAN
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "BE68539007547034")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID, got: $result")
        assertTrue(result.contains("BE68 5390 0754 7034"), "Should include formatted IBAN")
    }

    @Test
    fun `valid IBAN with spaces returns VALID`() = runTest {
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "BE68 5390 0754 7034")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID, got: $result")
    }

    @Test
    fun `valid IBAN with dashes returns VALID`() = runTest {
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "BE68-5390-0754-7034")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID, got: $result")
    }

    @Test
    fun `valid lowercase IBAN returns VALID`() = runTest {
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "be68539007547034")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID for lowercase, got: $result")
    }

    @Test
    fun `invalid checksum returns INVALID`() = runTest {
        // Change last digit to make checksum invalid
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "BE68539007547035")
        )

        assertTrue(result.startsWith("INVALID:"), "Expected INVALID, got: $result")
        assertTrue(result.contains("checksum"), "Should mention checksum failure")
    }

    @Test
    fun `Belgian IBAN with wrong length returns INVALID`() = runTest {
        // Belgian IBAN must be exactly 16 characters
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "BE6853900754703")
        )

        assertTrue(result.startsWith("INVALID:"), "Expected INVALID for wrong length, got: $result")
        assertTrue(result.contains("16"), "Should mention 16-character requirement")
    }

    @Test
    fun `invalid format returns INVALID`() = runTest {
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "not an IBAN")
        )

        assertTrue(result.startsWith("INVALID:"), "Expected INVALID, got: $result")
        assertTrue(result.contains("format"), "Should mention format issue")
    }

    @Test
    fun `empty IBAN returns INVALID`() = runTest {
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "")
        )

        assertTrue(result.startsWith("INVALID:"), "Expected INVALID for empty, got: $result")
    }

    @Test
    fun `valid German IBAN returns VALID`() = runTest {
        // German IBAN is 22 characters
        // DE89370400440532013000 is a standard test IBAN
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "DE89370400440532013000")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID for German IBAN, got: $result")
    }

    @Test
    fun `valid Dutch IBAN returns VALID`() = runTest {
        // Dutch IBAN is 18 characters
        // NL91ABNA0417164300 is a standard test IBAN
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "NL91ABNA0417164300")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID for Dutch IBAN, got: $result")
    }

    @Test
    fun `OCR hint is provided for invalid IBAN`() = runTest {
        val result = ValidateIbanTool.execute(
            ValidateIbanTool.Args(iban = "BE68539007547035")
        )

        assertTrue(result.contains("0↔O") || result.contains("1↔I"), "Should provide OCR hints")
    }
}
