package tech.dokus.features.cashflow.mvi

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.enums.InvoiceDueDateMode
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceUiState
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.features.cashflow.mvi.model.InvoiceSection

@Immutable
data class CreateInvoiceState(
    val formState: CreateInvoiceFormState,
    val uiState: CreateInvoiceUiState,
    val invoiceNumberPreview: String? = null,
) : MVIState

@Immutable
sealed interface CreateInvoiceIntent : MVIIntent {
    // Client
    data class UpdateClientLookupQuery(val query: String) : CreateInvoiceIntent
    data class SetClientLookupExpanded(val expanded: Boolean) : CreateInvoiceIntent
    data class SelectClient(val client: ContactDto) : CreateInvoiceIntent
    data class SelectExternalClientCandidate(val candidate: ExternalClientCandidate) : CreateInvoiceIntent
    data class CreateClientManuallyFromQuery(val query: String) : CreateInvoiceIntent
    data object ClearClient : CreateInvoiceIntent
    data class RefreshPeppolStatus(val contactId: ContactId, val force: Boolean = false) : CreateInvoiceIntent

    // Dates & terms
    data object OpenIssueDatePicker : CreateInvoiceIntent
    data object OpenDueDatePicker : CreateInvoiceIntent
    data object CloseDatePicker : CreateInvoiceIntent
    data class SelectDate(val date: LocalDate) : CreateInvoiceIntent
    data class UpdateIssueDate(val date: LocalDate) : CreateInvoiceIntent
    data class UpdateDueDate(val date: LocalDate) : CreateInvoiceIntent
    data class UpdatePaymentTermsDays(val days: Int) : CreateInvoiceIntent
    data class UpdateDueDateMode(val mode: InvoiceDueDateMode) : CreateInvoiceIntent

    // Line items
    data class ExpandItem(val itemId: String) : CreateInvoiceIntent
    data object CollapseItem : CreateInvoiceIntent
    data class ToggleItemExpanded(val itemId: String) : CreateInvoiceIntent
    data object AddLineItem : CreateInvoiceIntent
    data class RemoveLineItem(val itemId: String) : CreateInvoiceIntent
    data class UpdateItemDescription(val itemId: String, val description: String) : CreateInvoiceIntent
    data class UpdateItemQuantity(val itemId: String, val quantity: Double) : CreateInvoiceIntent
    data class UpdateItemUnitPrice(val itemId: String, val unitPrice: String) : CreateInvoiceIntent
    data class UpdateItemVatRate(val itemId: String, val vatRatePercent: Int) : CreateInvoiceIntent
    data object ApplyLatestInvoiceLines : CreateInvoiceIntent
    data object DismissLatestInvoiceSuggestion : CreateInvoiceIntent

    // Payment & delivery
    data class UpdateStructuredCommunication(val value: String) : CreateInvoiceIntent
    data class UpdateSenderIban(val value: String) : CreateInvoiceIntent
    data class UpdateSenderBic(val value: String) : CreateInvoiceIntent
    data class SelectDeliveryPreference(val method: InvoiceDeliveryMethod) : CreateInvoiceIntent
    data class UpdateNotes(val notes: String) : CreateInvoiceIntent

    // Section visibility
    data class ToggleSection(val section: InvoiceSection) : CreateInvoiceIntent
    data class ExpandSection(val section: InvoiceSection) : CreateInvoiceIntent
    data class CollapseSection(val section: InvoiceSection) : CreateInvoiceIntent

    // Preview + submission
    data class SetPreviewVisible(val visible: Boolean) : CreateInvoiceIntent
    data object SaveAsDraft : CreateInvoiceIntent
    data object SubmitWithResolvedDelivery : CreateInvoiceIntent
    data object BackClicked : CreateInvoiceIntent
    data object ResetForm : CreateInvoiceIntent
    data object LoadDefaults : CreateInvoiceIntent
}

@Immutable
sealed interface CreateInvoiceAction : MVIAction {
    data object NavigateBack : CreateInvoiceAction
    data class NavigateToCreateContact(
        val prefillCompanyName: String? = null,
        val prefillVat: String? = null,
        val prefillAddress: String? = null,
        val origin: String? = null
    ) : CreateInvoiceAction
    data class NavigateToInvoice(val invoiceId: InvoiceId) : CreateInvoiceAction
    data class ShowError(val message: String) : CreateInvoiceAction
    data class ShowValidationError(val message: String) : CreateInvoiceAction
    data class OpenExternalUrl(val url: String) : CreateInvoiceAction
    data class ShowSuccess(val message: String) : CreateInvoiceAction
}
