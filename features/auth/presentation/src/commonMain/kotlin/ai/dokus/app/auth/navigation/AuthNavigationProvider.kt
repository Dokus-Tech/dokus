package ai.dokus.app.auth.navigation

import ai.dokus.app.auth.screen.ForgotPasswordScreen
import ai.dokus.app.auth.screen.LoginScreen
import ai.dokus.app.auth.screen.NewPasswordScreen
import ai.dokus.app.auth.screen.RegisterScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.AuthDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object AuthNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<AuthDestination.Login> {
            LoginScreen()
        }
        composable<AuthDestination.Register> {
            RegisterScreen()
        }
        composable<AuthDestination.ForgotPassword> {
            ForgotPasswordScreen()
        }
        composable<AuthDestination.PasswordChangeRequested> {
            NewPasswordScreen()
        }
    }
}
