package ai.dokus.media.backend.config

import ai.dokus.foundation.ktor.DokusRabbitMq
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.messaging.core.MessagePublisher
import ai.dokus.foundation.messaging.integration.createDefaultRabbitMqConfig
import ai.dokus.foundation.messaging.integration.messagingModule
import ai.dokus.foundation.messaging.messages.MediaProcessingRequestedMessage
import ai.dokus.foundation.messaging.transport.RabbitMqTransport
import ai.dokus.foundation.messaging.transport.RabbitMqTransportConfig
import ai.dokus.media.backend.database.tables.MediaTable
import ai.dokus.media.backend.repository.MediaRepository
import ai.dokus.media.backend.storage.LocalMediaStorage
import ai.dokus.media.backend.storage.MediaStorage
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val rabbitConfig = createRabbitConfig(appConfig)

    install(Koin) {
        modules(
            coreModule(appConfig),
            databaseModule,
            mediaModule,
            messagingModule(rabbitConfig, "media"),
            mediaMessagingModule(rabbitConfig)
        )
    }
}

private fun coreModule(appConfig: AppBaseConfig) = module {
    single { appConfig }
    single { JwtValidator(appConfig.jwt) }
}

private val databaseModule = module {
    single {
        DatabaseFactory(get(), "media-pool").apply {
            runBlocking {
                init(MediaTable)
            }
        }
    }
}

private val mediaModule = module {
    single<MediaStorage> {
        LocalMediaStorage(
            storageBasePath = "./storage/media",
            maxFileSizeMb = 20
        )
    }
    single { MediaRepository() }
}

private fun mediaMessagingModule(
    rabbitConfig: RabbitMqTransportConfig
): Module = module {
    single { rabbitConfig }
    single<MessagePublisher<MediaProcessingRequestedMessage>> {
        val transport: RabbitMqTransport = get()
        transport.createPublisher(
            channelName = MediaProcessingRequestedMessage.channelName,
            serializer = serializer<MediaProcessingRequestedMessage>(),
            publisherConfig = rabbitConfig.publisher
        )
    }
}

private fun createRabbitConfig(appConfig: AppBaseConfig): RabbitMqTransportConfig {
    val rabbit = DokusRabbitMq.from(appConfig.rabbitmq)
    return createDefaultRabbitMqConfig(
        host = rabbit.host,
        port = rabbit.port,
        username = rabbit.username,
        password = rabbit.password,
        virtualHost = rabbit.virtualHost,
        serviceName = "media-service"
    )
}
