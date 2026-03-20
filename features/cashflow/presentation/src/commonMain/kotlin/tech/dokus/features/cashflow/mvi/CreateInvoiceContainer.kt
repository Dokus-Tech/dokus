package tech.dokus.features.cashflow.mvi

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.enums.InvoiceDueDateMode
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetInvoiceNumberPreviewUseCase
import tech.dokus.features.auth.usecases.GetTenantSettingsUseCase
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceUiState
import tech.dokus.features.cashflow.mvi.model.DatePickerTarget
import tech.dokus.features.cashflow.mvi.model.DeliveryResolution
import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem
import tech.dokus.features.cashflow.mvi.model.InvoiceResolvedAction
import tech.dokus.features.cashflow.mvi.model.InvoiceSection
import tech.dokus.features.cashflow.mvi.model.LatestInvoiceSuggestion
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.ValidateInvoiceUseCase
import tech.dokus.features.cashflow.usecases.GetContactPeppolStatusUseCase
import tech.dokus.features.cashflow.usecases.GetLatestInvoiceForContactUseCase
import tech.dokus.features.cashflow.usecases.SubmitInvoiceWithDeliveryUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.features.contacts.usecases.LookupContactsUseCase
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val MaxPaymentTermsDays = 365

internal typealias CreateInvoiceCtx = PipelineContext<CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction>

