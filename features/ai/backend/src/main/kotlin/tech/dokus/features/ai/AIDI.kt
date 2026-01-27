package tech.dokus.features.ai

import ai.koog.agents.core.tools.ToolRegistry
import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.features.ai.tools.TenantDocumentsRegistry
import tech.dokus.foundation.backend.config.AppBaseConfig

fun aiModule(appConfig: AppBaseConfig) = module {
    factory<ToolRegistry>(named<TenantDocumentsRegistry>()) { (args: TenantDocumentsRegistry.Args) ->
        TenantDocumentsRegistry(
            tenantId = args.tenantId,
            documentFetcher = get<DocumentFetcher>()
        )
    }
}