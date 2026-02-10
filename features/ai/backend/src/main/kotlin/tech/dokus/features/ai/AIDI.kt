package tech.dokus.features.ai

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import tech.dokus.features.ai.agents.ChatAgent
import tech.dokus.features.ai.agents.DocumentProcessingAgent
import tech.dokus.features.ai.config.AIModels
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.config.ModelSet
import tech.dokus.features.ai.prompts.ChatPrompt
import tech.dokus.features.ai.services.ChunkingService
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.services.DocumentImageCache
import tech.dokus.features.ai.services.EmbeddingService
import tech.dokus.features.ai.services.RAGService
import tech.dokus.features.ai.services.RedisDocumentImageCache
import tech.dokus.features.ai.tools.TenantDocumentsRegistry
import tech.dokus.foundation.backend.cache.RedisClient
import tech.dokus.foundation.backend.cache.RedisNamespace
import tech.dokus.foundation.backend.cache.redis
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.CachingConfig

fun aiModule() = module {
    // =========================================================================
    // AI Infrastructure
    // =========================================================================

    // Redis cache for vision image processing
    single<RedisClient>(named("ai-cache")) {
        val cachingConfig = get<CachingConfig>()
        redis {
            config = cachingConfig.redis
            namespace = RedisNamespace.Ai
        }
    }
    single<DocumentImageCache> { RedisDocumentImageCache(get(named("ai-cache"))) }

    // Koog prompt executor (shared across AI features)
    single<PromptExecutor> {
        AIProviderFactory.createOpenAiExecutor(get<AIConfig>())
    }

    // Model set derived from AI config
    single { AIModels.forMode(get<AIConfig>().mode) }

    // =========================================================================
    // AI Services
    // =========================================================================

    singleOf(::ChunkingService)
    singleOf(::EmbeddingService)
    singleOf(::RAGService)

    // =========================================================================
    // AI Agents
    // =========================================================================

    singleOf(::DocumentProcessingAgent)

    single {
        val models = get<ModelSet>()
        ChatAgent(get(), models.chat, get<RAGService>(), ChatPrompt)
    }

    // =========================================================================
    // Tool Registries
    // =========================================================================

    factory<ToolRegistry>(named<TenantDocumentsRegistry>()) { (args: TenantDocumentsRegistry.Args) ->
        TenantDocumentsRegistry(
            tenantId = args.tenantId,
            documentFetcher = get<DocumentFetcher>(),
        )
    }
}
