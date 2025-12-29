package ai.dokus.app.auth.di

import ai.dokus.app.auth.AuthInitializer
import ai.dokus.app.auth.viewmodel.ForgotPasswordAction
import ai.dokus.app.auth.viewmodel.ForgotPasswordContainer
import ai.dokus.app.auth.viewmodel.ForgotPasswordIntent
import ai.dokus.app.auth.viewmodel.ForgotPasswordState
import ai.dokus.app.auth.viewmodel.LoginAction
import ai.dokus.app.auth.viewmodel.LoginContainer
import ai.dokus.app.auth.viewmodel.LoginIntent
import ai.dokus.app.auth.viewmodel.LoginState
import ai.dokus.app.auth.viewmodel.NewPasswordAction
import ai.dokus.app.auth.viewmodel.NewPasswordContainer
import ai.dokus.app.auth.viewmodel.NewPasswordIntent
import ai.dokus.app.auth.viewmodel.NewPasswordState
import ai.dokus.app.auth.viewmodel.ProfileSettingsAction
import ai.dokus.app.auth.viewmodel.ProfileSettingsContainer
import ai.dokus.app.auth.viewmodel.ProfileSettingsIntent
import ai.dokus.app.auth.viewmodel.ProfileSettingsState
import ai.dokus.app.auth.viewmodel.RegisterAction
import ai.dokus.app.auth.viewmodel.RegisterContainer
import ai.dokus.app.auth.viewmodel.RegisterIntent
import ai.dokus.app.auth.viewmodel.RegisterState
import ai.dokus.app.auth.viewmodel.ServerConnectionAction
import ai.dokus.app.auth.viewmodel.ServerConnectionContainer
import ai.dokus.app.auth.viewmodel.ServerConnectionIntent
import ai.dokus.app.auth.viewmodel.ServerConnectionState
import ai.dokus.app.auth.viewmodel.WorkspaceCreateAction
import ai.dokus.app.auth.viewmodel.WorkspaceCreateContainer
import ai.dokus.app.auth.viewmodel.WorkspaceCreateIntent
import ai.dokus.app.auth.viewmodel.WorkspaceCreateState
import ai.dokus.app.auth.viewmodel.WorkspaceSelectAction
import ai.dokus.app.auth.viewmodel.WorkspaceSelectContainer
import ai.dokus.app.auth.viewmodel.WorkspaceSelectIntent
import ai.dokus.app.auth.viewmodel.WorkspaceSelectState
import org.koin.dsl.module
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
        WorkspaceSelectContainer(tenantDataSource = get(), selectTenantUseCase = get())
    }
    container<WorkspaceCreateContainer, WorkspaceCreateState, WorkspaceCreateIntent, WorkspaceCreateAction> {
        WorkspaceCreateContainer(authRepository = get(), searchCompanyUseCase = get())
    }
    container<ProfileSettingsContainer, ProfileSettingsState, ProfileSettingsIntent, ProfileSettingsAction> {
        ProfileSettingsContainer(authRepository = get())
    }
    container<ServerConnectionContainer, ServerConnectionState, ServerConnectionIntent, ServerConnectionAction> { (params: ServerConnectionContainer.Companion.Params) ->
        ServerConnectionContainer(initialConfig = params.initialConfig, connectToServerUseCase = get())
    }
}
