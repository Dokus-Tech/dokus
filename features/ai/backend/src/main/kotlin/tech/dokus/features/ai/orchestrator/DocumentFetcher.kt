package tech.dokus.features.ai.orchestrator

import tech.dokus.features.ai.orchestrator.tools.GetDocumentImagesTool

fun interface DocumentFetcher {
    suspend operator fun invoke(documentId: String, tenantId: String): GetDocumentImagesTool.DocumentData?
}
