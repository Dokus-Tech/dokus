package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.usecase.SubmitInvoiceUseCase
import ai.dokus.app.cashflow.usecase.ValidateInvoiceUseCase
import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceUiState
import ai.dokus.app.cashflow.viewmodel.model.DatePickerTarget
import ai.dokus.app.cashflow.viewmodel.model.InvoiceCreationStep
import ai.dokus.app.cashflow.viewmodel.model.InvoiceDeliveryMethod
import ai.dokus.app.cashflow.viewmodel.model.InvoiceLineItem
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for creating a new invoice with form validation and submission.
 */
internal class CreateInvoiceViewModel : BaseViewModel<DokusState<FinancialDocumentDto.InvoiceDto>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<CreateInvoiceViewModel>()
    private val validateInvoice: ValidateInvoiceUseCase by inject()
    private val submitInvoice: SubmitInvoiceUseCase by inject()

    private val _clientsState = MutableStateFlow<DokusState<List<ContactDto>>>(DokusState.idle())
    val clientsState: StateFlow<DokusState<List<ContactDto>>> = _clientsState.asStateFlow()

    private val _uiState = MutableStateFlow(CreateInvoiceUiState())
    val uiState: StateFlow<CreateInvoiceUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(createInitialFormState())
    val formState: StateFlow<CreateInvoiceFormState> = _formState.asStateFlow()

    private val _createdInvoiceId = MutableStateFlow<InvoiceId?>(null)
    val createdInvoiceId: StateFlow<InvoiceId?> = _createdInvoiceId.asStateFlow()

    @OptIn(ExperimentalTime::class)
    private fun createInitialFormState(): CreateInvoiceFormState {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstItem = InvoiceLineItem()
        _uiState.update { ui -> ui.copy(expandedItemId = firstItem.id) }
        return CreateInvoiceFormState(
            issueDate = today,
            dueDate = today.plus(30, DateTimeUnit.DAY),
            items = listOf(firstItem)
        )
    }

    init { loadClients() }

    fun loadClients() {
        scope.launch {
            logger.d { "Loading contacts for invoice creation" }
            _clientsState.value = DokusState.loading()
            // TODO: Replace with contacts data source when available
            logger.w { "Contacts data source not yet implemented" }
            _clientsState.value = DokusState.success(emptyList())
        }
    }

    fun getFilteredClients(): List<ContactDto> {
        val clients = (_clientsState.value as? DokusState.Success)?.data ?: return emptyList()
        val query = _uiState.value.clientSearchQuery.trim().lowercase()
        if (query.isBlank()) return clients
        return clients.filter { client ->
            client.name.value.lowercase().contains(query) ||
            client.email?.value?.lowercase()?.contains(query) == true ||
            client.vatNumber?.value?.lowercase()?.contains(query) == true
        }
    }

    fun updateClientSearchQuery(query: String) { _uiState.update { it.copy(clientSearchQuery = query) } }
    fun openClientPanel() { _uiState.update { it.copy(isClientPanelOpen = true, clientSearchQuery = "") } }
    fun closeClientPanel() { _uiState.update { it.copy(isClientPanelOpen = false, clientSearchQuery = "") } }

    fun selectClientAndClose(client: ContactDto) {
        _formState.update { it.copy(selectedClient = client, errors = it.errors - "client") }
        closeClientPanel()
    }

    fun openIssueDatePicker() { _uiState.update { it.copy(isDatePickerOpen = DatePickerTarget.ISSUE_DATE) } }
    fun openDueDatePicker() { _uiState.update { it.copy(isDatePickerOpen = DatePickerTarget.DUE_DATE) } }
    fun closeDatePicker() { _uiState.update { it.copy(isDatePickerOpen = null) } }

    fun selectDate(date: LocalDate) {
        val target = _uiState.value.isDatePickerOpen ?: return
        when (target) {
            DatePickerTarget.ISSUE_DATE -> _formState.update { it.copy(issueDate = date) }
            DatePickerTarget.DUE_DATE -> _formState.update { it.copy(dueDate = date) }
        }
        closeDatePicker()
    }

    fun expandItem(itemId: String) { _uiState.update { it.copy(expandedItemId = itemId) } }
    fun collapseItem() { _uiState.update { it.copy(expandedItemId = null) } }
    fun toggleItemExpanded(itemId: String) {
        _uiState.update { if (it.expandedItemId == itemId) it.copy(expandedItemId = null) else it.copy(expandedItemId = itemId) }
    }

    fun addLineItem(): String {
        val newItem = InvoiceLineItem()
        _formState.update { it.copy(items = it.items + newItem) }
        _uiState.update { it.copy(expandedItemId = newItem.id) }
        return newItem.id
    }

    fun removeLineItem(itemId: String) {
        _formState.update { state ->
            val newItems = state.items.filter { it.id != itemId }
            state.copy(items = if (newItems.isEmpty()) listOf(InvoiceLineItem()) else newItems)
        }
        if (_uiState.value.expandedItemId == itemId) {
            _uiState.update { it.copy(expandedItemId = _formState.value.items.firstOrNull()?.id) }
        }
    }

    fun updateLineItem(itemId: String, updater: (InvoiceLineItem) -> InvoiceLineItem) {
        _formState.update { state ->
            state.copy(items = state.items.map { if (it.id == itemId) updater(it) else it }, errors = state.errors - "items")
        }
    }

    fun updateItemDescription(itemId: String, description: String) { updateLineItem(itemId) { it.copy(description = description) } }
    fun updateItemQuantity(itemId: String, quantity: Double) { updateLineItem(itemId) { it.copy(quantity = quantity) } }
    fun updateItemUnitPrice(itemId: String, unitPrice: String) { updateLineItem(itemId) { it.copy(unitPrice = unitPrice) } }
    fun updateItemVatRate(itemId: String, vatRatePercent: Int) { updateLineItem(itemId) { it.copy(vatRatePercent = vatRatePercent) } }

    fun selectClient(client: ContactDto?) { _formState.update { it.copy(selectedClient = client, errors = it.errors - "client") } }
    fun updateIssueDate(date: LocalDate) { _formState.update { it.copy(issueDate = date) } }
    fun updateDueDate(date: LocalDate) { _formState.update { it.copy(dueDate = date) } }
    fun updateNotes(notes: String) { _formState.update { it.copy(notes = notes) } }

    fun selectDeliveryMethod(method: InvoiceDeliveryMethod) { _uiState.update { it.copy(selectedDeliveryMethod = method) } }
    fun isPeppolAvailable(): Boolean = _formState.value.let { it.selectedClient != null && it.clientHasPeppolId }

    fun goToSendOptions() { _uiState.update { it.copy(currentStep = InvoiceCreationStep.SEND_OPTIONS) } }
    fun goBackToEditInvoice() { _uiState.update { it.copy(currentStep = InvoiceCreationStep.EDIT_INVOICE) } }

    private fun validateForm(): Boolean {
        val result = validateInvoice(_formState.value)
        _formState.update { it.copy(errors = result.errors) }
        return result.isValid
    }

    fun saveAsDraft() {
        if (!validateForm()) { logger.w { "Form validation failed" }; return }
        scope.launch {
            val form = _formState.value
            _formState.update { it.copy(isSaving = true) }
            mutableState.emitLoading()
            logger.d { "Creating invoice for client: ${form.selectedClient?.name?.value}" }
            submitInvoice(form).fold(
                onSuccess = { invoice ->
                    logger.i { "Invoice created: ${invoice.invoiceNumber}" }
                    _createdInvoiceId.value = invoice.id
                    _formState.update { it.copy(isSaving = false) }
                    mutableState.emit(DokusState.success(invoice))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create invoice" }
                    _formState.update { it.copy(isSaving = false, errors = it.errors + ("general" to (error.message ?: "Failed"))) }
                    mutableState.emit(DokusState.error(error) { saveAsDraft() })
                }
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    fun resetForm() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstItem = InvoiceLineItem()
        _formState.value = CreateInvoiceFormState(issueDate = today, dueDate = today.plus(30, DateTimeUnit.DAY), items = listOf(firstItem))
        _uiState.value = CreateInvoiceUiState(expandedItemId = firstItem.id)
        _createdInvoiceId.value = null
        mutableState.value = DokusState.idle()
    }
}
