package tech.dokus.app

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.app.infrastructure.ServerConfigManagerImpl
import tech.dokus.app.local.DefaultLocalDatabaseCleaner
import tech.dokus.app.viewmodel.BootstrapAction
import tech.dokus.app.viewmodel.BootstrapContainer
import tech.dokus.app.viewmodel.BootstrapIntent
import tech.dokus.app.viewmodel.BootstrapState
import tech.dokus.app.viewmodel.DashboardAction
import tech.dokus.app.viewmodel.DashboardContainer
import tech.dokus.app.viewmodel.DashboardIntent
import tech.dokus.app.viewmodel.DashboardState
import tech.dokus.app.viewmodel.HomeAction
import tech.dokus.app.viewmodel.HomeContainer
import tech.dokus.app.viewmodel.HomeIntent
import tech.dokus.app.viewmodel.HomeState
import tech.dokus.app.viewmodel.SettingsAction
import tech.dokus.app.viewmodel.SettingsContainer
import tech.dokus.app.viewmodel.SettingsIntent
import tech.dokus.app.viewmodel.SettingsState
import tech.dokus.app.viewmodel.TeamSettingsAction
import tech.dokus.app.viewmodel.TeamSettingsContainer
import tech.dokus.app.viewmodel.TeamSettingsIntent
import tech.dokus.app.viewmodel.TeamSettingsState
import tech.dokus.app.viewmodel.WorkspaceSettingsAction
import tech.dokus.app.viewmodel.WorkspaceSettingsContainer
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.flags.FeatureFlagService
import tech.dokus.foundation.app.database.LocalDatabaseCleaner
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.style.ThemeManager

internal val diModuleApp = module {
    // Server configuration management (bridges platform settings with domain types)
    singleOf(::ServerConfigManagerImpl) bind ServerConfigManager::class

    // Dynamic endpoint provider (bridges server config to HTTP clients)
    singleOf(::DynamicDokusEndpointProvider)

    // Note: ServerConnectionMonitor is now registered in AppDataMainModuleDi
    // and wired into HTTP clients for event-driven connection tracking

    // Theme management (singleton)
    singleOf(::ThemeManager)

    // FlowMVI Containers
    container<BootstrapContainer, BootstrapState, BootstrapIntent, BootstrapAction> {
        BootstrapContainer(
            authInitializer = get(),
            tokenManager = get(),
            serverConfigManager = get(),
        )
    }
    container<DashboardContainer, DashboardState, DashboardIntent, DashboardAction> {
        DashboardContainer(
            getCurrentTenantUseCase = get(),
            watchPendingDocuments = get(),
        )
    }
    container<HomeContainer, HomeState, HomeIntent, HomeAction> {
        HomeContainer()
    }
    container<SettingsContainer, SettingsState, SettingsIntent, SettingsAction> {
        SettingsContainer(getCurrentTenantUseCase = get())
    }
    container<WorkspaceSettingsContainer, WorkspaceSettingsState, WorkspaceSettingsIntent, WorkspaceSettingsAction> {
        WorkspaceSettingsContainer(
            getCurrentTenantUseCase = get(),
            tenantDataSource = get(),
        )
    }
    container<TeamSettingsContainer, TeamSettingsState, TeamSettingsIntent, TeamSettingsAction> {
        TeamSettingsContainer(teamDataSource = get())
    }

    single<FeatureFlagService> { FeatureFlagService.defaultsOnly }
    singleOf(::DefaultLocalDatabaseCleaner) bind LocalDatabaseCleaner::class
}

internal val diModuleUseCases = module {
    // UseCases are now wired in their respective feature modules
}
