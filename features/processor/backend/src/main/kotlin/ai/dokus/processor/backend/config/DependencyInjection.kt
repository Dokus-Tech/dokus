package ai.dokus.processor.backend.config

import ai.dokus.foundation.database.di.repositoryModuleProcessor
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.config.MinioConfig
import ai.dokus.foundation.ktor.storage.DocumentStorageService
import ai.dokus.foundation.ktor.storage.MinioStorage
import ai.dokus.foundation.ktor.storage.ObjectStorage
import ai.dokus.processor.backend.extraction.AIConfig
import ai.dokus.processor.backend.extraction.ExtractionProviderFactory
import ai.dokus.processor.backend.worker.DocumentProcessingWorker
import ai.dokus.processor.backend.worker.WorkerConfig
import com.typesafe.config.ConfigFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ProcessorDependencyInjection")

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val processorConfig = loadProcessorConfig()

    install(Koin) {
        modules(
            configModule(appConfig, processorConfig),
            httpClientModule(),
            storageModule(appConfig),
            extractionModule(processorConfig),
            repositoryModuleProcessor,
            workerModule(processorConfig)
        )
    }
}

private fun configModule(appConfig: AppBaseConfig, processorConfig: ProcessorConfig) = module {
    single { appConfig }
    single { processorConfig }
}

private fun httpClientModule() = module {
    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            engine {
                requestTimeout = 120_000 // 2 minutes for AI calls
            }
        }
    }
}

private fun storageModule(appConfig: AppBaseConfig) = module {
    // MinIO Object Storage
    single<ObjectStorage> {
        val minioConfig = MinioConfig.loadOrNull(appConfig)
        if (minioConfig != null) {
            logger.info("MinIO storage configured: endpoint=${minioConfig.endpoint}, bucket=${minioConfig.bucket}")
            MinioStorage.create(minioConfig)
        } else {
            throw IllegalStateException("MinIO not configured. Processor requires object storage.")
        }
    }

    // MinIO Document Storage Service (high-level API)
    single { DocumentStorageService(get<ObjectStorage>()) }
}

private fun extractionModule(config: ProcessorConfig) = module {
    single {
        AIConfig(
            defaultProvider = config.ai.defaultProvider,
            openaiApiKey = config.ai.openai.apiKey,
            openaiModel = config.ai.openai.model,
            openaiBaseUrl = config.ai.openai.baseUrl,
            anthropicApiKey = config.ai.anthropic.apiKey,
            anthropicModel = config.ai.anthropic.model,
            anthropicBaseUrl = config.ai.anthropic.baseUrl,
            localBaseUrl = config.ai.local.baseUrl,
            localModel = config.ai.local.model
        )
    }
    single { ExtractionProviderFactory(get(), get()) }
}

private fun workerModule(config: ProcessorConfig) = module {
    single {
        WorkerConfig(
            pollingInterval = config.pollingInterval,
            batchSize = config.batchSize,
            maxAttempts = config.maxAttempts
        )
    }
    single {
        DocumentProcessingWorker(
            processingRepository = get(),
            documentStorage = get(),
            providerFactory = get(),
            config = get()
        )
    }
}

// Configuration classes
data class ProcessorConfig(
    val pollingInterval: Long = 5000L,
    val maxAttempts: Int = 3,
    val batchSize: Int = 10,
    val ai: AIProviderConfig = AIProviderConfig()
)

data class AIProviderConfig(
    val defaultProvider: String = "openai",
    val openai: OpenAIConfig = OpenAIConfig(),
    val anthropic: AnthropicConfig = AnthropicConfig(),
    val local: LocalConfig = LocalConfig()
)

data class OpenAIConfig(
    val apiKey: String = "",
    val model: String = "gpt-4o",
    val baseUrl: String = "https://api.openai.com/v1"
)

data class AnthropicConfig(
    val apiKey: String = "",
    val model: String = "claude-sonnet-4-20250514",
    val baseUrl: String = "https://api.anthropic.com"
)

data class LocalConfig(
    val baseUrl: String = "http://localhost:11434",
    val model: String = "llama3.2"
)

private fun loadProcessorConfig(): ProcessorConfig {
    val config = ConfigFactory.load()

    return if (config.hasPath("processor")) {
        val processor = config.getConfig("processor")
        ProcessorConfig(
            pollingInterval = processor.getLong("pollingInterval"),
            maxAttempts = processor.getInt("maxAttempts"),
            batchSize = processor.getInt("batchSize"),
            ai = AIProviderConfig(
                defaultProvider = processor.getString("ai.defaultProvider"),
                openai = OpenAIConfig(
                    apiKey = processor.getString("ai.openai.apiKey"),
                    model = processor.getString("ai.openai.model"),
                    baseUrl = processor.getString("ai.openai.baseUrl")
                ),
                anthropic = AnthropicConfig(
                    apiKey = processor.getString("ai.anthropic.apiKey"),
                    model = processor.getString("ai.anthropic.model"),
                    baseUrl = processor.getString("ai.anthropic.baseUrl")
                ),
                local = LocalConfig(
                    baseUrl = processor.getString("ai.local.baseUrl"),
                    model = processor.getString("ai.local.model")
                )
            )
        )
    } else {
        logger.warn("No processor config found, using defaults")
        ProcessorConfig()
    }
}
