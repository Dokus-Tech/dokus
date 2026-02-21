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
    isOnline: Boolean,
    contentPadding: PaddingValues,
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
                    state = contactState
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

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactDetailsContentPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    val now = kotlinx.datetime.LocalDateTime(2026, 1, 15, 10, 0)
    val contactId = tech.dokus.domain.ids.ContactId.generate()
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactDetailsContent(
            contactState = DokusState.success(
                ContactDto(
                    id = contactId,
                    tenantId = tech.dokus.domain.ids.TenantId.generate(),
                    name = tech.dokus.domain.Name("Acme Corporation"),
                    email = tech.dokus.domain.Email("info@acme.be"),
                    vatNumber = tech.dokus.domain.ids.VatNumber("BE0123456789"),
                    defaultPaymentTerms = 30,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
            ),
            activityState = DokusState.success(
                ContactActivitySummary(
                    contactId = contactId,
                    invoiceCount = 5,
                    invoiceTotal = "10,000.00",
                    inboundInvoiceCount = 2,
                    inboundInvoiceTotal = "3,000.00",
                    expenseCount = 3,
                    expenseTotal = "500.00"
                )
            ),
            notesState = DokusState.success(emptyList()),
            isOnline = true,
            contentPadding = PaddingValues(0.dp),
            onAddNote = {},
            onEditNote = {},
            onDeleteNote = {}
        )
    }
}
