package tech.dokus.contacts.screens

import tech.dokus.contacts.components.create.ConfirmStepContent
import tech.dokus.contacts.components.create.LookupStepContent
import tech.dokus.contacts.components.create.ManualStepContent
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.navigation.destinations.ContactCreateOrigin
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
import tech.dokus.foundation.aura.components.DokusCardSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
    prefillCompanyName: String? = null,
    prefillVat: String? = null,
    prefillAddress: String? = null,
    origin: ContactCreateOrigin? = null,
    container: CreateContactContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge
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

    with(container.store) {
        val state by subscribe { action ->
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

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { contentPadding ->
            if (isLargeScreen) {
                CreateContactPage(
                    state = state,
                    onIntent = ::intent,
                    prefillCompanyName = prefillCompanyName,
                    prefillVat = prefillVat,
                    prefillAddress = prefillAddress,
                    origin = origin,
                    onExistingContactSelected = onExistingContactSelected,
                    modifier = Modifier.padding(contentPadding)
                )
            } else {
                CreateContactFullScreen(
                    state = state,
                    onIntent = ::intent,
                    prefillCompanyName = prefillCompanyName,
                    prefillVat = prefillVat,
                    prefillAddress = prefillAddress,
                    origin = origin,
                    onExistingContactSelected = onExistingContactSelected,
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
    prefillCompanyName: String?,
    prefillVat: String?,
    prefillAddress: String?,
    origin: ContactCreateOrigin?,
    onExistingContactSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        DokusCardSurface(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxHeight()
                .padding(vertical = 24.dp),
        ) {
            CreateContactContent(
                state = state,
                onIntent = onIntent,
                prefillCompanyName = prefillCompanyName,
                prefillVat = prefillVat,
                prefillAddress = prefillAddress,
                origin = origin,
                onExistingContactSelected = onExistingContactSelected,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
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
    prefillCompanyName: String?,
    prefillVat: String?,
    prefillAddress: String?,
    origin: ContactCreateOrigin?,
    onExistingContactSelected: (String) -> Unit,
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
            prefillCompanyName = prefillCompanyName,
            prefillVat = prefillVat,
            prefillAddress = prefillAddress,
            origin = origin,
            onExistingContactSelected = onExistingContactSelected,
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
    prefillCompanyName: String?,
    prefillVat: String?,
    prefillAddress: String?,
    origin: ContactCreateOrigin?,
    onExistingContactSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var manualPrefillApplied by remember(prefillCompanyName, prefillVat, prefillAddress, origin) {
        mutableStateOf(false)
    }

    LaunchedEffect(state, prefillCompanyName, prefillVat, origin) {
        if (origin != ContactCreateOrigin.DocumentReview) return@LaunchedEffect
        if (manualPrefillApplied) return@LaunchedEffect
        val manualState = state as? CreateContactState.ManualStep ?: return@LaunchedEffect

        if (!prefillCompanyName.isNullOrBlank() && manualState.formData.companyName.isBlank()) {
            onIntent(CreateContactIntent.ManualFieldChanged("companyName", prefillCompanyName))
        }
        if (!prefillVat.isNullOrBlank() && manualState.formData.vatNumber.isBlank()) {
            onIntent(CreateContactIntent.ManualFieldChanged("vatNumber", prefillVat))
        }
        manualPrefillApplied = true
    }

    when (state) {
        is CreateContactState.LookupStep -> LookupStepContent(
            state = state,
            onIntent = onIntent,
            initialQuery = prefillVat?.takeIf { it.isNotBlank() } ?: prefillCompanyName,
            onExistingContactSelected = if (origin == ContactCreateOrigin.DocumentReview) {
                onExistingContactSelected
            } else {
                null
            },
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
