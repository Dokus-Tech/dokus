package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.contacts_add_note
import tech.dokus.aura.resources.contacts_notes
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import kotlinx.datetime.LocalDateTime
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val ContentPadding = Constraints.Spacing.large
private val DragHandlePadding = Constraints.Spacing.medium
private val DragHandleWidth = Constraints.Spacing.xxLarge
private val DragHandleHeight = Constraints.Spacing.xSmall
private val DragHandleCornerRadius = Constraints.Spacing.xxSmall
private val ContentSpacing = Constraints.Spacing.large
private val HeaderButtonSpacing = Constraints.Spacing.xSmall
private const val DragHandleAlpha = 0.4f
private const val DividerAlpha = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    notesState: DokusState<List<ContactNoteDto>>,
    noteContent: String,
    onNoteContentChange: (String) -> Unit,
    isSavingNote: Boolean,
    editingNote: ContactNoteDto?,
    showComposer: Boolean,
    onShowAddNote: () -> Unit,
    onSaveNote: () -> Unit,
    onEditNoteClick: (ContactNoteDto) -> Unit,
    onDeleteNoteClick: (ContactNoteDto) -> Unit,
    onDismissComposer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { NotesBottomSheetDragHandle() },
        modifier = modifier.testTag(ContactNotesTestTags.BottomSheetRoot)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ContentPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            NotesBottomSheetHeader(
                onClose = onDismiss,
                onAddClick = onShowAddNote
            )

            Spacer(modifier = Modifier.height(ContentSpacing))

            if (showComposer) {
                ContactNotesComposer(
                    noteContent = noteContent,
                    onNoteContentChange = onNoteContentChange,
                    isSaving = isSavingNote,
                    isEditing = editingNote != null,
                    onSave = onSaveNote,
                    onCancel = onDismissComposer
                )

                Spacer(modifier = Modifier.height(ContentSpacing))

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DividerAlpha)
                )

                Spacer(modifier = Modifier.height(ContentSpacing))
            }

            ContactNotesList(
                notesState = notesState,
                editingNote = editingNote,
                onEditClick = onEditNoteClick,
                onDeleteClick = onDeleteNoteClick
            )

            Spacer(modifier = Modifier.height(ContentSpacing))
        }
    }
}

@Composable
private fun NotesBottomSheetDragHandle(
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Box(
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
            horizontalArrangement = Arrangement.spacedBy(HeaderButtonSpacing)
        ) {
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Lucide.Plus,
                    contentDescription = stringResource(Res.string.contacts_add_note),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = stringResource(Res.string.action_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
private fun NotesBottomSheetPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 1, 15, 10, 0)
    TestWrapper(parameters) {
        NotesBottomSheet(
            isVisible = true,
            onDismiss = {},
            notesState = DokusState.success(
                listOf(
                    ContactNoteDto(
                        id = ContactNoteId.generate(),
                        contactId = ContactId.generate(),
                        tenantId = TenantId.generate(),
                        content = "Called about invoice #2024-001. Will pay by end of month.",
                        authorName = "John Doe",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            ),
            noteContent = "",
            onNoteContentChange = {},
            isSavingNote = false,
            editingNote = null,
            showComposer = true,
            onShowAddNote = {},
            onSaveNote = {},
            onEditNoteClick = {},
            onDeleteNoteClick = {},
            onDismissComposer = {},
        )
    }
}
