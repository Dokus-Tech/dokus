package tech.dokus.app.viewmodel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.app.navigation.HomeNavigationCommand
import tech.dokus.domain.Email
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.AppSurface
import tech.dokus.domain.model.auth.SurfaceAvailability
import tech.dokus.features.auth.AuthInitializer
import tech.dokus.features.auth.usecases.AuthSessionUseCase
import tech.dokus.features.auth.usecases.GetAccountMeUseCase
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BootstrapContainerTest {

    @Test
    fun `bookkeeper-console-only authenticated user opens console clients`() = runTest {
        withAuthInitializer(isAuthenticated = true) { authInitializer, _ ->
            val container = BootstrapContainer(
                authInitializer = authInitializer,
                tokenManager = BootstrapFakeTokenManager(selectedTenantId = null),
                serverConfigManager = BootstrapFakeServerConfigManager(),
                getAccountMeUseCase = BootstrapFakeGetAccountMeUseCase(
                    Result.success(
                        accountMe(
                            SurfaceAvailability(
                                canCompanyManager = false,
                                canBookkeeperConsole = true,
                                defaultSurface = AppSurface.BookkeeperConsole
                            )
                        )
                    )
                )
            )

            container.store.subscribeAndTest {
                BootstrapIntent.Load resultsIn BootstrapAction.NavigateToMain(
                    initialHomeCommand = HomeNavigationCommand.OpenConsoleClients
                )
            }
        }
    }

    @Test
    fun `company manager user without selected tenant goes to tenant selection`() = runTest {
        withAuthInitializer(isAuthenticated = true) { authInitializer, _ ->
            val container = BootstrapContainer(
                authInitializer = authInitializer,
                tokenManager = BootstrapFakeTokenManager(selectedTenantId = null),
                serverConfigManager = BootstrapFakeServerConfigManager(),
                getAccountMeUseCase = BootstrapFakeGetAccountMeUseCase(
                    Result.success(
                        accountMe(
                            SurfaceAvailability(
                                canCompanyManager = true,
                                canBookkeeperConsole = true,
                                defaultSurface = AppSurface.CompanyManager
                            )
                        )
                    )
                )
            )

            container.store.subscribeAndTest {
                BootstrapIntent.Load resultsIn BootstrapAction.NavigateToTenantSelection
            }
        }
    }

    @Test
    fun `company manager user with selected tenant opens main`() = runTest {
        withAuthInitializer(isAuthenticated = true) { authInitializer, _ ->
            val container = BootstrapContainer(
                authInitializer = authInitializer,
                tokenManager = BootstrapFakeTokenManager(
                    selectedTenantId = TenantId("00000000-0000-0000-0000-000000000777")
                ),
                serverConfigManager = BootstrapFakeServerConfigManager(),
                getAccountMeUseCase = BootstrapFakeGetAccountMeUseCase(
                    Result.success(
                        accountMe(
                            SurfaceAvailability(
                                canCompanyManager = true,
                                canBookkeeperConsole = true,
                                defaultSurface = AppSurface.CompanyManager
                            )
                        )
                    )
                )
            )

            container.store.subscribeAndTest {
                BootstrapIntent.Load resultsIn BootstrapAction.NavigateToMain()
            }
        }
    }

    @Test
    fun `account me failure falls back to current tenant selection behavior`() = runTest {
        withAuthInitializer(isAuthenticated = true) { authInitializer, _ ->
            val container = BootstrapContainer(
                authInitializer = authInitializer,
                tokenManager = BootstrapFakeTokenManager(selectedTenantId = null),
                serverConfigManager = BootstrapFakeServerConfigManager(),
                getAccountMeUseCase = BootstrapFakeGetAccountMeUseCase(
                    Result.failure(IllegalStateException("surface unavailable"))
                )
            )

            container.store.subscribeAndTest {
                BootstrapIntent.Load resultsIn BootstrapAction.NavigateToTenantSelection
            }
        }
    }

    @Test
    fun `routes to login when token manager becomes unauthenticated before tenant decision`() = runTest {
        withAuthInitializer(isAuthenticated = true) { authInitializer, _ ->
            val container = BootstrapContainer(
                authInitializer = authInitializer,
                tokenManager = BootstrapFakeTokenManager(
                    isAuthenticated = false,
                    selectedTenantId = null
                ),
                serverConfigManager = BootstrapFakeServerConfigManager(),
                getAccountMeUseCase = BootstrapFakeGetAccountMeUseCase(
                    Result.success(
                        accountMe(
                            SurfaceAvailability(
                                canCompanyManager = true,
                                canBookkeeperConsole = true,
                                defaultSurface = AppSurface.CompanyManager
                            )
                        )
                    )
                )
            )

            container.store.subscribeAndTest {
                BootstrapIntent.Load resultsIn BootstrapAction.NavigateToLogin
            }
        }
    }

    @Test
    fun `routes to login when auth becomes invalid during tenant lookup before main routing`() = runTest {
        withAuthInitializer(isAuthenticated = true) { authInitializer, _ ->
            val container = BootstrapContainer(
                authInitializer = authInitializer,
                tokenManager = BootstrapFakeTokenManager(
                    isAuthenticated = true,
                    selectedTenantId = TenantId.generate(),
                    invalidateOnTenantLookup = true
                ),
                serverConfigManager = BootstrapFakeServerConfigManager(),
                getAccountMeUseCase = BootstrapFakeGetAccountMeUseCase(
                    Result.success(
                        accountMe(
                            SurfaceAvailability(
                                canCompanyManager = true,
                                canBookkeeperConsole = true,
                                defaultSurface = AppSurface.CompanyManager
                            )
                        )
                    )
                )
            )

            container.store.subscribeAndTest {
                BootstrapIntent.Load resultsIn BootstrapAction.NavigateToLogin
            }
        }
    }

    private suspend fun withAuthInitializer(
        isAuthenticated: Boolean,
        block: suspend (AuthInitializer, BootstrapFakeAuthSessionUseCase) -> Unit
    ) {
        runCatching { stopKoin() }
        val authSessionUseCase = BootstrapFakeAuthSessionUseCase(isAuthenticated)
        startKoin {
            modules(
                module {
                    single<AuthSessionUseCase> { authSessionUseCase }
                }
            )
        }
        try {
            block(AuthInitializer(), authSessionUseCase)
        } finally {
            stopKoin()
        }
    }
}

