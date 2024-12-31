package ai.thepredict.app.onboarding

import ai.thepredict.app.navigation.OnboardingNavigation
import ai.thepredict.app.onboarding.authentication.LoginScreen
import ai.thepredict.app.onboarding.authentication.RegisterScreen
import ai.thepredict.app.onboarding.authentication.restore.ForgotPasswordScreen
import ai.thepredict.app.onboarding.workspaces.create.WorkspaceCreateScreen
import ai.thepredict.app.onboarding.workspaces.overview.WorkspacesScreen
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

    register<OnboardingNavigation.Workspaces.All> {
        WorkspacesScreen()
    }

    register<OnboardingNavigation.Workspaces.Create> {
        WorkspaceCreateScreen()
    }
}