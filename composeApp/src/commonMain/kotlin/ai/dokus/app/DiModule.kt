package ai.dokus.app

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.auth.database.AuthDatabase
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.database.LocalDatabaseCleaner
import ai.dokus.app.local.DefaultLocalDatabaseCleaner
import ai.dokus.app.auth.datasource.TeamRemoteDataSource
import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.infrastructure.ServerConfigManagerImpl
import ai.dokus.app.viewmodel.AppVersionCheckViewModel
import ai.dokus.app.viewmodel.BootstrapViewModel
import ai.dokus.app.viewmodel.DashboardViewModel
import ai.dokus.app.viewmodel.HomeViewModel
import ai.dokus.app.viewmodel.SettingsViewModel
import ai.dokus.app.viewmodel.TeamSettingsViewModel
import ai.dokus.app.viewmodel.WorkspaceSettingsViewModel
import ai.dokus.foundation.design.style.ThemeManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DynamicDokusEndpointProvider
import ai.dokus.foundation.domain.config.ServerConfigManager
import ai.dokus.foundation.domain.flags.FeatureFlagService
import androidx.lifecycle.SavedStateHandle
import com.russhwolf.settings.Settings
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val diModuleApp = module {
    // Server configuration management (bridges platform settings with domain types)
    single<ServerConfigManager> { ServerConfigManagerImpl(get<Settings>()) }

    // Dynamic endpoint provider (bridges server config to HTTP clients)
    single<DynamicDokusEndpointProvider> { DynamicDokusEndpointProvider(get<ServerConfigManager>()) }

    // Theme management (singleton)
    single { ThemeManager() }

    viewModel<BootstrapViewModel> {
        BootstrapViewModel(
            get<AuthInitializer>(),
            get<TokenManager>(),
            get<ServerConfigManager>(),
        )
    }
    viewModel { AppVersionCheckViewModel() }
    viewModel { DashboardViewModel(get<GetCurrentTenantUseCase>(), get<WatchPendingDocumentsUseCase>()) }
    viewModel { HomeViewModel(SavedStateHandle.createHandle(null, null)) }
    viewModel { SettingsViewModel(get<GetCurrentTenantUseCase>()) }
    viewModel { WorkspaceSettingsViewModel(get<GetCurrentTenantUseCase>(), get<TenantRemoteDataSource>()) }
    viewModel { TeamSettingsViewModel(get<TeamRemoteDataSource>()) }

    single<FeatureFlagService> { FeatureFlagService.defaultsOnly }
    single<LocalDatabaseCleaner> {
        DefaultLocalDatabaseCleaner(
            authDatabase = get<AuthDatabase>(),
        )
    }
}

internal val diModuleUseCases = module {
    // UseCases are now wired in their respective feature modules
}
