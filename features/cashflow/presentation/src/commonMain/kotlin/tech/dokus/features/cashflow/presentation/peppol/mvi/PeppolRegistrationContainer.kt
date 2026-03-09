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
import tech.dokus.features.cashflow.usecases.PollPeppolTransferUseCase
import tech.dokus.features.cashflow.usecases.VerifyPeppolIdUseCase
import tech.dokus.features.cashflow.usecases.WaitForPeppolTransferUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
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
    private val pollTransfer: PollPeppolTransferUseCase
) : Container<PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction> {

    private val logger = Logger.forClass<PeppolRegistrationContainer>()

    override val store: Store<PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction> =
        store(PeppolRegistrationState()) {
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
        updateState { copy(setupContext = setupContext.asLoading) }

        val tenant = getCurrentTenant().getOrElse { error ->
            logger.e(error) { "Failed to load tenant context" }
            updateState {
                copy(
                    setupContext = DokusState.error(
                        exception = error.asDokusException,
                        retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                    )
                )
            }
            return
        } ?: run {
            updateState {
                copy(
                    setupContext = DokusState.error(
                        exception = DokusException.BadRequest("No workspace selected"),
                        retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                    )
                )
            }
            return
        }

        val vatNumber = tenant.vatNumber

        val computedPeppolId = "0208:${vatNumber.normalized}"

        val registration = getRegistration().getOrElse { error ->
            logger.e(error) { "Failed to load PEPPOL registration" }
            updateState {
                copy(
                    setupContext = DokusState.error(
                        exception = error.asDokusException,
                        retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                    )
                )
            }
            return
        }

        val context = PeppolSetupContext(
            companyName = tenant.legalName.value,
            peppolId = registration?.peppolId ?: computedPeppolId,
        )

        when (registration?.status) {
            PeppolRegistrationStatus.Active -> updateState {
                copy(setupContext = DokusState.success(context), phase = PeppolRegistrationPhase.Active)
            }

            PeppolRegistrationStatus.Pending -> updateState {
                copy(setupContext = DokusState.success(context), phase = PeppolRegistrationPhase.Activating)
            }

            PeppolRegistrationStatus.WaitingTransfer -> updateState {
                copy(setupContext = DokusState.success(context), phase = PeppolRegistrationPhase.WaitingTransfer)
            }

            PeppolRegistrationStatus.SendingOnly -> updateState {
                copy(setupContext = DokusState.success(context), phase = PeppolRegistrationPhase.SendingOnly)
            }

            PeppolRegistrationStatus.External -> updateState {
                copy(setupContext = DokusState.success(context), phase = PeppolRegistrationPhase.External)
            }

            PeppolRegistrationStatus.Failed -> updateState {
                copy(
                    setupContext = DokusState.success(context),
                    phase = PeppolRegistrationPhase.Failed,
                    failureMessage = registration.errorMessage
                )
            }

            PeppolRegistrationStatus.NotConfigured, null -> {
                verifyPeppolId(vatNumber).fold(
                    onSuccess = { verification ->
                        updateState {
                            copy(
                                setupContext = DokusState.success(context),
                                phase = if (verification.isBlocked) PeppolRegistrationPhase.Blocked
                                else PeppolRegistrationPhase.Fresh
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to verify PEPPOL ID" }
                        updateState {
                            copy(
                                setupContext = DokusState.error(
                                    exception = error.asDokusException,
                                    retryHandler = { intent(PeppolRegistrationIntent.Refresh) }
                                )
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

        updateState {
            copy(setupContext = DokusState.success(context), phase = PeppolRegistrationPhase.Activating)
        }

        enablePeppol().fold(
            onSuccess = { response ->
                val newContext = context.copy(peppolId = response.registration.peppolId)
                when {
                    response.nextAction == tech.dokus.domain.model.PeppolNextAction.WAIT_FOR_TRANSFER ->
                        updateState {
                            copy(setupContext = DokusState.success(newContext), phase = PeppolRegistrationPhase.Blocked)
                        }

                    response.registration.status == PeppolRegistrationStatus.Active ->
                        updateState {
                            copy(setupContext = DokusState.success(newContext), phase = PeppolRegistrationPhase.Active)
                        }

                    response.registration.status == PeppolRegistrationStatus.Pending ->
                        updateState {
                            copy(setupContext = DokusState.success(newContext), phase = PeppolRegistrationPhase.Activating)
                        }

                    response.registration.status == PeppolRegistrationStatus.WaitingTransfer ->
                        updateState {
                            copy(setupContext = DokusState.success(newContext), phase = PeppolRegistrationPhase.WaitingTransfer)
                        }

                    response.registration.status == PeppolRegistrationStatus.SendingOnly ->
                        updateState {
                            copy(setupContext = DokusState.success(newContext), phase = PeppolRegistrationPhase.SendingOnly)
                        }

                    response.registration.status == PeppolRegistrationStatus.External ->
                        updateState {
                            copy(setupContext = DokusState.success(newContext), phase = PeppolRegistrationPhase.External)
                        }

                    response.registration.status == PeppolRegistrationStatus.Failed ->
                        updateState {
                            copy(
                                setupContext = DokusState.success(newContext),
                                phase = PeppolRegistrationPhase.Failed,
                                failureMessage = response.registration.errorMessage
                            )
                        }

                    else ->
                        updateState {
                            copy(setupContext = DokusState.success(newContext), phase = PeppolRegistrationPhase.Fresh)
                        }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to enable PEPPOL" }
                updateState {
                    copy(
                        setupContext = DokusState.success(context),
                        phase = PeppolRegistrationPhase.Failed,
                        failureMessage = error.message
                    )
                }
            }
        )
    }

    private suspend fun PeppolRegistrationCtx.handleEnableSendingOnly() {
        withState {
            if (phase != PeppolRegistrationPhase.Blocked) return@withState
            updateState { copy(isWorking = true) }

            enableSendingOnly().fold(
                onSuccess = { response ->
                    val context = (setupContext as? DokusState.Success)?.data ?: return@fold
                    val newContext = context.copy(peppolId = response.registration.peppolId)
                    updateState {
                        copy(
                            setupContext = DokusState.success(newContext),
                            phase = PeppolRegistrationPhase.SendingOnly,
                            isWorking = false
                        )
                    }
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
        withState {
            if (phase != PeppolRegistrationPhase.Blocked) return@withState
            updateState { copy(isWorking = true) }

            waitForTransfer().fold(
                onSuccess = { response ->
                    val context = (setupContext as? DokusState.Success)?.data ?: return@fold
                    val newContext = context.copy(peppolId = response.registration.peppolId)
                    updateState {
                        copy(
                            setupContext = DokusState.success(newContext),
                            phase = PeppolRegistrationPhase.WaitingTransfer,
                            isWorking = false
                        )
                    }
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
        withState {
            if (phase != PeppolRegistrationPhase.WaitingTransfer) return@withState

            pollTransfer().fold(
                onSuccess = { response ->
                    val context = (setupContext as? DokusState.Success)?.data ?: return@fold
                    val newContext = context.copy(peppolId = response.registration.peppolId)
                    val newPhase = when (response.registration.status) {
                        PeppolRegistrationStatus.Active -> PeppolRegistrationPhase.Active
                        PeppolRegistrationStatus.Failed -> PeppolRegistrationPhase.Failed
                        PeppolRegistrationStatus.SendingOnly -> PeppolRegistrationPhase.SendingOnly
                        PeppolRegistrationStatus.External -> PeppolRegistrationPhase.External
                        PeppolRegistrationStatus.Pending -> PeppolRegistrationPhase.Activating
                        PeppolRegistrationStatus.WaitingTransfer -> PeppolRegistrationPhase.WaitingTransfer
                        PeppolRegistrationStatus.NotConfigured -> PeppolRegistrationPhase.Blocked
                    }
                    updateState {
                        copy(
                            setupContext = DokusState.success(newContext),
                            phase = newPhase,
                            failureMessage = if (newPhase == PeppolRegistrationPhase.Failed) response.registration.errorMessage else null
                        )
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
        action(PeppolRegistrationAction.NavigateToHome)
    }

    private suspend fun PeppolRegistrationCtx.handleRetry() {
        val context = currentContextOrNull() ?: run {
            handleRefresh()
            return
        }
        updateState {
            copy(
                setupContext = DokusState.success(context),
                phase = PeppolRegistrationPhase.Failed,
                failureMessage = null,
                isRetrying = true
            )
        }
        handleEnablePeppol()
    }

    private suspend fun PeppolRegistrationCtx.currentContextOrNull(): PeppolSetupContext? {
        var ctx: PeppolSetupContext? = null
        withState {
            if (setupContext.isSuccess()) {
                ctx = setupContext.data
            }
        }
        return ctx
    }
}
