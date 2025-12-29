package ai.dokus.app.contacts.screens

import ai.dokus.app.contacts.components.create.ConfirmStepContent
import ai.dokus.app.contacts.components.create.LookupStepContent
import ai.dokus.app.contacts.components.create.ManualStepContent
import ai.dokus.app.contacts.viewmodel.CreateContactAction
import ai.dokus.app.contacts.viewmodel.CreateContactContainer
import ai.dokus.app.contacts.viewmodel.CreateContactIntent
import ai.dokus.app.contacts.viewmodel.CreateContactState
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import ai.dokus.foundation.navigation.destinations.ContactsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container

/**
 * Screen for creating a new contact using VAT-first split flow.
 *
 * On desktop: Shows as a side pane sliding from right
 * On mobile: Shows as a full-screen view
 *
 * Navigation is handled internally using LocalNavController.
 */
@Composable
internal fun CreateContactScreen(
    container: CreateContactContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge

    // Subscribe to actions
    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CreateContactAction.NavigateBack -> navController.popBackStack()
            is CreateContactAction.NavigateToContact -> {
                navController.navigateTo(
                    ContactsDestination.ContactDetails(action.contactId.toString())
                )
            }
            is CreateContactAction.ContactCreated -> {
                navController.popBackStack()
            }
            is CreateContactAction.ShowError -> {
                // TODO: Show snackbar
            }
        }
    }

    if (isLargeScreen) {
        CreateContactPane(
            state = state,
            onIntent = { container.store.intent(it) },
            onDismiss = { navController.popBackStack() },
        )
    } else {
        CreateContactFullScreen(
            state = state,
            onIntent = { container.store.intent(it) },
        )
    }
}

/**
 * Desktop version: Side pane with backdrop
 */
@Composable
private fun CreateContactPane(
    state: CreateContactState,
    onIntent: (CreateContactIntent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )

        // Side pane
        BoxWithConstraints(
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            val paneWidth = (maxWidth * 0.4f).coerceIn(400.dp, 600.dp)

            Card(
                modifier = Modifier
                    .width(paneWidth)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume click */ }
                    ),
                shape = MaterialTheme.shapes.large.copy(
                    topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                    bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                CreateContactContent(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Mobile version: Full screen
 */
@Composable
private fun CreateContactFullScreen(
    state: CreateContactState,
    onIntent: (CreateContactIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .imePadding()
    ) {
        CreateContactContent(
            state = state,
            onIntent = onIntent,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Main content - renders based on current state/step
 */
@Composable
private fun CreateContactContent(
    state: CreateContactState,
    onIntent: (CreateContactIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is CreateContactState.LookupStep -> LookupStepContent(
            state = state,
            onIntent = onIntent,
            modifier = modifier
        )
        is CreateContactState.ConfirmStep -> ConfirmStepContent(
            state = state,
            onIntent = onIntent,
            modifier = modifier
        )
        is CreateContactState.ManualStep -> ManualStepContent(
            state = state,
            onIntent = onIntent,
            modifier = modifier
        )
    }
}
