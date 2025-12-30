package tech.dokus.contacts.navigation

import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.dokus.contacts.screens.ContactsScreen

/**
 * Navigation provider for the Contacts feature.
 *
 * Each screen handles its own navigation internally using LocalNavController,
 * following the pattern established in CashflowNavigationProvider.
 */
internal object ContactsHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Contacts> {
            ContactsScreen()
        }
    }
}
