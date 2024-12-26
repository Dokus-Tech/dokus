package ai.thepredict.app.onboarding

import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.app.onboarding.login.LoginScreen
import ai.thepredict.app.onboarding.register.RegisterScreen
import ai.thepredict.app.onboarding.restore.ForgotPasswordScreen
import ai.thepredict.app.onboarding.workspaces.WorkspaceSelectionScreen
import cafe.adriel.voyager.core.registry.screenModule

val onboardingScreensModule = screenModule {
    register<OnboardingNavigation.Authorization.LoginScreen> {
        LoginScreen()
    }

    register<OnboardingNavigation.Authorization.RegisterScreen> {
        RegisterScreen()
    }

    register<OnboardingNavigation.Authorization.ForgotPasswordScreen> {
        ForgotPasswordScreen()
    }

    register<OnboardingNavigation.Workspaces.WorkspacesSelectionScreen> {
        WorkspaceSelectionScreen()
    }
}