package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.action_delete
import tech.dokus.aura.resources.action_save
import tech.dokus.aura.resources.contacts_add_first_note_hint
import tech.dokus.aura.resources.contacts_add_note
import tech.dokus.aura.resources.contacts_delete_note
import tech.dokus.aura.resources.contacts_delete_note_confirm
import tech.dokus.aura.resources.contacts_delete_note_warning
import tech.dokus.aura.resources.contacts_edit_note
import tech.dokus.aura.resources.contacts_load_notes_failed
import tech.dokus.aura.resources.contacts_no_notes
import tech.dokus.aura.resources.contacts_note_by
import tech.dokus.aura.resources.contacts_note_content
import tech.dokus.aura.resources.contacts_notes
import tech.dokus.aura.resources.contacts_saving
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.components.fields.PTextFieldFree
import tech.dokus.foundation.aura.constrains.Constrains

// UI dimension constants
private val ContentPadding = Constrains.Spacing.large
private val SpacingSmall = Constrains.Spacing.small
private val SpacingMedium = Constrains.Spacing.medium
private val SpacingDefault = Constrains.Spacing.large
private val DragHandlePadding = Constrains.Spacing.medium
private val DragHandleWidth = Constrains.Spacing.xxLarge
private val DragHandleHeight = Constrains.Spacing.xSmall
private val DragHandleCornerRadius = Constrains.Spacing.xxSmall
private val IconSizeSmall = Constrains.IconSize.xSmall
private val IconSizeMedium = Constrains.IconSize.small
private val IconSizeLarge = Constrains.IconSize.xLarge
private val CardCornerRadius = Constrains.Spacing.small
private val ButtonSpacing = Constrains.Spacing.xSmall
private val NoteItemPadding = Constrains.Spacing.medium
private val ContentMinHeight = Constrains.SearchField.minWidth

// Alpha constants
private const val DragHandleAlpha = 0.4f
private const val DividerAlpha = 0.5f
private const val ContainerAlphaSelected = 0.3f
private const val ContainerAlphaDefault = 0.5f
private const val IconAlphaDisabled = 0.5f
private const val TextAlphaSecondary = 0.7f

// Content preview constants
private const val NotePreviewMaxLength = 100

/**
 * Bottom sheet for managing contact notes on mobile.
 * Displays a list of notes with add, edit, and delete functionality.
 *
 * This component replaces the AlertDialog-based NoteDialog for mobile,
 * providing a native mobile experience with swipe-to-dismiss support.
 *
 * @param isVisible Whether the bottom sheet is visible
 * @param onDismiss Callback when the bottom sheet should be dismissed
 * @param notesState The current state of notes (loading, success, error)
 * @param noteContent The current note content being edited/added
 * @param onNoteContentChange Callback when note content changes
 * @param isSavingNote Whether a note is currently being saved
 * @param isDeletingNote Whether a note is currently being deleted
 * @param editingNote The note currently being edited, or null if adding new
 * @param onAddNote Callback to add a new note
 * @param onUpdateNote Callback to update an existing note
 * @param onDeleteNote Callback to delete a note
 * @param onEditNoteClick Callback when edit button is clicked on a note
 * @param onCancelEdit Callback to cancel the current edit/add operation
 * @param modifier Optional modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    notesState: DokusState<List<ContactNoteDto>>,
    noteContent: String,
    onNoteContentChange: (String) -> Unit,
    isSavingNote: Boolean,
    isDeletingNote: Boolean,
    editingNote: ContactNoteDto?,
    onAddNote: () -> Unit,
    onUpdateNote: () -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit,
    onEditNoteClick: (ContactNoteDto) -> Unit,
    onCancelEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    var noteToDelete by remember { mutableStateOf<ContactNoteDto?>(null) }
    var showAddNoteForm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Header
            NotesBottomSheetHeader(
                onClose = onDismiss,
                onAddClick = {
                    showAddNoteForm = true
                    onCancelEdit() // Clear any existing edit state
                }
            )

            Spacer(modifier = Modifier.height(SpacingDefault))

            // Add/Edit Note Form
            if (showAddNoteForm || editingNote != null) {
                NotesBottomSheetForm(
                    title = if (editingNote != null) {
                        stringResource(Res.string.contacts_edit_note)
                    } else {
                        stringResource(Res.string.contacts_add_note)
                    },
                    noteContent = noteContent,
                    onNoteContentChange = onNoteContentChange,
                    isSaving = isSavingNote,
                    onSave = {
                        if (editingNote != null) {
                            onUpdateNote()
                        } else {
                            onAddNote()
                        }
                        showAddNoteForm = false
                    },
                    onCancel = {
                        showAddNoteForm = false
                        onCancelEdit()
                    }
                )

                Spacer(modifier = Modifier.height(SpacingDefault))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DividerAlpha)
                )

                Spacer(modifier = Modifier.height(SpacingDefault))
            }

            // Notes list
            NotesBottomSheetContent(
                notesState = notesState,
                editingNote = editingNote,
                onEditClick = { note ->
                    showAddNoteForm = false
                    onEditNoteClick(note)
                },
                onDeleteClick = { note -> noteToDelete = note }
            )

            // Bottom spacing
            Spacer(modifier = Modifier.height(SpacingDefault))
        }
    }

    // Delete confirmation dialog
    noteToDelete?.let { note ->
        NotesBottomSheetDeleteConfirmation(
            note = note,
            isDeleting = isDeletingNote,
            onConfirm = {
                onDeleteNote(note)
                noteToDelete = null
            },
            onDismiss = { noteToDelete = null }
        )
    }
}

/**
 * Drag handle for the bottom sheet.
 */
