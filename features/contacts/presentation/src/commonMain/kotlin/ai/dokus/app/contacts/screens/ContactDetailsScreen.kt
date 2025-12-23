package ai.dokus.app.contacts.screens

import ai.dokus.app.contacts.components.ActivitySummarySection
import ai.dokus.app.contacts.components.ContactInfoSection
import ai.dokus.app.contacts.components.ContactMergeDialog
import ai.dokus.app.contacts.components.NotesSection
import ai.dokus.app.contacts.viewmodel.ContactDetailsViewModel
import ai.dokus.app.contacts.viewmodel.EnrichmentSuggestion
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.ShimmerLine
import ai.dokus.foundation.design.components.fields.PTextFieldFree
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.model.ContactActivitySummary
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactNoteDto
import ai.dokus.foundation.navigation.destinations.ContactsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Note
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
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
 * @param contactId The ID of the contact to display
 * @param showBackButton Whether to show the back button in the top bar (true for standalone, false for embedded)
 * @param viewModel The ViewModel for managing contact details state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactDetailsScreen(
    contactId: ContactId,
    showBackButton: Boolean = false,
    viewModel: ContactDetailsViewModel = koinViewModel()
) {
    val navController = LocalNavController.current

    // State collection
    val contactState by viewModel.state.collectAsState()
    val activityState by viewModel.activityState.collectAsState()
    val notesState by viewModel.notesState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val enrichmentSuggestions by viewModel.enrichmentSuggestions.collectAsState()
    val isTogglingPeppol by viewModel.isTogglingPeppol.collectAsState()
    val isSavingNote by viewModel.isSavingNote.collectAsState()
    val isDeletingNote by viewModel.isDeletingNote.collectAsState()

    // Load contact on first composition or when contactId changes
    LaunchedEffect(contactId) {
        viewModel.loadContact(contactId)
    }

    Scaffold(
        topBar = {
            ContactDetailsTopBar(
                contactState = contactState,
                showBackButton = showBackButton,
                hasEnrichmentSuggestions = enrichmentSuggestions.isNotEmpty(),
                onBackClick = { navController.popBackStack() },
                onEditClick = {
                    navController.navigateTo(ContactsDestination.EditContact(contactId.toString()))
                },
                onEnrichmentClick = viewModel::showEnrichmentPanel,
                onMergeClick = viewModel::showMergeDialog
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        ContactDetailsContent(
            contactState = contactState,
            activityState = activityState,
            notesState = notesState,
            isTogglingPeppol = isTogglingPeppol,
            contentPadding = contentPadding,
            onPeppolToggle = viewModel::togglePeppol,
            onAddNote = viewModel::showAddNoteDialog,
            onEditNote = viewModel::showEditNoteDialog,
            onDeleteNote = viewModel::showDeleteNoteConfirmation,
            onRetry = viewModel::refresh
        )
    }

    // Add Note Dialog
    if (uiState.showAddNoteDialog) {
        NoteDialog(
            title = "Add Note",
            noteContent = uiState.noteContent,
            onNoteContentChange = viewModel::updateNoteContent,
            isSaving = isSavingNote,
            onSave = viewModel::addNote,
            onDismiss = viewModel::hideAddNoteDialog
        )
    }

    // Edit Note Dialog
    if (uiState.showEditNoteDialog && uiState.editingNote != null) {
        NoteDialog(
            title = "Edit Note",
            noteContent = uiState.noteContent,
            onNoteContentChange = viewModel::updateNoteContent,
            isSaving = isSavingNote,
            onSave = viewModel::updateNote,
            onDismiss = viewModel::hideEditNoteDialog
        )
    }

    // Delete Note Confirmation Dialog
    if (uiState.showDeleteNoteConfirmation && uiState.deletingNote != null) {
        DeleteNoteConfirmationDialog(
            note = uiState.deletingNote!!,
            isDeleting = isDeletingNote,
            onConfirm = viewModel::deleteNote,
            onDismiss = viewModel::hideDeleteNoteConfirmation
        )
    }

    // Enrichment Panel Dialog (simplified for now)
    if (uiState.showEnrichmentPanel && enrichmentSuggestions.isNotEmpty()) {
        EnrichmentSuggestionsDialog(
            suggestions = enrichmentSuggestions,
            onApply = { selected ->
                viewModel.applyEnrichmentSuggestions(selected)
            },
            onDismiss = viewModel::hideEnrichmentPanel
        )
    }

    // Merge Dialog
    if (uiState.showMergeDialog) {
        val contact = (contactState as? DokusState.Success)?.data
        val activity = (activityState as? DokusState.Success)?.data

        if (contact != null) {
            ContactMergeDialog(
                sourceContact = contact,
                sourceActivity = activity,
                preselectedTarget = null,
                onMergeComplete = { result ->
                    // Navigate to the merged (target) contact
                    navController.navigateTo(
                        ContactsDestination.ContactDetails(result.targetContactId.toString())
                    )
                },
                onDismiss = viewModel::hideMergeDialog
            )
        }
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
    onMergeClick: () -> Unit
) {
    Column {
        TopAppBar(
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                            text = "Contact Details",
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
                                contentDescription = "Enrichment suggestions available",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd),
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ) { }
                        }
                    }
                }

                // Merge button (simplified - full merge dialog in phase 5)
                IconButton(onClick = onMergeClick) {
                    Icon(
                        imageVector = Icons.Default.MergeType,
                        contentDescription = "Merge contacts"
                    )
                }

                // Edit button
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit contact"
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

                // Activity Summary Section
                ActivitySummarySection(
                    state = activityState
                )

                // Notes Section
                NotesSection(
                    state = notesState,
                    onAddNote = onAddNote,
                    onEditNote = onEditNote,
                    onDeleteNote = onDeleteNote
                )

                // Bottom spacing
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Dialog for adding or editing a note.
 */
@Composable
private fun NoteDialog(
    title: String,
    noteContent: String,
    onNoteContentChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Note,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                PTextFieldFree(
                    fieldName = "Note",
                    value = noteContent,
                    onValueChange = onNoteContentChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (isSaving) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Saving...",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                TextButton(
                    onClick = onSave,
                    enabled = noteContent.isNotBlank()
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Medium,
                        color = if (noteContent.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        },
        dismissButton = {
            if (!isSaving) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/**
 * Confirmation dialog for deleting a note.
 */
@Composable
private fun DeleteNoteConfirmationDialog(
    note: ContactNoteDto,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Delete Note",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete this note?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = note.content.take(100) + if (note.content.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            if (isDeleting) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Deleting...",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = "Delete",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        dismissButton = {
            if (!isDeleting) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancel",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
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
    val selectedSuggestions = remember { mutableStateListOf<EnrichmentSuggestion>().apply { addAll(suggestions) } }

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
                text = "Enrichment Suggestions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "We found additional information that may improve this contact's data. Select which suggestions to apply:",
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
                                text = "Deselect All",
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
                                text = "Select All",
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
                        "Apply All"
                    } else {
                        "Apply Selected (${selectedSuggestions.size})"
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
                    text = "Not Now",
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
                            text = "Current:",
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
            text = "$percentage%",
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

