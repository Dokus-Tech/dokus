package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
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
import tech.dokus.aura.resources.contacts_saving
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.fields.PTextFieldFree
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.datetime.LocalDateTime
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

internal object ContactNotesTestTags {
    const val BottomSheetRoot = "contact_notes_bottom_sheet"
    const val PaneRoot = "contact_notes_pane"
    const val ComposerRoot = "contact_notes_composer"
    const val DeleteConfirmationRoot = "contact_notes_delete_confirmation"
}

private const val ContainerAlphaSelected = 0.3f
private const val ContainerAlphaDefault = 0.5f
private const val IconAlphaDisabled = 0.5f
private const val TextAlphaSecondary = 0.7f
private const val DividerAlpha = 0.5f
private const val NotePreviewMaxLength = 100

private val SpacingSmall = Constraints.Spacing.small
private val SpacingMedium = Constraints.Spacing.medium
private val SpacingLarge = Constraints.Spacing.large
private val IconSizeSmall = Constraints.IconSize.xSmall
private val IconSizeMedium = Constraints.IconSize.small
private val IconSizeLarge = Constraints.IconSize.xLarge
private val CardCornerRadius = Constraints.Spacing.small
private val NoteItemPadding = Constraints.Spacing.medium
private val ContentMinHeight = Constraints.SearchField.minWidth

internal fun shouldShowNoteComposer(
    showAddNoteDialog: Boolean,
    showEditNoteDialog: Boolean,
): Boolean = showAddNoteDialog || showEditNoteDialog

@Composable
internal fun ContactNotesComposer(
    noteContent: String,
    onNoteContentChange: (String) -> Unit,
    isSaving: Boolean,
    isEditing: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(ContactNotesTestTags.ComposerRoot),
        verticalArrangement = Arrangement.spacedBy(SpacingMedium)
    ) {
        Text(
            text = if (isEditing) {
                stringResource(Res.string.contacts_edit_note)
            } else {
                stringResource(Res.string.contacts_add_note)
            },
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

                Spacer(modifier = Modifier.height(SpacingSmall))

                PButton(
                    text = stringResource(Res.string.action_save),
                    isEnabled = noteContent.isNotBlank(),
                    onClick = onSave,
                )
            }
        }
    }
}

@Composable
internal fun ContactNotesList(
    notesState: DokusState<List<ContactNoteDto>>,
    editingNote: ContactNoteDto?,
    onEditClick: (ContactNoteDto) -> Unit,
    onDeleteClick: (ContactNoteDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (notesState) {
        is DokusState.Loading, is DokusState.Idle -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = ContentMinHeight),
                contentAlignment = Alignment.Center
            ) {
                DokusLoader()
            }
        }

        is DokusState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(min = ContentMinHeight),
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
                        .heightIn(min = ContentMinHeight),
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
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = if (editingNote?.id == note.id) {
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
                                        IconButton(onClick = { onEditClick(note) }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(Res.string.contacts_edit_note),
                                                modifier = Modifier.size(IconSizeMedium),
                                                tint = if (editingNote?.id == note.id) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                        }
                                        IconButton(onClick = { onDeleteClick(note) }) {
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
                }
            }
        }
    }
}

@Composable
internal fun ContactNoteDeleteDialog(
    note: ContactNoteDto,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DokusDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        title = stringResource(Res.string.contacts_delete_note),
        modifier = Modifier.testTag(ContactNotesTestTags.DeleteConfirmationRoot),
        icon = {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(SpacingSmall)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_delete_note_confirm),
                    style = MaterialTheme.typography.bodyLarge
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = DividerAlpha),
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

// ============================================================================
// PREVIEWS
// ============================================================================

private val PreviewNow = LocalDateTime(2026, 1, 15, 10, 0)

private val PreviewNotes = listOf(
    ContactNoteDto(
        id = ContactNoteId.generate(),
        contactId = ContactId.generate(),
        tenantId = TenantId.generate(),
        content = "Called about invoice #2024-001. Will pay by end of month.",
        authorName = "John Doe",
        createdAt = PreviewNow,
        updatedAt = PreviewNow
    ),
    ContactNoteDto(
        id = ContactNoteId.generate(),
        contactId = ContactId.generate(),
        tenantId = TenantId.generate(),
        content = "Prefers email communication over phone.",
        authorName = null,
        createdAt = PreviewNow,
        updatedAt = PreviewNow
    )
)

@Preview
@Composable
private fun ContactNotesComposerPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ContactNotesComposer(
            noteContent = "Draft note content",
            onNoteContentChange = {},
            isSaving = false,
            isEditing = false,
            onSave = {},
            onCancel = {}
        )
    }
}

@Preview
@Composable
private fun ContactNotesListPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ContactNotesList(
            notesState = DokusState.success(PreviewNotes),
            editingNote = null,
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview
@Composable
private fun ContactNoteDeleteDialogPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ContactNoteDeleteDialog(
            note = PreviewNotes.first(),
            isDeleting = false,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
