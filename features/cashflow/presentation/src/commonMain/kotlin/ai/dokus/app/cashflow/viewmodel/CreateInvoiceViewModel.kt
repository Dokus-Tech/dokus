package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
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
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// ============================================================================
// DELIVERY METHOD
// ============================================================================

/**
 * Available delivery methods for sending invoices.
 */
enum class InvoiceDeliveryMethod {
    PDF_EXPORT,
    PEPPOL,
    EMAIL
}

// ============================================================================
// UI STATE
// ============================================================================

/**
 * UI state for the interactive invoice editor.
 * Separate from form data to keep concerns isolated.
 */
data class CreateInvoiceUiState(
    val expandedItemId: String? = null,
    val isClientPanelOpen: Boolean = false,
    val clientSearchQuery: String = "",
    val selectedDeliveryMethod: InvoiceDeliveryMethod = InvoiceDeliveryMethod.PDF_EXPORT,
    val isDatePickerOpen: DatePickerTarget? = null,
    val currentStep: InvoiceCreationStep = InvoiceCreationStep.EDIT_INVOICE
)

/**
 * Which date picker is currently open.
 */
enum class DatePickerTarget {
    ISSUE_DATE,
    DUE_DATE
}

/**
 * Steps in the invoice creation flow (for mobile).
 */
enum class InvoiceCreationStep {
    EDIT_INVOICE,
    SEND_OPTIONS
}

// ============================================================================
// FORM STATE
// ============================================================================

/**
 * State representing the invoice creation form data.
 */
data class CreateInvoiceFormState(
    val selectedClient: ContactDto? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val notes: String = "",
    val items: List<InvoiceLineItem> = listOf(InvoiceLineItem()),
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap()
) {
    val subtotal: String
        get() {
            val total = items.sumOf { it.lineTotalDouble }
            return formatMoney(total)
        }

    val vatAmount: String
        get() {
            val total = items.sumOf { it.vatAmountDouble }
            return formatMoney(total)
        }

    val total: String
        get() {
            val subtotalVal = items.sumOf { it.lineTotalDouble }
            val vatVal = items.sumOf { it.vatAmountDouble }
            return formatMoney(subtotalVal + vatVal)
        }

    val isValid: Boolean
        get() = selectedClient != null && items.any { it.isValid }

    /**
     * Check if selected client is Belgian (for Peppol validation).
     */
    val isClientBelgian: Boolean
        get() = selectedClient?.country?.equals("BE", ignoreCase = true) == true ||
                selectedClient?.country?.equals("Belgium", ignoreCase = true) == true

    /**
     * Check if selected client has Peppol ID configured.
     */
    val clientHasPeppolId: Boolean
        get() = selectedClient?.peppolId != null

    /**
     * Show Peppol warning for Belgian clients without Peppol ID.
     */
    val showPeppolWarning: Boolean
        get() = isClientBelgian && !clientHasPeppolId && selectedClient != null
}

/**
 * Represents a line item in the invoice form.
 */
data class InvoiceLineItem(
    val id: String = Random.nextLong().toString(),
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: String = "",  // Stored as string for form input
    val vatRatePercent: Int = 21  // 21%, 12%, 6%, 0%
) {
    val unitPriceDouble: Double
        get() = unitPrice.toDoubleOrNull() ?: 0.0

    val lineTotalDouble: Double
        get() = unitPriceDouble * quantity

    val vatRateDecimal: Double
        get() = vatRatePercent / 100.0

    val vatAmountDouble: Double
        get() = lineTotalDouble * vatRateDecimal

    val lineTotal: String
        get() = formatMoney(lineTotalDouble)

    val isValid: Boolean
        get() = description.isNotBlank() && quantity > 0 && unitPriceDouble > 0

    val isEmpty: Boolean
        get() = description.isBlank() && unitPrice.isBlank()
}

/**
 * Format a double value as money (e.g., "€123.45").
 * Multiplatform-compatible formatting.
 */
internal fun formatMoney(value: Double): String {
    val rounded = round(value * 100) / 100
    val isNegative = rounded < 0
    val absValue = rounded.absoluteValue
    val intPart = absValue.toLong()
    val decPart = ((absValue - intPart) * 100 + 0.5).toInt()
    val sign = if (isNegative) "-" else ""
    return "$sign€$intPart.${decPart.toString().padStart(2, '0')}"
}

// ============================================================================
// VIEW MODEL
// ============================================================================

/**
 * ViewModel for creating a new invoice.
 * Handles form state, UI state, client selection, line items, and API calls.
 */
