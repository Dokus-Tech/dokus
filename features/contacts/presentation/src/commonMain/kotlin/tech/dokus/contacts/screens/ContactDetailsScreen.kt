package tech.dokus.contacts.screens

import tech.dokus.contacts.components.ActivitySummarySection
import tech.dokus.contacts.components.ContactInfoSection
import tech.dokus.contacts.components.ContactMergeDialog
import tech.dokus.contacts.components.NotesBottomSheet
import tech.dokus.contacts.components.NotesSection
import tech.dokus.contacts.components.NotesSidePanel
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.contacts_contact_details
import tech.dokus.aura.resources.contacts_current
import tech.dokus.aura.resources.contacts_deselect_all
import tech.dokus.aura.resources.contacts_edit_contact
import tech.dokus.aura.resources.contacts_enrichment_applied_plural
import tech.dokus.aura.resources.contacts_enrichment_applied_single
import tech.dokus.aura.resources.contacts_enrichment_apply_all
import tech.dokus.aura.resources.contacts_enrichment_apply_selected_count
import tech.dokus.aura.resources.contacts_enrichment_available
import tech.dokus.aura.resources.contacts_enrichment_hint
import tech.dokus.aura.resources.contacts_enrichment_not_now
import tech.dokus.aura.resources.contacts_enrichment_suggestions
import tech.dokus.aura.resources.contacts_merge
import tech.dokus.aura.resources.contacts_note_added
import tech.dokus.aura.resources.contacts_note_deleted
import tech.dokus.aura.resources.contacts_note_updated
import tech.dokus.aura.resources.contacts_peppol_update_success
import tech.dokus.aura.resources.contacts_select_all
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.OfflineOverlay
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.extensions.localized
import ai.dokus.foundation.navigation.destinations.ContactsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.contacts.viewmodel.ContactDetailsAction
import tech.dokus.contacts.viewmodel.ContactDetailsContainer
import tech.dokus.contacts.viewmodel.ContactDetailsIntent
import tech.dokus.contacts.viewmodel.ContactDetailsState
import tech.dokus.contacts.viewmodel.ContactDetailsSuccess
import tech.dokus.contacts.viewmodel.EnrichmentSuggestion
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.rememberIsOnline
import tech.dokus.foundation.app.state.DokusState

