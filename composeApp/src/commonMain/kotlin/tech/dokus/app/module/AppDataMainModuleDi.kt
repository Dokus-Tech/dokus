package tech.dokus.app.module

import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.features.auth.initializer.AuthDataInitializer
import tech.dokus.features.auth.manager.AuthManagerMutable
import tech.dokus.features.auth.manager.TokenManagerMutable
import tech.dokus.features.contacts.initializer.ContactsDataInitializer
import tech.dokus.foundation.app.AppDataInitializer
import tech.dokus.foundation.app.AppDataModuleDi
import tech.dokus.foundation.app.SharedQualifiers
import tech.dokus.foundation.app.database.LocalDatabaseCleaner
import tech.dokus.foundation.app.network.ServerConnectionMonitor
import tech.dokus.foundation.app.network.createDynamicAuthenticatedHttpClient
import tech.dokus.foundation.app.network.createDynamicBaseHttpClient
import tech.dokus.foundation.platform.Logger
import tech.dokus.foundation.platform.platformModule

internal object AppDataMainModuleDi : AppDataModuleDi {
    override val platform = platformModule
    override val network = networkModule
    override val data = null
}

private val appLogger = Logger.withTag("AppDataMainModuleDi")

private val networkModule = module {
    // Connection monitor tracks connection state based on actual API request results
    singleOf(::ServerConnectionMonitor)
    single<AppDataInitializer> {
        AppDataInitializer(get<AuthDataInitializer>(), get<ContactsDataInitializer>())
    }

    single<HttpClient>(SharedQualifiers.httpClientNoAuth) {
        createDynamicBaseHttpClient(
            endpointProvider = get<DynamicDokusEndpointProvider>(),
            connectionMonitor = get<ServerConnectionMonitor>()
        )
    }

    single<HttpClient> {
        createDynamicAuthenticatedHttpClient(
            endpointProvider = get<DynamicDokusEndpointProvider>(),
            tokenManager = get<TokenManagerMutable>(),
            connectionMonitor = get<ServerConnectionMonitor>(),
            onAuthenticationFailed = {
                val authManager = get<AuthManagerMutable>()
                val tokenManager = get<TokenManagerMutable>()
                // Fast in-memory state changes first — unblocks the HTTP pipeline
                tokenManager.onAuthenticationFailed()
                authManager.onAuthenticationFailed()
                // Best-effort DB cleanup — must not block the HTTP send coroutine
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    get<LocalDatabaseCleaner>().clearAll()
                        .onFailure { error ->
                            appLogger.w(error) { "Failed to clear local database during forced logout" }
                        }
                }
            }
        )
    }
}
