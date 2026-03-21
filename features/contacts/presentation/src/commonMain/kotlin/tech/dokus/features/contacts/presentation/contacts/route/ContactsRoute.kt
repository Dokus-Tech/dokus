package tech.dokus.features.contacts.presentation.contacts.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_create
import tech.dokus.aura.resources.contacts_subtitle
import tech.dokus.aura.resources.nav_contacts
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.mvi.ContactsAction
import tech.dokus.features.contacts.mvi.ContactsContainer
import tech.dokus.features.contacts.mvi.ContactsIntent
import tech.dokus.features.contacts.presentation.contacts.screen.ContactsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

private const val HOME_ROUTE_CONTACTS = "contacts"

@Composable
internal fun ContactsRoute(
    container: ContactsContainer = container(),
) {
    val navController = LocalNavController.current
    val contactsTitle = stringResource(Res.string.nav_contacts)
    val contactsSubtitle = stringResource(Res.string.contacts_subtitle)
    val createLabel = "+ ${stringResource(Res.string.contacts_create)}"

    val onCreateContact = remember(navController) {
        {
            navController.navigateTo(ContactsDestination.CreateContact())
        }
    }

    val topBarConfig = remember(
        contactsTitle,
        contactsSubtitle,
        createLabel,
        onCreateContact
    ) {
        HomeShellTopBarConfig(
            mode = HomeShellTopBarMode.Title(
                title = contactsTitle,
                subtitle = contactsSubtitle
            ),
            actions = listOf(
                HomeShellTopBarAction.Text(
                    label = createLabel,
                    onClick = onCreateContact
                )
            )
        )
    }

    RegisterHomeShellTopBar(
        route = HOME_ROUTE_CONTACTS,
        config = topBarConfig
    )

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ContactsAction.NavigateToContactDetails -> {
                navController.navigateTo(
                    ContactsDestination.ContactDetails(action.contactId.toString())
                )
            }
            is ContactsAction.NavigateToCreateContact -> {
                navController.navigateTo(ContactsDestination.CreateContact())
            }
            is ContactsAction.NavigateToEditContact -> {
                navController.navigateTo(
                    ContactsDestination.EditContact(action.contactId.toString())
                )
            }
        }
    }

    val onIntent = remember(container) {
        { intent: ContactsIntent ->
            container.store.intent(intent)
        }
    }
    val onSelectContact = remember(onIntent) {
        { contact: ContactDto ->
            onIntent(ContactsIntent.SelectContact(contact.id))
        }
    }
    val onOpenContact = remember(navController) {
        { contact: ContactDto ->
            navController.navigateTo(ContactsDestination.ContactDetails(contact.id.toString()))
        }
    }

    ContactsScreen(
        state = state,
        onIntent = onIntent,
        onSelectContact = onSelectContact,
        onOpenContact = onOpenContact,
        onCreateContact = onCreateContact
    )
}
