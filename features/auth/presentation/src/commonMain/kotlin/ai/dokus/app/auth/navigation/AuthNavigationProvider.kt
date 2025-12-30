package ai.dokus.app.auth.navigation

import ai.dokus.app.auth.screen.*
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.AuthDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

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
        composable<AuthDestination.WorkspaceCreate> {
            WorkspaceCreateScreen()
        }
        composable<AuthDestination.WorkspaceSelect> {
            WorkspaceSelectScreen()
        }
        composable<AuthDestination.ProfileSettings> {
            ProfileSettingsScreen()
        }
        composable<AuthDestination.ServerConnection> { entry ->
            val route = entry.toRoute<AuthDestination.ServerConnection>()
            ServerConnectionScreen(
                host = route.host,
                port = route.port,
                protocol = route.protocol
            )
        }
    }
}
