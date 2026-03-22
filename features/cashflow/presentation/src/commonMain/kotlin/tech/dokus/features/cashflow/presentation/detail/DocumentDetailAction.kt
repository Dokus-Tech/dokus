package tech.dokus.features.cashflow.presentation.detail

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.DocumentId

@Immutable
sealed interface DocumentDetailAction : MVIAction {
    data object NavigateBack : DocumentDetailAction
    data class NavigateToEntity(val entityId: String, val entityType: DocumentType) : DocumentDetailAction
    data class NavigateToCashflowEntry(val entryId: CashflowEntryId) : DocumentDetailAction
    data class DownloadDocument(val documentId: DocumentId) : DocumentDetailAction
}