/**
 * Contact Details Screen displaying all information about a contact.
 *
 * Features:
 * - Contact information display with Peppol toggle
 * - Activity summary (invoices, bills, expenses counts and totals)
 * - Notes management (add, edit, delete notes)
 * - Enrichment suggestions badge (when available)
 * - Edit button to navigate to edit form
 *
 * This screen can be used:
 * - Embedded in master-detail layout (desktop) - showBackButton = false
 * - Standalone navigation (mobile) - showBackButton = true
 *
 * Uses FlowMVI container/subscribe pattern for state management.
 *
 * @param contactId The ID of the contact to display
 * @param showBackButton Whether to show the back button in the top bar (true for standalone, false for embedded)
 * @param container The FlowMVI container for managing contact details state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactDetailsScreen(
    contactId: ContactId,
    showBackButton: Boolean = false,
    container: ContactDetailsContainer = container {
        parametersOf(
            ContactDetailsContainer.Companion.Params(contactId)
        )
    }
) {
    val navController = LocalNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<ContactDetailsSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            ContactDetailsSuccess.PeppolUpdated ->
                stringResource(Res.string.contacts_peppol_update_success)

            ContactDetailsSuccess.NoteAdded ->
                stringResource(Res.string.contacts_note_added)

            ContactDetailsSuccess.NoteUpdated ->
                stringResource(Res.string.contacts_note_updated)

            ContactDetailsSuccess.NoteDeleted ->
                stringResource(Res.string.contacts_note_deleted)

            is ContactDetailsSuccess.EnrichmentApplied -> {
                if (success.count == 1) {
                    stringResource(Res.string.contacts_enrichment_applied_single, success.count)
                } else {
                    stringResource(Res.string.contacts_enrichment_applied_plural, success.count)
                }
            }
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    // Subscribe to state and handle actions
    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            ContactDetailsAction.NavigateBack -> navController.popBackStack()
            is ContactDetailsAction.NavigateToEditContact -> {
                navController.navigateTo(ContactsDestination.EditContact(action.contactId.toString()))
            }

            is ContactDetailsAction.NavigateToMergedContact -> {
                navController.navigateTo(ContactsDestination.ContactDetails(action.contactId.toString()))
            }

            is ContactDetailsAction.ShowError -> {
                pendingError = action.error
            }

            is ContactDetailsAction.ShowSuccess -> {
                pendingSuccess = action.success
            }
        }
    }

    // Load contact on first composition or when contactId changes
    LaunchedEffect(contactId) {
        container.store.intent(ContactDetailsIntent.LoadContact(contactId))
    }

    // Check connection status for disabling offline actions
    val isOnline = rememberIsOnline()

    // Use BoxWithConstraints to detect viewport size for responsive notes UI
    BoxWithConstraints {
        val isDesktop = maxWidth >= 600.dp

        with(container.store) {
            ContactDetailsScreenContent(
                state = state,
                showBackButton = showBackButton,
                isDesktop = isDesktop,
                isOnline = isOnline,
                contactId = contactId,
                snackbarHostState = snackbarHostState,
                onBackClick = { navController.popBackStack() },
                onEditClick = {
                    navController.navigateTo(ContactsDestination.EditContact(contactId.toString()))
                }
            )
        }
    }
}

/**
 * Main content of the contact details screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntentReceiver<ContactDetailsIntent>.ContactDetailsScreenContent(
    state: ContactDetailsState,
    showBackButton: Boolean,
    isDesktop: Boolean,
    isOnline: Boolean,
    contactId: ContactId,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit
) {
    // Extract state values based on current state type
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

    val isSavingNote: Boolean = when (state) {
        is ContactDetailsState.Content -> state.isSavingNote
        else -> false
    }

    val isDeletingNote: Boolean = when (state) {
        is ContactDetailsState.Content -> state.isDeletingNote
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
                onEnrichmentClick = { intent(ContactDetailsIntent.ShowEnrichmentPanel) },
                onMergeClick = { intent(ContactDetailsIntent.ShowMergeDialog) },
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
            onPeppolToggle = { enabled -> intent(ContactDetailsIntent.TogglePeppol(enabled)) },
            onAddNote = {
                // Show notes panel (desktop) or bottom sheet (mobile)
                if (isDesktop) {
                    intent(ContactDetailsIntent.ShowNotesSidePanel)
                } else {
                    intent(ContactDetailsIntent.ShowNotesBottomSheet)
                }
            },
            onEditNote = { note -> intent(ContactDetailsIntent.ShowEditNoteDialog(note)) },
            onDeleteNote = { note -> intent(ContactDetailsIntent.ShowDeleteNoteConfirmation(note)) },
            onRetry = { intent(ContactDetailsIntent.Refresh) }
        )
    }

    // Responsive Notes UI - Side panel for desktop, bottom sheet for mobile
    val uiState = (state as? ContactDetailsState.Content)?.uiState

    if (isDesktop) {
        // Desktop: Notes Side Panel
        NotesSidePanel(
            isVisible = uiState?.showNotesSidePanel == true,
            onDismiss = { intent(ContactDetailsIntent.HideNotesSidePanel) },
            notesState = notesState,
            noteContent = uiState?.noteContent ?: "",
            onNoteContentChange = { content -> intent(ContactDetailsIntent.UpdateNoteContent(content)) },
            isSavingNote = isSavingNote,
            isDeletingNote = isDeletingNote,
            editingNote = uiState?.editingNote,
            onAddNote = { intent(ContactDetailsIntent.AddNote) },
            onUpdateNote = { intent(ContactDetailsIntent.UpdateNote) },
            onDeleteNote = { note ->
                intent(ContactDetailsIntent.ShowDeleteNoteConfirmation(note))
                intent(ContactDetailsIntent.DeleteNote)
            },
            onEditNoteClick = { note -> intent(ContactDetailsIntent.ShowEditNoteDialog(note)) },
            onCancelEdit = { intent(ContactDetailsIntent.HideEditNoteDialog) }
        )
    } else {
        // Mobile: Notes Bottom Sheet
        NotesBottomSheet(
            isVisible = uiState?.showNotesBottomSheet == true,
            onDismiss = { intent(ContactDetailsIntent.HideNotesBottomSheet) },
            notesState = notesState,
            noteContent = uiState?.noteContent ?: "",
            onNoteContentChange = { content -> intent(ContactDetailsIntent.UpdateNoteContent(content)) },
            isSavingNote = isSavingNote,
            isDeletingNote = isDeletingNote,
            editingNote = uiState?.editingNote,
            onAddNote = { intent(ContactDetailsIntent.AddNote) },
            onUpdateNote = { intent(ContactDetailsIntent.UpdateNote) },
            onDeleteNote = { note ->
                intent(ContactDetailsIntent.ShowDeleteNoteConfirmation(note))
                intent(ContactDetailsIntent.DeleteNote)
            },
            onEditNoteClick = { note -> intent(ContactDetailsIntent.ShowEditNoteDialog(note)) },
            onCancelEdit = { intent(ContactDetailsIntent.HideEditNoteDialog) }
        )
    }

    // Enrichment Panel Dialog (simplified for now)
    if (uiState?.showEnrichmentPanel == true && enrichmentSuggestions.isNotEmpty()) {
        EnrichmentSuggestionsDialog(
            suggestions = enrichmentSuggestions,
            onApply = { selected ->
                intent(ContactDetailsIntent.ApplyEnrichmentSuggestions(selected))
            },
            onDismiss = { intent(ContactDetailsIntent.HideEnrichmentPanel) }
        )
    }

    // Merge Dialog
    if (uiState?.showMergeDialog == true) {
        val contact = state.contact
        val activity = state.activityState.let {
            (it as? DokusState.Success)?.data
        }

        ContactMergeDialog(
            sourceContact = contact,
            sourceActivity = activity,
            preselectedTarget = null,
            onMergeComplete = { result ->
                // Navigate to the merged (target) contact
                intent(ContactDetailsIntent.HideMergeDialog)
                // Navigation will be handled by the action
            },
            onDismiss = { intent(ContactDetailsIntent.HideMergeDialog) }
        )
    }
}

/**
 * Top app bar for the contact details screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsTopBar(
    contactState: DokusState<ContactDto>,
    showBackButton: Boolean,
    hasEnrichmentSuggestions: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onEnrichmentClick: () -> Unit,
    onMergeClick: () -> Unit,
    isOnline: Boolean = true
) {
    Column {
        TopAppBar(
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                }
            },
            title = {
                when (contactState) {
                    is DokusState.Success -> {
                        Text(
                            text = contactState.data.name.value,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    is DokusState.Loading -> {
                        ShimmerLine(modifier = Modifier.width(150.dp), height = 24.dp)
                    }

                    else -> {
                        Text(
                            text = stringResource(Res.string.contacts_contact_details),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            actions = {
                // Enrichment badge
                if (hasEnrichmentSuggestions) {
                    IconButton(onClick = onEnrichmentClick) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(Res.string.contacts_enrichment_available),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd),
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ) { }
                        }
                    }
                }

                // Merge button (disabled when offline - requires network)
                IconButton(
                    onClick = onMergeClick,
                    enabled = isOnline
                ) {
                    Icon(
                        imageVector = Icons.Default.MergeType,
                        contentDescription = stringResource(Res.string.contacts_merge)
                    )
                }

                // Edit button (disabled when offline - requires network)
                IconButton(
                    onClick = onEditClick,
                    enabled = isOnline
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.contacts_edit_contact)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                navigationIconContentColor = Color.Unspecified,
                titleContentColor = Color.Unspecified,
                actionIconContentColor = Color.Unspecified
            )
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

/**
 * Main content of the contact details screen.
 * Displays contact info, activity summary, and notes sections.
 */
