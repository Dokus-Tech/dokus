package tech.dokus.features.ai.config

import ai.koog.prompt.params.LLMParams

fun LLMParams.Companion.finishToolOnlyVision(finishToolName: String) = LLMParams(
    temperature = 0.1,
    toolChoice = LLMParams.ToolChoice.Named(name = finishToolName)
)

fun LLMParams.Companion.finishToolOnlyText(finishToolName: String) = LLMParams(
    temperature = 0.1,
    toolChoice = LLMParams.ToolChoice.Named(name = finishToolName)
)

val LLMParams.Companion.externalToolWorkflow
    get() = LLMParams(temperature = 0.1, toolChoice = LLMParams.ToolChoice.Required)

val LLMParams.Companion.chatNoTools
    get() = LLMParams(temperature = 0.1, toolChoice = LLMParams.ToolChoice.None)

const val finishToolVisionAssistantResponseRepeatMax = 2
const val finishToolTextAssistantResponseRepeatMax = 1
const val externalToolAssistantResponseRepeatMax = 3
