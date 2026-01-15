@file:Suppress("TooManyFunctions")

package tech.dokus.features.cashflow.presentation.peppol.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.VatNumber
import tech.dokus.features.cashflow.usecases.EnablePeppolUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolRegistrationUseCase
import tech.dokus.features.cashflow.usecases.OptOutPeppolUseCase
import tech.dokus.features.cashflow.usecases.PollPeppolTransferUseCase
import tech.dokus.features.cashflow.usecases.VerifyPeppolIdUseCase
import tech.dokus.features.cashflow.usecases.WaitForPeppolTransferUseCase
import tech.dokus.foundation.platform.Logger

internal typealias PeppolRegistrationCtx =
    PipelineContext<PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction>

/**
 * Container for the PEPPOL Registration screen.
 *
 * Manages the registration state machine and API interactions.
 */
internal class PeppolRegistrationContainer(
    private val getRegistration: GetPeppolRegistrationUseCase,
    private val verifyPeppolId: VerifyPeppolIdUseCase,
    private val enablePeppol: EnablePeppolUseCase,
    private val waitForTransfer: WaitForPeppolTransferUseCase,
    private val optOut: OptOutPeppolUseCase,
    private val pollTransfer: PollPeppolTransferUseCase
) : Container<PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction> {

    private val logger = Logger.forClass<PeppolRegistrationContainer>()

    override val store: Store<PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction> =
        store(PeppolRegistrationState.Loading) {
            reduce { intent ->
                when (intent) {
                    is PeppolRegistrationIntent.Refresh -> handleRefresh()
                    is PeppolRegistrationIntent.UpdateEnterpriseNumber -> handleUpdateEnterpriseNumber(intent.value)
                    is PeppolRegistrationIntent.VerifyPeppolId -> handleVerifyPeppolId()
                    is PeppolRegistrationIntent.EnablePeppol -> handleEnablePeppol()
                    is PeppolRegistrationIntent.WaitForTransfer -> handleWaitForTransfer()
                    is PeppolRegistrationIntent.OptOut -> handleOptOut()
                    is PeppolRegistrationIntent.PollTransfer -> handlePollTransfer()
                    is PeppolRegistrationIntent.BackToWelcome -> handleBackToWelcome()
                }
            }
        }

    private suspend fun PeppolRegistrationCtx.handleRefresh() {
        logger.d { "Refreshing PEPPOL registration status" }
        updateState { PeppolRegistrationState.Loading }

        getRegistration().fold(
            onSuccess = { registration ->
                if (registration == null) {
                    logger.d { "No registration found, showing welcome" }
                    updateState { PeppolRegistrationState.Welcome() }
                } else {
                    logger.d { "Registration found: ${registration.status}" }
                    updateState { registration.toUiState() }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load registration" }
                updateState {
                    PeppolRegistrationState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                    )
                }
            }
        )
    }

    private suspend fun PeppolRegistrationCtx.handleUpdateEnterpriseNumber(value: String) {
        withState<PeppolRegistrationState.Welcome, _> {
            updateState { copy(enterpriseNumber = value, verificationError = null) }
        }
    }

    private suspend fun PeppolRegistrationCtx.handleVerifyPeppolId() {
        withState<PeppolRegistrationState.Welcome, _> {
            val trimmed = enterpriseNumber.trim().replace(" ", "").replace(".", "")

            // Validate enterprise number format (Belgian enterprise number is 10 digits)
            if (!trimmed.all { it.isDigit() } || trimmed.length < 9 || trimmed.length > 10) {
                updateState { copy(verificationError = "Invalid enterprise number format (9-10 digits expected)") }
                return@withState
            }

            logger.d { "Verifying PEPPOL ID for enterprise: $trimmed" }
            updateState { copy(isVerifying = true, verificationError = null) }

            // Pass VatNumber - backend handles PEPPOL ID construction
            val vatNumber = VatNumber("BE$trimmed")

            verifyPeppolId(vatNumber).fold(
                onSuccess = { result ->
                    logger.d { "Verification result: blocked=${result.isBlocked}" }
                    updateState {
                        PeppolRegistrationState.VerificationResult(
                            result = result,
                            enterpriseNumber = trimmed
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to verify PEPPOL ID" }
                    updateState {
                        copy(
                            isVerifying = false,
                            verificationError = error.message ?: "Verification failed"
                        )
                    }
                }
            )
        }
    }

    private suspend fun PeppolRegistrationCtx.handleEnablePeppol() {
        withState<PeppolRegistrationState.VerificationResult, _> {
            if (!result.canProceed) {
                logger.w { "Cannot enable PEPPOL - ID is blocked" }
                return@withState
            }

            logger.d { "Enabling PEPPOL for enterprise: $enterpriseNumber" }
            updateState { copy(isEnabling = true) }

            val vatNumber = VatNumber("BE$enterpriseNumber")
            enablePeppol(vatNumber).fold(
                onSuccess = { response ->
                    logger.d { "PEPPOL enabled: ${response.registration.status}" }
                    updateState { response.registration.toUiState() }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to enable PEPPOL" }
                    updateState { copy(isEnabling = false) }
                    action(PeppolRegistrationAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PeppolRegistrationCtx.handleWaitForTransfer() {
        withState<PeppolRegistrationState.VerificationResult, _> {
            logger.d { "Opting to wait for transfer" }
            updateState { copy(isEnabling = true) }

            waitForTransfer().fold(
                onSuccess = { response ->
                    logger.d { "Wait for transfer set: ${response.registration.status}" }
                    updateState { response.registration.toUiState() }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to set wait for transfer" }
                    updateState { copy(isEnabling = false) }
                    action(PeppolRegistrationAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PeppolRegistrationCtx.handleOptOut() {
        withState<PeppolRegistrationState.VerificationResult, _> {
            logger.d { "Opting out of PEPPOL" }
            updateState { copy(isEnabling = true) }

            optOut().fold(
                onSuccess = {
                    logger.d { "Opted out of PEPPOL" }
                    // Refresh to get updated state
                    handleRefresh()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to opt out" }
                    updateState { copy(isEnabling = false) }
                    action(PeppolRegistrationAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PeppolRegistrationCtx.handlePollTransfer() {
        withState<PeppolRegistrationState.WaitingTransfer, _> {
            logger.d { "Polling transfer status" }
            updateState { copy(isPolling = true) }

            pollTransfer().fold(
                onSuccess = { response ->
                    logger.d { "Poll result: ${response.registration.status}" }
                    updateState { response.registration.toUiState() }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to poll transfer" }
                    updateState { copy(isPolling = false) }
                    action(PeppolRegistrationAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PeppolRegistrationCtx.handleBackToWelcome() {
        updateState { PeppolRegistrationState.Welcome() }
    }
}
