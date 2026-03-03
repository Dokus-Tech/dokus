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
import tech.dokus.domain.Money
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.OfflineOverlay

private val SectionSpacing = 16.dp

@Composable
internal fun ContactDetailsContent(
    contactState: DokusState<ContactDto>,
    activityState: DokusState<ContactActivitySummary>,
    invoiceSnapshotState: DokusState<ContactInvoiceSnapshot>,
    peppolStatusState: DokusState<PeppolStatusResponse>,
    notesState: DokusState<List<ContactNoteDto>>,
    isOnline: Boolean,
    contentPadding: PaddingValues,
    showInlineActions: Boolean,
    hasEnrichmentSuggestions: Boolean,
    onEditContact: () -> Unit,
    onMergeContact: () -> Unit,
    onShowEnrichment: () -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (ContactNoteDto) -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit
) {
    // Kept for merge-route data consistency; section intentionally hidden in v16 layout.
    activityState

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
            val contact = (contactState as? DokusState.Success)?.data

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(SectionSpacing)
            ) {
                ContactHeroSection(
                    contactState = contactState,
                    peppolStatusState = peppolStatusState,
                    showInlineActions = showInlineActions,
                    hasEnrichmentSuggestions = hasEnrichmentSuggestions,
                    isOnline = isOnline,
                    onEditContact = onEditContact,
                    onMergeContact = onMergeContact,
                    onShowEnrichment = onShowEnrichment
                )

                ContactStatsSection(invoiceSnapshotState = invoiceSnapshotState)
                ContactInfoSectionCompact(contact = contact)
                RecentDocumentsSection(invoiceSnapshotState = invoiceSnapshotState)

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

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

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
                ContactActivitySummary(contactId = contactId, invoiceCount = 5)
            ),
            invoiceSnapshotState = DokusState.success(
                ContactInvoiceSnapshot(
                    documentsCount = 3,
                    totalVolume = Money(129652),
                    outstanding = Money(96252),
                    recentDocuments = emptyList()
                )
            ),
            peppolStatusState = DokusState.success(
                PeppolStatusResponse(
                    status = PeppolStatusResponse.STATUS_FOUND,
                    refreshed = false
                )
            ),
            notesState = DokusState.success(emptyList()),
            isOnline = true,
            contentPadding = PaddingValues(0.dp),
            showInlineActions = true,
            hasEnrichmentSuggestions = true,
            onEditContact = {},
            onMergeContact = {},
            onShowEnrichment = {},
            onAddNote = {},
            onEditNote = {},
            onDeleteNote = {}
        )
    }
}
