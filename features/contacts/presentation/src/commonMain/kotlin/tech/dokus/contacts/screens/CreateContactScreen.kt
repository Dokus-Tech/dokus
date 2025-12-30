package tech.dokus.contacts.screens

import tech.dokus.contacts.components.create.ConfirmStepContent
import tech.dokus.contacts.components.create.LookupStepContent
import tech.dokus.contacts.components.create.ManualStepContent
import ai.dokus.foundation.design.extensions.localized
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.navigation.destinations.ContactsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.contacts.viewmodel.CreateContactAction
import tech.dokus.contacts.viewmodel.CreateContactContainer
import tech.dokus.contacts.viewmodel.CreateContactIntent
import tech.dokus.contacts.viewmodel.CreateContactState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.mvi.container

/**
 * Screen for creating a new contact using VAT-first split flow.
 *
 * On desktop: Shows as a side pane sliding from right
 * On mobile: Shows as a full-screen view
 *
 * Navigation is handled internally using LocalNavController.
 * All user interactions go through intents → container → actions → navigation.
 */
@Composable
internal fun CreateContactScreen(
    container: CreateContactContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    with(container.store) {
        val state by subscribe { action ->
            when (action) {
                is CreateContactAction.NavigateBack -> navController.popBackStack()
                is CreateContactAction.NavigateToContact -> {
                    navController.navigateTo(ContactsDestination.ContactDetails(action.contactId.toString()))
                }

                is CreateContactAction.ContactCreated -> {
                    navController.popBackStack()
                }

                is CreateContactAction.ShowError -> {
                    pendingError = action.error
                }
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { contentPadding ->
            if (isLargeScreen) {
                CreateContactPage(
                    state = state,
                    onIntent = ::intent,
                    modifier = Modifier.padding(contentPadding)
                )
            } else {
                CreateContactFullScreen(
                    state = state,
                    onIntent = ::intent,
                    modifier = Modifier.padding(contentPadding)
                )
            }
        }
    }
}

/**
 * Desktop version: Full-page centered card layout.
 * Clean, professional design without overlay/backdrop.
 */
@Composable
private fun CreateContactPage(
    state: CreateContactState,
    onIntent: (CreateContactIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxHeight()
                .padding(vertical = 32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            CreateContactContent(
                state = state,
                onIntent = onIntent,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            )
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