@Composable
private fun ContactDetailsContent(
    contactState: DokusState<ContactDto>,
    activityState: DokusState<ContactActivitySummary>,
    notesState: DokusState<List<ContactNoteDto>>,
    isTogglingPeppol: Boolean,
    isOnline: Boolean,
    contentPadding: PaddingValues,
    onPeppolToggle: (Boolean) -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (ContactNoteDto) -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit,
    onRetry: () -> Unit
) {
    // Check for global error state (contact not found)
    when (contactState) {
        is DokusState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                DokusErrorContent(
                    exception = contactState.exception,
                    retryHandler = contactState.retryHandler
                )
            }
        }

        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Contact Information Section
                ContactInfoSection(
                    state = contactState,
                    onPeppolToggle = onPeppolToggle,
                    isTogglingPeppol = isTogglingPeppol
                )

                // Activity Summary Section - requires network connection
                // When offline with error, show loading skeleton behind overlay instead of error
                OfflineOverlay(isOffline = !isOnline) {
                    ActivitySummarySection(
                        state = if (!isOnline && activityState is DokusState.Error) {
                            DokusState.loading()
                        } else {
                            activityState
                        }
                    )
                }

                // Notes Section - requires network connection
                // When offline with error, show loading skeleton behind overlay instead of error
                OfflineOverlay(isOffline = !isOnline) {
                    NotesSection(
                        state = if (!isOnline && notesState is DokusState.Error) {
                            DokusState.loading()
                        } else {
                            notesState
                        },
                        onAddNote = onAddNote,
                        onEditNote = onEditNote,
                        onDeleteNote = onDeleteNote
                    )
                }

                // Bottom spacing
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}