class CreateInvoiceViewModel : BaseViewModel<DokusState<FinancialDocumentDto.InvoiceDto>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<CreateInvoiceViewModel>()
    private val dataSource: CashflowRemoteDataSource by inject()

    // Clients state for selection (now contacts)
    private val _clientsState = MutableStateFlow<DokusState<List<ContactDto>>>(DokusState.idle())
    val clientsState: StateFlow<DokusState<List<ContactDto>>> = _clientsState.asStateFlow()

    // UI state (interaction state) - must be initialized before _formState
    private val _uiState = MutableStateFlow(CreateInvoiceUiState())
    val uiState: StateFlow<CreateInvoiceUiState> = _uiState.asStateFlow()

    // Form state (invoice data)
    private val _formState = MutableStateFlow(createInitialFormState())
    val formState: StateFlow<CreateInvoiceFormState> = _formState.asStateFlow()

    // Created invoice ID (for navigation after save)
    private val _createdInvoiceId = MutableStateFlow<InvoiceId?>(null)
    val createdInvoiceId: StateFlow<InvoiceId?> = _createdInvoiceId.asStateFlow()

    @OptIn(ExperimentalTime::class)
    private fun createInitialFormState(): CreateInvoiceFormState {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstItem = InvoiceLineItem()
        // Set first item as expanded by default
        _uiState.update { ui -> ui.copy(expandedItemId = firstItem.id) }
        return CreateInvoiceFormState(
            issueDate = today,
            dueDate = today.plus(30, DateTimeUnit.DAY),
            items = listOf(firstItem)
        )
    }

    init {
        loadClients()
    }

    // ========================================================================
    // CLIENT LOADING & FILTERING
    // ========================================================================

    fun loadClients() {
        scope.launch {
            logger.d { "Loading contacts for invoice creation" }
            _clientsState.value = DokusState.loading()

            // TODO: Replace with contacts data source when available
            // Contacts are now managed by the contacts microservice
            // For now, return empty list until contacts data layer is implemented
            logger.w { "Contacts data source not yet implemented - returning empty list" }
            _clientsState.value = DokusState.success(emptyList())
        }
    }

    /**
     * Get filtered clients based on search query.
     */
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

    fun updateClientSearchQuery(query: String) {
        _uiState.update { it.copy(clientSearchQuery = query) }
    }

    // ========================================================================
    // CLIENT PANEL
    // ========================================================================

    fun openClientPanel() {
        _uiState.update { it.copy(isClientPanelOpen = true, clientSearchQuery = "") }
    }

    fun closeClientPanel() {
        _uiState.update { it.copy(isClientPanelOpen = false, clientSearchQuery = "") }
    }

    fun selectClientAndClose(client: ContactDto) {
        _formState.update { it.copy(selectedClient = client, errors = it.errors - "client") }
        closeClientPanel()
    }

    // ========================================================================
    // DATE PICKERS
    // ========================================================================

    fun openIssueDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = DatePickerTarget.ISSUE_DATE) }
    }

    fun openDueDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = DatePickerTarget.DUE_DATE) }
    }

    fun closeDatePicker() {
        _uiState.update { it.copy(isDatePickerOpen = null) }
    }

    fun selectDate(date: LocalDate) {
        val target = _uiState.value.isDatePickerOpen ?: return
        when (target) {
            DatePickerTarget.ISSUE_DATE -> _formState.update { it.copy(issueDate = date) }
            DatePickerTarget.DUE_DATE -> _formState.update { it.copy(dueDate = date) }
        }
        closeDatePicker()
    }

    // ========================================================================
    // LINE ITEMS - EXPAND/COLLAPSE
    // ========================================================================

    fun expandItem(itemId: String) {
        _uiState.update { it.copy(expandedItemId = itemId) }
    }

    fun collapseItem() {
        _uiState.update { it.copy(expandedItemId = null) }
    }

    fun toggleItemExpanded(itemId: String) {
        _uiState.update {
            if (it.expandedItemId == itemId) {
                it.copy(expandedItemId = null)
            } else {
                it.copy(expandedItemId = itemId)
            }
        }
    }

    // ========================================================================
    // LINE ITEMS MANAGEMENT
    // ========================================================================

    fun addLineItem(): String {
        val newItem = InvoiceLineItem()
        _formState.update { it.copy(items = it.items + newItem) }
        // Expand the new item
        _uiState.update { it.copy(expandedItemId = newItem.id) }
        return newItem.id
    }

    fun removeLineItem(itemId: String) {
        _formState.update { state ->
            val newItems = state.items.filter { it.id != itemId }
            // Ensure at least one item exists
            state.copy(items = if (newItems.isEmpty()) listOf(InvoiceLineItem()) else newItems)
        }
        // If removed item was expanded, collapse
        if (_uiState.value.expandedItemId == itemId) {
            _uiState.update { it.copy(expandedItemId = _formState.value.items.firstOrNull()?.id) }
        }
    }

    fun updateLineItem(itemId: String, updater: (InvoiceLineItem) -> InvoiceLineItem) {
        _formState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == itemId) updater(item) else item
                },
                errors = state.errors - "items"
            )
        }
    }

    fun updateItemDescription(itemId: String, description: String) {
        updateLineItem(itemId) { it.copy(description = description) }
    }

    fun updateItemQuantity(itemId: String, quantity: Double) {
        updateLineItem(itemId) { it.copy(quantity = quantity) }
    }

    fun updateItemUnitPrice(itemId: String, unitPrice: String) {
        updateLineItem(itemId) { it.copy(unitPrice = unitPrice) }
    }

    fun updateItemVatRate(itemId: String, vatRatePercent: Int) {
        updateLineItem(itemId) { it.copy(vatRatePercent = vatRatePercent) }
    }

    // ========================================================================
    // FORM STATE UPDATES
    // ========================================================================

    fun selectClient(client: ContactDto?) {
        _formState.update { it.copy(selectedClient = client, errors = it.errors - "client") }
    }

    fun updateIssueDate(date: LocalDate) {
        _formState.update { it.copy(issueDate = date) }
    }

    fun updateDueDate(date: LocalDate) {
        _formState.update { it.copy(dueDate = date) }
    }

    fun updateNotes(notes: String) {
        _formState.update { it.copy(notes = notes) }
    }

    // ========================================================================
    // DELIVERY METHOD
    // ========================================================================

    fun selectDeliveryMethod(method: InvoiceDeliveryMethod) {
        _uiState.update { it.copy(selectedDeliveryMethod = method) }
    }

    /**
     * Check if Peppol delivery is available for current client.
     */
    fun isPeppolAvailable(): Boolean {
        val form = _formState.value
        return form.selectedClient != null && form.clientHasPeppolId
    }

    // ========================================================================
    // MOBILE NAVIGATION
    // ========================================================================

    fun goToSendOptions() {
        _uiState.update { it.copy(currentStep = InvoiceCreationStep.SEND_OPTIONS) }
    }

    fun goBackToEditInvoice() {
        _uiState.update { it.copy(currentStep = InvoiceCreationStep.EDIT_INVOICE) }
    }

    // ========================================================================
    // FORM VALIDATION
    // ========================================================================

    private fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()
        val form = _formState.value

        if (form.selectedClient == null) {
            errors["client"] = "Please select a client"
        }

        if (!form.items.any { it.isValid }) {
            errors["items"] = "Please add at least one valid line item"
        }

        val issueDate = form.issueDate
        val dueDate = form.dueDate
        if (issueDate != null && dueDate != null && dueDate < issueDate) {
            errors["dueDate"] = "Due date cannot be before issue date"
        }

        _formState.update { it.copy(errors = errors) }
        return errors.isEmpty()
    }

    // ========================================================================
    // SAVE INVOICE
    // ========================================================================

    /**
     * Save the invoice as a draft.
     */
    fun saveAsDraft() {
        if (!validateForm()) {
            logger.w { "Form validation failed" }
            return
        }

        scope.launch {
            val form = _formState.value
            _formState.update { it.copy(isSaving = true) }
            mutableState.emitLoading()

            val request = CreateInvoiceRequest(
                contactId = form.selectedClient!!.id,
                items = form.items.filter { it.isValid }.mapIndexed { index, item ->
                    InvoiceItemDto(
                        description = item.description,
                        quantity = item.quantity,
                        unitPrice = Money.fromDouble(item.unitPriceDouble),
                        vatRate = VatRate("${item.vatRatePercent}.00"),
                        lineTotal = Money.fromDouble(item.lineTotalDouble),
                        vatAmount = Money.fromDouble(item.vatAmountDouble),
                        sortOrder = index
                    )
                },
                issueDate = form.issueDate!!,
                dueDate = form.dueDate!!,
                notes = form.notes.takeIf { it.isNotBlank() }
            )

            logger.d { "Creating invoice for client: ${form.selectedClient.name.value}" }

            dataSource.createInvoice(request).fold(
                onSuccess = { invoice ->
                    logger.i { "Invoice created: ${invoice.invoiceNumber}" }
                    _createdInvoiceId.value = invoice.id
                    _formState.update { it.copy(isSaving = false) }
                    mutableState.emit(DokusState.success(invoice))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create invoice" }
                    _formState.update {
                        it.copy(
                            isSaving = false,
                            errors = it.errors + ("general" to (error.message ?: "Failed to create invoice"))
                        )
                    }
                    mutableState.emit(DokusState.error(error) { saveAsDraft() })
                }
            )
        }
    }

    /**
     * Reset the form to initial state.
     */
    @OptIn(ExperimentalTime::class)
    fun resetForm() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstItem = InvoiceLineItem()
        _formState.value = CreateInvoiceFormState(
            issueDate = today,
            dueDate = today.plus(30, DateTimeUnit.DAY),
            items = listOf(firstItem)
        )
        _uiState.value = CreateInvoiceUiState(expandedItemId = firstItem.id)
        _createdInvoiceId.value = null
        mutableState.value = DokusState.idle()
    }
}
