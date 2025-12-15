package ai.dokus.auth.backend.config

import ai.dokus.auth.backend.database.AuthTables
import ai.dokus.foundation.database.di.repositoryModuleAuth
import ai.dokus.foundation.database.repository.auth.PasswordResetTokenRepository
import ai.dokus.foundation.database.repository.auth.RefreshTokenRepository
import ai.dokus.foundation.database.repository.auth.UserRepository
import ai.dokus.auth.backend.jobs.RateLimitCleanupJob
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.auth.backend.services.DisabledEmailService
import ai.dokus.auth.backend.services.EmailConfig
import ai.dokus.auth.backend.services.EmailService
import ai.dokus.auth.backend.services.EmailVerificationService
import ai.dokus.auth.backend.services.PasswordResetService
import ai.dokus.auth.backend.services.RateLimitService
import ai.dokus.auth.backend.services.SmtpEmailService
import ai.dokus.auth.backend.services.TeamService
import ai.dokus.foundation.ktor.DokusRabbitMq
import ai.dokus.foundation.ktor.cache.RedisClient
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService4j
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtGenerator
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.ktor.security.InMemoryTokenBlacklistService
import ai.dokus.foundation.ktor.security.RedisTokenBlacklistService
import ai.dokus.foundation.ktor.security.TokenBlacklistService
import ai.dokus.foundation.messaging.integration.createDefaultRabbitMqConfig
import ai.dokus.foundation.messaging.integration.messagingModule
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.slf4j.LoggerFactory

private val appModule = module {
    // Database - connect and initialize auth-owned tables only
    single {
        DatabaseFactory(get(), "auth-pool").apply {
            runBlocking {
                connect()
                AuthTables.initialize()
            }
        }
    }

    // Password crypto service
    single<PasswordCryptoService> { PasswordCryptoService4j() }

    // JWT token generation
    single {
        val appConfig = get<AppBaseConfig>()
        JwtGenerator(appConfig.jwt)
    }

    // JWT token validation
    single {
        val appConfig = get<AppBaseConfig>()
        JwtValidator(appConfig.jwt)
    }

    // Email service (SMTP or disabled based on configuration)
    single<EmailService> {
        val appConfig = get<AppBaseConfig>()
        val emailConfig = EmailConfig.load(appConfig)

        if (emailConfig.enabled && emailConfig.provider == "smtp") {
            SmtpEmailService(emailConfig)
        } else {
            DisabledEmailService()
        }
    }

    // Email verification service
    single { EmailVerificationService(get<UserRepository>(), get<EmailService>()) }

    // Password reset service
    single {
        PasswordResetService(
            get<UserRepository>(),
            get<PasswordResetTokenRepository>(),
            get<RefreshTokenRepository>(),
            get<EmailService>(),
            getOrNull<TokenBlacklistService>()
        )
    }

    // Rate limit service - prevents brute force attacks
    single { RateLimitService() }

    // Background cleanup job for rate limiting
    single { RateLimitCleanupJob(get()) }

    // Token blacklist service - uses Redis if available, falls back to in-memory
    single<TokenBlacklistService> {
        val logger = LoggerFactory.getLogger("TokenBlacklistService")
        try {
            val redisClient = getOrNull<RedisClient>()
            if (redisClient != null) {
                logger.info("Using Redis-backed token blacklist service")
                RedisTokenBlacklistService(redisClient)
            } else {
                logger.warn("Redis not available, using in-memory token blacklist (not suitable for multi-instance)")
                InMemoryTokenBlacklistService()
            }
        } catch (e: Exception) {
            logger.warn("Failed to create Redis blacklist service, using in-memory fallback: ${e.message}")
            InMemoryTokenBlacklistService()
        }
    }

    // Authentication service
    single { AuthService(get(), get(), get(), get(), get(), get(), get()) }

    // Team management service
    single { TeamService(get(), get(), get()) }
}

fun Application.configureDependencyInjection(appConfig: AppBaseConfig) {
    val coreModule = module {
        single<AppBaseConfig> { appConfig }
    }

    // Load RabbitMQ configuration from typed config
    val rabbitMq = DokusRabbitMq.from(appConfig.rabbitmq)
    val rabbitmqConfig = createDefaultRabbitMqConfig(
        host = rabbitMq.host,
        port = rabbitMq.port,
        username = rabbitMq.username,
        password = rabbitMq.password,
        virtualHost = rabbitMq.virtualHost,
        serviceName = "auth-service"
    )

    install(Koin) {
        modules(
            coreModule,
            repositoryModuleAuth,
            appModule,
            redisModule(appConfig, RedisNamespace.Auth),
            messagingModule(rabbitmqConfig, "auth")
        )
    }
}