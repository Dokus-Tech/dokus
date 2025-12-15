package ai.dokus.app.auth.di

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.auth.usecases.SelectTenantUseCase
import ai.dokus.app.auth.viewmodel.WorkspaceCreateViewModel
import ai.dokus.app.auth.viewmodel.WorkspaceSelectViewModel
import ai.dokus.app.auth.viewmodel.ForgotPasswordViewModel
import ai.dokus.app.auth.viewmodel.LoginViewModel
import ai.dokus.app.auth.viewmodel.NewPasswordViewModel
import ai.dokus.app.auth.viewmodel.ProfileSettingsViewModel
import ai.dokus.app.auth.viewmodel.RegisterViewModel
import ai.dokus.app.auth.viewmodel.ServerConnectionViewModel
import ai.dokus.foundation.domain.config.ServerConfig
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authPresentationModule = module {
    single<AuthInitializer> { AuthInitializer() }

    viewModel { LoginViewModel() }
    viewModel { RegisterViewModel() }
    viewModel { ForgotPasswordViewModel() }
    viewModel { NewPasswordViewModel() }
    viewModel { WorkspaceSelectViewModel(get<TenantRemoteDataSource>(), get<SelectTenantUseCase>()) }
    viewModel { WorkspaceCreateViewModel(get<AuthRepository>()) }
    viewModel { ProfileSettingsViewModel() }
    viewModel { (initialConfig: ServerConfig?) -> ServerConnectionViewModel(initialConfig) }
}
