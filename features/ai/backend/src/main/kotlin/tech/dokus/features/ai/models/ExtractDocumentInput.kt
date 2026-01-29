package tech.dokus.features.ai.models

import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentType

@Serializable
data class ExtractDocumentInput(
    @property:LLMDescription("The detected document type")
    val documentType: DocumentType,
    @property:LLMDescription("Detected language: nl, fr, en, or de")
    val language: String
)