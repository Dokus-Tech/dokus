package tech.dokus.features.contacts.presentation.contacts.screen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import tech.dokus.features.contacts.presentation.contacts.components.ContactDetailsContent
import tech.dokus.features.contacts.presentation.contacts.components.ContactDetailsTopBar
import tech.dokus.features.contacts.presentation.contacts.components.EnrichmentSuggestionsDialog
import tech.dokus.features.contacts.mvi.ContactDetailsIntent
import tech.dokus.features.contacts.mvi.ContactDetailsState
import tech.dokus.features.contacts.mvi.EnrichmentSuggestion
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.state.DokusState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactDetailsScreen(
    state: ContactDetailsState,
    showBackButton: Boolean,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (ContactDetailsIntent) -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    BoxWithConstraints {
        val isDesktop = maxWidth >= 600.dp

        ContactDetailsScreenContent(
            state = state,
            showBackButton = showBackButton,
            isDesktop = isDesktop,
            isOnline = isOnline,
            snackbarHostState = snackbarHostState,
            onIntent = onIntent,
            onBackClick = onBackClick,
            onEditClick = onEditClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsScreenContent(
    state: ContactDetailsState,
    showBackButton: Boolean,
    isDesktop: Boolean,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
    onIntent: (ContactDetailsIntent) -> Unit,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
) {
    val contactState: DokusState<ContactDto> = when (state) {
        is ContactDetailsState.Loading -> DokusState.loading()
        is ContactDetailsState.Content -> DokusState.success(state.contact)
        is ContactDetailsState.Error -> DokusState.error(state.exception, state.retryHandler)
    }

    val activityState: DokusState<ContactActivitySummary> = when (state) {
        is ContactDetailsState.Content -> state.activityState
        else -> DokusState.loading()
    }

    val notesState: DokusState<List<ContactNoteDto>> = when (state) {
        is ContactDetailsState.Content -> state.notesState
        else -> DokusState.loading()
    }

    val enrichmentSuggestions: List<EnrichmentSuggestion> = when (state) {
        is ContactDetailsState.Content -> state.enrichmentSuggestions
        else -> emptyList()
    }

    val isTogglingPeppol: Boolean = when (state) {
        is ContactDetailsState.Content -> state.isTogglingPeppol
        else -> false
    }

    Scaffold(
        topBar = {
            ContactDetailsTopBar(
                contactState = contactState,
                showBackButton = showBackButton,
                hasEnrichmentSuggestions = enrichmentSuggestions.isNotEmpty(),
                onBackClick = onBackClick,
                onEditClick = onEditClick,
                onEnrichmentClick = { onIntent(ContactDetailsIntent.ShowEnrichmentPanel) },
                onMergeClick = { onIntent(ContactDetailsIntent.ShowMergeDialog) },
                isOnline = isOnline
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        ContactDetailsContent(
            contactState = contactState,
            activityState = activityState,
            notesState = notesState,
            isTogglingPeppol = isTogglingPeppol,
            isOnline = isOnline,
            contentPadding = contentPadding,
            onPeppolToggle = { enabled -> onIntent(ContactDetailsIntent.TogglePeppol(enabled)) },
            onAddNote = {
                if (isDesktop) {
                    onIntent(ContactDetailsIntent.ShowNotesSidePanel)
                } else {
                    onIntent(ContactDetailsIntent.ShowNotesBottomSheet)
                }
            },
            onEditNote = { note -> onIntent(ContactDetailsIntent.ShowEditNoteDialog(note)) },
            onDeleteNote = { note -> onIntent(ContactDetailsIntent.ShowDeleteNoteConfirmation(note)) }
        )
    }

    val contentState = state as? ContactDetailsState.Content
    if (contentState?.uiState?.showEnrichmentPanel == true && enrichmentSuggestions.isNotEmpty()) {
        EnrichmentSuggestionsDialog(
            suggestions = enrichmentSuggestions,
            onApply = { selected ->
                onIntent(ContactDetailsIntent.ApplyEnrichmentSuggestions(selected))
            },
            onDismiss = { onIntent(ContactDetailsIntent.HideEnrichmentPanel) }
        )
    }
}
