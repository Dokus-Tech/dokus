package tech.dokus.features.ai

import ai.koog.agents.core.tools.ToolRegistry
import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.tools.TenantDocumentsRegistry

fun aiModule() = module {
    factory<ToolRegistry>(named<TenantDocumentsRegistry>()) { (args: TenantDocumentsRegistry.Args) ->
        TenantDocumentsRegistry(
            tenantId = args.tenantId,
            documentFetcher = get<DocumentFetcher>(),
        )
    }
}
