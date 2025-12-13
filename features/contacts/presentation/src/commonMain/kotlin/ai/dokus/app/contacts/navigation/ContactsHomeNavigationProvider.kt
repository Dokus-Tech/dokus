package ai.dokus.app.contacts.navigation

import ai.dokus.app.contacts.screens.ContactsScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object ContactsHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Contacts> {
            ContactsScreen()
        }
    }
}
