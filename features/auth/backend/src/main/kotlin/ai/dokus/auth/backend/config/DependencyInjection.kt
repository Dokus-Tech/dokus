package ai.dokus.auth.backend.config

import ai.dokus.auth.backend.database.repository.OrganizationRepository
import ai.dokus.auth.backend.database.repository.PasswordResetTokenRepository
import ai.dokus.auth.backend.database.repository.RefreshTokenRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.auth.backend.database.tables.OrganizationMembersTable
import ai.dokus.auth.backend.database.tables.OrganizationSettingsTable
import ai.dokus.auth.backend.database.tables.OrganizationTable
import ai.dokus.auth.backend.database.tables.PasswordResetTokensTable
import ai.dokus.auth.backend.database.tables.RefreshTokensTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.auth.backend.jobs.RateLimitCleanupJob
import ai.dokus.auth.backend.rpc.AuthValidationRemoteServiceImpl
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.auth.backend.services.DisabledEmailService
import ai.dokus.auth.backend.services.EmailConfig
import ai.dokus.auth.backend.services.EmailService
import ai.dokus.auth.backend.services.EmailVerificationService
import ai.dokus.auth.backend.services.PasswordResetService
import ai.dokus.auth.backend.services.RateLimitService
import ai.dokus.auth.backend.services.SmtpEmailService
import ai.dokus.foundation.domain.rpc.AuthValidationRemoteService
import ai.dokus.foundation.ktor.DokusRabbitMq
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService
import ai.dokus.foundation.ktor.crypto.PasswordCryptoService4j
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.foundation.ktor.security.JwtGenerator
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.messaging.integration.createDefaultRabbitMqConfig
import ai.dokus.foundation.messaging.integration.messagingModule
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // Database
    single {
        DatabaseFactory(get(), "auth-pool").apply {
            runBlocking {
                init(
                    OrganizationTable,
                    OrganizationSettingsTable,
                    UsersTable,
                    OrganizationMembersTable,
                    RefreshTokensTable,
                    PasswordResetTokensTable
                )
            }
        }
    }

    // Password crypto service
    single<PasswordCryptoService> { PasswordCryptoService4j() }

    // Repositories (data access layer)
    single<OrganizationRepository> { OrganizationRepository() }
    single<UserRepository> { UserRepository(get()) }
    single<RefreshTokenRepository> { RefreshTokenRepository() }
    single<PasswordResetTokenRepository> { PasswordResetTokenRepository() }

    // JWT token generation
    single {
        val appConfig = get<AppBaseConfig>()
        JwtGenerator(
            secret = appConfig.jwt.secret,
            issuer = appConfig.jwt.issuer
        )
    }

    // JWT token validation
    single {
        val appConfig = get<AppBaseConfig>()
        JwtValidator(
            secret = appConfig.jwt.secret,
            envIssuer = appConfig.jwt.issuer
        )
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
            get<EmailService>()
        )
    }

    // Rate limit service - prevents brute force attacks
    single { RateLimitService() }

    // Background cleanup job for rate limiting
    single { RateLimitCleanupJob(get()) }

    // Authentication service
    single { AuthService(get(), get(), get(), get(), get(), get()) }

    // RPC API implementations
    single<AuthValidationRemoteService> { AuthValidationRemoteServiceImpl(get(), get()) }
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
            appModule,
            redisModule(appConfig, RedisNamespace.Auth),
            messagingModule(rabbitmqConfig, "auth"),
            rpcClientModule
        )
    }
}