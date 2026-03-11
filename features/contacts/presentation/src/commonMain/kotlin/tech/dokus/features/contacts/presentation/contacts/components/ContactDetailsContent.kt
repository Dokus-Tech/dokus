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
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
import tech.dokus.foundation.aura.components.common.OfflineOverlay
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.Name
import tech.dokus.domain.Email
import kotlinx.datetime.LocalDateTime

private val SectionSpacing = 16.dp

@Composable
internal fun ContactDetailsContent(
    contactState: DokusState<ContactDto>,
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
    onDocumentClick: (DocumentId) -> Unit,
    onAddNote: () -> Unit,
    onEditNote: (ContactNoteDto) -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing)
    ) {
        if (contactState is DokusState.Error) {
            DokusErrorBanner(
                exception = contactState.exception,
                retryHandler = contactState.retryHandler,
            )
        }

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
        ContactInfoSectionCompact(contact = (contactState as? DokusState.Success)?.data)
        RecentDocumentsSection(
            invoiceSnapshotState = invoiceSnapshotState,
            onDocumentClick = onDocumentClick,
        )

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

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactDetailsContentPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 1, 15, 10, 0)
    val contactId = ContactId.generate()
    TestWrapper(parameters) {
        ContactDetailsContent(
            contactState = DokusState.success(
                ContactDto(
                    id = contactId,
                    tenantId = TenantId.generate(),
                    name = Name("Acme Corporation"),
                    email = Email("info@acme.be"),
                    vatNumber = VatNumber("BE0123456789"),
                    defaultPaymentTerms = 30,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
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
            onDocumentClick = {},
            onAddNote = {},
            onEditNote = {},
            onDeleteNote = {}
        )
    }
}
