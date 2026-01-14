package tech.dokus.features.ai.coordinator

import tech.dokus.features.ai.validation.CheckType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProcessingConfigTest {

    // =========================================================================
    // Default Configuration Tests
    // =========================================================================

    @Test
    fun `DEFAULT config has sensible defaults`() {
        val config = ProcessingConfig.DEFAULT

        assertTrue(config.enableEnsemble)
        assertTrue(config.parallelExtraction)
        assertTrue(config.enableSelfCorrection)
        assertTrue(config.enableExternalValidation)
        assertEquals(CheckType.entries.toSet(), config.enabledChecks)
        assertFalse(config.useLlmForJudgment)
        assertTrue(config.failFastOnUnknownType)
        assertFalse(config.includeProvenance)
        assertEquals(0.3, config.minClassificationConfidence)
    }

    // =========================================================================
    // Preset Configuration Tests
    // =========================================================================

    @Test
    fun `FAST config disables ensemble and self-correction`() {
        val config = ProcessingConfig.FAST

        assertFalse(config.enableEnsemble)
        assertFalse(config.enableSelfCorrection)
        assertFalse(config.enableExternalValidation)
    }

    @Test
    fun `THOROUGH config enables all features`() {
        val config = ProcessingConfig.THOROUGH

        assertTrue(config.enableEnsemble)
        assertTrue(config.parallelExtraction)
        assertTrue(config.enableSelfCorrection)
        assertTrue(config.includeProvenance)
    }

    @Test
    fun `OFFLINE config excludes external validation checks`() {
        val config = ProcessingConfig.OFFLINE

        assertFalse(config.enableExternalValidation)
        assertTrue(CheckType.MATH in config.enabledChecks)
        assertTrue(CheckType.CHECKSUM_OGM in config.enabledChecks)
        assertTrue(CheckType.CHECKSUM_IBAN in config.enabledChecks)
        assertTrue(CheckType.VAT_RATE in config.enabledChecks)
        assertFalse(CheckType.COMPANY_EXISTS in config.enabledChecks)
        assertFalse(CheckType.COMPANY_NAME in config.enabledChecks)
    }

    @Test
    fun `DEVELOPMENT config is optimized for testing`() {
        val config = ProcessingConfig.DEVELOPMENT

        assertFalse(config.enableEnsemble)
        assertFalse(config.enableSelfCorrection)
        assertFalse(config.enableExternalValidation)
        assertTrue(config.includeProvenance)
    }

    // =========================================================================
    // Validation Tests
    // =========================================================================

    @Test
    fun `validate returns empty list for valid config`() {
        val config = ProcessingConfig.DEFAULT

        val issues = config.validate()

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `validate detects invalid minClassificationConfidence below 0`() {
        val config = ProcessingConfig(minClassificationConfidence = -0.1)

        val issues = config.validate()

        assertTrue(issues.any { it.contains("minClassificationConfidence") })
    }

    @Test
    fun `validate detects invalid minClassificationConfidence above 1`() {
        val config = ProcessingConfig(minClassificationConfidence = 1.1)

        val issues = config.validate()

        assertTrue(issues.any { it.contains("minClassificationConfidence") })
    }

    @Test
    fun `validate detects empty enabled checks`() {
        val config = ProcessingConfig(enabledChecks = emptySet())

        val issues = config.validate()

        assertTrue(issues.any { it.contains("check type") })
    }

    @Test
    fun `validate detects negative maxRetries`() {
        val config = ProcessingConfig(
            retryConfig = tech.dokus.features.ai.retry.RetryConfig(maxRetries = -1)
        )

        val issues = config.validate()

        assertTrue(issues.any { it.contains("maxRetries") })
    }

    // =========================================================================
    // hasExternalChecks Property Tests
    // =========================================================================

    @Test
    fun `hasExternalChecks is true when external validation enabled and checks include company`() {
        val config = ProcessingConfig(
            enableExternalValidation = true,
            enabledChecks = setOf(CheckType.COMPANY_EXISTS, CheckType.MATH)
        )

        assertTrue(config.hasExternalChecks)
    }

    @Test
    fun `hasExternalChecks is false when external validation disabled`() {
        val config = ProcessingConfig(
            enableExternalValidation = false,
            enabledChecks = setOf(CheckType.COMPANY_EXISTS, CheckType.MATH)
        )

        assertFalse(config.hasExternalChecks)
    }

    @Test
    fun `hasExternalChecks is false when no company checks enabled`() {
        val config = ProcessingConfig(
            enableExternalValidation = true,
            enabledChecks = setOf(CheckType.MATH, CheckType.CHECKSUM_OGM)
        )

        assertFalse(config.hasExternalChecks)
    }
}
