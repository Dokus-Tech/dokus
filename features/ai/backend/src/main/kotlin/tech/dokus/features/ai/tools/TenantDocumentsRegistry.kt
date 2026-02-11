package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.lookup.CbeApiClient

object TenantDocumentsRegistry {
    @Serializable
    data class Args(
        val tenantId: TenantId
    )

    operator fun invoke(
        tenantId: TenantId,
        cbeApiClient: CbeApiClient,
    ) = ToolRegistry {
        tools(LegalEntitiesTools(cbeApiClient))
    }
}