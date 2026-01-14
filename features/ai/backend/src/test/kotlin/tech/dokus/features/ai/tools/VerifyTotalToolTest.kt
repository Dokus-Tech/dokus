package tech.dokus.features.ai.tools

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class VerifyTotalToolTest {

    @Test
    fun `valid math returns VALID`() = runTest {
        val result = VerifyTotalsTool.execute(
            VerifyTotalsTool.Args(
                subtotal = "100.00",
                vatAmount = "21.00",
                total = "121.00"
            )
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID, got: $result")
    }

    @Test
    fun `invalid math returns ERROR`() = runTest {
        val result = VerifyTotalsTool.execute(
            VerifyTotalsTool.Args(
                subtotal = "100.00",
                vatAmount = "21.00",
                total = "120.00" // Wrong!
            )
        )

        assertTrue(result.startsWith("ERROR:"), "Expected ERROR, got: $result")
        assertTrue(result.contains("121.00"), "Should suggest correct total")
    }

    @Test
    fun `tolerates small rounding differences`() = runTest {
        val result = VerifyTotalsTool.execute(
            VerifyTotalsTool.Args(
                subtotal = "100.00",
                vatAmount = "21.00",
                total = "121.01" // Within tolerance
            )
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID for small rounding, got: $result")
    }

    @Test
    fun `handles European decimal separator`() = runTest {
        val result = VerifyTotalsTool.execute(
            VerifyTotalsTool.Args(
                subtotal = "100,00",
                vatAmount = "21,00",
                total = "121,00"
            )
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID with comma separator, got: $result")
    }

    @Test
    fun `handles amounts with euro symbol`() = runTest {
        val result = VerifyTotalsTool.execute(
            VerifyTotalsTool.Args(
                subtotal = "€100.00",
                vatAmount = "€21.00",
                total = "€121.00"
            )
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID with euro symbols, got: $result")
    }

    @Test
    fun `invalid amount format returns error`() = runTest {
        val result = VerifyTotalsTool.execute(
            VerifyTotalsTool.Args(
                subtotal = "invalid",
                vatAmount = "21.00",
                total = "121.00"
            )
        )

        assertTrue(result.startsWith("ERROR:"), "Expected ERROR for invalid format, got: $result")
    }

    @Test
    fun `handles amounts with spaces`() = runTest {
        val result = VerifyTotalsTool.execute(
            VerifyTotalsTool.Args(
                subtotal = "1 000.00",
                vatAmount = "210.00",
                total = "1 210.00"
            )
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID with space thousands separator, got: $result")
    }

    @Test
    fun `large amounts work correctly`() = runTest {
        val result = VerifyTotalsTool.execute(
            VerifyTotalsTool.Args(
                subtotal = "999999.99",
                vatAmount = "209999.9979",
                total = "1209999.9879"
            )
        )

        assertTrue(result.startsWith("VALID:"), "Expected VALID for large amounts, got: $result")
    }
}
