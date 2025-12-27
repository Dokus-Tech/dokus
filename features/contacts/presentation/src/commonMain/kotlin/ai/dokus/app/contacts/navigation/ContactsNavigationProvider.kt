package ai.dokus.app.contacts.navigation

import ai.dokus.app.contacts.screens.ContactDetailsScreen
import ai.dokus.app.contacts.screens.ContactFormScreen
import tech.dokus.domain.ids.ContactId
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.ContactsDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute

/**
 * Navigation provider for the Contacts feature.
 *
 * Each screen handles its own navigation internally using LocalNavController,
 * following the pattern established in CashflowNavigationProvider.
 */
internal object ContactsNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<ContactsDestination.CreateContact> {
            ContactFormScreen(contactId = null)
        }
        composable<ContactsDestination.EditContact> { backStackEntry ->
            val route = backStackEntry.toRoute<ContactsDestination.EditContact>()
            val contactId = ContactId.parse(route.contactId)
            ContactFormScreen(contactId = contactId)
        }
        composable<ContactsDestination.ContactDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<ContactsDestination.ContactDetails>()
            val contactId = ContactId.parse(route.contactId)
            ContactDetailsScreen(contactId = contactId, showBackButton = true)
        }
    }
}
