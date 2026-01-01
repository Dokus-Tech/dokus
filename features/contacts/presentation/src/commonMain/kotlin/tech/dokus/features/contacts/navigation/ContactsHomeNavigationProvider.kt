package tech.dokus.features.contacts.navigation

import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.dokus.features.contacts.presentation.contacts.route.ContactsRoute

/**
 * Navigation provider for the Contacts feature.
 *
 * Routes handle navigation and side-effects for Contacts destinations.
 */
internal object ContactsHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Contacts> {
            ContactsRoute()
        }
    }
}
