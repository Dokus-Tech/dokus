package tech.dokus.features.ai.graph.nodes

import tech.dokus.domain.enums.DocumentType

data class ExtractDocumentInput(
    val documentType: DocumentType,
    val language: String
)
