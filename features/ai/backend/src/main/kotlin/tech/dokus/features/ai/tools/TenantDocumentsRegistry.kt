package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.ToolRegistry
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.features.ai.services.DocumentImageService

object TenantDocumentsRegistry {
    data class Args(
        val tenantId: TenantId
    )

    operator fun invoke(
        tenantId: TenantId,
        documentFetcher: DocumentFetcher,
        imageService: DocumentImageService
    ) = ToolRegistry {}
}