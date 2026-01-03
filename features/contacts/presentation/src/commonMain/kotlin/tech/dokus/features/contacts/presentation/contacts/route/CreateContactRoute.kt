package tech.dokus.features.contacts.presentation.contacts.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.contacts.mvi.CreateContactAction
import tech.dokus.features.contacts.mvi.CreateContactContainer
import tech.dokus.features.contacts.presentation.contacts.screen.CreateContactScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.navigation.destinations.ContactCreateOrigin
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun CreateContactRoute(
    prefillCompanyName: String? = null,
    prefillVat: String? = null,
    prefillAddress: String? = null,
    origin: ContactCreateOrigin? = null,
    container: CreateContactContainer = container(),
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    val resultKey = remember { "documentReview_contactId" }

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val onExistingContactSelected: (String) -> Unit = { contactId ->
        if (origin == ContactCreateOrigin.DocumentReview) {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set(resultKey, contactId)
            navController.popBackStack()
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CreateContactAction.NavigateBack -> navController.popBackStack()
            is CreateContactAction.NavigateToContact -> {
                navController.navigateTo(ContactsDestination.ContactDetails(action.contactId.toString()))
            }
            is CreateContactAction.ContactCreated -> {
                if (origin == ContactCreateOrigin.DocumentReview) {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(resultKey, action.contactId.toString())
                }
                navController.popBackStack()
            }
            is CreateContactAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    CreateContactScreen(
        state = state,
        prefillCompanyName = prefillCompanyName,
        prefillVat = prefillVat,
        prefillAddress = prefillAddress,
        origin = origin,
        snackbarHostState = snackbarHostState,
        onIntent = { container.store.intent(it) },
        onExistingContactSelected = onExistingContactSelected
    )
}
