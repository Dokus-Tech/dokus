package tech.dokus.features.contacts.presentation.contacts.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_create_success
import tech.dokus.aura.resources.contacts_delete_success
import tech.dokus.aura.resources.contacts_update_success
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.contacts.mvi.ContactsAction
import tech.dokus.features.contacts.mvi.ContactsContainer
import tech.dokus.features.contacts.mvi.ContactsIntent
import tech.dokus.features.contacts.mvi.ContactsSuccess
import tech.dokus.features.contacts.presentation.contacts.screen.ContactsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun ContactsRoute(
    container: ContactsContainer = container(),
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<ContactsSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            ContactsSuccess.Created -> stringResource(Res.string.contacts_create_success)
            ContactsSuccess.Updated -> stringResource(Res.string.contacts_update_success)
            ContactsSuccess.Deleted -> stringResource(Res.string.contacts_delete_success)
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

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
            is ContactsAction.ShowError -> {
                pendingError = action.error
            }
            is ContactsAction.ShowSuccess -> {
                pendingSuccess = action.success
            }
        }
    }

    ConnectionSnackbarEffect(snackbarHostState)

    LaunchedEffect(Unit) {
        container.store.intent(ContactsIntent.Refresh)
    }

    ContactsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
        onSelectContact = { contact -> container.store.intent(ContactsIntent.SelectContact(contact.id)) },
        onOpenContact = { contact ->
            navController.navigateTo(ContactsDestination.ContactDetails(contact.id.toString()))
        },
        onCreateContact = { navController.navigateTo(ContactsDestination.CreateContact()) }
    )
}