internal class CreateInvoiceContainer(
    private val getInvoiceNumberPreview: GetInvoiceNumberPreviewUseCase,
    private val getTenantSettings: GetTenantSettingsUseCase,
    private val getCurrentTenant: GetCurrentTenantUseCase,
    private val validateInvoice: ValidateInvoiceUseCase,
    private val submitInvoiceWithDelivery: SubmitInvoiceWithDeliveryUseCase,
    private val getContactPeppolStatus: GetContactPeppolStatusUseCase,
    private val getLatestInvoiceForContact: GetLatestInvoiceForContactUseCase,
    private val listContacts: ListContactsUseCase,
    private val lookupContacts: LookupContactsUseCase,
    private val searchCompanyUseCase: SearchCompanyUseCase,
) : Container<CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction> {

    private val clientHandlers = CreateInvoiceClientHandlers(
        listContacts = listContacts,
        lookupContacts = lookupContacts,
        searchCompanyUseCase = searchCompanyUseCase,
        getContactPeppolStatus = getContactPeppolStatus,
        getLatestInvoiceForContact = getLatestInvoiceForContact,
        updateInvoice = { transform -> updateInvoice(transform) },
        snapshotState = { snapshotState() },
        synchronizeDueDate = ::synchronizeDueDate,
        toSuggestion = { toSuggestion() },
    )

    private val submitHandlers = CreateInvoiceSubmitHandlers(
        getInvoiceNumberPreview = getInvoiceNumberPreview,
        getTenantSettings = getTenantSettings,
        getCurrentTenant = getCurrentTenant,
        validateInvoice = validateInvoice,
        submitInvoiceWithDelivery = submitInvoiceWithDelivery,
        updateInvoice = { transform -> updateInvoice(transform) },
        snapshotState = { snapshotState() },
        synchronizeDueDate = ::synchronizeDueDate,
    )

    override val store: Store<CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction> =
        store(createInitialState()) {
            reduce { intent ->
                when (intent) {
                    is CreateInvoiceIntent.UpdateClientLookupQuery ->
                        with(clientHandlers) { handleUpdateClientLookupQuery(intent.query) }
                    is CreateInvoiceIntent.SetClientLookupExpanded ->
                        with(clientHandlers) { handleSetClientLookupExpanded(intent.expanded) }
                    is CreateInvoiceIntent.SelectClient ->
                        with(clientHandlers) { handleSelectClient(intent.client) }
                    is CreateInvoiceIntent.SelectExternalClientCandidate -> action(
                        CreateInvoiceAction.NavigateToCreateContact(
                            prefillCompanyName = intent.candidate.name,
                            prefillVat = intent.candidate.vatNumber?.value,
                            prefillAddress = intent.candidate.prefillAddress,
                            origin = "InvoiceCreate"
                        )
                    )
                    is CreateInvoiceIntent.CreateClientManuallyFromQuery -> {
                        val normalizedVat = VatNumber.from(intent.query)?.takeIf { it.isValid }?.value
                        action(
                            CreateInvoiceAction.NavigateToCreateContact(
                                prefillCompanyName = intent.query.takeIf { it.isNotBlank() },
                                prefillVat = normalizedVat,
                                origin = "InvoiceCreate"
                            )
                        )
                    }
                    is CreateInvoiceIntent.ClearClient -> updateInvoice {
                        it.copy(
                            formState = it.formState.copy(
                                selectedClient = null,
                                peppolStatus = null,
                                peppolStatusLoading = false,
                                errors = it.formState.errors - ValidateInvoiceUseCase.FIELD_CLIENT
                            ),
                            uiState = it.uiState.copy(
                                latestInvoiceSuggestion = null,
                                clientLookupState = it.uiState.clientLookupState.copy(
                                    query = "",
                                    isExpanded = false,
                                    localResults = emptyList(),
                                    externalResults = emptyList(),
                                    mergedSuggestions = emptyList(),
                                    isLocalLoading = false,
                                    isExternalLoading = false,
                                    errorHint = null
                                )
                            )
                        )
                    }
                    is CreateInvoiceIntent.RefreshPeppolStatus -> {
                        updateInvoice {
                            it.copy(formState = it.formState.copy(peppolStatusLoading = true))
                        }
                        val statusResult = getContactPeppolStatus(intent.contactId, intent.force)
                        updateInvoice { state ->
                            // Guard: ignore stale result if client changed during the request
                            if (state.formState.selectedClient?.id != intent.contactId) return@updateInvoice state
                            state.copy(
                                formState = state.formState.copy(
                                    peppolStatus = statusResult.getOrNull(),
                                    peppolStatusLoading = false
                                )
                            )
                        }
                    }

                    is CreateInvoiceIntent.OpenIssueDatePicker -> updateInvoice {
                        it.copy(uiState = it.uiState.copy(isDatePickerOpen = DatePickerTarget.IssueDate))
                    }
                    is CreateInvoiceIntent.OpenDueDatePicker -> updateInvoice {
                        it.copy(uiState = it.uiState.copy(isDatePickerOpen = DatePickerTarget.DueDate))
                    }
                    is CreateInvoiceIntent.CloseDatePicker -> updateInvoice {
                        it.copy(uiState = it.uiState.copy(isDatePickerOpen = null))
                    }
                    is CreateInvoiceIntent.SelectDate -> handleSelectDate(intent.date)
                    is CreateInvoiceIntent.UpdateIssueDate -> updateInvoice { state ->
                        state.copy(formState = synchronizeDueDate(state.formState.copy(issueDate = intent.date)))
                    }
                    is CreateInvoiceIntent.UpdateDueDate -> updateInvoice {
                        it.copy(formState = it.formState.copy(dueDate = intent.date))
                    }
                    is CreateInvoiceIntent.UpdatePaymentTermsDays -> updateInvoice { state ->
                        val clamped = intent.days.coerceIn(0, MaxPaymentTermsDays)
                        state.copy(
                            formState = synchronizeDueDate(
                                state.formState.copy(paymentTermsDays = clamped)
                            )
                        )
                    }
                    is CreateInvoiceIntent.UpdateDueDateMode -> updateInvoice { state ->
                        state.copy(
                            formState = synchronizeDueDate(
                                state.formState.copy(dueDateMode = intent.mode)
                            )
                        )
                    }

                    is CreateInvoiceIntent.ExpandItem -> updateInvoice {
                        it.copy(uiState = it.uiState.copy(expandedItemId = intent.itemId))
                    }
                    is CreateInvoiceIntent.CollapseItem -> updateInvoice {
                        it.copy(uiState = it.uiState.copy(expandedItemId = null))
                    }
                    is CreateInvoiceIntent.ToggleItemExpanded -> updateInvoice { state ->
                        val newExpanded = if (state.uiState.expandedItemId == intent.itemId) null else intent.itemId
                        state.copy(uiState = state.uiState.copy(expandedItemId = newExpanded))
                    }
                    is CreateInvoiceIntent.AddLineItem -> updateInvoice { state ->
                        val newItem = InvoiceLineItem()
                        state.copy(
                            formState = state.formState.copy(items = state.formState.items + newItem),
                            uiState = state.uiState.copy(expandedItemId = newItem.id)
                        )
                    }
                    is CreateInvoiceIntent.RemoveLineItem -> updateInvoice { state ->
                        val filtered = state.formState.items.filterNot { it.id == intent.itemId }
                        val finalItems = if (filtered.isEmpty()) listOf(InvoiceLineItem()) else filtered
                        val newExpanded = if (state.uiState.expandedItemId == intent.itemId) finalItems.first().id else state.uiState.expandedItemId
                        state.copy(
                            formState = state.formState.copy(
                                items = finalItems,
                                errors = state.formState.errors - ValidateInvoiceUseCase.FIELD_ITEMS
                            ),
                            uiState = state.uiState.copy(expandedItemId = newExpanded)
                        )
                    }
                    is CreateInvoiceIntent.UpdateItemDescription -> updateLineItem(intent.itemId) {
                        it.copy(description = intent.description)
                    }
                    is CreateInvoiceIntent.UpdateItemQuantity -> updateLineItem(intent.itemId) {
                        it.copy(quantity = intent.quantity)
                    }
                    is CreateInvoiceIntent.UpdateItemUnitPrice -> updateLineItem(intent.itemId) {
                        it.copy(unitPrice = intent.unitPrice)
                    }
                    is CreateInvoiceIntent.UpdateItemVatRate -> updateLineItem(intent.itemId) {
                        it.copy(vatRatePercent = intent.vatRatePercent)
                    }
                    is CreateInvoiceIntent.ApplyLatestInvoiceLines -> updateInvoice { state ->
                        val suggestion = state.uiState.latestInvoiceSuggestion ?: return@updateInvoice state
                        val appliedLines = if (suggestion.lines.isEmpty()) {
                            listOf(InvoiceLineItem())
                        } else {
                            suggestion.lines.map { it.copy(id = InvoiceLineItem().id) }
                        }
                        state.copy(
                            formState = state.formState.copy(
                                items = appliedLines,
                                errors = state.formState.errors - ValidateInvoiceUseCase.FIELD_ITEMS
                            ),
                            uiState = state.uiState.copy(
                                expandedItemId = appliedLines.firstOrNull()?.id,
                                latestInvoiceSuggestion = null,
                                suggestedSection = InvoiceSection.PaymentDelivery
                            )
                        )
                    }
                    is CreateInvoiceIntent.DismissLatestInvoiceSuggestion -> updateInvoice {
                        it.copy(uiState = it.uiState.copy(latestInvoiceSuggestion = null))
                    }

                    is CreateInvoiceIntent.UpdateStructuredCommunication -> updateInvoice {
                        it.copy(formState = it.formState.copy(structuredCommunication = intent.value))
                    }
                    is CreateInvoiceIntent.UpdateSenderIban -> updateInvoice {
                        it.copy(formState = it.formState.copy(senderIban = intent.value))
                    }
                    is CreateInvoiceIntent.UpdateSenderBic -> updateInvoice {
                        it.copy(formState = it.formState.copy(senderBic = intent.value))
                    }
                    is CreateInvoiceIntent.SelectDeliveryPreference -> updateInvoice {
                        it.copy(uiState = it.uiState.copy(selectedDeliveryPreference = intent.method))
                    }
                    is CreateInvoiceIntent.UpdateNotes -> updateInvoice {
                        it.copy(formState = it.formState.copy(notes = intent.notes))
                    }

                    is CreateInvoiceIntent.ToggleSection -> updateInvoice { state ->
                        val expanded = state.uiState.expandedSections.toMutableSet()
                        if (!expanded.add(intent.section)) expanded.remove(intent.section)
                        state.copy(uiState = state.uiState.copy(expandedSections = expanded))
                    }
                    is CreateInvoiceIntent.ExpandSection -> updateInvoice { state ->
                        state.copy(uiState = state.uiState.copy(expandedSections = state.uiState.expandedSections + intent.section))
                    }
                    is CreateInvoiceIntent.CollapseSection -> updateInvoice { state ->
                        state.copy(uiState = state.uiState.copy(expandedSections = state.uiState.expandedSections - intent.section))
                    }

                    is CreateInvoiceIntent.SetPreviewVisible -> updateInvoice { state ->
                        val canOpenPreview = state.formState.selectedClient != null
                        val visible = if (intent.visible) canOpenPreview else false
                        state.copy(uiState = state.uiState.copy(isPreviewVisible = visible))
                    }

                    is CreateInvoiceIntent.SaveAsDraft ->
                        with(submitHandlers) { submitInvoice(deliveryMethod = null) }
                    is CreateInvoiceIntent.SubmitWithResolvedDelivery -> {
                        val current = snapshotState()
                        val deliveryMethod = when (current.uiState.resolvedDeliveryAction.action) {
                            InvoiceResolvedAction.Peppol -> InvoiceDeliveryMethod.Peppol
                            InvoiceResolvedAction.PdfExport -> InvoiceDeliveryMethod.PdfExport
                        }
                        with(submitHandlers) { submitInvoice(deliveryMethod = deliveryMethod) }
                    }

                    is CreateInvoiceIntent.BackClicked -> action(CreateInvoiceAction.NavigateBack)
                    is CreateInvoiceIntent.ResetForm -> updateInvoice {
                        val form = CreateInvoiceFormState.createInitial()
                        it.copy(
                            formState = form.copy(structuredCommunication = generateStructuredCommunication()),
                            uiState = it.uiState.copy(
                                expandedItemId = form.items.first().id,
                                latestInvoiceSuggestion = null,
                                clientLookupState = it.uiState.clientLookupState.copy(
                                    query = "",
                                    isExpanded = false,
                                    localResults = emptyList(),
                                    externalResults = emptyList(),
                                    mergedSuggestions = emptyList(),
                                    isLocalLoading = false,
                                    isExternalLoading = false,
                                    errorHint = null
                                )
                            )
                        )
                    }
                    is CreateInvoiceIntent.LoadDefaults ->
                        with(submitHandlers) { loadDefaults() }
                }
            }
        }

    init {
        store.intent(CreateInvoiceIntent.LoadDefaults)
    }

    private suspend fun CreateInvoiceCtx.handleSelectDate(date: LocalDate) {
        updateInvoice { state ->
            when (state.uiState.isDatePickerOpen) {
                DatePickerTarget.IssueDate -> state.copy(
                    formState = synchronizeDueDate(state.formState.copy(issueDate = date)),
                    uiState = state.uiState.copy(isDatePickerOpen = null)
                )
                DatePickerTarget.DueDate -> state.copy(
                    formState = state.formState.copy(dueDate = date),
                    uiState = state.uiState.copy(isDatePickerOpen = null)
                )
                null -> state
            }
        }
    }

    private suspend fun CreateInvoiceCtx.updateLineItem(
        itemId: String,
        updater: (InvoiceLineItem) -> InvoiceLineItem
    ) {
        updateInvoice { state ->
            state.copy(
                formState = state.formState.copy(
                    items = state.formState.items.map { if (it.id == itemId) updater(it) else it },
                    errors = state.formState.errors - ValidateInvoiceUseCase.FIELD_ITEMS
                )
            )
        }
    }

    private suspend fun CreateInvoiceCtx.updateInvoice(
        transform: (CreateInvoiceState) -> CreateInvoiceState
    ) {
        updateState {
            val updated = transform(this)
            updated.copy(
                uiState = updated.uiState.copy(
                    resolvedDeliveryAction = resolveDelivery(updated.formState, updated.uiState)
                )
            )
        }
    }

    private suspend fun CreateInvoiceCtx.snapshotState(): CreateInvoiceState {
        var snapshot: CreateInvoiceState? = null
        withState { snapshot = this }
        return requireNotNull(snapshot)
    }

    private fun resolveDelivery(
        formState: CreateInvoiceFormState,
        uiState: CreateInvoiceUiState
    ): DeliveryResolution {
        if (uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.PdfExport) {
            return DeliveryResolution(InvoiceResolvedAction.PdfExport)
        }

        if (formState.selectedClient == null) {
            return DeliveryResolution(
                action = InvoiceResolvedAction.PdfExport,
                reason = "Select a client to enable PEPPOL."
            )
        }

        if (formState.peppolStatusLoading) {
            return DeliveryResolution(
                action = InvoiceResolvedAction.PdfExport,
                reason = "Checking PEPPOL availability."
            )
        }

        return if (formState.peppolStatus?.isFound == true) {
            DeliveryResolution(InvoiceResolvedAction.Peppol)
        } else {
            DeliveryResolution(
                action = InvoiceResolvedAction.PdfExport,
                reason = formState.peppolStatus?.errorMessage
                    ?: "Client is not PEPPOL-eligible."
            )
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun synchronizeDueDate(formState: CreateInvoiceFormState): CreateInvoiceFormState {
        if (formState.dueDateMode != InvoiceDueDateMode.Terms) return formState

        val issueDate = formState.issueDate ?: Clock.System.todayIn(TimeZone.currentSystemDefault())
        val dueDate = issueDate.plus(formState.paymentTermsDays, DateTimeUnit.DAY)
        return formState.copy(issueDate = issueDate, dueDate = dueDate)
    }

    private fun tech.dokus.domain.model.DocDto.Invoice.Confirmed.toSuggestion(): LatestInvoiceSuggestion? {
        if (lineItems.isEmpty()) return null
        val date = issueDate ?: return null
        return LatestInvoiceSuggestion(
            issueDate = date,
            lines = lineItems.map { item ->
                InvoiceLineItem(
                    description = item.description,
                    quantity = item.quantity?.value ?: 1.0,
                    unitPrice = item.unitPrice?.toDisplayString().orEmpty(),
                    vatRatePercent = item.vatRate?.basisPoints?.div(100) ?: 21
                )
            }
        )
    }

    private fun createInitialState(): CreateInvoiceState {
        val form = CreateInvoiceFormState.createInitial()
        return CreateInvoiceState(
            formState = form.copy(structuredCommunication = generateStructuredCommunication()),
            uiState = CreateInvoiceUiState(expandedItemId = form.items.first().id),
            invoiceNumberPreview = null
        )
    }
}
