package tech.dokus.features.contacts.presentation.contacts.screen

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Name
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.features.contacts.mvi.ContactDetailsIntent
import tech.dokus.features.contacts.mvi.ContactDetailsState
import tech.dokus.features.contacts.mvi.EnrichmentSuggestion
import tech.dokus.features.contacts.presentation.contacts.components.ContactDetailsContent
import tech.dokus.features.contacts.presentation.contacts.components.ContactDetailsTopBar
import tech.dokus.features.contacts.presentation.contacts.components.EnrichmentSuggestionsDialog
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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
            isOnline = isOnline,
            contentPadding = contentPadding,
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

@Preview
@Composable
private fun ContactDetailsScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val mockContactId = ContactId.generate()
    val now = LocalDateTime(2026, 1, 1, 0, 0)
    TestWrapper(parameters) {
        ContactDetailsScreen(
            state = ContactDetailsState.Content(
                contactId = mockContactId,
                contact = ContactDto(
                    id = mockContactId,
                    tenantId = TenantId.generate(),
                    name = Name("Acme Corp"),
                    createdAt = now,
                    updatedAt = now
                )
            ),
            showBackButton = true,
            isOnline = true,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onBackClick = {},
            onEditClick = {}
        )
    }
}
