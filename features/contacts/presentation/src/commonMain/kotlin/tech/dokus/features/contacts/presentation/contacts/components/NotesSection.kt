package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.ShimmerLine

// UI dimension constants
private val SpacingSmall = 4.dp
private val SpacingDefault = 8.dp
private val SpacingMedium = 16.dp
private val SpacingLarge = 24.dp
private val ErrorPaddingVertical = 32.dp
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
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(IconSizeMedium)
                    )
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(stringResource(Res.string.contacts_add_note))
                }
            }

            when (state) {
                is DokusState.Loading, is DokusState.Idle -> {
                    NotesSkeleton()
                }
                is DokusState.Success -> {
                    if (state.data.isEmpty()) {
                        NotesEmptyState()
                    } else {
                        NotesContent(
                            notes = state.data,
                            onEditNote = onEditNote,
                            onDeleteNote = onDeleteNote
                        )
                    }
                }
                is DokusState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ErrorPaddingVertical),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusErrorContent(
                            exception = state.exception,
                            retryHandler = state.retryHandler
                        )
                    }
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

                    Spacer(modifier = Modifier.height(SpacingDefault))

                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.contacts_edit_note),
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
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
            imageVector = Icons.AutoMirrored.Filled.Note,
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
