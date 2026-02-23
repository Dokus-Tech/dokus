package tech.dokus.app.viewmodel

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.features.auth.usecases.DeleteWorkspaceAvatarUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetTenantAddressUseCase
import tech.dokus.features.auth.usecases.GetTenantSettingsUseCase
import tech.dokus.features.auth.usecases.UpdateTenantSettingsUseCase
import tech.dokus.features.auth.usecases.UploadWorkspaceAvatarUseCase
import tech.dokus.features.auth.usecases.WatchCurrentTenantUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolActivityUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolRegistrationUseCase
import tech.dokus.foundation.platform.Logger

internal typealias WorkspaceSettingsCtx =
    PipelineContext<WorkspaceSettingsState, WorkspaceSettingsIntent, WorkspaceSettingsAction>

private const val MAX_PAYMENT_TERMS_DAYS = 365
private const val MIN_INVOICE_PADDING = 1
private const val MAX_INVOICE_PADDING = 8

/**
 * Container for Workspace Settings screen using FlowMVI.
 *
 * Manages tenant/company settings including company info, banking details,
 * invoice configuration, and avatar management.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class WorkspaceSettingsContainer(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val getTenantSettings: GetTenantSettingsUseCase,
    private val getTenantAddress: GetTenantAddressUseCase,
    private val updateTenantSettings: UpdateTenantSettingsUseCase,
    private val uploadWorkspaceAvatar: UploadWorkspaceAvatarUseCase,
    private val deleteWorkspaceAvatar: DeleteWorkspaceAvatarUseCase,
    private val watchCurrentTenantUseCase: WatchCurrentTenantUseCase,
    private val getPeppolRegistration: GetPeppolRegistrationUseCase,
    private val getPeppolActivity: GetPeppolActivityUseCase,
) : Container<WorkspaceSettingsState, WorkspaceSettingsIntent, WorkspaceSettingsAction> {

    private val logger = Logger.forClass<WorkspaceSettingsContainer>()

    override val store: Store<WorkspaceSettingsState, WorkspaceSettingsIntent, WorkspaceSettingsAction> =
        store(WorkspaceSettingsState.Loading) {
            reduce { intent ->
                when (intent) {
                    is WorkspaceSettingsIntent.Load -> handleLoad()
                    is WorkspaceSettingsIntent.UpdateCompanyName -> handleUpdateCompanyName(intent.value)
                    is WorkspaceSettingsIntent.UpdateVatNumber -> handleUpdateVatNumber(intent.value)
                    is WorkspaceSettingsIntent.UpdateIban -> handleUpdateIban(intent.value)
                    is WorkspaceSettingsIntent.UpdateBic -> handleUpdateBic(intent.value)
                    is WorkspaceSettingsIntent.UpdateAddress -> handleUpdateAddress(intent.value)
                    is WorkspaceSettingsIntent.UpdateInvoicePrefix -> handleUpdateInvoicePrefix(intent.value)
                    is WorkspaceSettingsIntent.UpdateDefaultPaymentTerms -> handleUpdateDefaultPaymentTerms(
                        intent.value
                    )
                    is WorkspaceSettingsIntent.UpdateInvoiceYearlyReset -> handleUpdateInvoiceYearlyReset(intent.value)
                    is WorkspaceSettingsIntent.UpdateInvoicePadding -> handleUpdateInvoicePadding(intent.value)
                    is WorkspaceSettingsIntent.UpdateInvoiceIncludeYear -> handleUpdateInvoiceIncludeYear(intent.value)
                    is WorkspaceSettingsIntent.UpdateInvoiceTimezone -> handleUpdateInvoiceTimezone(intent.value)
                    is WorkspaceSettingsIntent.UpdatePaymentTermsText -> handleUpdatePaymentTermsText(intent.value)
                    is WorkspaceSettingsIntent.EnterEditMode -> handleEnterEditMode(intent.section)
                    is WorkspaceSettingsIntent.CancelEditMode -> handleCancelEditMode()
                    is WorkspaceSettingsIntent.SaveSection -> handleSaveSection(intent.section)
                    is WorkspaceSettingsIntent.SaveSettings -> handleSaveSettings()
                    is WorkspaceSettingsIntent.ResetSaveState -> handleResetSaveState()
                    is WorkspaceSettingsIntent.UploadAvatar -> handleUploadAvatar(intent.imageBytes, intent.filename)
                    is WorkspaceSettingsIntent.DeleteAvatar -> handleDeleteAvatar()
                    is WorkspaceSettingsIntent.ResetAvatarState -> handleResetAvatarState()
                }
            }
        }

    private suspend fun WorkspaceSettingsCtx.handleLoad() {
        logger.d { "Loading workspace settings" }

        updateState { WorkspaceSettingsState.Loading }

        val tenantResult = getCurrentTenantUseCase()
        val settingsResult = getTenantSettings()
        val addressResult = getTenantAddress()
        val peppolRegistrationResult = getPeppolRegistration()
        val peppolActivityResult = getPeppolActivity()

        val tenant = tenantResult.getOrNull()
        val settings = settingsResult.getOrNull()
        val address = addressResult.getOrNull()
        // PEPPOL data is optional - gracefully handle if not available
        val peppolRegistration = peppolRegistrationResult.getOrNull()
        val peppolActivity = peppolActivityResult.getOrNull()

        if (tenant != null && settings != null) {
            logger.i { "Workspace settings loaded for ${tenant.displayName.value}" }
            val formState = populateFormFromSettings(tenant, settings, address)
            updateState {
                WorkspaceSettingsState.Content(
                    tenant = tenant,
                    settings = settings,
                    form = formState,
                    currentAvatar = tenant.avatar,
                    peppolRegistration = peppolRegistration,
                    peppolActivity = peppolActivity,
                )
            }
        } else {
            val error = tenantResult.exceptionOrNull() ?: settingsResult.exceptionOrNull()
                ?: IllegalStateException("Failed to load workspace settings")
            logger.e(error) { "Failed to load workspace settings" }
            updateState {
                WorkspaceSettingsState.Error(
                    exception = error.asDokusException,
                    retryHandler = { intent(WorkspaceSettingsIntent.Load) }
                )
            }
        }
    }

    private fun populateFormFromSettings(
        tenant: Tenant,
        settings: TenantSettings,
        address: Address?
    ): WorkspaceSettingsState.Content.FormState {
        return WorkspaceSettingsState.Content.FormState(
            companyName = settings.companyName ?: tenant.displayName.value,
            legalName = tenant.legalName.value,
            vatNumber = tenant.vatNumber.value,
            iban = settings.companyIban?.value ?: "",
            bic = settings.companyBic?.value ?: "",
            address = address?.toDisplayString().orEmpty(),
            invoicePrefix = settings.invoicePrefix,
            defaultPaymentTerms = settings.defaultPaymentTerms,
            invoiceYearlyReset = settings.invoiceYearlyReset,
            invoicePadding = settings.invoicePadding,
            invoiceIncludeYear = settings.invoiceIncludeYear,
            invoiceTimezone = settings.invoiceTimezone,
            paymentTermsText = settings.paymentTermsText ?: ""
        )
    }

    // Form field update handlers
    private suspend fun WorkspaceSettingsCtx.handleUpdateCompanyName(value: String) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(companyName = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateVatNumber(value: String) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(vatNumber = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateIban(value: String) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(iban = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateBic(value: String) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(bic = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateAddress(value: String) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(address = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateInvoicePrefix(value: String) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(invoicePrefix = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateDefaultPaymentTerms(value: String) {
        val terms = value.toIntOrNull() ?: return
        if (terms in 0..MAX_PAYMENT_TERMS_DAYS) {
            withState<WorkspaceSettingsState.Content, _> {
                updateState { copy(form = form.copy(defaultPaymentTerms = terms)) }
            }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateInvoiceYearlyReset(value: Boolean) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(invoiceYearlyReset = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateInvoicePadding(value: Int) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState {
                copy(
                    form = form.copy(invoicePadding = value.coerceIn(MIN_INVOICE_PADDING, MAX_INVOICE_PADDING))
                )
            }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateInvoiceIncludeYear(value: Boolean) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(invoiceIncludeYear = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdateInvoiceTimezone(value: String) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(invoiceTimezone = value)) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleUpdatePaymentTermsText(value: String) {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(form = form.copy(paymentTermsText = value)) }
        }
    }

    // Section edit mode handlers
    private suspend fun WorkspaceSettingsCtx.handleEnterEditMode(
        section: WorkspaceSettingsState.Content.EditingSection
    ) {
        withState<WorkspaceSettingsState.Content, _> {
            logger.d { "Entering edit mode for section: $section" }
            updateState { copy(editingSection = section) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleCancelEditMode() {
        withState<WorkspaceSettingsState.Content, _> {
            logger.d { "Cancelling edit mode" }
            // Restore form state from original settings
            val formState = populateFormFromSettings(tenant, settings, null)
            updateState { copy(editingSection = null, form = formState) }
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleSaveSection(
        section: WorkspaceSettingsState.Content.EditingSection
    ) {
        withState<WorkspaceSettingsState.Content, _> {
            logger.d { "Saving section: $section" }
            // For simplicity, use the same save logic as full save
            // Future optimization: save only changed fields per section
            handleSaveSettings()
            // Exit edit mode on success
            withState<WorkspaceSettingsState.Content, _> {
                if (saveState is WorkspaceSettingsState.Content.SaveState.Success) {
                    updateState { copy(editingSection = null) }
                }
            }
        }
    }

    // Save operations
    private suspend fun WorkspaceSettingsCtx.handleSaveSettings() {
        withState<WorkspaceSettingsState.Content, _> {
            logger.d { "Saving workspace settings" }
            updateState { copy(saveState = WorkspaceSettingsState.Content.SaveState.Saving) }

            val updatedSettings = settings.copy(
                companyName = form.companyName.ifBlank { null },
                companyIban = form.iban.takeIf { it.isNotBlank() }?.let { Iban(it) },
                companyBic = form.bic.takeIf { it.isNotBlank() }?.let { Bic(it) },
                invoicePrefix = form.invoicePrefix.ifBlank { "INV" },
                defaultPaymentTerms = form.defaultPaymentTerms,
                invoiceYearlyReset = form.invoiceYearlyReset,
                invoicePadding = form.invoicePadding,
                invoiceIncludeYear = form.invoiceIncludeYear,
                invoiceTimezone = form.invoiceTimezone,
                paymentTermsText = form.paymentTermsText.ifBlank { null }
            )

            updateTenantSettings(updatedSettings).fold(
                onSuccess = {
                    logger.i { "Workspace settings saved" }
                    watchCurrentTenantUseCase.refresh()
                    updateState {
                        copy(
                            settings = updatedSettings,
                            saveState = WorkspaceSettingsState.Content.SaveState.Success
                        )
                    }
                    action(WorkspaceSettingsAction.ShowSuccess(WorkspaceSettingsSuccess.SettingsSaved))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to save workspace settings" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.WorkspaceSettingsSaveFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(saveState = WorkspaceSettingsState.Content.SaveState.Error(displayException))
                    }
                    action(WorkspaceSettingsAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleResetSaveState() {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(saveState = WorkspaceSettingsState.Content.SaveState.Idle) }
        }
    }

    // Avatar operations
    private suspend fun WorkspaceSettingsCtx.handleUploadAvatar(imageBytes: ByteArray, filename: String) {
        withState<WorkspaceSettingsState.Content, _> {
            logger.d { "Uploading avatar" }
            updateState {
                copy(avatarState = WorkspaceSettingsState.Content.AvatarState.Uploading(0f))
            }

            val contentType = when {
                filename.endsWith(".png", ignoreCase = true) -> "image/png"
                filename.endsWith(".gif", ignoreCase = true) -> "image/gif"
                filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }

            uploadWorkspaceAvatar(
                imageBytes = imageBytes,
                filename = filename,
                contentType = contentType,
                onProgress = { progress ->
                    // Note: Progress updates would need to be handled differently in FlowMVI
                    // For now, we just set the initial state
                }
            ).fold(
                onSuccess = { response ->
                    logger.i { "Avatar uploaded successfully" }
                    watchCurrentTenantUseCase.refresh()
                    updateState {
                        copy(
                            currentAvatar = response.avatar,
                            avatarState = WorkspaceSettingsState.Content.AvatarState.Success
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to upload avatar" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.WorkspaceAvatarUploadFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(avatarState = WorkspaceSettingsState.Content.AvatarState.Error(displayException))
                    }
                    action(WorkspaceSettingsAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleDeleteAvatar() {
        withState<WorkspaceSettingsState.Content, _> {
            logger.d { "Deleting avatar" }
            updateState {
                copy(avatarState = WorkspaceSettingsState.Content.AvatarState.Deleting)
            }

            deleteWorkspaceAvatar().fold(
                onSuccess = {
                    logger.i { "Avatar deleted" }
                    watchCurrentTenantUseCase.refresh()
                    updateState {
                        copy(
                            currentAvatar = null,
                            avatarState = WorkspaceSettingsState.Content.AvatarState.Idle
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete avatar" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.WorkspaceAvatarDeleteFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(avatarState = WorkspaceSettingsState.Content.AvatarState.Error(displayException))
                    }
                    action(WorkspaceSettingsAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun WorkspaceSettingsCtx.handleResetAvatarState() {
        withState<WorkspaceSettingsState.Content, _> {
            updateState { copy(avatarState = WorkspaceSettingsState.Content.AvatarState.Idle) }
        }
    }
}

@Suppress("NoMultipleSpaces")
private fun Address.toDisplayString(): String {
    val parts = buildList {
        streetLine1?.let { add(it) }
        streetLine2?.takeIf { it.isNotBlank() }?.let { add(it) }
        val cityLine = listOfNotNull(postalCode, city).filter { it.isNotBlank() }.joinToString(" ")
        if (cityLine.isNotBlank()) add(cityLine)
        country?.let { add(it) } // country is now ISO-2 string directly
    }
    return parts.joinToString(", ")
}