/**
 * Dialog displaying enrichment suggestions with individual selection.
 *
 * Features:
 * - Displays each suggestion with field name, confidence score, and source
 * - Checkboxes allow selecting individual suggestions to apply
 * - Shows current vs. suggested values for comparison
 * - "Apply Selected" button applies only checked suggestions
 * - Confidence score displayed as percentage with color coding
 *
 * @param suggestions List of enrichment suggestions from backend
 * @param onApply Callback with list of selected suggestions to apply
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
private fun EnrichmentSuggestionsDialog(
    suggestions: List<EnrichmentSuggestion>,
    onApply: (List<EnrichmentSuggestion>) -> Unit,
    onDismiss: () -> Unit
) {
    // Track selected suggestions - all selected by default
    val selectedSuggestions =
        remember { mutableStateListOf<EnrichmentSuggestion>().apply { addAll(suggestions) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.contacts_enrichment_suggestions),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_enrichment_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                suggestions.forEach { suggestion ->
                    val isSelected = suggestion in selectedSuggestions

                    EnrichmentSuggestionItem(
                        suggestion = suggestion,
                        isSelected = isSelected,
                        onSelectionChange = { selected ->
                            if (selected) {
                                selectedSuggestions.add(suggestion)
                            } else {
                                selectedSuggestions.remove(suggestion)
                            }
                        }
                    )
                }

                // Select all / Deselect all controls
                if (suggestions.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { selectedSuggestions.clear() }
                        ) {
                            Text(
                                text = stringResource(Res.string.contacts_deselect_all),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = {
                                selectedSuggestions.clear()
                                selectedSuggestions.addAll(suggestions)
                            }
                        ) {
                            Text(
                                text = stringResource(Res.string.contacts_select_all),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(selectedSuggestions.toList()) },
                enabled = selectedSuggestions.isNotEmpty()
            ) {
                Text(
                    text = if (selectedSuggestions.size == suggestions.size) {
                        stringResource(Res.string.contacts_enrichment_apply_all)
                    } else {
                        stringResource(
                            Res.string.contacts_enrichment_apply_selected_count,
                            selectedSuggestions.size
                        )
                    },
                    fontWeight = FontWeight.Medium,
                    color = if (selectedSuggestions.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.contacts_enrichment_not_now),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

/**
 * Individual enrichment suggestion item with checkbox for selection.
 *
 * Displays:
 * - Checkbox for selection
 * - Field name with confidence badge
 * - Suggested value
 * - Current value (if exists) for comparison
 * - Source badge showing where the suggestion came from
 *
 * @param suggestion The enrichment suggestion to display
 * @param isSelected Whether this suggestion is currently selected
 * @param onSelectionChange Callback when selection state changes
 */
@Composable
private fun EnrichmentSuggestionItem(
    suggestion: EnrichmentSuggestion,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Surface(
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Field name and confidence row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = suggestion.field,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )

                    // Confidence badge with color coding
                    ConfidenceBadge(confidence = suggestion.confidence)
                }

                // Suggested value
                Text(
                    text = suggestion.suggestedValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Current value (if exists)
                if (suggestion.currentValue != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.contacts_current),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = suggestion.currentValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Source badge
                SourceBadge(source = suggestion.source)
            }
        }
    }
}

/**
 * Badge displaying confidence score with color coding.
 * - High confidence (>=80%): Green
 * - Medium confidence (>=60%): Yellow/Orange
 * - Low confidence (<60%): Gray
 */
@Composable
private fun ConfidenceBadge(confidence: Float) {
    val percentage = (confidence * 100).toInt()
    val backgroundColor = when {
        confidence >= 0.8f -> MaterialTheme.colorScheme.tertiaryContainer
        confidence >= 0.6f -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        confidence >= 0.8f -> MaterialTheme.colorScheme.onTertiaryContainer
        confidence >= 0.6f -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.common_percent_value, percentage),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Badge displaying the source of the enrichment suggestion.
 * (e.g., "AI", "Public Registry", "User Input")
 */
@Composable
private fun SourceBadge(source: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
