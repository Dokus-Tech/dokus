package tech.dokus.features.ai.config

import ai.koog.prompt.params.LLMParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LLMParamsProfilesTest {

    @Test
    fun `finish tool only vision uses named tool choice`() {
        val params = LLMParams.finishToolOnlyVision("submit_classification")

        assertEquals(0.1, params.temperature)
        val toolChoice = assertIs<LLMParams.ToolChoice.Named>(params.toolChoice)
        assertEquals("submit_classification", toolChoice.name)
    }

    @Test
    fun `finish tool only text uses named tool choice`() {
        val params = LLMParams.finishToolOnlyText("submit_business_profile_content")

        assertEquals(0.1, params.temperature)
        val toolChoice = assertIs<LLMParams.ToolChoice.Named>(params.toolChoice)
        assertEquals("submit_business_profile_content", toolChoice.name)
    }

    @Test
    fun `external tool workflow requires tool calls`() {
        val params = LLMParams.externalToolWorkflow

        assertEquals(0.1, params.temperature)
        assertEquals(LLMParams.ToolChoice.Required, params.toolChoice)
    }

    @Test
    fun `chat profile disables tool calls`() {
        val params = LLMParams.chatNoTools

        assertEquals(0.1, params.temperature)
        assertEquals(LLMParams.ToolChoice.None, params.toolChoice)
    }
}
