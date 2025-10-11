package ai.dokus.foundation.navigation

import androidx.navigation.NavHostController

/**
 * Navigation routes for the entire application
 */
object AppRoutes {
    // Core routes
    const val SPLASH = "splash"
    const val HOME = "home"

    // Authentication routes
    const val LOGIN = "auth/login"
    const val REGISTER = "auth/register"
    const val REGISTER_CONFIRMATION = "auth/register_confirmation"
    const val FORGOT_PASSWORD = "auth/forgot_password"
    const val NEW_PASSWORD = "auth/new_password"

    // Configuration routes
    const val SERVER_CONNECTION = "config/server"

    // Workspace routes
    const val WORKSPACES_LIST = "workspaces/list"
    const val WORKSPACE_CREATE = "workspaces/create"

    // Home tab routes
    const val TAB_DASHBOARD = "tab/dashboard"
    const val TAB_CONTACTS = "tab/contacts"
    const val TAB_CASHFLOW = "tab/cashflow"
    const val TAB_SIMULATIONS = "tab/simulations"
    const val TAB_INVENTORY = "tab/inventory"
    const val TAB_BANKING = "tab/banking"
    const val TAB_PROFILE = "tab/profile"
}

/**
 * Navigation actions for the app
 */
class AppNavigator(private val navController: NavHostController) {

    fun navigateToLogin() {
        navController.navigate(AppRoutes.LOGIN) {
            popUpTo(AppRoutes.SPLASH) { inclusive = true }
        }
    }

    fun navigateToRegister() {
        navController.navigate(AppRoutes.REGISTER)
    }

    fun navigateToRegisterConfirmation() {
        navController.navigate(AppRoutes.REGISTER_CONFIRMATION) {
            popUpTo(AppRoutes.REGISTER) { inclusive = true }
        }
    }

    fun navigateToForgotPassword() {
        navController.navigate(AppRoutes.FORGOT_PASSWORD)
    }

    fun navigateToNewPassword() {
        navController.navigate(AppRoutes.NEW_PASSWORD)
    }

    fun navigateToServerConnection() {
        navController.navigate(AppRoutes.SERVER_CONNECTION)
    }

    fun navigateToWorkspacesList() {
        navController.navigate(AppRoutes.WORKSPACES_LIST) {
            popUpTo(AppRoutes.LOGIN) { inclusive = true }
        }
    }

    fun navigateToWorkspaceCreate() {
        navController.navigate(AppRoutes.WORKSPACE_CREATE)
    }

    fun navigateToHome() {
        navController.navigate(AppRoutes.HOME) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(AppRoutes.HOME)
            launchSingleTop = true
        }
    }

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateBackToLogin() {
        navController.navigate(AppRoutes.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }
}