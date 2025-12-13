package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.ClientDto
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.random.Random

/**
 * State representing the invoice creation form.
 */
data class CreateInvoiceFormState(
    val selectedClient: ClientDto? = null,
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
}

/**
 * Format a double value as money (e.g., "€123.45").
 * Multiplatform-compatible formatting.
 */
private fun formatMoney(value: Double): String {
    val rounded = round(value * 100) / 100
    val isNegative = rounded < 0
    val absValue = rounded.absoluteValue
    val intPart = absValue.toLong()
    val decPart = ((absValue - intPart) * 100 + 0.5).toInt()
    val sign = if (isNegative) "-" else ""
    return "$sign€$intPart.${decPart.toString().padStart(2, '0')}"
}

/**
 * ViewModel for creating a new invoice.
 * Handles form state, client selection, line items, and API calls.
 */
class CreateInvoiceViewModel : BaseViewModel<DokusState<FinancialDocumentDto.InvoiceDto>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<CreateInvoiceViewModel>()
    private val dataSource: CashflowRemoteDataSource by inject()

    // Clients state for selection dropdown
    private val _clientsState = MutableStateFlow<DokusState<List<ClientDto>>>(DokusState.idle())
    val clientsState: StateFlow<DokusState<List<ClientDto>>> = _clientsState.asStateFlow()

    // Form state
    private val _formState = MutableStateFlow(createInitialFormState())
    val formState: StateFlow<CreateInvoiceFormState> = _formState.asStateFlow()

    // Created invoice ID (for navigation after save)
    private val _createdInvoiceId = MutableStateFlow<InvoiceId?>(null)
    val createdInvoiceId: StateFlow<InvoiceId?> = _createdInvoiceId.asStateFlow()

    @OptIn(ExperimentalTime::class)
    private fun createInitialFormState(): CreateInvoiceFormState {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return CreateInvoiceFormState(
            issueDate = today,
            dueDate = today.plus(30, DateTimeUnit.DAY)
        )
    }

    init {
        loadClients()
    }

    // ========================================================================
    // CLIENT LOADING
    // ========================================================================

    /**
     * Load clients for the selection dropdown.
     * Only loads active clients.
     */
    fun loadClients() {
        scope.launch {
            logger.d { "Loading clients for invoice creation" }
            _clientsState.value = DokusState.loading()

            dataSource.listClients(
                activeOnly = true,
                limit = 100,
                offset = 0
            ).fold(
                onSuccess = { response ->
                    logger.d { "Loaded ${response.items.size} clients" }
                    _clientsState.value = DokusState.success(response.items)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load clients" }
                    _clientsState.value = DokusState.error(error) { loadClients() }
                }
            )
        }
    }

    // ========================================================================
    // FORM STATE UPDATES
    // ========================================================================

    fun selectClient(client: ClientDto?) {
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
    // LINE ITEMS MANAGEMENT
    // ========================================================================

    fun addLineItem() {
        _formState.update { it.copy(items = it.items + InvoiceLineItem()) }
    }

    fun removeLineItem(itemId: String) {
        _formState.update { state ->
            val newItems = state.items.filter { it.id != itemId }
            // Ensure at least one item exists
            state.copy(items = if (newItems.isEmpty()) listOf(InvoiceLineItem()) else newItems)
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
                clientId = form.selectedClient!!.id,
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
    fun resetForm() {
        _formState.value = createInitialFormState()
        _createdInvoiceId.value = null
        mutableState.value = DokusState.idle()
    }
}
