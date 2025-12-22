package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.domain.model.PeppolConnectRequest
import ai.dokus.foundation.domain.model.PeppolConnectStatus
import ai.dokus.foundation.domain.model.PeppolProvider
import ai.dokus.foundation.domain.model.RecommandCompanySummary
import ai.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.dsl.updateState
import pro.respawn.flowmvi.plugins.reduce

internal typealias Ctx = PipelineContext<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction>

/**
 * Container for Peppol provider connection using FlowMVI.
 * Manages the connection flow from credentials entry to connected state.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class PeppolConnectContainer(
    provider: PeppolProvider,
    private val dataSource: CashflowRemoteDataSource,
) : Container<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction> {

    companion object {
        data class Params(
            val provider: PeppolProvider
        )
    }

    private val logger = Logger.forClass<PeppolConnectContainer>()

    override val store: Store<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction> =
        store(PeppolConnectState.EnteringCredentials(provider)) {
            reduce { intent ->
                when (intent) {
                    is PeppolConnectIntent.UpdateApiKey -> handleUpdateApiKey(intent.value)
                    is PeppolConnectIntent.UpdateApiSecret -> handleUpdateApiSecret(intent.value)
                    is PeppolConnectIntent.ContinueClicked -> handleContinue()
                    is PeppolConnectIntent.SelectCompany -> handleSelectCompany(intent.companyId)
                    is PeppolConnectIntent.CreateCompanyClicked -> handleCreateCompany()
                    is PeppolConnectIntent.BackClicked -> action(PeppolConnectAction.NavigateBack)
                }
            }
        }

    private suspend fun Ctx.handleUpdateApiKey(value: String) {
        updateState<PeppolConnectState.EnteringCredentials, _> {
            copy(
                apiKey = value,
                apiKeyError = null
            )
        }
    }

    private suspend fun Ctx.handleUpdateApiSecret(value: String) {
        updateState<PeppolConnectState.EnteringCredentials, _> {
            copy(
                apiSecret = value,
                apiSecretError = null
            )
        }
    }

    private suspend fun Ctx.handleContinue() {
        withState<PeppolConnectState.EnteringCredentials, _> {
            // Validate
            var hasErrors = false
            var apiKeyError: String? = null
            var apiSecretError: String? = null

            if (apiKey.isBlank()) {
                apiKeyError = "API Key is required"
                hasErrors = true
            }
            if (apiSecret.isBlank()) {
                apiSecretError = "API Secret is required"
                hasErrors = true
            }

            if (hasErrors) {
                updateState { copy(apiKeyError = apiKeyError, apiSecretError = apiSecretError) }
                return@withState
            }

            val currentApiKey = apiKey
            val currentApiSecret = apiSecret

            // Transition to loading state
            updateState {
                PeppolConnectState.LoadingCompanies(
                    provider = provider,
                    apiKey = currentApiKey,
                    apiSecret = currentApiSecret
                )
            }

            // Fetch companies
            logger.d { "Connecting to Peppol with credentials" }
            val request = PeppolConnectRequest(
                apiKey = currentApiKey,
                apiSecret = currentApiSecret,
                isEnabled = true,
                testMode = false,
                createCompanyIfMissing = false
            )

            dataSource.connectPeppol(request).fold(
                onSuccess = { response ->
                    handleConnectResponse(
                        status = response.status,
                        companies = response.candidates,
                        apiKey = currentApiKey,
                        apiSecret = currentApiSecret
                    )
                },
                onFailure = { error ->
                    logger.e(error) { "Peppol connection failed" }
                    updateState {
                        PeppolConnectState.Error(
                            provider = provider,
                            apiKey = currentApiKey,
                            apiSecret = currentApiSecret,
                            exception = error.asDokusException,
                            retryHandler = { intent(PeppolConnectIntent.CreateCompanyClicked) }
                        )
                    }
                }
            )
        }
    }

    private suspend fun Ctx.handleConnectResponse(
        status: PeppolConnectStatus,
        companies: List<RecommandCompanySummary>,
        apiKey: String,
        apiSecret: String
    ) {
        when (status) {
            PeppolConnectStatus.Connected -> {
                logger.i { "Connected to Peppol" }
                action(PeppolConnectAction.NavigateToSettings)
            }

            PeppolConnectStatus.MultipleMatches -> {
                logger.i { "Multiple companies found: ${companies.size}" }
                updateState {
                    PeppolConnectState.SelectingCompany(
                        provider = provider,
                        apiKey = apiKey,
                        apiSecret = apiSecret,
                        companies = companies
                    )
                }
            }

            PeppolConnectStatus.NoCompanyFound -> {
                logger.i { "No matching company found" }
                updateState {
                    PeppolConnectState.NoCompaniesFound(
                        provider = provider,
                        apiKey = apiKey,
                        apiSecret = apiSecret
                    )
                }
            }

            PeppolConnectStatus.MissingVatNumber -> {
                updateState {
                    PeppolConnectState.Error(
                        provider = provider,
                        apiKey = apiKey,
                        apiSecret = apiSecret,
                        message = "Please configure your company VAT number in workspace settings first"
                    )
                }
            }

            PeppolConnectStatus.MissingCompanyAddress -> {
                updateState {
                    PeppolConnectState.Error(
                        provider = provider,
                        apiKey = apiKey,
                        apiSecret = apiSecret,
                        message = "Please configure your company address in workspace settings first"
                    )
                }
            }

            PeppolConnectStatus.InvalidCredentials -> {
                updateState {
                    PeppolConnectState.Error(
                        provider = provider,
                        apiKey = apiKey,
                        apiSecret = apiSecret,
                        message = "Invalid API credentials. Please check your API Key and Secret."
                    )
                }
            }
        }
    }

    private suspend fun Ctx.handleSelectCompany(
        companyId: String
    ) {
        withState<PeppolConnectState.SelectingCompany, _> {
            val currentApiKey = apiKey
            val currentApiSecret = apiSecret

            updateState {
                PeppolConnectState.Connecting(
                    provider = provider,
                    apiKey = currentApiKey,
                    apiSecret = currentApiSecret,
                    selectedCompanyId = companyId
                )
            }

            logger.d { "Selecting company: $companyId" }
            val request = PeppolConnectRequest(
                apiKey = currentApiKey,
                apiSecret = currentApiSecret,
                isEnabled = true,
                testMode = false,
                companyId = companyId,
                createCompanyIfMissing = false
            )

            dataSource.connectPeppol(request).fold(
                onSuccess = { response ->
                    if (response.status == PeppolConnectStatus.Connected) {
                        logger.i { "Connected to company: $companyId" }
                        action(PeppolConnectAction.NavigateToSettings)
                    } else {
                        handleConnectResponse(
                            status = response.status,
                            companies = response.candidates,
                            apiKey = currentApiKey,
                            apiSecret = currentApiSecret
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to select company" }
                    updateState {
                        PeppolConnectState.Error(
                            provider = provider,
                            apiKey = currentApiKey,
                            apiSecret = currentApiSecret,
                            exception = error.asDokusException,
                            retryHandler = { intent(PeppolConnectIntent.SelectCompany(companyId)) }
                        )
                    }
                }
            )
        }
    }

    private suspend fun Ctx.handleCreateCompany() {
        withState<PeppolConnectState.NoCompaniesFound, _> {
            updateState {
                PeppolConnectState.CreatingCompany(
                    provider = provider,
                    apiKey = apiKey,
                    apiSecret = apiSecret
                )
            }

            logger.d { "Creating company on Recommand" }
            val request = PeppolConnectRequest(
                apiKey = apiKey,
                apiSecret = apiSecret,
                isEnabled = true,
                testMode = false,
                createCompanyIfMissing = true
            )

            dataSource.connectPeppol(request).fold(
                onSuccess = { response ->
                    if (response.status == PeppolConnectStatus.Connected) {
                        logger.i { "Company created and connected" }
                        action(PeppolConnectAction.NavigateToSettings)
                    } else {
                        handleConnectResponse(
                            status = response.status,
                            companies = response.candidates,
                            apiKey = apiKey,
                            apiSecret = apiSecret
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create company" }
                    updateState {
                        PeppolConnectState.Error(
                            provider = provider,
                            apiKey = apiKey,
                            apiSecret = apiSecret,
                            message = error.message ?: "Failed to create company"
                        )
                    }
                }
            )
        }
    }
}
