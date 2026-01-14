package tech.dokus.features.ai.retry

import tech.dokus.features.ai.validation.AuditCheck
import tech.dokus.features.ai.validation.CheckType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetryModelsTest {

    // =========================================================================
    // RetryResult Tests
    // =========================================================================

    @Test
    fun `NoRetryNeeded has no data`() {
        val result: RetryResult<String> = RetryResult.NoRetryNeeded

        assertNull(result.dataOrNull())
        assertTrue(result.isSuccess)
    }

    @Test
    fun `CorrectedOnRetry contains corrected data`() {
        val originalFailure = AuditCheck.criticalFailure(
            CheckType.MATH, "total", "Math error", "Check total"
        )

        val result = RetryResult.CorrectedOnRetry(
            data = "Corrected data",
            attempt = 1,
            correctedFields = listOf("total", "subtotal"),
            originalFailures = listOf(originalFailure)
        )

        assertEquals("Corrected data", result.dataOrNull())
        assertEquals(1, result.attempt)
        assertEquals(listOf("total", "subtotal"), result.correctedFields)
        assertEquals(1, result.originalFailures.size)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `StillFailing contains best attempt data`() {
        val remainingFailure = AuditCheck.criticalFailure(
            CheckType.CHECKSUM_OGM, "paymentReference", "Still invalid", "Check OGM"
        )

        val result = RetryResult.StillFailing(
            data = "Best effort data",
            attempts = 2,
            remainingFailures = listOf(remainingFailure)
        )

        assertEquals("Best effort data", result.dataOrNull())
        assertEquals(2, result.attempts)
        assertEquals(1, result.remainingFailures.size)
        assertFalse(result.isSuccess)
    }

    @Test
    fun `isSuccess is true for NoRetryNeeded and CorrectedOnRetry`() {
        val noRetry: RetryResult<String> = RetryResult.NoRetryNeeded
        val corrected = RetryResult.CorrectedOnRetry(
            data = "data",
            attempt = 1,
            correctedFields = emptyList(),
            originalFailures = emptyList()
        )
        val failing = RetryResult.StillFailing(
            data = "data",
            attempts = 2,
            remainingFailures = emptyList()
        )

        assertTrue(noRetry.isSuccess)
        assertTrue(corrected.isSuccess)
        assertFalse(failing.isSuccess)
    }

    // =========================================================================
    // RetryConfig Tests
    // =========================================================================

    @Test
    fun `default config has reasonable values`() {
        val config = RetryConfig.DEFAULT

        assertEquals(2, config.maxRetries)
        assertFalse(config.retryOnWarnings)
        assertEquals(0.0, config.minConfidenceImprovement)
    }

    @Test
    fun `aggressive config has more retries`() {
        val config = RetryConfig.AGGRESSIVE

        assertEquals(3, config.maxRetries)
        assertTrue(config.retryOnWarnings)
        assertTrue(config.minConfidenceImprovement > 0)
    }

    @Test
    fun `custom config can be created`() {
        val config = RetryConfig(
            maxRetries = 5,
            retryOnWarnings = true,
            minConfidenceImprovement = 0.1
        )

        assertEquals(5, config.maxRetries)
        assertTrue(config.retryOnWarnings)
        assertEquals(0.1, config.minConfidenceImprovement)
    }

    // =========================================================================
    // RetryResult Type Safety Tests
    // =========================================================================

    @Test
    fun `result type is preserved through sealed class`() {
        data class TestData(val value: String)

        val result: RetryResult<TestData> = RetryResult.CorrectedOnRetry(
            data = TestData("test"),
            attempt = 1,
            correctedFields = listOf("value"),
            originalFailures = emptyList()
        )

        assertIs<RetryResult.CorrectedOnRetry<TestData>>(result)
        assertEquals("test", result.data.value)
    }

    @Test
    fun `NoRetryNeeded works with any type parameter`() {
        // NoRetryNeeded is Nothing-typed, so it works with any expected type
        val stringResult: RetryResult<String> = RetryResult.NoRetryNeeded
        val intResult: RetryResult<Int> = RetryResult.NoRetryNeeded

        assertNull(stringResult.dataOrNull())
        assertNull(intResult.dataOrNull())
    }
}
