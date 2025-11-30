package ai.dokus.app

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.auth.database.AuthDatabase
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.app.core.database.LocalDatabaseCleaner
import ai.dokus.app.local.DefaultLocalDatabaseCleaner
import ai.dokus.app.viewmodel.AppVersionCheckViewModel
import ai.dokus.app.viewmodel.BootstrapViewModel
import ai.dokus.app.viewmodel.DashboardViewModel
import ai.dokus.app.viewmodel.HomeViewModel
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.flags.FeatureFlagService
import androidx.lifecycle.SavedStateHandle
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val diModuleApp = module {
    viewModel<BootstrapViewModel> {
        BootstrapViewModel(
            get<AuthInitializer>(),
            get<TokenManager>(),
        )
    }
    viewModel { AppVersionCheckViewModel() }
    viewModel { DashboardViewModel(get<GetCurrentTenantUseCase>()) }
    viewModel { HomeViewModel(SavedStateHandle.createHandle(null, null)) }

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
