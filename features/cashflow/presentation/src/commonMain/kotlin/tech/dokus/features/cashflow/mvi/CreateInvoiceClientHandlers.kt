package tech.dokus.features.cashflow.mvi

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tech.dokus.domain.LegalName
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.features.cashflow.mvi.model.InvoiceSection
import tech.dokus.features.cashflow.mvi.model.LatestInvoiceSuggestion
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.ValidateInvoiceUseCase
import tech.dokus.features.cashflow.usecases.GetContactPeppolStatusUseCase
import tech.dokus.features.cashflow.usecases.GetLatestInvoiceForContactUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.features.contacts.usecases.LookupContactsUseCase
import tech.dokus.foundation.platform.Logger

internal class CreateInvoiceClientHandlers(
    private val listContacts: ListContactsUseCase,
    private val lookupContacts: LookupContactsUseCase,
    private val searchCompanyUseCase: SearchCompanyUseCase,
    private val getContactPeppolStatus: GetContactPeppolStatusUseCase,
    private val getLatestInvoiceForContact: GetLatestInvoiceForContactUseCase,
    private val updateInvoice: suspend CreateInvoiceCtx.(transform: (CreateInvoiceState) -> CreateInvoiceState) -> Unit,
    private val snapshotState: suspend CreateInvoiceCtx.() -> CreateInvoiceState,
    private val synchronizeDueDate: (CreateInvoiceFormState) -> CreateInvoiceFormState,
    private val toSuggestion: DocDto.Invoice.Confirmed.() -> LatestInvoiceSuggestion?,
) {

    private val logger = Logger.forClass<CreateInvoiceClientHandlers>()
    internal var clientLookupJob: Job? = null

    suspend fun CreateInvoiceCtx.handleUpdateClientLookupQuery(query: String) {
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

    suspend fun CreateInvoiceCtx.handleSetClientLookupExpanded(expanded: Boolean) {
        clientLookupJob?.cancel()
        val query = snapshotState().uiState.clientLookupState.query

        updateInvoice { state ->
            state.copy(
                uiState = state.uiState.copy(
                    clientLookupState = state.uiState.clientLookupState.copy(
                        isExpanded = expanded,
                        isLocalLoading = expanded,
                        isExternalLoading = expanded && query.isNotBlank() && shouldLookupExternalClient(
                            query
                        )
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

    suspend fun CreateInvoiceCtx.handleSelectClient(client: tech.dokus.domain.model.contact.ContactDto) {
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
                        senderIban = latest.iban?.value.orEmpty()
                            .ifBlank { state.formState.senderIban }
                    )
                )
                state.copy(
                    formState = formState,
                    uiState = state.uiState.copy(
                        latestInvoiceSuggestion = latest.toSuggestion()
                    )
                )
            }
        }.onFailure { logger.w { "Failed to load latest invoice defaults: ${it.message}" } }

        intent(CreateInvoiceIntent.RefreshPeppolStatus(contactId = client.id))
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

        val merged =
            mergeClientLookupSuggestions(query, normalizedVat, localResults, externalResults)

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
}
