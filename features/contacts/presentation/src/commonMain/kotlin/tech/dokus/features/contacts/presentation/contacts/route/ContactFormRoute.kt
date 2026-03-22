package tech.dokus.features.contacts.presentation.contacts.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.contacts.mvi.ContactFormAction
import tech.dokus.features.contacts.mvi.ContactFormContainer
import tech.dokus.features.contacts.presentation.contacts.screen.ContactFormScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun ContactFormRoute(
    contactId: String,
    container: ContactFormContainer = container {
        val parsedContactId = ContactId.parse(contactId)
        parametersOf(ContactFormContainer.Companion.Params(parsedContactId))
    }
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ContactFormAction.NavigateBack -> navController.popBackStack()
            is ContactFormAction.NavigateToContact -> {
                navController.popBackStack()
            }
        }
    }

    ContactFormScreen(
        state = state,
        onIntent = { container.store.intent(it) },
        onNavigateToDuplicate = { duplicateId ->
            navController.navigateTo(ContactsDestination.ContactDetails(duplicateId.toString()))
        }
    )
}
