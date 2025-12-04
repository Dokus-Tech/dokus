package ai.dokus.ai.config

import ai.dokus.ai.service.AIService
import com.typesafe.config.Config
import org.koin.dsl.module
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AIModule")

/**
 * Koin module for AI service dependencies.
 *
 * Usage:
 * ```kotlin
 * install(Koin) {
 *     modules(
 *         aiModule(appConfig.config),
 *         // ... other modules
 *     )
 * }
 * ```
 */
fun aiModule(config: Config) = module {
    // AI Configuration
    single {
        val aiConfig = AIConfig.fromConfigOrNull(config)
        if (aiConfig != null) {
            logger.info("AI configured: provider=${aiConfig.defaultProvider}")
            aiConfig
        } else {
            logger.info("AI not configured, using local defaults")
            AIConfig.localDefault()
        }
    }

    // AI Service
    single {
        AIService(get())
    }
}

/**
 * Create AI module with explicit configuration.
 */
fun aiModule(config: AIConfig) = module {
    single { config }
    single { AIService(get()) }
}
