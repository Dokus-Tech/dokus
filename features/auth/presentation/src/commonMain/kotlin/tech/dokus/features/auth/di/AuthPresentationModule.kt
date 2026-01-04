package tech.dokus.features.auth.di

import org.koin.dsl.module
import tech.dokus.features.auth.AuthInitializer
import tech.dokus.features.auth.mvi.ForgotPasswordAction
import tech.dokus.features.auth.mvi.ForgotPasswordContainer
import tech.dokus.features.auth.mvi.ForgotPasswordIntent
import tech.dokus.features.auth.mvi.ForgotPasswordState
import tech.dokus.features.auth.mvi.LoginAction
import tech.dokus.features.auth.mvi.LoginContainer
import tech.dokus.features.auth.mvi.LoginIntent
import tech.dokus.features.auth.mvi.LoginState
import tech.dokus.features.auth.mvi.NewPasswordAction
import tech.dokus.features.auth.mvi.NewPasswordContainer
import tech.dokus.features.auth.mvi.NewPasswordIntent
import tech.dokus.features.auth.mvi.NewPasswordState
import tech.dokus.features.auth.mvi.ProfileSettingsAction
import tech.dokus.features.auth.mvi.ProfileSettingsContainer
import tech.dokus.features.auth.mvi.ProfileSettingsIntent
import tech.dokus.features.auth.mvi.ProfileSettingsState
import tech.dokus.features.auth.mvi.RegisterAction
import tech.dokus.features.auth.mvi.RegisterContainer
import tech.dokus.features.auth.mvi.RegisterIntent
import tech.dokus.features.auth.mvi.RegisterState
import tech.dokus.features.auth.mvi.ServerConnectionAction
import tech.dokus.features.auth.mvi.ServerConnectionContainer
import tech.dokus.features.auth.mvi.ServerConnectionIntent
import tech.dokus.features.auth.mvi.ServerConnectionState
import tech.dokus.features.auth.mvi.WorkspaceCreateAction
import tech.dokus.features.auth.mvi.WorkspaceCreateContainer
import tech.dokus.features.auth.mvi.WorkspaceCreateIntent
import tech.dokus.features.auth.mvi.WorkspaceCreateState
import tech.dokus.features.auth.mvi.WorkspaceSelectAction
import tech.dokus.features.auth.mvi.WorkspaceSelectContainer
import tech.dokus.features.auth.mvi.WorkspaceSelectIntent
import tech.dokus.features.auth.mvi.WorkspaceSelectState
import tech.dokus.foundation.app.mvi.container

val authPresentationModule = module {
    single<AuthInitializer> { AuthInitializer() }

    // FlowMVI Containers
    container<LoginContainer, LoginState, LoginIntent, LoginAction> {
        LoginContainer(loginUseCase = get(), tokenManager = get())
    }
    container<RegisterContainer, RegisterState, RegisterIntent, RegisterAction> {
        RegisterContainer(registerAndLoginUseCase = get(), tokenManager = get())
    }
    container<ForgotPasswordContainer, ForgotPasswordState, ForgotPasswordIntent, ForgotPasswordAction> {
        ForgotPasswordContainer()
    }
    container<NewPasswordContainer, NewPasswordState, NewPasswordIntent, NewPasswordAction> {
        NewPasswordContainer()
    }
    container<WorkspaceSelectContainer, WorkspaceSelectState, WorkspaceSelectIntent, WorkspaceSelectAction> {
        WorkspaceSelectContainer(listMyTenants = get(), selectTenantUseCase = get())
    }
    container<WorkspaceCreateContainer, WorkspaceCreateState, WorkspaceCreateIntent, WorkspaceCreateAction> {
        WorkspaceCreateContainer(
            hasFreelancerTenant = get(),
            getCurrentUser = get(),
            createTenant = get(),
            searchCompanyUseCase = get()
        )
    }
    container<ProfileSettingsContainer, ProfileSettingsState, ProfileSettingsIntent, ProfileSettingsAction> {
        ProfileSettingsContainer(getCurrentUser = get(), updateProfile = get())
    }
    container<ServerConnectionContainer, ServerConnectionState, ServerConnectionIntent, ServerConnectionAction> {
            (params: ServerConnectionContainer.Companion.Params) ->
        ServerConnectionContainer(initialConfig = params.initialConfig, connectToServerUseCase = get())
    }
}
