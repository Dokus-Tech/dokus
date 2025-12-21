package tech.dokus.app.viewmodel

import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.domain.ids.Bic
import ai.dokus.foundation.domain.ids.Iban
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.CompanyAvatar
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.foundation.platform.Logger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for workspace/company settings screen.
 * Manages tenant settings like company info, banking details, and invoice configuration.
 */
class WorkspaceSettingsViewModel(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val tenantDataSource: TenantRemoteDataSource
) : ViewModel() {

    private val logger = Logger.forClass<WorkspaceSettingsViewModel>()

    private val _state = MutableStateFlow<DokusState<WorkspaceSettingsData>>(DokusState.idle())
    val state: StateFlow<DokusState<WorkspaceSettingsData>> = _state.asStateFlow()

    private val _formState = MutableStateFlow(WorkspaceFormState())
    val formState: StateFlow<WorkspaceFormState> = _formState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _avatarState = MutableStateFlow<AvatarState>(AvatarState.Idle)
    val avatarState: StateFlow<AvatarState> = _avatarState.asStateFlow()

    private val _currentAvatar = MutableStateFlow<CompanyAvatar?>(null)
    val currentAvatar: StateFlow<CompanyAvatar?> = _currentAvatar.asStateFlow()

    /**
     * Load workspace settings from backend.
     */
    fun loadWorkspaceSettings() {
        viewModelScope.launch {
            logger.d { "Loading workspace settings" }
            _state.value = DokusState.loading()

            val tenantResult = getCurrentTenantUseCase()
            val settingsResult = tenantDataSource.getTenantSettings()

            val tenant = tenantResult.getOrNull()
            val settings = settingsResult.getOrNull()

            if (tenant != null && settings != null) {
                logger.i { "Workspace settings loaded for ${tenant.displayName.value}" }
                _state.value = DokusState.success(WorkspaceSettingsData(tenant, settings))
                populateFormFromSettings(tenant, settings)
                // Use avatar from tenant (already included in response)
                _currentAvatar.value = tenant.avatar
            } else {
                val error = tenantResult.exceptionOrNull() ?: settingsResult.exceptionOrNull()
                    ?: IllegalStateException("Failed to load workspace settings")
                logger.e(error) { "Failed to load workspace settings" }
                _state.value = DokusState.error(error) { loadWorkspaceSettings() }
            }
        }
    }

    /**
     * Save workspace settings to backend.
     */
    fun saveWorkspaceSettings(onSuccess: () -> Unit = {}) {
        val form = _formState.value
        val currentData = (_state.value as? DokusState.Success)?.data ?: return

        viewModelScope.launch {
            logger.d { "Saving workspace settings" }
            _saveState.value = SaveState.Saving

            val updatedSettings = currentData.settings.copy(
                companyName = form.companyName.ifBlank { null },
                companyAddress = form.address.ifBlank { null },
                companyVatNumber = form.vatNumber.takeIf { it.isNotBlank() }?.let { VatNumber(it) },
                companyIban = form.iban.takeIf { it.isNotBlank() }?.let { Iban(it) },
                companyBic = form.bic.takeIf { it.isNotBlank() }?.let { Bic(it) },
                invoicePrefix = form.invoicePrefix.ifBlank { "INV" },
                defaultPaymentTerms = form.defaultPaymentTerms
            )

            tenantDataSource.updateTenantSettings(updatedSettings).fold(
                onSuccess = {
                    logger.i { "Workspace settings saved" }
                    _state.value = DokusState.success(
                        currentData.copy(settings = updatedSettings)
                    )
                    _saveState.value = SaveState.Success
                    onSuccess()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save workspace settings" }
                    _saveState.value = SaveState.Error(error.message ?: "Failed to save settings")
                }
            )
        }
    }

    /**
     * Reset save state to idle.
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    // Form field updates
    fun updateCompanyName(value: String) {
        _formState.value = _formState.value.copy(companyName = value)
    }

    fun updateVatNumber(value: String) {
        _formState.value = _formState.value.copy(vatNumber = value)
    }

    fun updateIban(value: String) {
        _formState.value = _formState.value.copy(iban = value)
    }

    fun updateBic(value: String) {
        _formState.value = _formState.value.copy(bic = value)
    }

    fun updateAddress(value: String) {
        _formState.value = _formState.value.copy(address = value)
    }

    fun updateInvoicePrefix(value: String) {
        _formState.value = _formState.value.copy(invoicePrefix = value)
    }

    fun updateDefaultPaymentTerms(value: String) {
        val terms = value.toIntOrNull() ?: return
        if (terms in 0..365) {
            _formState.value = _formState.value.copy(defaultPaymentTerms = terms)
        }
    }

    // ===== Avatar Operations =====

    /**
     * Called when user selects an image for cropping.
     */
    fun onImageSelected(imageBytes: ByteArray) {
        _avatarState.value = AvatarState.Cropping(imageBytes)
    }

    /**
     * Cancel the cropping operation.
     */
    fun cancelCrop() {
        _avatarState.value = AvatarState.Idle
    }

    /**
     * Called when user confirms the crop, starts upload.
     */
    fun onCropComplete(croppedImageBytes: ByteArray, filename: String = "avatar.png") {
        viewModelScope.launch {
            logger.d { "Uploading avatar" }
            _avatarState.value = AvatarState.Uploading(0f)

            val contentType = when {
                filename.endsWith(".png", ignoreCase = true) -> "image/png"
                filename.endsWith(".gif", ignoreCase = true) -> "image/gif"
                filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }

            tenantDataSource.uploadAvatar(
                imageBytes = croppedImageBytes,
                filename = filename,
                contentType = contentType,
                onProgress = { progress ->
                    _avatarState.value = AvatarState.Uploading(progress)
                }
            ).fold(
                onSuccess = { response ->
                    logger.i { "Avatar uploaded successfully" }
                    _currentAvatar.value = response.avatar
                    _avatarState.value = AvatarState.Success
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to upload avatar" }
                    _avatarState.value = AvatarState.Error(error.message ?: "Failed to upload avatar")
                }
            )
        }
    }

    /**
     * Delete the current avatar.
     */
    fun deleteAvatar() {
        viewModelScope.launch {
            logger.d { "Deleting avatar" }
            _avatarState.value = AvatarState.Deleting

            tenantDataSource.deleteAvatar().fold(
                onSuccess = {
                    logger.i { "Avatar deleted" }
                    _currentAvatar.value = null
                    _avatarState.value = AvatarState.Idle
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete avatar" }
                    _avatarState.value = AvatarState.Error(error.message ?: "Failed to delete avatar")
                }
            )
        }
    }

    /**
     * Reset avatar state to idle.
     */
    fun resetAvatarState() {
        _avatarState.value = AvatarState.Idle
    }

    private fun populateFormFromSettings(tenant: Tenant, settings: TenantSettings) {
        _formState.value = WorkspaceFormState(
            companyName = settings.companyName ?: tenant.displayName.value,
            legalName = tenant.legalName.value,
            vatNumber = settings.companyVatNumber?.value ?: tenant.vatNumber?.value ?: "",
            iban = settings.companyIban?.value ?: "",
            bic = settings.companyBic?.value ?: "",
            address = settings.companyAddress ?: "",
            invoicePrefix = settings.invoicePrefix,
            defaultPaymentTerms = settings.defaultPaymentTerms
        )
    }
}

/**
 * Combined data class for workspace settings screen.
 */
data class WorkspaceSettingsData(
    val tenant: Tenant,
    val settings: TenantSettings
)

/**
 * Form state for workspace settings.
 */
data class WorkspaceFormState(
    val companyName: String = "",
    val legalName: String = "", // Read-only
    val vatNumber: String = "",
    val iban: String = "",
    val bic: String = "",
    val address: String = "",
    val invoicePrefix: String = "INV",
    val defaultPaymentTerms: Int = 30
)

/**
 * Save operation state.
 */
sealed class SaveState {
    data object Idle : SaveState()
    data object Saving : SaveState()
    data object Success : SaveState()
    data class Error(val message: String) : SaveState()
}

/**
 * Avatar upload/delete operation state.
 */
sealed class AvatarState {
    data object Idle : AvatarState()
    data class Cropping(val imageBytes: ByteArray) : AvatarState()
    data class Uploading(val progress: Float) : AvatarState()
    data object Deleting : AvatarState()
    data object Success : AvatarState()
    data class Error(val message: String) : AvatarState()
}
