package tech.dokus.domain.validators

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidateOgmUseCaseTest {

    // =========================================================================
    // Valid OGM tests
    // =========================================================================

    @Test
    fun `valid OGM with plus signs`() {
        // Base: 0123456789, Checksum: 123456789 % 97 = 39
        val result = ValidateOgmUseCase.validate("+++012/3456/78939+++")

        assertTrue(result is OgmValidationResult.Valid)
        assertEquals("+++012/3456/78939+++", (result as OgmValidationResult.Valid).normalized)
    }

    @Test
    fun `valid OGM with asterisks`() {
        val result = ValidateOgmUseCase.validate("***012/3456/78939***")

        assertTrue(result is OgmValidationResult.Valid)
    }

    @Test
    fun `valid OGM with check digit 97`() {
        // When base % 97 == 0, check digit should be 97
        // Base: 0000000097 % 97 = 0, so check digit = 97
        val result = ValidateOgmUseCase.validate("+++000/0000/09797+++")

        assertTrue(result is OgmValidationResult.Valid)
    }

    @Test
    fun `valid OGM without delimiters accepted`() {
        // Same as first test but without slashes between groups
        val result = ValidateOgmUseCase.validate("+++012345678939+++")

        assertTrue(result is OgmValidationResult.Valid)
        assertEquals("+++012/3456/78939+++", (result as OgmValidationResult.Valid).normalized)
    }

    @Test
    fun `OGM generate produces valid OGM`() {
        val generated = ValidateOgmUseCase.generate(1234567890L)
        val result = ValidateOgmUseCase.validate(generated)

        assertTrue(result is OgmValidationResult.Valid)
    }

    // =========================================================================
    // Invalid format tests
    // =========================================================================

    @Test
    fun `empty string returns invalid format`() {
        val result = ValidateOgmUseCase.validate("")

        assertTrue(result is OgmValidationResult.InvalidFormat)
    }

    @Test
    fun `missing plus or asterisk prefix returns invalid format`() {
        val result = ValidateOgmUseCase.validate("012/3456/78906")

        assertTrue(result is OgmValidationResult.InvalidFormat)
    }

    @Test
    fun `wrong number of digits returns invalid format`() {
        val result = ValidateOgmUseCase.validate("+++12/3456/78906+++") // Only 11 digits

        assertTrue(result is OgmValidationResult.InvalidFormat)
    }

    // =========================================================================
    // Invalid checksum tests
    // =========================================================================

    @Test
    fun `wrong checksum returns invalid checksum`() {
        // Correct checksum for 0123456789 is 39, not 99
        val result = ValidateOgmUseCase.validate("+++012/3456/78999+++")

        assertTrue(result is OgmValidationResult.InvalidChecksum)
        val checksumResult = result as OgmValidationResult.InvalidChecksum
        assertEquals(39, checksumResult.expected)
        assertEquals(99, checksumResult.actual)
    }

    // =========================================================================
    // OCR correction tests
    // =========================================================================

    @Test
    fun `corrects O to 0 (zero)`() {
        // O12/3456/78939 should be corrected to 012/3456/78939
        // Base: 0123456789 % 97 = 39
        val result = ValidateOgmUseCase.validate("+++O12/3456/78939+++")

        assertTrue(result is OgmValidationResult.CorrectedValid)
        assertEquals("+++012/3456/78939+++", (result as OgmValidationResult.CorrectedValid).normalized)
    }

    @Test
    fun `corrects I to 1 (one)`() {
        // Base: 1111111111 % 97 = 70
        val result = ValidateOgmUseCase.validate("+++III/IIII/III70+++")

        assertTrue(result is OgmValidationResult.CorrectedValid)
        assertEquals("+++111/1111/11170+++", (result as OgmValidationResult.CorrectedValid).normalized)
    }

    @Test
    fun `corrects lowercase l to 1 (one)`() {
        // Base: 1111111111 % 97 = 70
        val result = ValidateOgmUseCase.validate("+++lll/llll/lll70+++")

        assertTrue(result is OgmValidationResult.CorrectedValid)
        assertEquals("+++111/1111/11170+++", (result as OgmValidationResult.CorrectedValid).normalized)
    }

    @Test
    fun `corrects B to 8`() {
        // Base: 8888888888 % 97 = 75
        val result = ValidateOgmUseCase.validate("+++BBB/BBBB/BBB75+++")

        assertTrue(result is OgmValidationResult.CorrectedValid)
        assertEquals("+++888/8888/88875+++", (result as OgmValidationResult.CorrectedValid).normalized)
    }

    @Test
    fun `corrects S to 5`() {
        // Base: 5555555555 % 97 = 59
        val result = ValidateOgmUseCase.validate("+++SSS/SSSS/SSS59+++")

        assertTrue(result is OgmValidationResult.CorrectedValid)
        assertEquals("+++555/5555/55559+++", (result as OgmValidationResult.CorrectedValid).normalized)
    }

    @Test
    fun `corrects G to 6`() {
        // Base: 6666666666 % 97 = 32
        val result = ValidateOgmUseCase.validate("+++GGG/GGGG/GGG32+++")

        assertTrue(result is OgmValidationResult.CorrectedValid)
        assertEquals("+++666/6666/66632+++", (result as OgmValidationResult.CorrectedValid).normalized)
    }

    // =========================================================================
    // Helper function tests
    // =========================================================================

    @Test
    fun `looksLikeOgm returns true for OGM-like strings`() {
        assertTrue(ValidateOgmUseCase.looksLikeOgm("+++123/4567/89012+++"))
        assertTrue(ValidateOgmUseCase.looksLikeOgm("***123/4567/89012***"))
        assertTrue(ValidateOgmUseCase.looksLikeOgm("   +++123/4567/89012+++   "))
    }

    @Test
    fun `looksLikeOgm returns false for non-OGM strings`() {
        assertFalse(ValidateOgmUseCase.looksLikeOgm("RF12345"))
        assertFalse(ValidateOgmUseCase.looksLikeOgm("123456789"))
        assertFalse(ValidateOgmUseCase.looksLikeOgm(""))
    }

    @Test
    fun `isValid returns correct boolean`() {
        val valid = ValidateOgmUseCase.validate("+++012/3456/78939+++")
        assertTrue(valid.isValid)

        val invalid = ValidateOgmUseCase.validate("+++012/3456/78999+++")
        assertFalse(invalid.isValid)
    }

    @Test
    fun `normalizedOrNull returns value for valid OGM`() {
        val result = ValidateOgmUseCase.validate("+++012/3456/78939+++")
        assertEquals("+++012/3456/78939+++", result.normalizedOrNull)
    }

    @Test
    fun `normalizedOrNull returns null for invalid OGM`() {
        val result = ValidateOgmUseCase.validate("invalid")
        assertEquals(null, result.normalizedOrNull)
    }
}
