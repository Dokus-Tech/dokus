package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.SavePeppolSettingsRequest
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for Peppol settings management.
 * Handles configuration, connection testing, and provider management.
 */
class PeppolSettingsViewModel : BaseViewModel<DokusState<PeppolSettingsDto?>>(DokusState.idle()), KoinComponent {

    private val logger = Logger.forClass<PeppolSettingsViewModel>()
    private val dataSource: CashflowRemoteDataSource by inject()

    // Available providers
    private val _providers = MutableStateFlow<DokusState<List<String>>>(DokusState.idle())
    val providers: StateFlow<DokusState<List<String>>> = _providers.asStateFlow()

    // Connection test state
    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

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
                    // Pre-populate form if settings exist
                    settings?.let { populateFormFromSettings(it) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load Peppol settings" }
                    mutableState.emit(error) { loadSettings() }
                }
            )
        }
    }

    /**
     * Save Peppol settings.
     */
    fun saveSettings(
        onSuccess: (PeppolSettingsDto) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val form = _formState.value

        // Validate form
        val validationErrors = validateForm(form)
        if (validationErrors.isNotEmpty()) {
            _formState.value = form.copy(errors = validationErrors)
            onError(IllegalArgumentException(validationErrors.values.first()))
            return
        }

        scope.launch {
            logger.d { "Saving Peppol settings" }
            mutableState.emitLoading()

            val request = SavePeppolSettingsRequest(
                companyId = form.companyId,
                apiKey = form.apiKey,
                apiSecret = form.apiSecret,
                peppolId = form.peppolId,
                isEnabled = form.isEnabled,
                testMode = form.testMode
            )

            dataSource.savePeppolSettings(request).fold(
                onSuccess = { settings ->
                    logger.i { "Peppol settings saved successfully" }
                    mutableState.value = DokusState.success(settings)
                    populateFormFromSettings(settings)
                    onSuccess(settings)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save Peppol settings" }
                    mutableState.emit(error) { saveSettings(onSuccess, onError) }
                    onError(error)
                }
            )
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
    // CONNECTION TEST
    // ========================================================================

    /**
     * Test the Peppol connection with current credentials.
     */
    fun testConnection() {
        scope.launch {
            logger.d { "Testing Peppol connection" }
            _connectionTestState.value = ConnectionTestState.Testing

            dataSource.testPeppolConnection().fold(
                onSuccess = { success ->
                    logger.i { "Connection test result: $success" }
                    _connectionTestState.value = if (success) {
                        ConnectionTestState.Success
                    } else {
                        ConnectionTestState.Failed("Connection test failed")
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Connection test error" }
                    _connectionTestState.value = ConnectionTestState.Failed(
                        error.message ?: "Unknown error"
                    )
                }
            )
        }
    }

    /**
     * Reset connection test state.
     */
    fun resetConnectionTestState() {
        _connectionTestState.value = ConnectionTestState.Idle
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

    /**
     * Update form field values.
     */
    fun updateCompanyId(value: String) {
        _formState.value = _formState.value.copy(
            companyId = value,
            errors = _formState.value.errors - "companyId"
        )
    }

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

    fun updatePeppolId(value: String) {
        _formState.value = _formState.value.copy(
            peppolId = value,
            errors = _formState.value.errors - "peppolId"
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

    // ========================================================================
    // HELPERS
    // ========================================================================

    private fun populateFormFromSettings(settings: PeppolSettingsDto) {
        _formState.value = PeppolSettingsFormState(
            companyId = settings.companyId,
            apiKey = "", // Never returned from server
            apiSecret = "", // Never returned from server
            peppolId = settings.peppolId.value,
            isEnabled = settings.isEnabled,
            testMode = settings.testMode,
            isEditing = true
        )
    }

    private fun validateForm(form: PeppolSettingsFormState): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        if (form.companyId.isBlank()) {
            errors["companyId"] = "Company ID is required"
        }

        // Only validate API credentials if not editing (new setup) or if provided
        if (!form.isEditing || form.apiKey.isNotBlank()) {
            if (form.apiKey.isBlank()) {
                errors["apiKey"] = "API Key is required"
            }
        }

        if (!form.isEditing || form.apiSecret.isNotBlank()) {
            if (form.apiSecret.isBlank()) {
                errors["apiSecret"] = "API Secret is required"
            }
        }

        if (form.peppolId.isBlank()) {
            errors["peppolId"] = "Peppol ID is required"
        } else if (!isValidPeppolIdFormat(form.peppolId)) {
            errors["peppolId"] = "Invalid Peppol ID format (expected: scheme:identifier)"
        }

        return errors
    }

    private fun isValidPeppolIdFormat(peppolId: String): Boolean {
        // Basic validation: should contain scheme:identifier format
        // Example: 0208:BE0123456789
        val parts = peppolId.split(":")
        return parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()
    }
}

/**
 * Form state for Peppol settings.
 */
data class PeppolSettingsFormState(
    val companyId: String = "",
    val apiKey: String = "",
    val apiSecret: String = "",
    val peppolId: String = "",
    val isEnabled: Boolean = false,
    val testMode: Boolean = true,
    val isEditing: Boolean = false,
    val errors: Map<String, String> = emptyMap()
)

/**
 * Connection test state.
 */
sealed class ConnectionTestState {
    data object Idle : ConnectionTestState()
    data object Testing : ConnectionTestState()
    data object Success : ConnectionTestState()
    data class Failed(val message: String) : ConnectionTestState()
}
