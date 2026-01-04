@file:Suppress("LongParameterList") // Pagination requires multiple parameters

package tech.dokus.features.cashflow.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.usecases.PeppolUseCase
import tech.dokus.foundation.platform.Logger

private typealias SendCtx = PipelineContext<PeppolSendState, PeppolSendIntent, PeppolSendAction>

private const val PAGE_SIZE = 50

/**
 * Container for Peppol sending operations using FlowMVI.
 * Handles recipient verification, invoice validation, sending, and transmission history.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class PeppolSendContainer(
    private val peppolUseCase: PeppolUseCase,
) : Container<PeppolSendState, PeppolSendIntent, PeppolSendAction> {

    private val logger = Logger.forClass<PeppolSendContainer>()

    override val store: Store<PeppolSendState, PeppolSendIntent, PeppolSendAction> =
        store(PeppolSendState.Idle) {
            reduce { intent ->
                when (intent) {
                    // Transmission history
                    is PeppolSendIntent.LoadTransmissions -> handleLoadTransmissions()
                    is PeppolSendIntent.Refresh -> handleRefresh()
                    is PeppolSendIntent.LoadNextPage -> handleLoadNextPage()

                    // Filters
                    is PeppolSendIntent.SetDirectionFilter -> handleSetDirectionFilter(intent.direction)
                    is PeppolSendIntent.SetStatusFilter -> handleSetStatusFilter(intent.status)
                    is PeppolSendIntent.ClearFilters -> handleClearFilters()

                    // Recipient verification
                    is PeppolSendIntent.VerifyRecipient -> handleVerifyRecipient(intent.peppolId)
                    is PeppolSendIntent.ResetVerificationState -> handleResetVerificationState()

                    // Invoice validation
                    is PeppolSendIntent.ValidateInvoice -> handleValidateInvoice(intent.invoiceId)
                    is PeppolSendIntent.ResetValidationState -> handleResetValidationState()

                    // Send invoice
                    is PeppolSendIntent.SendInvoice -> handleSendInvoice(intent.invoiceId)
                    is PeppolSendIntent.ResetSendState -> handleResetSendState()

                    // Inbox polling
                    is PeppolSendIntent.PollInbox -> handlePollInbox()
                    is PeppolSendIntent.ResetPollState -> handleResetPollState()

                    // Transmission lookup
                    is PeppolSendIntent.GetTransmissionForInvoice -> handleGetTransmissionForInvoice(intent.invoiceId)
                    is PeppolSendIntent.ResetTransmissionLookupState -> handleResetTransmissionLookupState()
                }
            }
        }

    // ========================================================================
    // TRANSMISSION HISTORY
    // ========================================================================

    private suspend fun SendCtx.handleLoadTransmissions() {
        logger.d { "Loading transmission history" }
        updateState { PeppolSendState.Loading }
        loadPage(
            page = 0,
            reset = true,
            directionFilter = null,
            statusFilter = null
        )
    }

    private suspend fun SendCtx.handleRefresh() {
        withState<PeppolSendState.Content, _> {
            logger.d { "Refreshing transmission history" }
            loadPage(
                page = 0,
                reset = true,
                directionFilter = directionFilter,
                statusFilter = statusFilter
            )
        }
    }

    private suspend fun SendCtx.handleLoadNextPage() {
        withState<PeppolSendState.Content, _> {
            if (pagination.isLoadingMore || !pagination.hasMorePages) return@withState

            val nextPage = pagination.currentPage + 1
            logger.d { "Loading next transmission page (current=${pagination.currentPage})" }

            updateState {
                copy(pagination = pagination.copy(isLoadingMore = true))
            }

            loadPage(
                page = nextPage,
                reset = false,
                directionFilter = directionFilter,
                statusFilter = statusFilter,
                existingTransmissions = transmissions,
                existingPagination = pagination
            )
        }
    }

    private suspend fun SendCtx.loadPage(
        page: Int,
        reset: Boolean,
        directionFilter: PeppolTransmissionDirection?,
        statusFilter: PeppolStatus?,
        existingTransmissions: List<PeppolTransmissionDto> = emptyList(),
        existingPagination: PaginationState<PeppolTransmissionDto>? = null
    ) {
        val offset = page * PAGE_SIZE
        val result = peppolUseCase.listPeppolTransmissions(
            direction = directionFilter,
            status = statusFilter,
            limit = PAGE_SIZE,
            offset = offset
        )

        result.fold(
            onSuccess = { transmissions ->
                logger.i { "Loaded ${transmissions.size} transmissions (offset=$offset)" }
                val allTransmissions = if (reset) transmissions else existingTransmissions + transmissions

                val newPagination = PaginationState(
                    data = allTransmissions,
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = transmissions.size == PAGE_SIZE,
                    pageSize = PAGE_SIZE
                )

                updateState {
                    PeppolSendState.Content(
                        transmissions = allTransmissions,
                        pagination = newPagination,
                        directionFilter = directionFilter,
                        statusFilter = statusFilter
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load transmissions" }
                if (existingTransmissions.isEmpty()) {
                    updateState {
                        PeppolSendState.Error(
                            exception = error.asDokusException,
                            retryHandler = { intent(PeppolSendIntent.LoadTransmissions) }
                        )
                    }
                } else {
                    // Keep existing content, just stop loading
                    updateState {
                        PeppolSendState.Content(
                            transmissions = existingTransmissions,
                            pagination = (existingPagination ?: PaginationState(pageSize = PAGE_SIZE)).copy(
                                isLoadingMore = false,
                                hasMorePages = false
                            ),
                            directionFilter = directionFilter,
                            statusFilter = statusFilter
                        )
                    }
                }
            }
        )
    }

    // ========================================================================
    // FILTERS
    // ========================================================================

    private suspend fun SendCtx.handleSetDirectionFilter(direction: PeppolTransmissionDirection?) {
        withState<PeppolSendState.Content, _> {
            logger.d { "Setting direction filter: $direction" }
            updateState { PeppolSendState.Loading }
            loadPage(
                page = 0,
                reset = true,
                directionFilter = direction,
                statusFilter = statusFilter
            )
        }
    }

    private suspend fun SendCtx.handleSetStatusFilter(status: PeppolStatus?) {
        withState<PeppolSendState.Content, _> {
            logger.d { "Setting status filter: $status" }
            updateState { PeppolSendState.Loading }
            loadPage(
                page = 0,
                reset = true,
                directionFilter = directionFilter,
                statusFilter = status
            )
        }
    }

    private suspend fun SendCtx.handleClearFilters() {
        withState<PeppolSendState.Content, _> {
            logger.d { "Clearing filters" }
            updateState { PeppolSendState.Loading }
            loadPage(
                page = 0,
                reset = true,
                directionFilter = null,
                statusFilter = null
            )
        }
    }

    // ========================================================================
    // RECIPIENT VERIFICATION
    // ========================================================================

    private suspend fun SendCtx.handleVerifyRecipient(peppolId: String) {
        withState<PeppolSendState.Content, _> {
            logger.d { "Verifying Peppol recipient: $peppolId" }
            updateState {
                copy(verificationState = OperationState.Loading)
            }

            peppolUseCase.verifyPeppolRecipient(peppolId).fold(
                onSuccess = { response ->
                    logger.i { "Recipient verified: registered=${response.registered}" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                verificationState = OperationState.Success(response)
                            )
                            else -> this
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to verify recipient: $peppolId" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                verificationState = OperationState.Error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(PeppolSendIntent.VerifyRecipient(peppolId)) }
                                )
                            )
                            else -> this
                        }
                    }
                }
            )
        }
    }

    private suspend fun SendCtx.handleResetVerificationState() {
        updateState {
            when (this) {
                is PeppolSendState.Content -> copy(verificationState = OperationState.Idle)
                else -> this
            }
        }
    }

    // ========================================================================
    // INVOICE VALIDATION
    // ========================================================================

    private suspend fun SendCtx.handleValidateInvoice(invoiceId: InvoiceId) {
        withState<PeppolSendState.Content, _> {
            logger.d { "Validating invoice for Peppol: $invoiceId" }
            updateState {
                copy(validationState = OperationState.Loading)
            }

            peppolUseCase.validateInvoiceForPeppol(invoiceId).fold(
                onSuccess = { result ->
                    logger.i { "Invoice validation: valid=${result.isValid}, errors=${result.errors.size}" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                validationState = OperationState.Success(result)
                            )
                            else -> this
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to validate invoice: $invoiceId" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                validationState = OperationState.Error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(PeppolSendIntent.ValidateInvoice(invoiceId)) }
                                )
                            )
                            else -> this
                        }
                    }
                }
            )
        }
    }

    private suspend fun SendCtx.handleResetValidationState() {
        updateState {
            when (this) {
                is PeppolSendState.Content -> copy(validationState = OperationState.Idle)
                else -> this
            }
        }
    }

    // ========================================================================
    // SEND INVOICE
    // ========================================================================

    private suspend fun SendCtx.handleSendInvoice(invoiceId: InvoiceId) {
        withState<PeppolSendState.Content, _> {
            logger.d { "Sending invoice via Peppol: $invoiceId" }
            updateState {
                copy(sendState = OperationState.Loading)
            }

            peppolUseCase.sendInvoiceViaPeppol(invoiceId).fold(
                onSuccess = { response ->
                    logger.i { "Invoice sent: transmissionId=${response.transmissionId}, status=${response.status}" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                sendState = OperationState.Success(response)
                            )
                            else -> this
                        }
                    }
                    action(PeppolSendAction.InvoiceSent(response))
                    // Refresh transmission history
                    intent(PeppolSendIntent.Refresh)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to send invoice: $invoiceId" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                sendState = OperationState.Error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(PeppolSendIntent.SendInvoice(invoiceId)) }
                                )
                            )
                            else -> this
                        }
                    }
                    action(PeppolSendAction.InvoiceSendFailed(error))
                }
            )
        }
    }

    private suspend fun SendCtx.handleResetSendState() {
        updateState {
            when (this) {
                is PeppolSendState.Content -> copy(sendState = OperationState.Idle)
                else -> this
            }
        }
    }

    // ========================================================================
    // INBOX POLLING
    // ========================================================================

    private suspend fun SendCtx.handlePollInbox() {
        withState<PeppolSendState.Content, _> {
            logger.d { "Polling Peppol inbox" }
            updateState {
                copy(pollState = OperationState.Loading)
            }

            peppolUseCase.pollPeppolInbox().fold(
                onSuccess = { response ->
                    logger.i { "Inbox polled: ${response.newDocuments} new documents" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                pollState = OperationState.Success(response)
                            )
                            else -> this
                        }
                    }
                    action(PeppolSendAction.InboxPolled(response))
                    // Refresh transmission history if new documents were processed
                    if (response.newDocuments > 0) {
                        intent(PeppolSendIntent.Refresh)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to poll inbox" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                pollState = OperationState.Error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(PeppolSendIntent.PollInbox) }
                                )
                            )
                            else -> this
                        }
                    }
                    action(PeppolSendAction.InboxPollFailed(error))
                }
            )
        }
    }

    private suspend fun SendCtx.handleResetPollState() {
        updateState {
            when (this) {
                is PeppolSendState.Content -> copy(pollState = OperationState.Idle)
                else -> this
            }
        }
    }

    // ========================================================================
    // TRANSMISSION LOOKUP
    // ========================================================================

    private suspend fun SendCtx.handleGetTransmissionForInvoice(invoiceId: InvoiceId) {
        withState<PeppolSendState.Content, _> {
            logger.d { "Getting transmission for invoice: $invoiceId" }
            updateState {
                copy(transmissionLookup = OperationState.Loading)
            }

            peppolUseCase.getPeppolTransmissionForInvoice(invoiceId).fold(
                onSuccess = { transmission ->
                    logger.d { "Transmission found: ${transmission != null}" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                transmissionLookup = OperationState.Success(transmission)
                            )
                            else -> this
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to get transmission for invoice: $invoiceId" }
                    updateState {
                        when (this) {
                            is PeppolSendState.Content -> copy(
                                transmissionLookup = OperationState.Error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(PeppolSendIntent.GetTransmissionForInvoice(invoiceId)) }
                                )
                            )
                            else -> this
                        }
                    }
                }
            )
        }
    }

    private suspend fun SendCtx.handleResetTransmissionLookupState() {
        updateState {
            when (this) {
                is PeppolSendState.Content -> copy(transmissionLookup = OperationState.Idle)
                else -> this
            }
        }
    }
}
