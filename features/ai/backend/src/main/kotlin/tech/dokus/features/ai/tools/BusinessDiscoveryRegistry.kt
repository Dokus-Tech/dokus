package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import kotlinx.serialization.Serializable
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import io.ktor.client.HttpClient

object BusinessDiscoveryRegistry {
    @Serializable
    data class Args(
        val maxPages: Int,
        val ignoreRobots: Boolean,
    )

    operator fun invoke(
        args: Args,
        httpClient: HttpClient,
        config: BusinessProfileEnrichmentConfig,
    ): ToolRegistry = ToolRegistry {
        tools(
            BusinessDiscoveryTools(
                httpClient = httpClient,
                config = config,
                maxPages = args.maxPages,
                ignoreRobots = args.ignoreRobots
            )
        )
    }
}