@Composable
private fun BottomSheetDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DragHandlePadding),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(DragHandleWidth)
                .height(DragHandleHeight),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DragHandleAlpha),
            shape = RoundedCornerShape(DragHandleCornerRadius)
        ) {}
    }
}

/**
 * Header for the notes bottom sheet with title and action buttons.
 */
@Composable
private fun NotesBottomSheetHeader(
    onClose: () -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.contacts_notes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonSpacing)
        ) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(Res.string.contacts_add_note),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.action_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Form for adding or editing a note.
 */
@Composable
private fun NotesBottomSheetForm(
    title: String,
    noteContent: String,
    onNoteContentChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        PTextFieldFree(
            fieldName = stringResource(Res.string.contacts_note_content),
            value = noteContent,
            onValueChange = onNoteContentChange,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSaving) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
                ) {
                    DokusLoader(size = DokusLoaderSize.Small)
                    Text(
                        text = stringResource(Res.string.contacts_saving),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                TextButton(onClick = onCancel) {
                    Text(
                        text = stringResource(Res.string.action_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(SpacingSmall))

                Button(
                    onClick = onSave,
                    enabled = noteContent.isNotBlank()
                ) {
                    Text(stringResource(Res.string.action_save))
                }
            }
        }
    }
}

/**
 * Content section displaying the notes list with loading/error/empty states.
 */
@Composable
private fun NotesBottomSheetContent(
    notesState: DokusState<List<ContactNoteDto>>,
    editingNote: ContactNoteDto?,
    onEditClick: (ContactNoteDto) -> Unit,
    onDeleteClick: (ContactNoteDto) -> Unit,
    modifier: Modifier = Modifier
) {
    when (notesState) {
        is DokusState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContentMinHeight),
                contentAlignment = Alignment.Center
            ) {
                DokusLoader()
            }
        }

        is DokusState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContentMinHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SpacingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(Res.string.contacts_load_notes_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        is DokusState.Success -> {
            val notes = notesState.data

            if (notes.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(ContentMinHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(SpacingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Note,
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeLarge),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = IconAlphaDisabled)
                        )
                        Text(
                            text = stringResource(Res.string.contacts_no_notes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.contacts_add_first_note_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = TextAlphaSecondary)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SpacingSmall)
                ) {
                    items(
                        items = notes,
                        key = { it.id.toString() }
                    ) { note ->
                        NotesBottomSheetListItem(
                            note = note,
                            isEditing = editingNote?.id == note.id,
                            onEditClick = { onEditClick(note) },
                            onDeleteClick = { onDeleteClick(note) }
                        )
                    }
                }
            }
        }

        else -> {
            // Idle state
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContentMinHeight),
                contentAlignment = Alignment.Center
            ) {
                DokusLoader()
            }
        }
    }
}

/**
 * Individual note item in the list.
 */
@Composable
private fun NotesBottomSheetListItem(
    note: ContactNoteDto,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isEditing) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = ContainerAlphaSelected)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ContainerAlphaDefault)
        },
        shape = RoundedCornerShape(CardCornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(NoteItemPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Note,
                            contentDescription = null,
                            modifier = Modifier.size(IconSizeSmall),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDateTime(note.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        note.authorName?.let { author ->
                            Text(
                                text = stringResource(Res.string.contacts_note_by, author),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(SpacingSmall))

                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.contacts_edit_note),
                            modifier = Modifier.size(IconSizeMedium),
                            tint = if (isEditing) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.contacts_delete_note),
                            modifier = Modifier.size(IconSizeMedium),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Delete confirmation dialog for a note.
 */
@Composable
private fun NotesBottomSheetDeleteConfirmation(
    note: ContactNoteDto,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DokusDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = stringResource(Res.string.contacts_delete_note),
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_delete_note_confirm),
                    style = MaterialTheme.typography.bodyLarge
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = ContainerAlphaDefault),
                    shape = RoundedCornerShape(CardCornerRadius)
                ) {
                    val previewText = note.content.take(NotePreviewMaxLength) +
                        if (note.content.length > NotePreviewMaxLength) "..." else ""
                    Text(
                        text = previewText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(NoteItemPadding)
                    )
                }
                Text(
                    text = stringResource(Res.string.contacts_delete_note_warning),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_delete),
            onClick = onConfirm,
            isLoading = isDeleting,
            isDestructive = true,
            enabled = !isDeleting
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_cancel),
            onClick = onDismiss,
            enabled = !isDeleting
        ),
        dismissOnBackPress = !isDeleting,
        dismissOnClickOutside = !isDeleting
    )
}
