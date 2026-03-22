@file:Suppress("TooManyFunctions")

package tech.dokus.features.cashflow.mvi.clientlookup

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
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.cashflow.mvi.CLIENT_LOOKUP_DEBOUNCE_MS
import tech.dokus.features.cashflow.mvi.CLIENT_LOOKUP_EXTERNAL_LIMIT
import tech.dokus.features.cashflow.mvi.CLIENT_LOOKUP_LOCAL_LIMIT
import tech.dokus.features.cashflow.mvi.mergeClientLookupSuggestions
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem
import tech.dokus.features.cashflow.mvi.model.LatestInvoiceSuggestion
import tech.dokus.features.cashflow.mvi.shouldLookupExternalClient
import tech.dokus.features.cashflow.mvi.shouldLookupLocalClient
import tech.dokus.features.cashflow.usecases.GetContactPeppolStatusUseCase
import tech.dokus.features.cashflow.usecases.GetLatestInvoiceForContactUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.features.contacts.usecases.LookupContactsUseCase
import tech.dokus.foundation.platform.Logger

private typealias ClientLookupCtx =
    PipelineContext<ClientLookupChildState, ClientLookupIntent, ClientLookupAction>

internal class ClientLookupContainer(
    private val listContacts: ListContactsUseCase,
    private val lookupContacts: LookupContactsUseCase,
    private val searchCompanyUseCase: SearchCompanyUseCase,
    private val getContactPeppolStatus: GetContactPeppolStatusUseCase,
    private val getLatestInvoiceForContact: GetLatestInvoiceForContactUseCase,
) : Container<ClientLookupChildState, ClientLookupIntent, ClientLookupAction> {

    private val logger = Logger.forClass<ClientLookupContainer>()
    private var clientLookupJob: Job? = null

    override val store: Store<ClientLookupChildState, ClientLookupIntent, ClientLookupAction> =
        store(ClientLookupChildState()) {
            reduce { intent ->
                when (intent) {
                    is ClientLookupIntent.UpdateQuery -> handleUpdateQuery(intent.query)
                    is ClientLookupIntent.SetExpanded -> handleSetExpanded(intent.expanded)
                    is ClientLookupIntent.SelectClient -> handleSelectClient(intent.client)
                    is ClientLookupIntent.SelectExternal -> handleSelectExternal(intent.candidate)
                    is ClientLookupIntent.CreateManually -> handleCreateManually(intent.query)
                    is ClientLookupIntent.Clear -> handleClear()
                    is ClientLookupIntent.RefreshPeppolStatus ->
                        handleRefreshPeppolStatus(intent.contactId, intent.force)
                }
            }
        }

    // region Update Query

    private suspend fun ClientLookupCtx.handleUpdateQuery(query: String) {
        val trimmed = query.trim()
        clientLookupJob?.cancel()

        var wasExpanded = false
        withState { wasExpanded = clientLookupState.isExpanded }

        if (trimmed.isBlank()) {
            updateState {
                copy(
                    clientLookupState = clientLookupState.copy(
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

        updateState {
            copy(
                clientLookupState = clientLookupState.copy(
                    query = trimmed,
                    isExpanded = true,
                    isLocalLoading = shouldLoadInitialLocal || lookupLocal,
                    isExternalLoading = lookupExternal,
                    errorHint = null
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

    // endregion

    // region Set Expanded

    private suspend fun ClientLookupCtx.handleSetExpanded(expanded: Boolean) {
        clientLookupJob?.cancel()

        var query = ""
        withState { query = clientLookupState.query }

        updateState {
            copy(
                clientLookupState = clientLookupState.copy(
                    isExpanded = expanded,
                    isLocalLoading = expanded,
                    isExternalLoading = expanded && query.isNotBlank() && shouldLookupExternalClient(query)
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

    // endregion

    // region Select Client

    private suspend fun ClientLookupCtx.handleSelectClient(
        client: tech.dokus.domain.model.contact.ContactDto,
    ) {
        clientLookupJob?.cancel()

        updateState {
            copy(
                clientLookupState = clientLookupState.copy(
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
        }

        // Emit the selection immediately so parent can update form state
        action(ClientLookupAction.ClientSelected(contact = client))

        // Load latest invoice defaults in the background
        getLatestInvoiceForContact(client.id).onSuccess { latest ->
            if (latest == null) return@onSuccess
            val suggestion = latest.toSuggestion() ?: return@onSuccess
            action(
                ClientLookupAction.ClientSelected(
                    contact = client,
                    latestInvoiceSuggestion = suggestion,
                    senderIban = latest.iban?.value?.takeIf { it.isNotBlank() },
                )
            )
        }.onFailure { logger.w { "Failed to load latest invoice defaults: ${it.message}" } }

        // Trigger peppol status refresh
        intent(ClientLookupIntent.RefreshPeppolStatus(contactId = client.id))
    }

    // endregion

    // region Select External / Create Manually / Clear

    private suspend fun ClientLookupCtx.handleSelectExternal(candidate: ExternalClientCandidate) {
        action(
            ClientLookupAction.NavigateToCreateContact(
                prefillCompanyName = candidate.name,
                prefillVat = candidate.vatNumber?.value,
                prefillAddress = candidate.prefillAddress,
                origin = "InvoiceCreate"
            )
        )
    }

    private suspend fun ClientLookupCtx.handleCreateManually(query: String) {
        val normalizedVat = VatNumber.from(query)?.takeIf { it.isValid }?.value
        action(
            ClientLookupAction.NavigateToCreateContact(
                prefillCompanyName = query.takeIf { it.isNotBlank() },
                prefillVat = normalizedVat,
                origin = "InvoiceCreate"
            )
        )
    }

    private suspend fun ClientLookupCtx.handleClear() {
        clientLookupJob?.cancel()
        updateState {
            copy(
                clientLookupState = clientLookupState.copy(
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
        }
        action(ClientLookupAction.ClientCleared)
    }

    // endregion

    // region Peppol Status

    private suspend fun ClientLookupCtx.handleRefreshPeppolStatus(
        contactId: tech.dokus.domain.ids.ContactId,
        force: Boolean,
    ) {
        val statusResult = getContactPeppolStatus(contactId, force)
        action(
            ClientLookupAction.PeppolStatusUpdated(
                contactId = contactId,
                peppolStatus = statusResult.getOrNull(),
            )
        )
    }

    // endregion

    // region Private Helpers

    private suspend fun ClientLookupCtx.loadInitialLocalSuggestions(query: String) {
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

        updateState {
            if (clientLookupState.query != query || !clientLookupState.isExpanded) {
                return@updateState this
            }
            copy(
                clientLookupState = clientLookupState.copy(
                    localResults = filteredLocalResults,
                    externalResults = emptyList(),
                    mergedSuggestions = suggestions,
                    isExpanded = true,
                    isLocalLoading = false,
                    isExternalLoading = false,
                    errorHint = null
                )
            )
        }
    }

    private suspend fun ClientLookupCtx.loadClientLookupSuggestions(query: String) {
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

        updateState {
            if (clientLookupState.query != query) {
                return@updateState this
            }
            copy(
                clientLookupState = clientLookupState.copy(
                    localResults = localResults,
                    externalResults = externalResults,
                    mergedSuggestions = merged,
                    isExpanded = true,
                    isLocalLoading = false,
                    isExternalLoading = false,
                    errorHint = errorHint
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

    private fun DocDto.Invoice.Confirmed.toSuggestion(): LatestInvoiceSuggestion? {
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

    // endregion
}
