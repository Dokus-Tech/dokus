package tech.dokus.features.ai.coordinator

import org.junit.jupiter.api.Test
import tech.dokus.foundation.backend.config.AutonomyLevel
import tech.dokus.foundation.backend.config.IntelligenceMode
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for LLM judgment gating based on IntelligenceMode.
 *
 * Rule: LLM judgment is gated purely by autonomy level.
 * - Assisted (LOW autonomy): NEVER allow LLM judgment
 * - Autonomous (MEDIUM autonomy): Allow LLM judgment
 * - Sovereign (HIGH autonomy): Allow LLM judgment
 */
class LlmJudgmentGatingTest {

    @Test
    fun `Assisted mode should not allow LLM judgment`() {
        val mode = IntelligenceMode.Assisted

        // Gating is purely by autonomy level
        val allowLlmJudgment = mode.autonomyLevel != AutonomyLevel.LOW

        assertFalse(
            allowLlmJudgment,
            "Assisted mode (LOW autonomy) must not allow LLM judgment"
        )
    }

    @Test
    fun `Autonomous mode should allow LLM judgment`() {
        val mode = IntelligenceMode.Autonomous

        val allowLlmJudgment = mode.autonomyLevel != AutonomyLevel.LOW

        assertTrue(
            allowLlmJudgment,
            "Autonomous mode (MEDIUM autonomy) should allow LLM judgment"
        )
    }

    @Test
    fun `Sovereign mode should allow LLM judgment`() {
        val mode = IntelligenceMode.Sovereign

        val allowLlmJudgment = mode.autonomyLevel != AutonomyLevel.LOW

        assertTrue(
            allowLlmJudgment,
            "Sovereign mode (HIGH autonomy) should allow LLM judgment"
        )
    }

    @Test
    fun `Assisted mode has LOW autonomy level`() {
        val mode = IntelligenceMode.Assisted
        assertTrue(
            mode.autonomyLevel == AutonomyLevel.LOW,
            "Assisted mode should have LOW autonomy"
        )
    }

    @Test
    fun `Autonomous mode has MEDIUM autonomy level`() {
        val mode = IntelligenceMode.Autonomous
        assertTrue(
            mode.autonomyLevel == AutonomyLevel.MEDIUM,
            "Autonomous mode should have MEDIUM autonomy"
        )
    }

    @Test
    fun `Sovereign mode has HIGH autonomy level`() {
        val mode = IntelligenceMode.Sovereign
        assertTrue(
            mode.autonomyLevel == AutonomyLevel.HIGH,
            "Sovereign mode should have HIGH autonomy"
        )
    }
}
