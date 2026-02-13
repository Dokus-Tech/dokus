package tech.dokus.app.viewmodel

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.features.auth.usecases.WatchCurrentTenantUseCase
import tech.dokus.features.auth.usecases.WatchCurrentUserUseCase
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
    private val watchCurrentTenantUseCase: WatchCurrentTenantUseCase,
    private val watchCurrentUserUseCase: WatchCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
) : Container<HomeState, HomeIntent, HomeAction> {

    private val logger = Logger.forClass<HomeContainer>()

    override val store: Store<HomeState, HomeIntent, HomeAction> =
        store(HomeState.Ready()) {
            init {
                launchObserveTenant()
                launchObserveUser()
            }

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
        logger.d { "Refreshing home shell data" }
        withState<HomeState.Ready, _> {
            updateState {
                copy(
                    tenantState = DokusState.loading(),
                    userState = DokusState.loading()
                )
            }
        }
        watchCurrentTenantUseCase.refresh()
        watchCurrentUserUseCase.refresh()
    }

    private suspend fun HomeCtx.launchObserveTenant() {
        launch {
            watchCurrentTenantUseCase().collectLatest { tenantResult ->
                tenantResult.fold(
                    onSuccess = { tenant ->
                        if (tenant == null) {
                            val error = DokusException.WorkspaceContextUnavailable
                            withState<HomeState.Ready, _> {
                                updateState {
                                    copy(
                                        tenantState = DokusState.error(error) {
                                            intent(HomeIntent.RefreshShellData)
                                        }
                                    )
                                }
                            }
                            action(HomeAction.ShowError(error))
                        } else {
                            withState<HomeState.Ready, _> {
                                updateState { copy(tenantState = DokusState.success(tenant)) }
                            }
                        }
                    },
                    onFailure = { throwable ->
                        val error = throwable.asDokusException
                        logger.e(error) { "Failed to observe current tenant for home shell" }
                        withState<HomeState.Ready, _> {
                            updateState {
                                copy(
                                    tenantState = DokusState.error(error) {
                                        intent(HomeIntent.RefreshShellData)
                                    }
                                )
                            }
                        }
                        action(HomeAction.ShowError(error))
                    }
                )
            }
        }
    }

    private suspend fun HomeCtx.launchObserveUser() {
        launch {
            watchCurrentUserUseCase().collectLatest { userResult ->
                userResult.fold(
                    onSuccess = { user ->
                        withState<HomeState.Ready, _> {
                            updateState { copy(userState = DokusState.success(user)) }
                        }
                    },
                    onFailure = { throwable ->
                        val error = throwable.asDokusException
                        logger.e(error) { "Failed to observe current user for home shell" }
                        withState<HomeState.Ready, _> {
                            updateState {
                                copy(
                                    userState = DokusState.error(error) {
                                        intent(HomeIntent.RefreshShellData)
                                    }
                                )
                            }
                        }
                        action(HomeAction.ShowError(error))
                    }
                )
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