private class BootstrapFakeAuthSessionUseCase(
    authenticated: Boolean
) : AuthSessionUseCase {
    override val isAuthenticated = MutableStateFlow(authenticated)

    override suspend fun initialize() = Unit
}

private class BootstrapFakeTokenManager(
    isAuthenticated: Boolean = true,
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

private class BootstrapFakeServerConfigManager : ServerConfigManager {
    override val currentServer = MutableStateFlow(ServerConfig.Cloud)
    override val isCloudServer = MutableStateFlow(true)

    override suspend fun setServer(config: ServerConfig) = Unit

    override suspend fun resetToCloud() = Unit

    override suspend fun initialize() = Unit
}

private class BootstrapFakeGetAccountMeUseCase(
    private val result: Result<AccountMeResponse>
) : GetAccountMeUseCase {
    override suspend fun invoke(): Result<AccountMeResponse> = result
}

private fun accountMe(surface: SurfaceAvailability): AccountMeResponse {
    return AccountMeResponse(
        user = User(
            id = UserId("00000000-0000-0000-0000-000000000111"),
            email = Email("bookkeeper@dokus.app"),
            firstName = null,
            lastName = null,
            emailVerified = true,
            isActive = true,
            lastLoginAt = null,
            createdAt = LocalDateTime(2026, 1, 1, 12, 0),
            updatedAt = LocalDateTime(2026, 1, 1, 12, 0)
        ),
        surface = surface
    )
}
