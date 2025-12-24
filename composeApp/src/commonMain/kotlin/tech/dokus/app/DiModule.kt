package tech.dokus.app

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.auth.database.AuthDatabase
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.database.LocalDatabaseCleaner
import tech.dokus.app.local.DefaultLocalDatabaseCleaner
import ai.dokus.app.auth.datasource.TeamRemoteDataSource
import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import tech.dokus.app.infrastructure.ServerConfigManagerImpl
import tech.dokus.app.viewmodel.AppVersionCheckViewModel
import tech.dokus.app.viewmodel.BootstrapViewModel
import tech.dokus.app.viewmodel.DashboardViewModel
import tech.dokus.app.viewmodel.HomeViewModel
import tech.dokus.app.viewmodel.SettingsViewModel
import tech.dokus.app.viewmodel.TeamSettingsViewModel
import tech.dokus.app.viewmodel.WorkspaceSettingsViewModel
import ai.dokus.foundation.design.style.ThemeManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DynamicDokusEndpointProvider
import ai.dokus.foundation.domain.config.ServerConfigManager
import ai.dokus.foundation.domain.flags.FeatureFlagService
import androidx.lifecycle.SavedStateHandle
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import tech.dokus.foundation.app.SharedQualifiers
import tech.dokus.foundation.app.network.ServerConnectionMonitor

internal val diModuleApp = module {
    // Server configuration management (bridges platform settings with domain types)
    single<ServerConfigManager> { ServerConfigManagerImpl(get<Settings>()) }

    // Dynamic endpoint provider (bridges server config to HTTP clients)
    single<DynamicDokusEndpointProvider> { DynamicDokusEndpointProvider(get<ServerConfigManager>()) }

    // Server connection monitor (uses unauthenticated HTTP client to ping health endpoint)
    single { ServerConnectionMonitor(get<HttpClient>(SharedQualifiers.httpClientNoAuth)) }

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
    viewModel {
        DashboardViewModel(
            get<GetCurrentTenantUseCase>(),
            get<WatchPendingDocumentsUseCase>(),
        )
    }
    viewModel { HomeViewModel(SavedStateHandle.createHandle(null, null)) }
    viewModel { SettingsViewModel(get<GetCurrentTenantUseCase>()) }
    viewModel {
        WorkspaceSettingsViewModel(
            get<GetCurrentTenantUseCase>(),
            get<TenantRemoteDataSource>()
        )
    }
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
