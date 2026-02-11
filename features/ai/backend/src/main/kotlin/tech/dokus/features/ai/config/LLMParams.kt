package tech.dokus.features.ai.config

import ai.koog.prompt.params.LLMParams

val LLMParams.Companion.documentProcessing
    get() = LLMParams(temperature = 0.1, toolChoice = LLMParams.ToolChoice.Required)