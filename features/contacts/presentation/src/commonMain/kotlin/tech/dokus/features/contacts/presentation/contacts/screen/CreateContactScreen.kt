package tech.dokus.features.contacts.presentation.contacts.screen

import tech.dokus.features.contacts.presentation.contacts.components.create.ConfirmStepContent
import tech.dokus.features.contacts.presentation.contacts.components.create.LookupStepContent
import tech.dokus.features.contacts.presentation.contacts.components.create.ManualStepContent
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.navigation.destinations.ContactCreateOrigin
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
import tech.dokus.features.contacts.mvi.CreateContactIntent
import tech.dokus.features.contacts.mvi.CreateContactState

/**
 * Screen for creating a new contact using VAT-first split flow.
 *
 * On desktop: Shows as a side pane sliding from right
 * On mobile: Shows as a full-screen view
 *
 * Pure UI: navigation and side effects are handled in the route.
 */
@Composable
internal fun CreateContactScreen(
    prefillCompanyName: String? = null,
    prefillVat: String? = null,
    prefillAddress: String? = null,
    origin: ContactCreateOrigin? = null,
    state: CreateContactState,
    snackbarHostState: SnackbarHostState,
    onIntent: (CreateContactIntent) -> Unit,
    onExistingContactSelected: (String) -> Unit,
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        if (isLargeScreen) {
            CreateContactPage(
                state = state,
                onIntent = onIntent,
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
                onIntent = onIntent,
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
