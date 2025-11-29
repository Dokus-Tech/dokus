package ai.dokus.app.auth.di

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.auth.domain.TenantRemoteService
import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.auth.usecases.SelectTenantUseCase
import ai.dokus.app.auth.viewmodel.WorkspaceCreateViewModel
import ai.dokus.app.auth.viewmodel.WorkspaceSelectViewModel
import ai.dokus.app.auth.viewmodel.ForgotPasswordViewModel
import ai.dokus.app.auth.viewmodel.LoginViewModel
import ai.dokus.app.auth.viewmodel.NewPasswordViewModel
import ai.dokus.app.auth.viewmodel.RegisterViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authPresentationModule = module {
    single<AuthInitializer> { AuthInitializer() }

    viewModel { LoginViewModel() }
    viewModel { RegisterViewModel() }
    viewModel { ForgotPasswordViewModel() }
    viewModel { NewPasswordViewModel() }
    viewModel { WorkspaceSelectViewModel(get<TenantRemoteService>(), get<SelectTenantUseCase>()) }
    viewModel { WorkspaceCreateViewModel(get<AuthRepository>()) }
}
