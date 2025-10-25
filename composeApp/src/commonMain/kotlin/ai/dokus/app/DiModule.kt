package ai.dokus.app

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.core.viewmodel.HealthStatusViewModel
import ai.dokus.app.viewmodel.AppVersionCheckViewModel
import ai.dokus.app.viewmodel.BootstrapViewModel
import ai.dokus.app.viewmodel.HomeViewModel
import ai.dokus.foundation.domain.flags.FeatureFlagService
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.domain.rpc.HealthRemoteService
import ai.dokus.foundation.domain.usecases.GetCombinedHealthStatusUseCase
import androidx.lifecycle.SavedStateHandle
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val diModuleApp = module {
    viewModel<BootstrapViewModel> {
        BootstrapViewModel(
            get<AuthInitializer>(),
        )
    }
    viewModel { AppVersionCheckViewModel() }
    viewModel { HomeViewModel(SavedStateHandle.createHandle(null, null)) }

    viewModel { HealthStatusViewModel(get<GetCombinedHealthStatusUseCase>()) }

    single<FeatureFlagService> { FeatureFlagService.defaultsOnly }
}

internal val diModuleUseCases = module {
    factory {
        GetCombinedHealthStatusUseCase(
            authHealthRemoteService = get<HealthRemoteService>(named(Feature.Auth)),
            expenseHealthRemoteService = get<HealthRemoteService>(named(Feature.Expense)),
            invoicingHealthRemoteService = get<HealthRemoteService>(named(Feature.Invoicing)),
        )
    }
}