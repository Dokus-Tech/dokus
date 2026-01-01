package tech.dokus.features.contacts.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import tech.dokus.features.contacts.presentation.contacts.route.ContactDetailsRoute
import tech.dokus.features.contacts.presentation.contacts.route.ContactFormRoute
import tech.dokus.features.contacts.presentation.contacts.route.CreateContactRoute
import tech.dokus.domain.ids.ContactId
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.ContactCreateOrigin
import tech.dokus.navigation.destinations.ContactsDestination

/**
 * Navigation provider for the Contacts feature.
 *
 * Routes handle navigation and side-effects for Contacts destinations.
 */
internal object ContactsNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<ContactsDestination.CreateContact> { backStackEntry ->
            val route = backStackEntry.toRoute<ContactsDestination.CreateContact>()
            CreateContactRoute(
                prefillCompanyName = route.prefillCompanyName,
                prefillVat = route.prefillVat,
                prefillAddress = route.prefillAddress,
                origin = ContactCreateOrigin.fromString(route.origin),
            )
        }
        composable<ContactsDestination.EditContact> { backStackEntry ->
            val route = backStackEntry.toRoute<ContactsDestination.EditContact>()
            val contactId = ContactId.parse(route.contactId)
            ContactFormRoute(contactId = contactId)
        }
        composable<ContactsDestination.ContactDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<ContactsDestination.ContactDetails>()
            val contactId = ContactId.parse(route.contactId)
            ContactDetailsRoute(contactId = contactId, showBackButton = true)
        }
    }
}
