package ai.dokus.app.auth.di

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.auth.viewmodel.CompanyCreateViewModel
import ai.dokus.app.auth.viewmodel.CompanySelectViewModel
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
    viewModel { CompanySelectViewModel() }
    viewModel { CompanyCreateViewModel() }
}