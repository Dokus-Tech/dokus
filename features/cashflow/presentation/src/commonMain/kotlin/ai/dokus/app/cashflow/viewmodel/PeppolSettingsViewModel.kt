package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.PeppolConnectRequest
import ai.dokus.foundation.domain.model.PeppolConnectStatus
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.RecommandCompanySummary
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel

/**
 * ViewModel for Peppol settings management.
 * Handles configuration, connection testing, and provider management.
 *
 * The connection flow:
 * 1. User enters API Key and API Secret
 * 2. User clicks Connect
 * 3. Backend validates credentials and searches for matching companies
 * 4. Possible outcomes:
 *    - Single match → Connected
 *    - Multiple matches → User selects one
 *    - No matches → User confirms company creation
 *    - Error → Show error message
 */
class PeppolSettingsViewModel : BaseViewModel<DokusState<PeppolSettingsDto?>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<PeppolSettingsViewModel>()
    private val dataSource: CashflowRemoteDataSource by inject()

    // Available providers
    private val _providers = MutableStateFlow<DokusState<List<String>>>(DokusState.idle())
    val providers: StateFlow<DokusState<List<String>>> = _providers.asStateFlow()

    // Connection flow state
    private val _connectionState = MutableStateFlow<PeppolConnectionState>(PeppolConnectionState.Idle)
    val connectionState: StateFlow<PeppolConnectionState> = _connectionState.asStateFlow()

    // Connected company info
    private val _connectedCompany = MutableStateFlow<RecommandCompanySummary?>(null)
    val connectedCompany: StateFlow<RecommandCompanySummary?> = _connectedCompany.asStateFlow()

    // Form state for creating/editing settings
    private val _formState = MutableStateFlow(PeppolSettingsFormState())
    val formState: StateFlow<PeppolSettingsFormState> = _formState.asStateFlow()

    // ========================================================================
    // SETTINGS OPERATIONS
    // ========================================================================

    /**
     * Load current Peppol settings.
     */
    fun loadSettings() {
        scope.launch {
            logger.d { "Loading Peppol settings" }
            mutableState.emitLoading()

            dataSource.getPeppolSettings().fold(
                onSuccess = { settings ->
                    logger.i { "Peppol settings loaded: ${if (settings != null) "configured" else "not configured"}" }
                    mutableState.value = DokusState.success(settings)
                    if (settings != null) {
                        populateFormFromSettings(settings)
                        _connectionState.value = PeppolConnectionState.Connected
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load Peppol settings" }
                    mutableState.emit(error) { loadSettings() }
                }
            )
        }
    }

    /**
     * Connect to Peppol by auto-discovering company via Recommand API.
     * Only requires API Key and API Secret.
     */
    fun connect() {
        val form = _formState.value

        // Validate credentials
        val validationErrors = validateCredentials(form)
        if (validationErrors.isNotEmpty()) {
            _formState.value = form.copy(errors = validationErrors)
            return
        }

        scope.launch {
            logger.d { "Connecting to Peppol" }
            _connectionState.value = PeppolConnectionState.Connecting

            val request = PeppolConnectRequest(
                apiKey = form.apiKey,
                apiSecret = form.apiSecret,
                isEnabled = form.isEnabled,
                testMode = form.testMode,
                createCompanyIfMissing = false
            )

            dataSource.connectPeppol(request).fold(
                onSuccess = { response ->
                    handleConnectResponse(response.status, response.settings, response.company, response.candidates)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to connect to Peppol" }
                    _connectionState.value = PeppolConnectionState.Error(
                        error.message ?: "Connection failed"
                    )
                }
            )
        }
    }

    /**
     * Select a company from multiple matches.
     */
    fun selectCompany(companyId: String) {
        val form = _formState.value

        scope.launch {
            logger.d { "Selecting company: $companyId" }
            _connectionState.value = PeppolConnectionState.Connecting

            val request = PeppolConnectRequest(
                apiKey = form.apiKey,
                apiSecret = form.apiSecret,
                isEnabled = form.isEnabled,
                testMode = form.testMode,
                companyId = companyId,
                createCompanyIfMissing = false
            )

            dataSource.connectPeppol(request).fold(
                onSuccess = { response ->
                    handleConnectResponse(response.status, response.settings, response.company, response.candidates)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to select company" }
                    _connectionState.value = PeppolConnectionState.Error(
                        error.message ?: "Failed to select company"
                    )
                }
            )
        }
    }

    /**
     * Confirm company creation on Recommand when no matching company exists.
     */
    fun confirmCreateCompany() {
        val form = _formState.value

        scope.launch {
            logger.d { "Creating company on Recommand" }
            _connectionState.value = PeppolConnectionState.Connecting

            val request = PeppolConnectRequest(
                apiKey = form.apiKey,
                apiSecret = form.apiSecret,
                isEnabled = form.isEnabled,
                testMode = form.testMode,
                createCompanyIfMissing = true
            )

            dataSource.connectPeppol(request).fold(
                onSuccess = { response ->
                    handleConnectResponse(response.status, response.settings, response.company, response.candidates)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create company" }
                    _connectionState.value = PeppolConnectionState.Error(
                        error.message ?: "Failed to create company"
                    )
                }
            )
        }
    }

    /**
     * Cancel the connection process.
     */
    fun cancelConnection() {
        _connectionState.value = PeppolConnectionState.Idle
    }

    private fun handleConnectResponse(
        status: PeppolConnectStatus,
        settings: PeppolSettingsDto?,
        company: RecommandCompanySummary?,
        candidates: List<RecommandCompanySummary>
    ) {
        when (status) {
            PeppolConnectStatus.Connected -> {
                logger.i { "Connected to Peppol" }
                _connectionState.value = PeppolConnectionState.Connected
                _connectedCompany.value = company
                settings?.let {
                    mutableState.value = DokusState.success(it)
                    populateFormFromSettings(it)
                }
            }
            PeppolConnectStatus.MultipleMatches -> {
                logger.i { "Multiple companies found: ${candidates.size}" }
                _connectionState.value = PeppolConnectionState.SelectCompany(candidates)
            }
            PeppolConnectStatus.NoCompanyFound -> {
                logger.i { "No matching company found, asking for confirmation" }
                _connectionState.value = PeppolConnectionState.ConfirmCreateCompany
            }
            PeppolConnectStatus.MissingVatNumber -> {
                logger.w { "Tenant VAT number is missing" }
                _connectionState.value = PeppolConnectionState.Error(
                    "Please configure your company VAT number in workspace settings first"
                )
            }
            PeppolConnectStatus.MissingCompanyAddress -> {
                logger.w { "Tenant company address is missing" }
                _connectionState.value = PeppolConnectionState.Error(
                    "Please configure your company address in workspace settings first"
                )
            }
            PeppolConnectStatus.InvalidCredentials -> {
                logger.w { "Invalid Peppol credentials" }
                _connectionState.value = PeppolConnectionState.Error(
                    "Invalid API credentials. Please check your API Key and Secret."
                )
            }
        }
    }

    /**
     * Delete Peppol settings.
     */
    fun deleteSettings(
        onSuccess: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            logger.d { "Deleting Peppol settings" }
            mutableState.emitLoading()

            dataSource.deletePeppolSettings().fold(
                onSuccess = {
                    logger.i { "Peppol settings deleted" }
                    mutableState.value = DokusState.success(null)
                    resetForm()
                    _connectionState.value = PeppolConnectionState.Idle
                    _connectedCompany.value = null
                    onSuccess()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete Peppol settings" }
                    mutableState.emit(error) { deleteSettings(onSuccess, onError) }
                    onError(error)
                }
            )
        }
    }

    // ========================================================================
    // PROVIDERS
    // ========================================================================

    /**
     * Load available Peppol providers.
     */
    fun loadProviders() {
        scope.launch {
            logger.d { "Loading Peppol providers" }
            _providers.value = DokusState.loading()

            dataSource.getPeppolProviders().fold(
                onSuccess = { providerList ->
                    logger.d { "Loaded ${providerList.size} providers" }
                    _providers.value = DokusState.success(providerList)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load providers" }
                    _providers.value = DokusState.error(
                        DokusException.Unknown(error)
                    ) { loadProviders() }
                }
            )
        }
    }

    // ========================================================================
    // FORM MANAGEMENT
    // ========================================================================

    fun updateApiKey(value: String) {
        _formState.value = _formState.value.copy(
            apiKey = value,
            errors = _formState.value.errors - "apiKey"
        )
    }

    fun updateApiSecret(value: String) {
        _formState.value = _formState.value.copy(
            apiSecret = value,
            errors = _formState.value.errors - "apiSecret"
        )
    }

    fun updateIsEnabled(value: Boolean) {
        _formState.value = _formState.value.copy(isEnabled = value)
    }

    fun updateTestMode(value: Boolean) {
        _formState.value = _formState.value.copy(testMode = value)
    }

    /**
     * Reset form to initial state.
     */
    fun resetForm() {
        _formState.value = PeppolSettingsFormState()
    }

    /**
     * Reset connection state to idle.
     */
    fun resetConnectionState() {
        _connectionState.value = PeppolConnectionState.Idle
    }

    // ========================================================================
    // HELPERS
    // ========================================================================

    private fun populateFormFromSettings(settings: PeppolSettingsDto) {
        _formState.value = PeppolSettingsFormState(
            apiKey = "", // Never returned from server
            apiSecret = "", // Never returned from server
            isEnabled = settings.isEnabled,
            testMode = settings.testMode,
            isConnected = true
        )
    }

    private fun validateCredentials(form: PeppolSettingsFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (form.apiKey.isBlank()) {
            errors["apiKey"] = "API Key is required"
        }

        if (form.apiSecret.isBlank()) {
            errors["apiSecret"] = "API Secret is required"
        }

        return errors
    }
}

/**
 * Form state for Peppol settings.
 * Simplified to only require API credentials for new connections.
 */
data class PeppolSettingsFormState(
    val apiKey: String = "",
    val apiSecret: String = "",
    val isEnabled: Boolean = false,
    val testMode: Boolean = true,
    val isConnected: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

/**
 * Connection flow state.
 */
sealed class PeppolConnectionState {
    /** Initial state, not connected */
    data object Idle : PeppolConnectionState()

    /** Connection in progress */
    data object Connecting : PeppolConnectionState()

    /** Successfully connected */
    data object Connected : PeppolConnectionState()

    /** Multiple companies found, user must select one */
    data class SelectCompany(val candidates: List<RecommandCompanySummary>) : PeppolConnectionState()

    /** No matching company found, asking user to confirm creation */
    data object ConfirmCreateCompany : PeppolConnectionState()

    /** Error occurred */
    data class Error(val message: String) : PeppolConnectionState()
}
