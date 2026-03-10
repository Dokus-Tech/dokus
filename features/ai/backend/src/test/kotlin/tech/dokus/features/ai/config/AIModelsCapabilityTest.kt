package tech.dokus.features.ai.config

import ai.koog.prompt.llm.LLMCapability
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import tech.dokus.foundation.backend.config.IntelligenceMode

class AIModelsCapabilityTest {

    @Test
    fun `all configured models declare temperature support`() {
        val models = AIModels.forMode(IntelligenceMode.Sovereign)

        assertTrue(LLMCapability.Temperature in models.orchestrator.capabilities.orEmpty())
        assertTrue(LLMCapability.Temperature in models.vision.capabilities.orEmpty())
        assertTrue(LLMCapability.Temperature in models.chat.capabilities.orEmpty())
    }

    @Test
    fun `tool capabilities are only declared on tool-enabled models`() {
        val models = AIModels.forMode(IntelligenceMode.Sovereign)

        assertTrue(LLMCapability.Tools in models.orchestrator.capabilities.orEmpty())
        assertTrue(LLMCapability.ToolChoice in models.orchestrator.capabilities.orEmpty())
        assertTrue(LLMCapability.Tools in models.vision.capabilities.orEmpty())
        assertTrue(LLMCapability.ToolChoice in models.vision.capabilities.orEmpty())

        assertFalse(LLMCapability.Tools in models.chat.capabilities.orEmpty())
        assertFalse(LLMCapability.ToolChoice in models.chat.capabilities.orEmpty())
    }
}
