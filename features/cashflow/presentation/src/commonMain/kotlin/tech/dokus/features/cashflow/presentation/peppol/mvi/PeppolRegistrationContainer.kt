package tech.dokus.features.cashflow.presentation.peppol.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.cashflow.usecases.EnablePeppolSendingOnlyUseCase
import tech.dokus.features.cashflow.usecases.EnablePeppolUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolRegistrationUseCase
import tech.dokus.features.cashflow.usecases.OptOutPeppolUseCase
import tech.dokus.features.cashflow.usecases.PollPeppolTransferUseCase
import tech.dokus.features.cashflow.usecases.VerifyPeppolIdUseCase
import tech.dokus.features.cashflow.usecases.WaitForPeppolTransferUseCase
import tech.dokus.foundation.platform.Logger

internal typealias PeppolRegistrationCtx = PipelineContext<PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction>

/**
 * Container for the Peppol registration/settings screen.
 *
 * Note: VAT is never asked from the user; it's taken from the current workspace.
 */
internal class PeppolRegistrationContainer(
    private val getCurrentTenant: GetCurrentTenantUseCase,
    private val getRegistration: GetPeppolRegistrationUseCase,
    private val verifyPeppolId: VerifyPeppolIdUseCase,
    private val enablePeppol: EnablePeppolUseCase,
    private val enableSendingOnly: EnablePeppolSendingOnlyUseCase,
    private val waitForTransfer: WaitForPeppolTransferUseCase,
    private val optOut: OptOutPeppolUseCase,
    private val pollTransfer: PollPeppolTransferUseCase
) : Container<PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction> {

    private val logger = Logger.forClass<PeppolRegistrationContainer>()

    override val store: Store<PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction> =
        store(PeppolRegistrationState.Loading) {
            reduce { intent ->
                when (intent) {
                    PeppolRegistrationIntent.Refresh -> handleRefresh()
                    PeppolRegistrationIntent.EnablePeppol -> handleEnablePeppol()
                    PeppolRegistrationIntent.EnableSendingOnly -> handleEnableSendingOnly()
                    PeppolRegistrationIntent.WaitForTransfer -> handleWaitForTransfer()
                    PeppolRegistrationIntent.PollTransfer -> handlePollTransfer()
                    PeppolRegistrationIntent.NotNow -> handleNotNow()
                    PeppolRegistrationIntent.Continue -> action(PeppolRegistrationAction.NavigateToHome)
                    PeppolRegistrationIntent.Retry -> handleRetry()
                }
            }
        }

    private suspend fun PeppolRegistrationCtx.handleRefresh() {
        updateState { PeppolRegistrationState.Loading }

        val tenant = getCurrentTenant().getOrElse { error ->
            logger.e(error) { "Failed to load tenant context" }
            updateState {
                PeppolRegistrationState.Error(
                    exception = error.asDokusException,
                    retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                )
            }
            return
        } ?: run {
            updateState {
                PeppolRegistrationState.Error(
                    exception = DokusException.BadRequest("No workspace selected"),
                    retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                )
            }
            return
        }

        val vatNumber = tenant.vatNumber ?: run {
            updateState {
                PeppolRegistrationState.Error(
                    exception = DokusException.Validation.InvalidVatNumber,
                    retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                )
            }
            return
        }

        val computedPeppolId = "0208:${vatNumber.normalized}"

        val registration = getRegistration().getOrElse { error ->
            logger.e(error) { "Failed to load PEPPOL registration" }
            updateState {
                PeppolRegistrationState.Error(
                    exception = error.asDokusException,
                    retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                )
            }
            return
        }

        val context = PeppolSetupContext(
            companyName = tenant.legalName.value,
            peppolId = registration?.peppolId ?: computedPeppolId,
        )

        when (registration?.status) {
            PeppolRegistrationStatus.Active -> updateState { PeppolRegistrationState.Active(context) }
            PeppolRegistrationStatus.Pending -> updateState {
                PeppolRegistrationState.Activating(
                    context
                )
            }

            PeppolRegistrationStatus.WaitingTransfer -> updateState {
                PeppolRegistrationState.WaitingTransfer(
                    context
                )
            }

            PeppolRegistrationStatus.SendingOnly -> updateState {
                PeppolRegistrationState.SendingOnly(
                    context
                )
            }

            PeppolRegistrationStatus.External -> updateState {
                PeppolRegistrationState.External(
                    context
                )
            }

            PeppolRegistrationStatus.Failed -> updateState {
                PeppolRegistrationState.Failed(
                    context = context,
                    message = registration.errorMessage
                )
            }

            PeppolRegistrationStatus.NotConfigured, null -> {
                // Determine whether we should show Fresh or Blocked without asking the user for VAT input.
                verifyPeppolId(vatNumber).fold(
                    onSuccess = { verification ->
                        updateState {
                            if (verification.isBlocked) {
                                PeppolRegistrationState.Blocked(context)
                            } else {
                                PeppolRegistrationState.Fresh(context)
                            }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to verify PEPPOL ID" }
                        updateState {
                            PeppolRegistrationState.Error(
                                exception = error.asDokusException,
                                retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                            )
                        }
                    }
                )
            }
        }
    }

    private suspend fun PeppolRegistrationCtx.handleEnablePeppol() {
        val context = currentContextOrNull() ?: run {
            handleRefresh()
            return
        }

        updateState { PeppolRegistrationState.Activating(context) }

        enablePeppol().fold(
            onSuccess = { response ->
                val newContext = context.copy(peppolId = response.registration.peppolId)
                when {
                    response.nextAction == tech.dokus.domain.model.PeppolNextAction.WAIT_FOR_TRANSFER ->
                        updateState { PeppolRegistrationState.Blocked(newContext) }

                    response.registration.status == PeppolRegistrationStatus.Active ->
                        updateState { PeppolRegistrationState.Active(newContext) }

                    response.registration.status == PeppolRegistrationStatus.Pending ->
                        updateState { PeppolRegistrationState.Activating(newContext) }

                    response.registration.status == PeppolRegistrationStatus.WaitingTransfer ->
                        updateState { PeppolRegistrationState.WaitingTransfer(newContext) }

                    response.registration.status == PeppolRegistrationStatus.SendingOnly ->
                        updateState { PeppolRegistrationState.SendingOnly(newContext) }

                    response.registration.status == PeppolRegistrationStatus.External ->
                        updateState { PeppolRegistrationState.External(newContext) }

                    response.registration.status == PeppolRegistrationStatus.Failed ->
                        updateState {
                            PeppolRegistrationState.Failed(
                                newContext,
                                response.registration.errorMessage
                            )
                        }

                    else ->
                        updateState { PeppolRegistrationState.Fresh(newContext) }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to enable PEPPOL" }
                updateState {
                    PeppolRegistrationState.Failed(
                        context = context,
                        message = error.message
                    )
                }
            }
        )
    }

    private suspend fun PeppolRegistrationCtx.handleEnableSendingOnly() {
        withState<PeppolRegistrationState.Blocked, _> {
            updateState { copy(isWorking = true) }

            enableSendingOnly().fold(
                onSuccess = { response ->
                    val newContext = context.copy(peppolId = response.registration.peppolId)
                    updateState { PeppolRegistrationState.SendingOnly(newContext) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to enable sending-only" }
                    updateState { copy(isWorking = false) }
                    action(PeppolRegistrationAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PeppolRegistrationCtx.handleWaitForTransfer() {
        withState<PeppolRegistrationState.Blocked, _> {
            updateState { copy(isWorking = true) }

            waitForTransfer().fold(
                onSuccess = { response ->
                    val newContext = context.copy(peppolId = response.registration.peppolId)
                    updateState { PeppolRegistrationState.WaitingTransfer(newContext) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to start waiting for transfer" }
                    updateState { copy(isWorking = false) }
                    action(PeppolRegistrationAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PeppolRegistrationCtx.handlePollTransfer() {
        withState<PeppolRegistrationState.WaitingTransfer, _> {
            pollTransfer().fold(
                onSuccess = { response ->
                    val newContext = context.copy(peppolId = response.registration.peppolId)
                    when (response.registration.status) {
                        PeppolRegistrationStatus.Active -> updateState {
                            PeppolRegistrationState.Active(
                                newContext
                            )
                        }

                        PeppolRegistrationStatus.Failed -> updateState {
                            PeppolRegistrationState.Failed(
                                newContext,
                                response.registration.errorMessage
                            )
                        }

                        PeppolRegistrationStatus.SendingOnly -> updateState {
                            PeppolRegistrationState.SendingOnly(
                                newContext
                            )
                        }

                        PeppolRegistrationStatus.External -> updateState {
                            PeppolRegistrationState.External(
                                newContext
                            )
                        }

                        PeppolRegistrationStatus.Pending -> updateState {
                            PeppolRegistrationState.Activating(
                                newContext
                            )
                        }

                        PeppolRegistrationStatus.WaitingTransfer -> updateState {
                            PeppolRegistrationState.WaitingTransfer(
                                newContext
                            )
                        }

                        PeppolRegistrationStatus.NotConfigured -> updateState {
                            PeppolRegistrationState.Blocked(
                                newContext
                            )
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to poll transfer status" }
                    action(PeppolRegistrationAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun PeppolRegistrationCtx.handleNotNow() {
        optOut().fold(
            onSuccess = {
                action(PeppolRegistrationAction.NavigateToHome)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to opt out" }
                action(PeppolRegistrationAction.ShowError(error.asDokusException))
            }
        )
    }

    private suspend fun PeppolRegistrationCtx.handleRetry() {
        val context = currentContextOrNull() ?: run {
            handleRefresh()
            return
        }
        updateState {
            PeppolRegistrationState.Failed(
                context = context,
                message = null,
                isRetrying = true
            )
        }
        handleEnablePeppol()
    }

    private suspend fun PeppolRegistrationCtx.currentContextOrNull(): PeppolSetupContext? {
        var ctx: PeppolSetupContext? = null

        withState<PeppolRegistrationState.Fresh, _> { ctx = context }
        withState<PeppolRegistrationState.Activating, _> { ctx = context }
        withState<PeppolRegistrationState.Active, _> { ctx = context }
        withState<PeppolRegistrationState.Blocked, _> { ctx = context }
        withState<PeppolRegistrationState.WaitingTransfer, _> { ctx = context }
        withState<PeppolRegistrationState.SendingOnly, _> { ctx = context }
        withState<PeppolRegistrationState.External, _> { ctx = context }
        withState<PeppolRegistrationState.Failed, _> { ctx = context }

        return ctx
    }
}
