package tech.dokus.contacts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import tech.dokus.contacts.screens.ContactDetailsScreen
import tech.dokus.contacts.screens.ContactFormScreen
import tech.dokus.contacts.screens.CreateContactScreen
import tech.dokus.domain.ids.ContactId
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.ContactCreateOrigin
import tech.dokus.navigation.destinations.ContactsDestination

/**
 * Navigation provider for the Contacts feature.
 *
 * Each screen handles its own navigation internally using LocalNavController,
 * following the pattern established in CashflowNavigationProvider.
 */
internal object ContactsNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<ContactsDestination.CreateContact> { backStackEntry ->
            val route = backStackEntry.toRoute<ContactsDestination.CreateContact>()
            CreateContactScreen(
                prefillCompanyName = route.prefillCompanyName,
                prefillVat = route.prefillVat,
                prefillAddress = route.prefillAddress,
                origin = ContactCreateOrigin.fromString(route.origin),
            )
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
