package ai.dokus.foundation.navigation

import ai.dokus.app.banking.screen.BankingScreen
import ai.dokus.app.cashflow.screen.CashflowScreen
import ai.dokus.app.contacts.screen.ContactsScreen
import ai.dokus.app.dashboard.screen.DashboardScreen
import ai.dokus.app.home.screen.HomeScreen
import ai.dokus.app.home.splash.SplashScreen
import ai.dokus.app.inventory.screen.InventoryScreen
import ai.dokus.app.onboarding.authentication.login.LoginScreen
import ai.dokus.app.onboarding.authentication.register.RegisterConfirmationScreen
import ai.dokus.app.onboarding.authentication.register.RegisterScreen
import ai.dokus.app.onboarding.authentication.restore.ForgotPasswordScreen
import ai.dokus.app.onboarding.authentication.restore.NewPasswordScreen
import ai.dokus.app.onboarding.server.ServerConnectionScreen
import ai.dokus.app.onboarding.workspaces.create.WorkspaceCreateScreen
import ai.dokus.app.onboarding.workspaces.overview.WorkspacesScreen
import ai.dokus.app.profile.screen.ProfileScreen
import ai.dokus.app.simulations.screen.SimulationScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Main navigation graph for the application
 */
@Composable
fun NavigationGraph(
    navController: NavHostController = rememberNavController()
) {
    val navigator = AppNavigator(navController)

    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH
    ) {
        // Splash Screen
        composable(AppRoutes.SPLASH) {
            SplashScreen(navigator)
        }

        // Authentication Flow
        composable(AppRoutes.LOGIN) {
            LoginScreen(navigator)
        }

        composable(AppRoutes.REGISTER) {
            RegisterScreen(navigator)
        }

        composable(AppRoutes.REGISTER_CONFIRMATION) {
            RegisterConfirmationScreen(navigator)
        }

        composable(AppRoutes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navigator)
        }

        composable(AppRoutes.NEW_PASSWORD) {
            NewPasswordScreen(navigator)
        }

        // Configuration
        composable(AppRoutes.SERVER_CONNECTION) {
            ServerConnectionScreen(navigator)
        }

        // Workspaces
        composable(AppRoutes.WORKSPACES_LIST) {
            WorkspacesScreen(navigator)
        }

        composable(AppRoutes.WORKSPACE_CREATE) {
            WorkspaceCreateScreen(navigator)
        }

        // Home with nested navigation
        composable(AppRoutes.HOME) {
            HomeScreen(navigator)
        }
    }
}

/**
 * Navigation graph for home tabs
 */
@Composable
fun HomeTabNavigationGraph(
    navController: NavHostController = rememberNavController(),
    parentNavigator: AppNavigator
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.TAB_DASHBOARD
    ) {
        composable(AppRoutes.TAB_DASHBOARD) {
            DashboardScreen(parentNavigator)
        }

        composable(AppRoutes.TAB_CONTACTS) {
            ContactsScreen(parentNavigator)
        }

        composable(AppRoutes.TAB_CASHFLOW) {
            CashflowScreen(parentNavigator)
        }

        composable(AppRoutes.TAB_SIMULATIONS) {
            SimulationScreen(parentNavigator)
        }

        composable(AppRoutes.TAB_INVENTORY) {
            InventoryScreen(parentNavigator)
        }

        composable(AppRoutes.TAB_BANKING) {
            BankingScreen(parentNavigator)
        }

        composable(AppRoutes.TAB_PROFILE) {
            ProfileScreen(parentNavigator)
        }
    }
}