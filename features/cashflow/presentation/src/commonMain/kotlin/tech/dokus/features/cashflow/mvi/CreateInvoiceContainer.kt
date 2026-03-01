@file:Suppress("TooManyFunctions")

package tech.dokus.features.cashflow.mvi

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.enums.InvoiceDueDateMode
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.validators.ValidateOgmUseCase
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetInvoiceNumberPreviewUseCase
import tech.dokus.features.auth.usecases.GetTenantSettingsUseCase
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceUiState
import tech.dokus.features.cashflow.mvi.model.DatePickerTarget
import tech.dokus.features.cashflow.mvi.model.DeliveryResolution
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem
import tech.dokus.features.cashflow.mvi.model.InvoiceResolvedAction
import tech.dokus.features.cashflow.mvi.model.InvoiceSection
import tech.dokus.features.cashflow.mvi.model.LatestInvoiceSuggestion
import tech.dokus.features.cashflow.presentation.cashflow.model.mapper.toCreateInvoiceRequest
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.ValidateInvoiceUseCase
import tech.dokus.features.cashflow.usecases.GetContactPeppolStatusUseCase
import tech.dokus.features.cashflow.usecases.GetLatestInvoiceForContactUseCase
import tech.dokus.features.cashflow.usecases.SubmitInvoiceWithDeliveryResult
import tech.dokus.features.cashflow.usecases.SubmitInvoiceWithDeliveryUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.features.contacts.usecases.LookupContactsUseCase
import tech.dokus.foundation.platform.Logger
import kotlin.math.absoluteValue
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

    private val logger = Logger.forClass<CreateInvoiceContainer>()
    private var clientLookupJob: Job? = null

    override val store: Store<CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction> =
        store(createInitialState()) {
            reduce { intent ->
                when (intent) {
                    is CreateInvoiceIntent.UpdateClientLookupQuery -> handleUpdateClientLookupQuery(intent.query)
                    is CreateInvoiceIntent.SetClientLookupExpanded -> handleSetClientLookupExpanded(intent.expanded)
                    is CreateInvoiceIntent.SelectClient -> handleSelectClient(intent.client)
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

                    is CreateInvoiceIntent.SaveAsDraft -> submitInvoice(deliveryMethod = null)
                    is CreateInvoiceIntent.SubmitWithResolvedDelivery -> {
                        val current = snapshotState()
                        val deliveryMethod = when (current.uiState.resolvedDeliveryAction.action) {
                            InvoiceResolvedAction.Peppol -> InvoiceDeliveryMethod.Peppol
                            InvoiceResolvedAction.PdfExport -> InvoiceDeliveryMethod.PdfExport
                        }
                        submitInvoice(deliveryMethod = deliveryMethod)
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
                    is CreateInvoiceIntent.LoadDefaults -> loadDefaults()
                }
            }
        }

    init {
        store.intent(CreateInvoiceIntent.LoadDefaults)
    }

    private suspend fun CreateInvoiceCtx.handleUpdateClientLookupQuery(query: String) {
        val trimmed = query.trim()
        clientLookupJob?.cancel()
        val wasExpanded = snapshotState().uiState.clientLookupState.isExpanded

        if (trimmed.isBlank()) {
            updateInvoice { state ->
                state.copy(
                    uiState = state.uiState.copy(
                        clientLookupState = state.uiState.clientLookupState.copy(
                            query = "",
                            isExpanded = wasExpanded,
                            localResults = emptyList(),
                            externalResults = emptyList(),
                            mergedSuggestions = emptyList(),
                            isLocalLoading = wasExpanded,
                            isExternalLoading = false,
                            errorHint = null
                        )
                    )
                )
            }
            if (wasExpanded) {
                clientLookupJob = launch {
                    delay(CLIENT_LOOKUP_DEBOUNCE_MS)
                    loadInitialLocalSuggestions(query = "")
                }
            }
            return
        }

        val lookupLocal = shouldLookupLocalClient(trimmed)
        val lookupExternal = shouldLookupExternalClient(trimmed)
        val shouldLoadInitialLocal = !lookupLocal

        updateInvoice { state ->
            state.copy(
                uiState = state.uiState.copy(
                    clientLookupState = state.uiState.clientLookupState.copy(
                        query = trimmed,
                        isExpanded = true,
                        isLocalLoading = shouldLoadInitialLocal || lookupLocal,
                        isExternalLoading = lookupExternal,
                        errorHint = null
                    )
                )
            )
        }

        clientLookupJob = launch {
            delay(CLIENT_LOOKUP_DEBOUNCE_MS)
            if (shouldLoadInitialLocal) {
                loadInitialLocalSuggestions(query = trimmed)
            } else {
                loadClientLookupSuggestions(trimmed)
            }
        }
    }

    private suspend fun CreateInvoiceCtx.handleSetClientLookupExpanded(expanded: Boolean) {
        clientLookupJob?.cancel()
        val query = snapshotState().uiState.clientLookupState.query

        updateInvoice { state ->
            state.copy(
                uiState = state.uiState.copy(
                    clientLookupState = state.uiState.clientLookupState.copy(
                        isExpanded = expanded,
                        isLocalLoading = expanded,
                        isExternalLoading = expanded && query.isNotBlank() && shouldLookupExternalClient(query)
                    )
                )
            )
        }

        if (!expanded) return

        clientLookupJob = launch {
            delay(CLIENT_LOOKUP_DEBOUNCE_MS)
            if (query.isBlank() || !shouldLookupLocalClient(query)) {
                loadInitialLocalSuggestions(query = query)
            } else {
                loadClientLookupSuggestions(query = query)
            }
        }
    }

    private suspend fun CreateInvoiceCtx.loadInitialLocalSuggestions(query: String) {
        val normalizedQuery = query.trim().lowercase()
        val localResults = listContacts(
            isActive = null,
            limit = CLIENT_LOOKUP_LOCAL_LIMIT,
            offset = 0
        ).getOrElse { emptyList() }

        val filteredLocalResults = if (normalizedQuery.isBlank()) {
            localResults
        } else {
            localResults.filter { contact ->
                contact.name.value.lowercase().contains(normalizedQuery) ||
                    (contact.vatNumber?.normalized?.contains(normalizedQuery) == true) ||
                    (contact.email?.value?.lowercase()?.contains(normalizedQuery) == true)
            }
        }

        val suggestions = buildList {
            filteredLocalResults.forEach { contact ->
                add(ClientSuggestion.LocalContact(contact))
            }
            if (query.isNotBlank()) {
                add(ClientSuggestion.CreateManual(query))
            }
        }

        updateInvoice { state ->
            if (state.uiState.clientLookupState.query != query || !state.uiState.clientLookupState.isExpanded) {
                return@updateInvoice state
            }
            state.copy(
                uiState = state.uiState.copy(
                    clientLookupState = state.uiState.clientLookupState.copy(
                        localResults = filteredLocalResults,
                        externalResults = emptyList(),
                        mergedSuggestions = suggestions,
                        isExpanded = true,
                        isLocalLoading = false,
                        isExternalLoading = false,
                        errorHint = null
                    )
                )
            )
        }
    }

    private suspend fun CreateInvoiceCtx.loadClientLookupSuggestions(query: String) {
        val vat = VatNumber.from(query)
        val normalizedVat = vat?.takeIf { it.isValid }
        val localEnabled = shouldLookupLocalClient(query)
        val externalEnabled = shouldLookupExternalClient(query)

        val (localResults, externalResults, errorHint) = coroutineScope {
            val localDeferred = if (localEnabled) {
                async {
                    lookupContacts(
                        query = query,
                        isActive = null,
                        limit = CLIENT_LOOKUP_LOCAL_LIMIT,
                        offset = 0
                    )
                }
            } else {
                null
            }

            val externalDeferred = if (externalEnabled) {
                async {
                    val companyName = if (normalizedVat != null) null else LegalName(query)
                    searchCompanyUseCase(
                        name = companyName,
                        number = normalizedVat
                    )
                }
            } else {
                null
            }

            val local = localDeferred?.await()?.getOrElse { emptyList() } ?: emptyList()
            val externalResult = externalDeferred?.await()

            val external = externalResult?.fold(
                onSuccess = { response ->
                    response.results
                        .map(::toExternalCandidate)
                        .distinctBy { candidate ->
                            candidate.vatNumber?.normalized
                                ?: candidate.enterpriseNumber
                        }
                        .take(CLIENT_LOOKUP_EXTERNAL_LIMIT)
                },
                onFailure = { emptyList() }
            ) ?: emptyList()

            val hint = if (externalResult?.isFailure == true) {
                "External company lookup unavailable. Showing local results."
            } else {
                null
            }

            Triple(local, external, hint)
        }

        val merged = mergeClientLookupSuggestions(query, normalizedVat, localResults, externalResults)

        updateInvoice { state ->
            if (state.uiState.clientLookupState.query != query) {
                return@updateInvoice state
            }
            state.copy(
                uiState = state.uiState.copy(
                    clientLookupState = state.uiState.clientLookupState.copy(
                        localResults = localResults,
                        externalResults = externalResults,
                        mergedSuggestions = merged,
                        isExpanded = true,
                        isLocalLoading = false,
                        isExternalLoading = false,
                        errorHint = errorHint
                    )
                )
            )
        }
    }

    private fun toExternalCandidate(lookup: EntityLookup): ExternalClientCandidate {
        return ExternalClientCandidate(
            name = lookup.name.value,
            vatNumber = lookup.vatNumber,
            enterpriseNumber = lookup.enterpriseNumber,
            prefillAddress = lookup.address?.let { address ->
                listOfNotNull(
                    address.streetLine1,
                    address.streetLine2,
                    listOfNotNull(address.postalCode, address.city)
                        .takeIf { it.isNotEmpty() }
                        ?.joinToString(" "),
                    address.country.dbValue
                ).joinToString(", ")
            }
        )
    }

    private suspend fun CreateInvoiceCtx.handleSelectClient(client: tech.dokus.domain.model.contact.ContactDto) {
        clientLookupJob?.cancel()
        updateInvoice { state ->
            state.copy(
                formState = state.formState.copy(
                    selectedClient = client,
                    peppolStatus = null,
                    peppolStatusLoading = false,
                    errors = state.formState.errors - ValidateInvoiceUseCase.FIELD_CLIENT
                ),
                uiState = state.uiState.copy(
                    suggestedSection = InvoiceSection.LineItems,
                    latestInvoiceSuggestion = null,
                    clientLookupState = state.uiState.clientLookupState.copy(
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

        getLatestInvoiceForContact(client.id).onSuccess { latest ->
            if (latest == null) return@onSuccess
            updateInvoice { state ->
                // Guard: ignore stale result if user already switched to a different client
                if (state.formState.selectedClient?.id != client.id) return@updateInvoice state
                val formState = synchronizeDueDate(
                    state.formState.copy(
                        paymentTermsDays = latest.paymentTermsDays ?: state.formState.paymentTermsDays,
                        dueDateMode = latest.dueDateMode,
                        senderIban = latest.senderIban?.value.orEmpty().ifBlank { state.formState.senderIban },
                        senderBic = latest.senderBic?.value.orEmpty().ifBlank { state.formState.senderBic }
                    )
                )
                state.copy(
                    formState = formState,
                    uiState = state.uiState.copy(
                        selectedDeliveryPreference = latest.deliveryMethod,
                        latestInvoiceSuggestion = latest.toSuggestion()
                    )
                )
            }
        }.onFailure { logger.w { "Failed to load latest invoice defaults: ${it.message}" } }

        intent(CreateInvoiceIntent.RefreshPeppolStatus(contactId = client.id))
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

    private suspend fun CreateInvoiceCtx.loadDefaults() {
        if (snapshotState().uiState.defaultsLoaded) return

        getInvoiceNumberPreview()
            .onSuccess { preview ->
                updateInvoice { it.copy(invoiceNumberPreview = preview) }
            }
            .onFailure { logger.w { "Could not load invoice number preview: ${it.message}" } }

        val tenantSettingsResult = getTenantSettings()
        val currentTenantResult = getCurrentTenant()

        tenantSettingsResult.onFailure { logger.w { "Could not load tenant defaults: ${it.message}" } }
        currentTenantResult.onFailure { logger.w { "Could not load current tenant: ${it.message}" } }

        val settings = tenantSettingsResult.getOrNull()
        val currentTenant = requireNotNull(currentTenantResult.getOrNull()) {
            "Current tenant must be available on Create Invoice screen."
        }

        updateInvoice { state ->
            val withDefaults = settings?.let { tenantSettings ->
                synchronizeDueDate(
                    state.formState.copy(
                        paymentTermsDays = tenantSettings.defaultPaymentTerms,
                        senderIban = tenantSettings.companyIban?.value.orEmpty(),
                        senderBic = tenantSettings.companyBic?.value.orEmpty(),
                        notes = state.formState.notes.ifBlank { tenantSettings.paymentTermsText.orEmpty() }
                    )
                )
            } ?: state.formState

            state.copy(
                formState = withDefaults,
                uiState = state.uiState.copy(
                    senderCompanyName = currentTenant.legalName.value,
                    senderCompanyVat = currentTenant.vatNumber.formatted
                )
            )
        }

        updateInvoice { state ->
            val updated = if (state.formState.structuredCommunication.isNotBlank()) {
                state
            } else {
                state.copy(
                    formState = state.formState.copy(
                        structuredCommunication = generateStructuredCommunication()
                    )
                )
            }
            updated.copy(uiState = updated.uiState.copy(defaultsLoaded = true))
        }
    }

    private suspend fun CreateInvoiceCtx.submitInvoice(deliveryMethod: InvoiceDeliveryMethod?) {
        val current = snapshotState()
        if (current.formState.isSaving) return

        val validation = validateInvoice(current.formState)
        if (!validation.isValid) {
            val firstError = validation.errors.entries.firstOrNull()
            val firstInvalidSection = sectionForError(firstError?.key)
            updateInvoice { state ->
                state.copy(
                    formState = state.formState.copy(errors = validation.errors),
                    uiState = state.uiState.copy(
                        expandedSections = state.uiState.expandedSections + firstInvalidSection,
                        suggestedSection = firstInvalidSection
                    )
                )
            }
            action(
                CreateInvoiceAction.ShowValidationError(
                    firstError?.value?.message ?: "Invoice is missing required fields."
                )
            )
            return
        }

        updateInvoice {
            it.copy(formState = it.formState.copy(isSaving = true, errors = emptyMap()))
        }

        // Persist the user's delivery preference for UX recall, but the actual
        // delivery action is determined by `deliveryMethod` (null = draft, no delivery).
        val persistedPreference = current.uiState.selectedDeliveryPreference
        val request = current.formState.toCreateInvoiceRequest(persistedPreference)
        submitInvoiceWithDelivery(request, deliveryMethod).fold(
            onSuccess = { result ->
                updateInvoice { it.copy(formState = it.formState.copy(isSaving = false)) }
                when (result) {
                    is SubmitInvoiceWithDeliveryResult.DraftSaved -> {
                        action(CreateInvoiceAction.ShowSuccess("Draft saved."))
                        action(CreateInvoiceAction.NavigateBack)
                    }
                    is SubmitInvoiceWithDeliveryResult.PeppolQueued -> {
                        action(CreateInvoiceAction.ShowSuccess("Invoice queued for PEPPOL."))
                        action(CreateInvoiceAction.NavigateBack)
                    }
                    is SubmitInvoiceWithDeliveryResult.PdfReady -> {
                        action(CreateInvoiceAction.OpenExternalUrl(result.downloadUrl))
                        action(CreateInvoiceAction.ShowSuccess("Invoice PDF is ready."))
                        action(CreateInvoiceAction.NavigateBack)
                    }
                    is SubmitInvoiceWithDeliveryResult.DeliveryFailed -> {
                        action(CreateInvoiceAction.ShowError("Invoice saved but delivery failed: ${result.error}"))
                        action(CreateInvoiceAction.NavigateBack)
                    }
                }
            },
            onFailure = { error ->
                updateInvoice {
                    it.copy(formState = it.formState.copy(isSaving = false))
                }
                action(CreateInvoiceAction.ShowError(error.message ?: "Failed to submit invoice."))
            }
        )
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

    private fun sectionForError(field: String?): InvoiceSection {
        return when (field) {
            ValidateInvoiceUseCase.FIELD_CLIENT -> InvoiceSection.Client
            ValidateInvoiceUseCase.FIELD_ITEMS -> InvoiceSection.LineItems
            ValidateInvoiceUseCase.FIELD_DUE_DATE -> InvoiceSection.DatesTerms
            else -> InvoiceSection.PaymentDelivery
        }
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

    private fun generateStructuredCommunication(): String {
        val base = Clock.System.now().toEpochMilliseconds().absoluteValue % 10_000_000_000L
        return ValidateOgmUseCase.generate(base)
    }

    private fun tech.dokus.domain.model.FinancialDocumentDto.InvoiceDto.toSuggestion(): LatestInvoiceSuggestion? {
        if (items.isEmpty()) return null
        return LatestInvoiceSuggestion(
            issueDate = issueDate,
            lines = items.map { item ->
                InvoiceLineItem(
                    description = item.description,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice.toDisplayString(),
                    vatRatePercent = item.vatRate.basisPoints / 100
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
