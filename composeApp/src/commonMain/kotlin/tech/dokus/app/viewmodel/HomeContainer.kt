package tech.dokus.app.viewmodel

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias HomeCtx = PipelineContext<HomeState, HomeIntent, HomeAction>

/**
 * Container for Home screen using FlowMVI.
 *
 * Manages the main navigation shell state. This is a lightweight container
 * since the Home screen is primarily a navigation container for:
 * - Bottom navigation (mobile)
 * - Navigation rail (desktop/tablet)
 * - Nested navigation host
 *
 * Also owns shell-level workspace/profile data and logout actions.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class HomeContainer(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
) : Container<HomeState, HomeIntent, HomeAction> {

    private val logger = Logger.forClass<HomeContainer>()

    override val store: Store<HomeState, HomeIntent, HomeAction> =
        store(HomeState.Ready()) {
            reduce { intent ->
                when (intent) {
                    is HomeIntent.ScreenAppeared -> handleScreenAppeared()
                    is HomeIntent.RefreshShellData -> handleRefreshShellData()
                    is HomeIntent.Logout -> handleLogout()
                }
            }
        }

    private suspend fun HomeCtx.handleScreenAppeared() {
        logger.d { "Home screen appeared" }
        handleRefreshShellData()
    }

    private suspend fun HomeCtx.handleRefreshShellData() {
        withState<HomeState.Ready, _> {
            logger.d { "Loading home shell data" }
            updateState {
                copy(
                    tenantState = DokusState.loading(),
                    userState = DokusState.loading()
                )
            }

            val tenantResult = getCurrentTenantUseCase()
            val userResult = getCurrentUserUseCase()

            updateState {
                copy(
                    tenantState = tenantResult.fold(
                        onSuccess = { tenant -> DokusState.success(tenant) },
                        onFailure = { error ->
                            DokusState.error(error.asDokusException) {
                                intent(HomeIntent.RefreshShellData)
                            }
                        }
                    ),
                    userState = userResult.fold(
                        onSuccess = { user -> DokusState.success(user) },
                        onFailure = { error ->
                            DokusState.error(error.asDokusException) {
                                intent(HomeIntent.RefreshShellData)
                            }
                        }
                    )
                )
            }

            tenantResult.exceptionOrNull()?.let { error ->
                logger.e(error) { "Failed to load current tenant for home shell" }
                action(HomeAction.ShowError(error.asDokusException))
            }

            userResult.exceptionOrNull()?.let { error ->
                logger.e(error) { "Failed to load current user for home shell" }
                action(HomeAction.ShowError(error.asDokusException))
            }
        }
    }

    private suspend fun HomeCtx.handleLogout() {
        withState<HomeState.Ready, _> {
            if (isLoggingOut) return@withState

            logger.d { "Logging out from home shell" }
            updateState { copy(isLoggingOut = true) }

            logoutUseCase().fold(
                onSuccess = {
                    logger.i { "Logout successful from home shell" }
                    updateState { copy(isLoggingOut = false) }
                },
                onFailure = { error ->
                    logger.e(error) { "Logout failed from home shell" }
                    updateState { copy(isLoggingOut = false) }
                    action(HomeAction.ShowError(error.asDokusException))
                }
            )
        }
    }
}
