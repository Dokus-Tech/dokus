package tech.dokus.features.contacts.presentation.contacts.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.contacts.mvi.ContactDetailsAction
import tech.dokus.features.contacts.mvi.ContactDetailsContainer
import tech.dokus.features.contacts.mvi.ContactDetailsIntent
import tech.dokus.features.contacts.presentation.contacts.components.merge.ContactMergeDialogRoute
import tech.dokus.features.contacts.presentation.contacts.screen.ContactDetailsScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.rememberIsOnline
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun ContactDetailsRoute(
    contactId: String,
    showBackButton: Boolean = false,
    container: ContactDetailsContainer = container {
        val parsedContactId = ContactId.parse(contactId)
        parametersOf(ContactDetailsContainer.Companion.Params(parsedContactId))
    },
) {
    val navController = LocalNavController.current
    val parsedContactId = remember(contactId) { ContactId.parse(contactId) }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ContactDetailsAction.NavigateBack -> navController.popBackStack()
            is ContactDetailsAction.NavigateToMergedContact -> {
                navController.navigateTo(
                    ContactsDestination.ContactDetails(action.contactId.toString())
                )
            }
        }
    }

    LaunchedEffect(parsedContactId) {
        container.store.intent(ContactDetailsIntent.LoadContact(parsedContactId))
    }

    val isOnline = rememberIsOnline()

    ContactDetailsScreen(
        state = state,
        showBackButton = showBackButton,
        isOnline = isOnline,
        onIntent = { container.store.intent(it) },
        onBackClick = { navController.popBackStack() },
        onDocumentClick = { documentId ->
            navController.navigateTo(
                CashFlowDestination.DocumentDetail(
                    documentId = documentId.toString(),
                    contactId = contactId,
                )
            )
        },
    )

    if (state.uiState.showMergeDialog) {
        val contactData = (state.contact as? DokusState.Success)?.data
        if (contactData != null) {
            ContactMergeDialogRoute(
                sourceContact = contactData,
                sourceActivity = (state.activityState as? DokusState.Success)?.data,
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
}
