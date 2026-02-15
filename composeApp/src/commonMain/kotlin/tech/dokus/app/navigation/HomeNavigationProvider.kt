package tech.dokus.app.navigation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import org.koin.compose.koinInject
import tech.dokus.app.screens.MoreScreen
import tech.dokus.app.screens.today.TodayScreen
import tech.dokus.app.screens.UnderDevelopmentScreen
import tech.dokus.app.screens.settings.route.AppearanceSettingsRoute
import tech.dokus.app.screens.settings.route.NotificationPreferencesRoute
import tech.dokus.app.screens.settings.route.TeamSettingsRoute
import tech.dokus.app.screens.settings.route.WorkspaceSettingsRoute
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.features.auth.presentation.auth.route.ChangePasswordRoute
import tech.dokus.features.auth.presentation.auth.route.MySessionsRoute
import tech.dokus.features.auth.presentation.auth.route.ProfileSettingsRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

internal object HomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Today> {
            TodayScreen()
        }
        composable<HomeDestination.Team> {
            TeamSettingsRoute()
        }
        composable<SettingsDestination.WorkspaceSettings> {
            WorkspaceSettingsRoute()
        }
        composable<SettingsDestination.AppearanceSettings> {
            AppearanceSettingsRoute()
        }
        composable<SettingsDestination.NotificationPreferences> {
            NotificationPreferencesRoute()
        }
        composable<AuthDestination.ProfileSettings> {
            ProfileSettingsRoute()
        }
        composable<AuthDestination.ChangePassword> {
            ChangePasswordRoute()
        }
        composable<AuthDestination.MySessions> {
            MySessionsRoute()
        }
        composable<HomeDestination.More> {
            MoreScreen()
        }
        composable<HomeDestination.Tomorrow> {
            // Tomorrow is One-tier only - redirect Core users to Today
            val tokenManager: TokenManager = koinInject()
            val navController = LocalNavController.current
            var userTier by remember { mutableStateOf<SubscriptionTier?>(null) }

            LaunchedEffect(Unit) {
                userTier = tokenManager.getCurrentClaims()?.tenant?.subscriptionTier ?: SubscriptionTier.Core
            }

            // Show content once tier is loaded
            when {
                userTier == null -> {
                    // Still loading tier
                }
                !SubscriptionTier.hasTomorrowAccess(userTier!!) -> {
                    LaunchedEffect(Unit) {
                        navController.navigateTo(HomeDestination.Today) {
                            popUpTo(HomeDestination.Tomorrow::class) { inclusive = true }
                        }
                    }
                }
                else -> {
                    // Show Tomorrow content (AI/Forecast hub) - for now, placeholder
                    UnderDevelopmentScreen()
                }
            }
        }
        composable<HomeDestination.UnderDevelopment> {
            UnderDevelopmentScreen()
        }
    }
}
