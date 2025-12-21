package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.foundation.domain.model.PeppolConnectRequest
import ai.dokus.foundation.domain.model.PeppolConnectStatus
import ai.dokus.foundation.domain.model.PeppolProvider
import ai.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce

/**
 * FlowMVI Container for Peppol provider connection.
 * Manages the connection flow from credentials entry to connected state.
 */
class PeppolConnectContainer(
    private val provider: PeppolProvider,
    private val dataSource: CashflowRemoteDataSource,
) : Container<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction> {

    private val logger = Logger.forClass<PeppolConnectContainer>()

    override val store = store(
        initial = PeppolConnectState.EnteringCredentials(provider)
    ) {
        configure {
            name = "PeppolConnectStore"
        }

        recover { e ->
            logger.e(e) { "Store error" }
            updateState {
                PeppolConnectState.Error(
                    provider = provider,
                    apiKey = apiKey,
                    apiSecret = apiSecret,
                    message = e.message ?: "Unknown error"
                )
            }
            null
        }

        reduce { intent ->
            when (intent) {
                is PeppolConnectIntent.UpdateApiKey -> handleUpdateApiKey(intent.value)
                is PeppolConnectIntent.UpdateApiSecret -> handleUpdateApiSecret(intent.value)
                is PeppolConnectIntent.ContinueClicked -> handleContinue()
                is PeppolConnectIntent.SelectCompany -> handleSelectCompany(intent.companyId)
                is PeppolConnectIntent.CreateCompanyClicked -> handleCreateCompany()
                is PeppolConnectIntent.RetryClicked -> handleRetry()
                is PeppolConnectIntent.BackClicked -> action(PeppolConnectAction.NavigateBack)
            }
        }
    }

    private suspend fun PipelineContext<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction>.handleUpdateApiKey(
        value: String
    ) {
        val current = state.value as? PeppolConnectState.EnteringCredentials ?: return
        updateState {
            current.copy(apiKey = value, apiKeyError = null)
        }
    }

    private suspend fun PipelineContext<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction>.handleUpdateApiSecret(
        value: String
    ) {
        val current = state.value as? PeppolConnectState.EnteringCredentials ?: return
        updateState {
            current.copy(apiSecret = value, apiSecretError = null)
        }
    }

    private suspend fun PipelineContext<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction>.handleContinue() {
        val current = state.value
        if (current !is PeppolConnectState.EnteringCredentials) return

        // Validate
        var hasErrors = false
        var apiKeyError: String? = null
        var apiSecretError: String? = null

        if (current.apiKey.isBlank()) {
            apiKeyError = "API Key is required"
            hasErrors = true
        }
        if (current.apiSecret.isBlank()) {
            apiSecretError = "API Secret is required"
            hasErrors = true
        }

        if (hasErrors) {
            updateState {
                current.copy(apiKeyError = apiKeyError, apiSecretError = apiSecretError)
            }
            return
        }

        // Transition to loading state
        updateState {
            PeppolConnectState.LoadingCompanies(
                provider = provider,
                apiKey = current.apiKey,
                apiSecret = current.apiSecret
            )
        }

        // Fetch companies
        logger.d { "Connecting to Peppol with credentials" }
        val request = PeppolConnectRequest(
            apiKey = current.apiKey,
            apiSecret = current.apiSecret,
            isEnabled = true,
            testMode = false,
            createCompanyIfMissing = false
        )

        dataSource.connectPeppol(request).fold(
            onSuccess = { response ->
                handleConnectResponse(
                    status = response.status,
                    companies = response.candidates,
                    apiKey = current.apiKey,
                    apiSecret = current.apiSecret
                )
            },
            onFailure = { error ->
                logger.e(error) { "Peppol connection failed" }
                updateState {
                    PeppolConnectState.Error(
                        provider = provider,
                        apiKey = current.apiKey,
                        apiSecret = current.apiSecret,
                        message = error.message ?: "Connection failed"
                    )
                }
            }
        )
    }

    private suspend fun PipelineContext<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction>.handleConnectResponse(
        status: PeppolConnectStatus,
        companies: List<ai.dokus.foundation.domain.model.RecommandCompanySummary>,
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

    private suspend fun PipelineContext<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction>.handleSelectCompany(
        companyId: String
    ) {
        val current = state.value
        if (current !is PeppolConnectState.SelectingCompany) return

        updateState {
            PeppolConnectState.Connecting(
                provider = provider,
                apiKey = current.apiKey,
                apiSecret = current.apiSecret,
                selectedCompanyId = companyId
            )
        }

        logger.d { "Selecting company: $companyId" }
        val request = PeppolConnectRequest(
            apiKey = current.apiKey,
            apiSecret = current.apiSecret,
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
                        apiKey = current.apiKey,
                        apiSecret = current.apiSecret
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to select company" }
                updateState {
                    PeppolConnectState.Error(
                        provider = provider,
                        apiKey = current.apiKey,
                        apiSecret = current.apiSecret,
                        message = error.message ?: "Failed to connect to company"
                    )
                }
            }
        )
    }

    private suspend fun PipelineContext<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction>.handleCreateCompany() {
        val current = state.value
        if (current !is PeppolConnectState.NoCompaniesFound) return

        updateState {
            PeppolConnectState.CreatingCompany(
                provider = provider,
                apiKey = current.apiKey,
                apiSecret = current.apiSecret
            )
        }

        logger.d { "Creating company on Recommand" }
        val request = PeppolConnectRequest(
            apiKey = current.apiKey,
            apiSecret = current.apiSecret,
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
                        apiKey = current.apiKey,
                        apiSecret = current.apiSecret
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to create company" }
                updateState {
                    PeppolConnectState.Error(
                        provider = provider,
                        apiKey = current.apiKey,
                        apiSecret = current.apiSecret,
                        message = error.message ?: "Failed to create company"
                    )
                }
            }
        )
    }

    private suspend fun PipelineContext<PeppolConnectState, PeppolConnectIntent, PeppolConnectAction>.handleRetry() {
        val current = state.value
        updateState {
            PeppolConnectState.EnteringCredentials(
                provider = provider,
                apiKey = current.apiKey,
                apiSecret = current.apiSecret
            )
        }
    }
}
