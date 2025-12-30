package tech.dokus.contacts.components

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
import tech.dokus.aura.resources.contacts_deleting
import tech.dokus.aura.resources.contacts_edit_note
import tech.dokus.aura.resources.contacts_load_notes_failed
import tech.dokus.aura.resources.contacts_no_notes
import tech.dokus.aura.resources.contacts_note_by
import tech.dokus.aura.resources.contacts_note_content
import tech.dokus.aura.resources.contacts_notes
import tech.dokus.aura.resources.contacts_saving
import tech.dokus.foundation.aura.components.fields.PTextFieldFree
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.state.DokusState

/**
 * Side panel for managing contact notes on desktop.
 * Displays a list of notes with add, edit, and delete functionality.
 *
 * This component replaces the AlertDialog-based NoteDialog for desktop,
 * providing a more spacious and user-friendly interface for notes management.
 *
 * @param isVisible Whether the panel is visible
 * @param onDismiss Callback when the panel should be dismissed
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
@Composable
fun NotesSidePanel(
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
    var noteToDelete by remember { mutableStateOf<ContactNoteDto?>(null) }
    var showAddNoteForm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
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
        }

        // Sidebar
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints {
                val sidebarWidth = (maxWidth / 3).coerceIn(320.dp, 400.dp)

                Card(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume click to prevent backdrop dismissal */ }
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Header
                        NotesSidePanelHeader(
                            onClose = onDismiss,
                            onAddClick = {
                                showAddNoteForm = true
                                onCancelEdit() // Clear any existing edit state
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Add/Edit Note Form
                        if (showAddNoteForm || editingNote != null) {
                            NoteForm(
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

                            Spacer(modifier = Modifier.height(16.dp))

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Notes list
                        when (notesState) {
                            is DokusState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            is DokusState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Note,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = stringResource(Res.string.contacts_no_notes),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = stringResource(Res.string.contacts_add_first_note_hint),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(
                                            items = notes,
                                            key = { it.id.toString() }
                                        ) { note ->
                                            NoteListItem(
                                                note = note,
                                                isEditing = editingNote?.id == note.id,
                                                onEditClick = {
                                                    showAddNoteForm = false
                                                    onEditNoteClick(note)
                                                },
                                                onDeleteClick = { noteToDelete = note }
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {
                                // Idle state
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    noteToDelete?.let { note ->
        DeleteNoteConfirmationDialog(
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

@Composable
private fun NotesSidePanelHeader(
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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

@Composable
private fun NoteForm(
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
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

                Spacer(modifier = Modifier.width(8.dp))

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

@Composable
private fun NoteListItem(
    note: ContactNoteDto,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isEditing) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDateTime(note.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        note.authorName?.let { author ->
                        Text(
                            text = stringResource(Res.string.contacts_note_by, author),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.contacts_edit_note),
                            modifier = Modifier.size(18.dp),
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
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

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
                text = stringResource(Res.string.contacts_delete_note),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.contacts_delete_note_confirm),
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
                    text = stringResource(Res.string.contacts_delete_note_warning),
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
                        text = stringResource(Res.string.contacts_deleting),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                TextButton(onClick = onConfirm) {
                    Text(
                        text = stringResource(Res.string.action_delete),
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
                        text = stringResource(Res.string.action_cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

/**
 * Format a LocalDateTime to a human-readable string.
 */
private fun formatDateTime(dateTime: LocalDateTime): String {
    val month = dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "${dateTime.dayOfMonth} $month ${dateTime.year}, ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}
