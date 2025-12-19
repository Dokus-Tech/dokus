package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.model.PeppolInboxPollResponse
import ai.dokus.foundation.domain.model.PeppolTransmissionDto
import ai.dokus.foundation.domain.model.PeppolValidationResult
import ai.dokus.foundation.domain.model.PeppolVerifyResponse
import ai.dokus.foundation.domain.model.SendInvoiceViaPeppolResponse
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for Peppol sending operations.
 * Handles recipient verification, invoice validation, sending, and transmission history.
 */
class PeppolSendViewModel : BaseViewModel<DokusState<PaginationState<PeppolTransmissionDto>>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<PeppolSendViewModel>()
    private val dataSource: CashflowRemoteDataSource by inject()

    // Pagination state for transmissions
    private val loadedTransmissions = MutableStateFlow<List<PeppolTransmissionDto>>(emptyList())
    private val paginationState = MutableStateFlow(PaginationState<PeppolTransmissionDto>(pageSize = PAGE_SIZE))

    // Filter state
    private val _directionFilter = MutableStateFlow<PeppolTransmissionDirection?>(null)
    val directionFilter: StateFlow<PeppolTransmissionDirection?> = _directionFilter.asStateFlow()

    private val _statusFilter = MutableStateFlow<PeppolStatus?>(null)
    val statusFilter: StateFlow<PeppolStatus?> = _statusFilter.asStateFlow()

    // Recipient verification state
    private val _verificationState = MutableStateFlow<DokusState<PeppolVerifyResponse>>(DokusState.idle())
    val verificationState: StateFlow<DokusState<PeppolVerifyResponse>> = _verificationState.asStateFlow()

    // Invoice validation state
    private val _validationState = MutableStateFlow<DokusState<PeppolValidationResult>>(DokusState.idle())
    val validationState: StateFlow<DokusState<PeppolValidationResult>> = _validationState.asStateFlow()

    // Send invoice state
    private val _sendState = MutableStateFlow<DokusState<SendInvoiceViaPeppolResponse>>(DokusState.idle())
    val sendState: StateFlow<DokusState<SendInvoiceViaPeppolResponse>> = _sendState.asStateFlow()

    // Inbox poll state
    private val _pollState = MutableStateFlow<DokusState<PeppolInboxPollResponse>>(DokusState.idle())
    val pollState: StateFlow<DokusState<PeppolInboxPollResponse>> = _pollState.asStateFlow()

    // Single transmission lookup
    private val _invoiceTransmission = MutableStateFlow<DokusState<PeppolTransmissionDto?>>(DokusState.idle())
    val invoiceTransmission: StateFlow<DokusState<PeppolTransmissionDto?>> = _invoiceTransmission.asStateFlow()

    // ========================================================================
    // TRANSMISSION HISTORY
    // ========================================================================

    /**
     * Load transmission history.
     */
    fun loadTransmissions() {
        scope.launch {
            logger.d { "Loading transmission history" }
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedTransmissions.value = emptyList()
            mutableState.emitLoading()
            loadPage(page = 0, reset = true)
        }
    }

    /**
     * Refresh transmission history.
     */
    fun refresh() {
        loadTransmissions()
    }

    /**
     * Load next page of transmissions.
     */
    fun loadNextPage() {
        val current = paginationState.value
        if (current.isLoadingMore || !current.hasMorePages) return

        scope.launch {
            logger.d { "Loading next transmission page (current=${current.currentPage})" }
            paginationState.value = current.copy(isLoadingMore = true)
            emitSuccess()
            loadPage(page = current.currentPage + 1, reset = false)
        }
    }

    private suspend fun loadPage(page: Int, reset: Boolean) {
        val offset = page * PAGE_SIZE
        val result = dataSource.listPeppolTransmissions(
            direction = _directionFilter.value,
            status = _statusFilter.value,
            limit = PAGE_SIZE,
            offset = offset
        )

        result.fold(
            onSuccess = { transmissions ->
                logger.i { "Loaded ${transmissions.size} transmissions (offset=$offset)" }
                loadedTransmissions.value = if (reset) transmissions else loadedTransmissions.value + transmissions

                paginationState.value = paginationState.value.copy(
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = transmissions.size == PAGE_SIZE,
                    pageSize = PAGE_SIZE
                )
                emitSuccess()
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load transmissions" }
                paginationState.value = paginationState.value.copy(
                    isLoadingMore = false,
                    hasMorePages = false
                )
                if (loadedTransmissions.value.isEmpty()) {
                    mutableState.emitError(error) { scope.launch { loadTransmissions() } }
                } else {
                    emitSuccess()
                }
            }
        )
    }

    // ========================================================================
    // FILTERS
    // ========================================================================

    /**
     * Set direction filter.
     */
    fun setDirectionFilter(direction: PeppolTransmissionDirection?) {
        _directionFilter.value = direction
        loadTransmissions()
    }

    /**
     * Set status filter.
     */
    fun setStatusFilter(status: PeppolStatus?) {
        _statusFilter.value = status
        loadTransmissions()
    }

    /**
     * Clear all filters.
     */
    fun clearFilters() {
        _directionFilter.value = null
        _statusFilter.value = null
        loadTransmissions()
    }

    // ========================================================================
    // RECIPIENT VERIFICATION
    // ========================================================================

    /**
     * Verify a Peppol recipient.
     */
    fun verifyRecipient(peppolId: String) {
        scope.launch {
            logger.d { "Verifying Peppol recipient: $peppolId" }
            _verificationState.value = DokusState.loading()

            dataSource.verifyPeppolRecipient(peppolId).fold(
                onSuccess = { response ->
                    logger.i { "Recipient verified: registered=${response.registered}" }
                    _verificationState.value = DokusState.success(response)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to verify recipient: $peppolId" }
                    _verificationState.emitError(error) { scope.launch { verifyRecipient(peppolId) } }
                }
            )
        }
    }

    /**
     * Reset verification state.
     */
    fun resetVerificationState() {
        _verificationState.value = DokusState.idle()
    }

    // ========================================================================
    // INVOICE VALIDATION
    // ========================================================================

    /**
     * Validate an invoice for Peppol sending.
     */
    fun validateInvoice(invoiceId: InvoiceId) {
        scope.launch {
            logger.d { "Validating invoice for Peppol: $invoiceId" }
            _validationState.value = DokusState.loading()

            dataSource.validateInvoiceForPeppol(invoiceId).fold(
                onSuccess = { result ->
                    logger.i { "Invoice validation: valid=${result.isValid}, errors=${result.errors.size}" }
                    _validationState.value = DokusState.success(result)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to validate invoice: $invoiceId" }
                    _validationState.emitError(error) { scope.launch { validateInvoice(invoiceId) } }
                }
            )
        }
    }

    /**
     * Reset validation state.
     */
    fun resetValidationState() {
        _validationState.value = DokusState.idle()
    }

    // ========================================================================
    // SEND INVOICE
    // ========================================================================

    /**
     * Send an invoice via Peppol.
     */
    fun sendInvoice(
        invoiceId: InvoiceId,
        onSuccess: (SendInvoiceViaPeppolResponse) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            logger.d { "Sending invoice via Peppol: $invoiceId" }
            _sendState.value = DokusState.loading()

            dataSource.sendInvoiceViaPeppol(invoiceId).fold(
                onSuccess = { response ->
                    logger.i { "Invoice sent: transmissionId=${response.transmissionId}, status=${response.status}" }
                    _sendState.value = DokusState.success(response)
                    onSuccess(response)
                    // Refresh transmission history
                    loadTransmissions()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to send invoice: $invoiceId" }
                    _sendState.emitError(error) { scope.launch { sendInvoice(invoiceId, onSuccess, onError) } }
                    onError(error)
                }
            )
        }
    }

    /**
     * Reset send state.
     */
    fun resetSendState() {
        _sendState.value = DokusState.idle()
    }

    // ========================================================================
    // INBOX POLLING
    // ========================================================================

    /**
     * Poll Peppol inbox for new documents.
     */
    fun pollInbox(
        onSuccess: (PeppolInboxPollResponse) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            logger.d { "Polling Peppol inbox" }
            _pollState.value = DokusState.loading()

            dataSource.pollPeppolInbox().fold(
                onSuccess = { response ->
                    logger.i { "Inbox polled: ${response.newDocuments} new documents" }
                    _pollState.value = DokusState.success(response)
                    onSuccess(response)
                    // Refresh transmission history if new documents were processed
                    if (response.newDocuments > 0) {
                        loadTransmissions()
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to poll inbox" }
                    _pollState.emitError(error) { scope.launch { pollInbox(onSuccess, onError) } }
                    onError(error)
                }
            )
        }
    }

    /**
     * Reset poll state.
     */
    fun resetPollState() {
        _pollState.value = DokusState.idle()
    }

    // ========================================================================
    // SINGLE TRANSMISSION LOOKUP
    // ========================================================================

    /**
     * Get transmission for a specific invoice.
     */
    fun getTransmissionForInvoice(invoiceId: InvoiceId) {
        scope.launch {
            logger.d { "Getting transmission for invoice: $invoiceId" }
            _invoiceTransmission.value = DokusState.loading()

            dataSource.getPeppolTransmissionForInvoice(invoiceId).fold(
                onSuccess = { transmission ->
                    logger.d { "Transmission found: ${transmission != null}" }
                    _invoiceTransmission.value = DokusState.success(transmission)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to get transmission for invoice: $invoiceId" }
                    _invoiceTransmission.emitError(error) { scope.launch { getTransmissionForInvoice(invoiceId) } }
                }
            )
        }
    }

    /**
     * Reset invoice transmission state.
     */
    fun resetInvoiceTransmissionState() {
        _invoiceTransmission.value = DokusState.idle()
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private fun emitSuccess() {
        val updatedState = paginationState.value.copy(
            data = loadedTransmissions.value,
            pageSize = PAGE_SIZE
        )
        paginationState.value = updatedState
        mutableState.value = DokusState.success(updatedState)
    }

    companion object {
        private const val PAGE_SIZE = 50
    }
}

// Extension for MutableStateFlow<DokusState<T>>
private fun <T> MutableStateFlow<DokusState<T>>.emitError(
    error: Throwable,
    retryHandler: () -> Unit
) {
    value = DokusState.error(
        DokusException.Unknown(error),
        retryHandler
    )
}
