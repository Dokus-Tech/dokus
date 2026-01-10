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
import tech.dokus.aura.resources.contacts_enrichment_applied_plural
import tech.dokus.aura.resources.contacts_enrichment_applied_single
import tech.dokus.aura.resources.contacts_note_added
import tech.dokus.aura.resources.contacts_note_deleted
import tech.dokus.aura.resources.contacts_note_updated
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.contacts.mvi.ContactDetailsAction
import tech.dokus.features.contacts.mvi.ContactDetailsContainer
import tech.dokus.features.contacts.mvi.ContactDetailsIntent
import tech.dokus.features.contacts.mvi.ContactDetailsState
import tech.dokus.features.contacts.mvi.ContactDetailsSuccess
import tech.dokus.features.contacts.presentation.contacts.components.merge.ContactMergeDialogRoute
import tech.dokus.features.contacts.presentation.contacts.screen.ContactDetailsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.rememberIsOnline
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun ContactDetailsRoute(
    contactId: ContactId,
    showBackButton: Boolean = false,
    container: ContactDetailsContainer = container {
        parametersOf(ContactDetailsContainer.Companion.Params(contactId))
    },
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<ContactDetailsSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            ContactDetailsSuccess.NoteAdded ->
                stringResource(Res.string.contacts_note_added)
            ContactDetailsSuccess.NoteUpdated ->
                stringResource(Res.string.contacts_note_updated)
            ContactDetailsSuccess.NoteDeleted ->
                stringResource(Res.string.contacts_note_deleted)
            is ContactDetailsSuccess.EnrichmentApplied -> {
                if (success.count == 1) {
                    stringResource(Res.string.contacts_enrichment_applied_single, success.count)
                } else {
                    stringResource(Res.string.contacts_enrichment_applied_plural, success.count)
                }
            }
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
            ContactDetailsAction.NavigateBack -> navController.popBackStack()
            is ContactDetailsAction.NavigateToEditContact -> {
                navController.navigateTo(
                    ContactsDestination.EditContact(action.contactId.toString())
                )
            }
            is ContactDetailsAction.NavigateToMergedContact -> {
                navController.navigateTo(
                    ContactsDestination.ContactDetails(action.contactId.toString())
                )
            }
            is ContactDetailsAction.ShowError -> pendingError = action.error
            is ContactDetailsAction.ShowSuccess -> pendingSuccess = action.success
        }
    }

    LaunchedEffect(contactId) {
        container.store.intent(ContactDetailsIntent.LoadContact(contactId))
    }

    val isOnline = rememberIsOnline()

    ContactDetailsScreen(
        state = state,
        showBackButton = showBackButton,
        isOnline = isOnline,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
        onBackClick = { navController.popBackStack() },
        onEditClick = {
            navController.navigateTo(ContactsDestination.EditContact(contactId.toString()))
        },
    )

    val contentState = state as? ContactDetailsState.Content
    if (contentState?.uiState?.showMergeDialog == true) {
        val activitySummary = (contentState.activityState as? DokusState.Success)?.data
        ContactMergeDialogRoute(
            sourceContact = contentState.contact,
            sourceActivity = activitySummary,
            onMergeComplete = { result ->
                container.store.intent(ContactDetailsIntent.HideMergeDialog)
                navController.navigateTo(
                    ContactsDestination.ContactDetails(result.targetContactId.toString())
                )
            },
            onDismiss = { container.store.intent(ContactDetailsIntent.HideMergeDialog) }
        )
    }
}
