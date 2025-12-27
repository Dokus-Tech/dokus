package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.cashflow.usecase.SubmitInvoiceUseCase
import ai.dokus.app.cashflow.usecase.ValidateInvoiceUseCase
import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceUiState
import ai.dokus.app.cashflow.viewmodel.model.DatePickerTarget
import ai.dokus.app.cashflow.viewmodel.model.InvoiceCreationStep
import ai.dokus.app.cashflow.viewmodel.model.InvoiceLineItem
import ai.dokus.foundation.domain.exceptions.asDokusException
import tech.dokus.domain.model.ContactDto
import ai.dokus.foundation.platform.Logger
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal typealias CreateInvoiceCtx = PipelineContext<CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction>

/**
 * Container for creating a new invoice using FlowMVI.
 * Manages the multi-step invoice creation flow with form validation and submission.
 *
 * Features:
 * - Multi-step form (edit invoice → send options)
 * - Client selection with search filtering
 * - Line item management (add, remove, update)
 * - Date picker management
 * - Form validation with field-specific errors
 * - Invoice submission with error handling
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class CreateInvoiceContainer(
    private val tenantDataSource: TenantRemoteDataSource,
    private val validateInvoice: ValidateInvoiceUseCase,
    private val submitInvoice: SubmitInvoiceUseCase,
) : Container<CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction> {

    private val logger = Logger.forClass<CreateInvoiceContainer>()

    @OptIn(ExperimentalTime::class)
    override val store: Store<CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction> =
        store(createInitialState()) {
            reduce { intent ->
                when (intent) {
                    // Client Selection
                    is CreateInvoiceIntent.OpenClientPanel -> handleOpenClientPanel()
                    is CreateInvoiceIntent.CloseClientPanel -> handleCloseClientPanel()
                    is CreateInvoiceIntent.UpdateClientSearchQuery -> handleUpdateClientSearchQuery(intent.query)
                    is CreateInvoiceIntent.SelectClient -> handleSelectClient(intent.client)
                    is CreateInvoiceIntent.ClearClient -> handleClearClient()

                    // Date Selection
                    is CreateInvoiceIntent.OpenIssueDatePicker -> handleOpenIssueDatePicker()
                    is CreateInvoiceIntent.OpenDueDatePicker -> handleOpenDueDatePicker()
                    is CreateInvoiceIntent.CloseDatePicker -> handleCloseDatePicker()
                    is CreateInvoiceIntent.SelectDate -> handleSelectDate(intent.date)
                    is CreateInvoiceIntent.UpdateIssueDate -> handleUpdateIssueDate(intent.date)
                    is CreateInvoiceIntent.UpdateDueDate -> handleUpdateDueDate(intent.date)

                    // Line Items
                    is CreateInvoiceIntent.ExpandItem -> handleExpandItem(intent.itemId)
                    is CreateInvoiceIntent.CollapseItem -> handleCollapseItem()
                    is CreateInvoiceIntent.ToggleItemExpanded -> handleToggleItemExpanded(intent.itemId)
                    is CreateInvoiceIntent.AddLineItem -> handleAddLineItem()
                    is CreateInvoiceIntent.RemoveLineItem -> handleRemoveLineItem(intent.itemId)
                    is CreateInvoiceIntent.UpdateItemDescription -> handleUpdateItemDescription(intent.itemId, intent.description)
                    is CreateInvoiceIntent.UpdateItemQuantity -> handleUpdateItemQuantity(intent.itemId, intent.quantity)
                    is CreateInvoiceIntent.UpdateItemUnitPrice -> handleUpdateItemUnitPrice(intent.itemId, intent.unitPrice)
                    is CreateInvoiceIntent.UpdateItemVatRate -> handleUpdateItemVatRate(intent.itemId, intent.vatRatePercent)

                    // Notes
                    is CreateInvoiceIntent.UpdateNotes -> handleUpdateNotes(intent.notes)

                    // Delivery Options
                    is CreateInvoiceIntent.SelectDeliveryMethod -> handleSelectDeliveryMethod(intent.method)

                    // Navigation
                    is CreateInvoiceIntent.GoToSendOptions -> handleGoToSendOptions()
                    is CreateInvoiceIntent.GoBackToEdit -> handleGoBackToEdit()
                    is CreateInvoiceIntent.BackClicked -> action(CreateInvoiceAction.NavigateBack)

                    // Form Actions
                    is CreateInvoiceIntent.ValidateForm -> handleValidateForm()
                    is CreateInvoiceIntent.SaveAsDraft -> handleSaveAsDraft()
                    is CreateInvoiceIntent.ResetForm -> handleResetForm()
                    is CreateInvoiceIntent.ReloadClients -> handleReloadClients()
                }
            }
        }

    init {
        // Load clients and invoice number preview on init
        store.intent(CreateInvoiceIntent.ReloadClients)
    }

    // === Client Selection Handlers ===

    private suspend fun CreateInvoiceCtx.handleOpenClientPanel() {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(isClientPanelOpen = true, clientSearchQuery = "")
        }
    }

    private suspend fun CreateInvoiceCtx.handleCloseClientPanel() {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(isClientPanelOpen = false, clientSearchQuery = "")
        }
    }

    private suspend fun CreateInvoiceCtx.handleUpdateClientSearchQuery(query: String) {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(clientSearchQuery = query)
        }
    }

    private suspend fun CreateInvoiceCtx.handleSelectClient(client: ContactDto) {
        updateEditingState { formState, uiState ->
            formState.copy(
                selectedClient = client,
                errors = formState.errors - "client"
            ) to uiState.copy(isClientPanelOpen = false, clientSearchQuery = "")
        }
    }

    private suspend fun CreateInvoiceCtx.handleClearClient() {
        updateEditingState { formState, uiState ->
            formState.copy(selectedClient = null) to uiState
        }
    }

    // === Date Selection Handlers ===

    private suspend fun CreateInvoiceCtx.handleOpenIssueDatePicker() {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(isDatePickerOpen = DatePickerTarget.ISSUE_DATE)
        }
    }

    private suspend fun CreateInvoiceCtx.handleOpenDueDatePicker() {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(isDatePickerOpen = DatePickerTarget.DUE_DATE)
        }
    }

    private suspend fun CreateInvoiceCtx.handleCloseDatePicker() {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(isDatePickerOpen = null)
        }
    }

    private suspend fun CreateInvoiceCtx.handleSelectDate(date: LocalDate) {
        updateEditingState { formState, uiState ->
            val newFormState = when (uiState.isDatePickerOpen) {
                DatePickerTarget.ISSUE_DATE -> formState.copy(issueDate = date)
                DatePickerTarget.DUE_DATE -> formState.copy(dueDate = date)
                null -> formState
            }
            newFormState to uiState.copy(isDatePickerOpen = null)
        }
    }

    private suspend fun CreateInvoiceCtx.handleUpdateIssueDate(date: LocalDate) {
        updateEditingState { formState, uiState ->
            formState.copy(issueDate = date) to uiState
        }
    }

    private suspend fun CreateInvoiceCtx.handleUpdateDueDate(date: LocalDate) {
        updateEditingState { formState, uiState ->
            formState.copy(dueDate = date) to uiState
        }
    }

    // === Line Item Handlers ===

    private suspend fun CreateInvoiceCtx.handleExpandItem(itemId: String) {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(expandedItemId = itemId)
        }
    }

    private suspend fun CreateInvoiceCtx.handleCollapseItem() {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(expandedItemId = null)
        }
    }

    private suspend fun CreateInvoiceCtx.handleToggleItemExpanded(itemId: String) {
        updateEditingState { formState, uiState ->
            val newExpandedId = if (uiState.expandedItemId == itemId) null else itemId
            formState to uiState.copy(expandedItemId = newExpandedId)
        }
    }

    private suspend fun CreateInvoiceCtx.handleAddLineItem() {
        updateEditingState { formState, uiState ->
            val newItem = InvoiceLineItem()
            formState.copy(items = formState.items + newItem) to uiState.copy(expandedItemId = newItem.id)
        }
    }

    private suspend fun CreateInvoiceCtx.handleRemoveLineItem(itemId: String) {
        updateEditingState { formState, uiState ->
            val newItems = formState.items.filter { it.id != itemId }
            val finalItems = if (newItems.isEmpty()) listOf(InvoiceLineItem()) else newItems
            val newExpandedId = if (uiState.expandedItemId == itemId) {
                finalItems.firstOrNull()?.id
            } else {
                uiState.expandedItemId
            }
            formState.copy(items = finalItems) to uiState.copy(expandedItemId = newExpandedId)
        }
    }

    private suspend fun CreateInvoiceCtx.handleUpdateItemDescription(itemId: String, description: String) {
        updateLineItem(itemId) { it.copy(description = description) }
    }

    private suspend fun CreateInvoiceCtx.handleUpdateItemQuantity(itemId: String, quantity: Double) {
        updateLineItem(itemId) { it.copy(quantity = quantity) }
    }

    private suspend fun CreateInvoiceCtx.handleUpdateItemUnitPrice(itemId: String, unitPrice: String) {
        updateLineItem(itemId) { it.copy(unitPrice = unitPrice) }
    }

    private suspend fun CreateInvoiceCtx.handleUpdateItemVatRate(itemId: String, vatRatePercent: Int) {
        updateLineItem(itemId) { it.copy(vatRatePercent = vatRatePercent) }
    }

    private suspend fun CreateInvoiceCtx.updateLineItem(itemId: String, updater: (InvoiceLineItem) -> InvoiceLineItem) {
        updateEditingState { formState, uiState ->
            formState.copy(
                items = formState.items.map { if (it.id == itemId) updater(it) else it },
                errors = formState.errors - "items"
            ) to uiState
        }
    }

    // === Notes Handler ===

    private suspend fun CreateInvoiceCtx.handleUpdateNotes(notes: String) {
        updateEditingState { formState, uiState ->
            formState.copy(notes = notes) to uiState
        }
    }

    // === Delivery Options Handler ===

    private suspend fun CreateInvoiceCtx.handleSelectDeliveryMethod(method: ai.dokus.app.cashflow.viewmodel.model.InvoiceDeliveryMethod) {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(selectedDeliveryMethod = method)
        }
    }

    // === Step Navigation Handlers ===

    private suspend fun CreateInvoiceCtx.handleGoToSendOptions() {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(currentStep = InvoiceCreationStep.SEND_OPTIONS)
        }
    }

    private suspend fun CreateInvoiceCtx.handleGoBackToEdit() {
        updateEditingState { formState, uiState ->
            formState to uiState.copy(currentStep = InvoiceCreationStep.EDIT_INVOICE)
        }
    }

    // === Form Action Handlers ===

    private suspend fun CreateInvoiceCtx.handleValidateForm(): Boolean {
        var isValid = false
        withState<CreateInvoiceState.Editing, _> {
            val result = validateInvoice(formState)
            if (!result.isValid) {
                updateState {
                    copy(formState = formState.copy(errors = result.errors))
                }
                // Show first error as action
                result.errors.values.firstOrNull()?.let { errorMessage ->
                    action(CreateInvoiceAction.ShowValidationError(errorMessage))
                }
            } else {
                updateState {
                    copy(formState = formState.copy(errors = emptyMap()))
                }
                isValid = true
            }
        }
        return isValid
    }

    private suspend fun CreateInvoiceCtx.handleSaveAsDraft() {
        withState<CreateInvoiceState.Editing, _> {
            // Validate first
            val result = validateInvoice(formState)
            if (!result.isValid) {
                logger.w { "Form validation failed" }
                updateState {
                    copy(formState = formState.copy(errors = result.errors))
                }
                result.errors.values.firstOrNull()?.let { errorMessage ->
                    action(CreateInvoiceAction.ShowValidationError(errorMessage))
                }
                return@withState
            }

            val currentFormState = formState
            val currentUiState = uiState

            // Transition to saving state
            updateState {
                CreateInvoiceState.Saving(
                    formState = currentFormState.copy(isSaving = true),
                    uiState = currentUiState
                )
            }

            logger.d { "Creating invoice for client: ${currentFormState.selectedClient?.name?.value}" }

            submitInvoice(currentFormState).fold(
                onSuccess = { invoice ->
                    logger.i { "Invoice created: ${invoice.invoiceNumber}" }
                    updateState {
                        CreateInvoiceState.Success(
                            formState = currentFormState.copy(isSaving = false),
                            uiState = currentUiState,
                            createdInvoiceId = invoice.id
                        )
                    }
                    action(CreateInvoiceAction.ShowSuccess("Invoice created successfully"))
                    action(CreateInvoiceAction.NavigateToInvoice(invoice.id))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create invoice" }
                    updateState {
                        CreateInvoiceState.Error(
                            formState = currentFormState.copy(
                                isSaving = false,
                                errors = currentFormState.errors + ("general" to (error.message ?: "Failed to create invoice"))
                            ),
                            uiState = currentUiState,
                            exception = error.asDokusException,
                            retryHandler = { intent(CreateInvoiceIntent.SaveAsDraft) }
                        )
                    }
                    action(CreateInvoiceAction.ShowError(error.message ?: "Failed to create invoice"))
                }
            )
        }

        // Also handle retry from Error state
        withState<CreateInvoiceState.Error, _> {
            val currentFormState = formState
            val currentUiState = uiState

            // Transition back to saving
            updateState {
                CreateInvoiceState.Saving(
                    formState = currentFormState.copy(isSaving = true),
                    uiState = currentUiState
                )
            }

            submitInvoice(currentFormState).fold(
                onSuccess = { invoice ->
                    logger.i { "Invoice created on retry: ${invoice.invoiceNumber}" }
                    updateState {
                        CreateInvoiceState.Success(
                            formState = currentFormState.copy(isSaving = false),
                            uiState = currentUiState,
                            createdInvoiceId = invoice.id
                        )
                    }
                    action(CreateInvoiceAction.ShowSuccess("Invoice created successfully"))
                    action(CreateInvoiceAction.NavigateToInvoice(invoice.id))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create invoice on retry" }
                    updateState {
                        CreateInvoiceState.Error(
                            formState = currentFormState.copy(
                                isSaving = false,
                                errors = currentFormState.errors + ("general" to (error.message ?: "Failed to create invoice"))
                            ),
                            uiState = currentUiState,
                            exception = error.asDokusException,
                            retryHandler = { intent(CreateInvoiceIntent.SaveAsDraft) }
                        )
                    }
                    action(CreateInvoiceAction.ShowError(error.message ?: "Failed to create invoice"))
                }
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun CreateInvoiceCtx.handleResetForm() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstItem = InvoiceLineItem()

        updateState {
            when (this) {
                is CreateInvoiceState.Editing -> CreateInvoiceState.Editing(
                    formState = CreateInvoiceFormState(
                        issueDate = today,
                        dueDate = today.plus(30, DateTimeUnit.DAY),
                        items = listOf(firstItem)
                    ),
                    uiState = CreateInvoiceUiState(expandedItemId = firstItem.id),
                    clients = clients,
                    clientsLoading = false,
                    invoiceNumberPreview = invoiceNumberPreview
                )
                else -> createInitialState()
            }
        }

        // Reload invoice number preview
        loadInvoiceNumberPreview()
    }

    private suspend fun CreateInvoiceCtx.handleReloadClients() {
        logger.d { "Loading contacts for invoice creation" }

        updateState {
            when (this) {
                is CreateInvoiceState.Editing -> copy(clientsLoading = true)
                else -> this
            }
        }

        // Load invoice number preview in parallel
        loadInvoiceNumberPreview()

        // TODO: Replace with contacts data source when available
        logger.w { "Contacts data source not yet implemented" }
        updateState {
            when (this) {
                is CreateInvoiceState.Editing -> copy(clients = emptyList(), clientsLoading = false)
                else -> this
            }
        }
    }

    // === Private Helpers ===

    private suspend fun CreateInvoiceCtx.loadInvoiceNumberPreview() {
        tenantDataSource.getInvoiceNumberPreview()
            .onSuccess { preview ->
                updateState {
                    when (this) {
                        is CreateInvoiceState.Editing -> copy(invoiceNumberPreview = preview)
                        else -> this
                    }
                }
            }
            .onFailure { error ->
                logger.w { "Could not load invoice number preview: ${error.message}" }
            }
    }

    /**
     * Helper to update the Editing state's form and UI state.
     * Handles state transitions from Error back to Editing.
     */
    private suspend fun CreateInvoiceCtx.updateEditingState(
        update: (CreateInvoiceFormState, CreateInvoiceUiState) -> Pair<CreateInvoiceFormState, CreateInvoiceUiState>
    ) {
        updateState {
            when (this) {
                is CreateInvoiceState.Editing -> {
                    val (newFormState, newUiState) = update(formState, uiState)
                    copy(formState = newFormState, uiState = newUiState)
                }
                is CreateInvoiceState.Error -> {
                    // Transition back to Editing when user makes changes
                    val (newFormState, newUiState) = update(formState, uiState)
                    CreateInvoiceState.Editing(
                        formState = newFormState,
                        uiState = newUiState,
                        clients = emptyList(),
                        clientsLoading = false,
                        invoiceNumberPreview = null
                    )
                }
                else -> this
            }
        }
    }

    /**
     * Filters clients based on search query.
     * Call this from the UI to get filtered client list.
     */
    fun getFilteredClients(clients: List<ContactDto>, query: String): List<ContactDto> {
        val trimmedQuery = query.trim().lowercase()
        if (trimmedQuery.isBlank()) return clients
        return clients.filter { client ->
            client.name.value.lowercase().contains(trimmedQuery) ||
                client.email?.value?.lowercase()?.contains(trimmedQuery) == true ||
                client.vatNumber?.value?.lowercase()?.contains(trimmedQuery) == true
        }
    }

    /**
     * Check if Peppol is available for the selected client.
     */
    fun isPeppolAvailable(formState: CreateInvoiceFormState): Boolean {
        return formState.selectedClient != null && formState.clientHasPeppolId
    }

    @OptIn(ExperimentalTime::class)
    private fun createInitialState(): CreateInvoiceState.Editing {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val firstItem = InvoiceLineItem()

        return CreateInvoiceState.Editing(
            formState = CreateInvoiceFormState(
                issueDate = today,
                dueDate = today.plus(30, DateTimeUnit.DAY),
                items = listOf(firstItem)
            ),
            uiState = CreateInvoiceUiState(
                expandedItemId = firstItem.id
            ),
            clients = emptyList(),
            clientsLoading = true,
            invoiceNumberPreview = null
        )
    }
}
