package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.StickyNote
import com.composables.icons.lucide.Trash2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_add_note
import tech.dokus.aura.resources.contacts_delete_note
import tech.dokus.aura.resources.contacts_edit_note
import tech.dokus.aura.resources.contacts_no_notes
import tech.dokus.aura.resources.contacts_note_by
import tech.dokus.aura.resources.contacts_notes
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.common.ErrorOverlay
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.ContactNoteId
import tech.dokus.domain.ids.TenantId
import kotlinx.datetime.LocalDateTime

// UI dimension constants
private val SpacingSmall = 4.dp
private val SpacingDefault = 8.dp
private val SpacingMedium = 16.dp
private val SpacingLarge = 24.dp
private val CardPadding = 12.dp
private val IconSizeSmall = 16.dp
private val IconSizeMedium = 18.dp
private val IconSizeLarge = 32.dp
private val ShimmerWidthMedium = 120.dp

// Alpha constants
private const val IconAlphaDisabled = 0.6f

// Skeleton repeat count
private const val SkeletonItemCount = 3

// Shimmer fill fraction
private const val ShimmerFillFraction = 0.8f

@Composable
internal fun NotesSection(
    state: DokusState<List<ContactNoteDto>>,
    onAddNote: () -> Unit,
    onEditNote: (ContactNoteDto) -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.contacts_notes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(onClick = onAddNote) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(IconSizeMedium)
                    )
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(stringResource(Res.string.contacts_add_note))
                }
            }

            ErrorOverlay(
                exception = if (state is DokusState.Error) state.exception else null,
                retryHandler = if (state is DokusState.Error) state.retryHandler else null,
            ) {
                when (state) {
                    is DokusState.Loading, is DokusState.Idle -> NotesSkeleton()
                    is DokusState.Success -> {
                        if (state.data.isEmpty()) {
                            NotesEmptyState()
                        } else {
                            NotesContent(
                                notes = state.data,
                                onEditNote = onEditNote,
                                onDeleteNote = onDeleteNote,
                            )
                        }
                    }
                    is DokusState.Error -> NotesEmptyState()
                }
            }
        }
    }
}

@Composable
private fun NotesContent(
    notes: List<ContactNoteDto>,
    onEditNote: (ContactNoteDto) -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SpacingDefault)
    ) {
        notes.forEach { note ->
            NoteItem(
                note = note,
                onEdit = { onEditNote(note) },
                onDelete = { onDeleteNote(note) }
            )
        }
    }
}

@Composable
private fun NoteItem(
    note: ContactNoteDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    DokusCardSurface(
        variant = DokusCardVariant.Soft,
    ) {
        Column(
            modifier = Modifier.padding(CardPadding),
            verticalArrangement = Arrangement.spacedBy(SpacingDefault)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SpacingDefault)
                    ) {
                        Icon(
                            imageVector = Lucide.StickyNote,
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

                    Spacer(modifier = Modifier.height(SpacingDefault))

                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Lucide.Pencil,
                            contentDescription = stringResource(Res.string.contacts_edit_note),
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Lucide.Trash2,
                            contentDescription = stringResource(Res.string.contacts_delete_note),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Lucide.StickyNote,
            contentDescription = null,
            modifier = Modifier.size(IconSizeLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = IconAlphaDisabled)
        )
        Spacer(modifier = Modifier.height(SpacingDefault))
        Text(
            text = stringResource(Res.string.contacts_no_notes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NotesSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(SpacingDefault)
    ) {
        repeat(SkeletonItemCount) {
            DokusCardSurface(
                variant = DokusCardVariant.Soft
            ) {
                Column(
                    modifier = Modifier.padding(CardPadding),
                    verticalArrangement = Arrangement.spacedBy(SpacingDefault)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ShimmerLine(modifier = Modifier.width(ShimmerWidthMedium), height = CardPadding)
                        ShimmerLine(modifier = Modifier.size(IconSizeSmall), height = IconSizeSmall)
                    }
                    ShimmerLine(modifier = Modifier.fillMaxWidth(), height = IconSizeSmall)
                    ShimmerLine(modifier = Modifier.fillMaxWidth(ShimmerFillFraction), height = IconSizeSmall)
                }
            }
        }
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun NotesSectionPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 1, 15, 10, 0)
    TestWrapper(parameters) {
        NotesSection(
            state = DokusState.success(
                listOf(
                    ContactNoteDto(
                        id = ContactNoteId.generate(),
                        contactId = ContactId.generate(),
                        tenantId = TenantId.generate(),
                        content = "Called about invoice #2024-001. Will pay by end of month.",
                        authorName = "John Doe",
                        createdAt = now,
                        updatedAt = now
                    ),
                    ContactNoteDto(
                        id = ContactNoteId.generate(),
                        contactId = ContactId.generate(),
                        tenantId = TenantId.generate(),
                        content = "Prefers email communication over phone.",
                        authorName = null,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            ),
            onAddNote = {},
            onEditNote = {},
            onDeleteNote = {}
        )
    }
}
