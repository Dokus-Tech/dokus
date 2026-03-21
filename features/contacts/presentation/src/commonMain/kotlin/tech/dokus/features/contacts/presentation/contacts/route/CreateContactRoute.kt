package tech.dokus.features.contacts.presentation.contacts.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.contacts.mvi.CreateContactAction
import tech.dokus.features.contacts.mvi.CreateContactContainer
import tech.dokus.features.contacts.presentation.contacts.screen.CreateContactScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.ContactCreateOrigin
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun CreateContactRoute(
    prefillCompanyName: String? = null,
    prefillVat: String? = null,
    prefillAddress: String? = null,
    origin: String? = null,
    container: CreateContactContainer = container(),
) {
    val navController = LocalNavController.current
    val parsedOrigin = remember(origin) { ContactCreateOrigin.fromString(origin) }
    val resultKey = remember { "documentReview_contactId" }

    val onExistingContactSelected: (String) -> Unit = { contactId ->
        if (parsedOrigin == ContactCreateOrigin.DocumentDetail) {
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
                if (parsedOrigin == ContactCreateOrigin.DocumentDetail) {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(resultKey, action.contactId.toString())
                }
                navController.popBackStack()
            }
        }
    }

    CreateContactScreen(
        state = state,
        prefillCompanyName = prefillCompanyName,
        prefillVat = prefillVat,
        prefillAddress = prefillAddress,
        origin = parsedOrigin,
        onIntent = { container.store.intent(it) },
        onExistingContactSelected = onExistingContactSelected
    )
}
