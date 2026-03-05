package tech.dokus.app.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.auth.AuthInitializer
import tech.dokus.features.auth.usecases.AuthSessionUseCase
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BootstrapContainerTest {

    @Test
    fun `routes to login when token manager becomes unauthenticated before tenant decision`() = runTest {
        val authSession = FakeAuthSessionUseCase(isAuthenticated = true)
        startKoin {
            modules(
                module {
                    single<AuthSessionUseCase> { authSession }
                }
            )
        }

        try {
            val container = BootstrapContainer(
                authInitializer = AuthInitializer(),
                tokenManager = FakeTokenManager(
                    isAuthenticated = false,
                    selectedTenantId = null
                ),
                serverConfigManager = FakeServerConfigManager()
            )

            container.store.subscribeAndTest {
                BootstrapIntent.Load resultsIn BootstrapAction.NavigateToLogin
            }
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `routes to login when auth becomes invalid during tenant lookup before main routing`() = runTest {
        val authSession = FakeAuthSessionUseCase(isAuthenticated = true)
        startKoin {
            modules(
                module {
                    single<AuthSessionUseCase> { authSession }
                }
            )
        }

        try {
            val container = BootstrapContainer(
                authInitializer = AuthInitializer(),
                tokenManager = FakeTokenManager(
                    isAuthenticated = true,
                    selectedTenantId = TenantId.generate(),
                    invalidateOnTenantLookup = true
                ),
                serverConfigManager = FakeServerConfigManager()
            )

            container.store.subscribeAndTest {
                BootstrapIntent.Load resultsIn BootstrapAction.NavigateToLogin
            }
        } finally {
            stopKoin()
        }
    }
}

private class FakeAuthSessionUseCase(isAuthenticated: Boolean) : AuthSessionUseCase {
    override val isAuthenticated = MutableStateFlow(isAuthenticated)

    override suspend fun initialize() = Unit
}

private class FakeTokenManager(
    isAuthenticated: Boolean,
    private val selectedTenantId: TenantId?,
    private val invalidateOnTenantLookup: Boolean = false
) : TokenManager {
    override val isAuthenticated = MutableStateFlow(isAuthenticated)

    override suspend fun getValidAccessToken(): String? = null

    override suspend fun getRefreshToken(): String? = null

    override suspend fun getSelectedTenantId(): TenantId? {
        if (invalidateOnTenantLookup) {
            isAuthenticated.value = false
        }
        return selectedTenantId
    }

    override suspend fun refreshToken(force: Boolean): String? = null

    override suspend fun onAuthenticationFailed() {
        isAuthenticated.value = false
    }
}

private class FakeServerConfigManager : ServerConfigManager {
    override val currentServer = MutableStateFlow(ServerConfig.Cloud)
    override val isCloudServer = MutableStateFlow(true)

    override suspend fun setServer(config: ServerConfig) = Unit

    override suspend fun resetToCloud() = Unit

    override suspend fun initialize() = Unit
}
