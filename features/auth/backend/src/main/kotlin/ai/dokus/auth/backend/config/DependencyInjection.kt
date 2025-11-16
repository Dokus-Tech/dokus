package ai.dokus.auth.backend.config

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.database.services.RefreshTokenService
import ai.dokus.auth.backend.database.services.RefreshTokenServiceImpl
import ai.dokus.auth.backend.database.services.TenantServiceImpl
import ai.dokus.auth.backend.database.services.UserServiceImpl
import ai.dokus.auth.backend.database.tables.*
import ai.dokus.auth.backend.rpc.AccountRemoteServiceImpl
import ai.dokus.auth.backend.rpc.AuthValidationRemoteServiceImpl
import ai.dokus.auth.backend.security.JwtGenerator
import ai.dokus.auth.backend.security.JwtValidator
import ai.dokus.foundation.ktor.database.DatabaseFactory
import ai.dokus.auth.backend.services.*
import ai.dokus.auth.backend.jobs.RateLimitCleanupJob
import ai.dokus.foundation.domain.rpc.*
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.DokusRabbitMq
import ai.dokus.foundation.ktor.cache.RedisNamespace
import ai.dokus.foundation.ktor.cache.redisModule
import ai.dokus.foundation.ktor.services.TenantService
import ai.dokus.foundation.ktor.services.UserService
import ai.dokus.foundation.messaging.integration.createDefaultRabbitMqConfig
import ai.dokus.foundation.messaging.integration.messagingModule
import io.ktor.server.application.Application
import io.ktor.server.application.install
import kotlinx.coroutines.runBlocking
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin

private val appModule = module {
    // Database
    single {
        DatabaseFactory(get(), "auth-pool").apply {
            runBlocking {
                init(TenantsTable, TenantSettingsTable, UsersTable, RefreshTokensTable, PasswordResetTokensTable)
            }
        }
    }

    // Password crypto service
    single<ai.dokus.foundation.ktor.crypto.PasswordCryptoService> {
        ai.dokus.foundation.ktor.crypto.PasswordCryptoService4j()
    }

    // Local database services
    single<TenantService> { TenantServiceImpl() }
    single<UserService> { UserServiceImpl(get()) }
    single<RefreshTokenService> { RefreshTokenServiceImpl() }

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
            issuer = appConfig.jwt.issuer
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
    single { EmailVerificationService(get()) }

    // Password reset service
    single { PasswordResetService(get(), get(), get()) }

    // Rate limit service - prevents brute force attacks
    single { RateLimitService() }

    // Background cleanup job for rate limiting
    single { RateLimitCleanupJob(get()) }

    // Authentication service
    single { AuthService(get(), get(), get(), get(), get(), get(), get()) }

    // RPC API implementations
    single<AccountRemoteService> { AccountRemoteServiceImpl(get()) }
    single<AuthValidationRemoteService> { AuthValidationRemoteServiceImpl(get(), get()) }
    single<TenantApi> { TenantApiImpl(get()) }
    single<ClientApi> { ClientApiImpl(get()) }
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