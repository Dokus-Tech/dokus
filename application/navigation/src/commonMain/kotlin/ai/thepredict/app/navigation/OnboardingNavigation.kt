package ai.thepredict.app.navigation

import cafe.adriel.voyager.core.registry.ScreenProvider

sealed interface OnboardingNavigation : ScreenProvider {
    sealed interface Authorization : OnboardingNavigation {
        data object LoginScreen : Authorization
        data object RegisterScreen : Authorization
        data object ForgotPasswordScreen : Authorization

        data object NewPasswordScreen : Authorization
    }

    sealed interface Configuration : OnboardingNavigation {
        data object ServerConnectionScreen : Configuration
    }

    sealed interface Workspaces : OnboardingNavigation {
        data object All : Workspaces
        data object Create : Workspaces
    }
}