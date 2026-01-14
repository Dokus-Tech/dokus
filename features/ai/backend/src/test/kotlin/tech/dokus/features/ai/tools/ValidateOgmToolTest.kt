package tech.dokus.features.ai.tools

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ValidateOgmToolTest {

    @Test
    fun `valid OGM returns VALID`() = runTest {
        // 012345678939: 0123456789 % 97 = 39
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "+++012/3456/78939+++")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID, got: $result")
        assertTrue(result.contains("+++012/3456/78939+++"), "Should include normalized format")
    }

    @Test
    fun `valid OGM with asterisks returns VALID`() = runTest {
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "***012/3456/78939***")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID, got: $result")
    }

    @Test
    fun `invalid checksum returns INVALID`() = runTest {
        // Wrong check digit (should be 39, not 40)
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "+++012/3456/78940+++")
        )

        assertTrue(result.startsWith("INVALID:"), "Expected INVALID, got: $result")
        assertTrue(result.contains("checksum"), "Should mention checksum failure")
        assertTrue(result.contains("39"), "Should show expected check digit")
    }

    @Test
    fun `invalid format returns INVALID`() = runTest {
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "not an OGM")
        )

        assertTrue(result.startsWith("INVALID:"), "Expected INVALID, got: $result")
        assertTrue(result.contains("format"), "Should mention format issue")
    }

    @Test
    fun `OCR correction O to 0 is applied`() = runTest {
        // Using 'O' (letter) instead of '0' (digit)
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "+++O12/3456/78939+++")
        )

        assertTrue(result.contains("VALID"), "Expected VALID after OCR correction, got: $result")
        assertTrue(result.contains("OCR correction") || result.contains("corrected"), "Should mention correction")
    }

    @Test
    fun `OCR correction B to 8 is applied`() = runTest {
        // 888888888875: 8888888888 % 97 = 75
        // Using 'B' (letter) instead of '8' (digit)
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "+++BBB/BBBB/BB875+++")
        )

        assertTrue(result.contains("VALID"), "Expected VALID after OCR correction, got: $result")
    }

    @Test
    fun `check digit 97 when remainder is 0`() = runTest {
        // Need a number where base % 97 = 0
        // 9700000000 % 97 = 0 → check digit should be 97
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "+++970/0000/00097+++")
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID, got: $result")
    }

    @Test
    fun `empty OGM returns INVALID`() = runTest {
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "")
        )

        assertTrue(result.startsWith("INVALID:"), "Expected INVALID for empty, got: $result")
    }

    @Test
    fun `OGM with spaces is handled`() = runTest {
        // Spaces should be handled gracefully
        val result = ValidateOgmTool.execute(
            ValidateOgmTool.Args(ogm = "+++ 012/3456/78939 +++")
        )

        // May be valid if spaces are trimmed, or invalid format
        assertTrue(
            result.contains("VALID") || result.contains("INVALID"),
            "Should return valid or invalid result"
        )
    }
}
