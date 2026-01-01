package tech.dokus.features.auth.navigation

import tech.dokus.features.auth.presentation.auth.route.ForgotPasswordRoute
import tech.dokus.features.auth.presentation.auth.route.LoginRoute
import tech.dokus.features.auth.presentation.auth.route.NewPasswordRoute
import tech.dokus.features.auth.presentation.auth.route.ProfileSettingsRoute
import tech.dokus.features.auth.presentation.auth.route.RegisterRoute
import tech.dokus.features.auth.presentation.auth.route.WorkspaceCreateRoute
import tech.dokus.features.auth.presentation.auth.route.ServerConnectionRoute
import tech.dokus.features.auth.presentation.auth.route.WorkspaceSelectRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.AuthDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

internal object AuthNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<AuthDestination.Login> {
            LoginRoute()
        }
        composable<AuthDestination.Register> {
            RegisterRoute()
        }
        composable<AuthDestination.ForgotPassword> {
            ForgotPasswordRoute()
        }
        composable<AuthDestination.PasswordChangeRequested> {
            NewPasswordRoute()
        }
        composable<AuthDestination.WorkspaceCreate> {
            WorkspaceCreateRoute()
        }
        composable<AuthDestination.WorkspaceSelect> {
            WorkspaceSelectRoute()
        }
        composable<AuthDestination.ProfileSettings> {
            ProfileSettingsRoute()
        }
        composable<AuthDestination.ServerConnection> { entry ->
            val route = entry.toRoute<AuthDestination.ServerConnection>()
            ServerConnectionRoute(
                host = route.host,
                port = route.port,
                protocol = route.protocol
            )
        }
    }
}
