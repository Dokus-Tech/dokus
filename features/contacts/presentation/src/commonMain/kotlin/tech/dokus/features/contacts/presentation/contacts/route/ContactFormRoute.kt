package tech.dokus.features.contacts.presentation.contacts.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_create_success
import tech.dokus.aura.resources.contacts_delete_success
import tech.dokus.aura.resources.contacts_update_success
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.contacts.mvi.ContactFormAction
import tech.dokus.features.contacts.mvi.ContactFormContainer
import tech.dokus.features.contacts.mvi.ContactFormIntent
import tech.dokus.features.contacts.mvi.ContactFormSuccess
import tech.dokus.features.contacts.presentation.contacts.screen.ContactFormScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun ContactFormRoute(
    contactId: ContactId,
    container: ContactFormContainer = container {
        parametersOf(ContactFormContainer.Companion.Params(contactId))
    }
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<ContactFormSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            ContactFormSuccess.Created -> stringResource(Res.string.contacts_create_success)
            ContactFormSuccess.Updated -> stringResource(Res.string.contacts_update_success)
            ContactFormSuccess.Deleted -> stringResource(Res.string.contacts_delete_success)
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
            ContactFormAction.NavigateBack -> navController.popBackStack()
            is ContactFormAction.NavigateToContact -> {
                navController.popBackStack()
            }
            is ContactFormAction.ShowError -> {
                pendingError = action.error
            }
            is ContactFormAction.ShowSuccess -> {
                pendingSuccess = action.success
            }
            is ContactFormAction.ShowFieldError -> {
                // Field errors are shown inline in the form
            }
        }
    }

    ContactFormScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
        onNavigateToDuplicate = { duplicateId ->
            navController.navigateTo(ContactsDestination.ContactDetails(duplicateId.toString()))
        }
    )
}
