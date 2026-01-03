package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.OfflineOverlay

@Composable
internal fun ContactDetailsContent(
    contactState: DokusState<ContactDto>,
    activityState: DokusState<ContactActivitySummary>,
    notesState: DokusState<List<ContactNoteDto>>,
    isTogglingPeppol: Boolean,
    isOnline: Boolean,
    contentPadding: PaddingValues,
    onPeppolToggle: (Boolean) -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (ContactNoteDto) -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit
) {
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
                ContactInfoSection(
                    state = contactState,
                    onPeppolToggle = onPeppolToggle,
                    isTogglingPeppol = isTogglingPeppol
                )

                OfflineOverlay(isOffline = !isOnline) {
                    ActivitySummarySection(
                        state = if (!isOnline && activityState is DokusState.Error) {
                            DokusState.loading()
                        } else {
                            activityState
                        }
                    )
                }

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

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
