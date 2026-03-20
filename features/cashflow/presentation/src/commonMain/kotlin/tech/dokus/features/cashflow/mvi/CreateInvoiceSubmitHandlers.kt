package tech.dokus.features.cashflow.mvi

import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.features.cashflow.mvi.model.InvoiceSection
import tech.dokus.features.cashflow.presentation.cashflow.model.mapper.toCreateInvoiceRequest
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.ValidateInvoiceUseCase
import tech.dokus.features.cashflow.usecases.SubmitInvoiceWithDeliveryResult
import tech.dokus.features.cashflow.usecases.SubmitInvoiceWithDeliveryUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetInvoiceNumberPreviewUseCase
import tech.dokus.features.auth.usecases.GetTenantSettingsUseCase
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.foundation.platform.Logger

internal class CreateInvoiceSubmitHandlers(
    private val getInvoiceNumberPreview: GetInvoiceNumberPreviewUseCase,
    private val getTenantSettings: GetTenantSettingsUseCase,
    private val getCurrentTenant: GetCurrentTenantUseCase,
    private val validateInvoice: ValidateInvoiceUseCase,
    private val submitInvoiceWithDelivery: SubmitInvoiceWithDeliveryUseCase,
    private val updateInvoice: suspend CreateInvoiceCtx.(
        transform: (CreateInvoiceState) -> CreateInvoiceState
    ) -> Unit,
    private val snapshotState: suspend CreateInvoiceCtx.() -> CreateInvoiceState,
    private val synchronizeDueDate: (CreateInvoiceFormState) -> CreateInvoiceFormState,
) {

    private val logger = Logger.forClass<CreateInvoiceSubmitHandlers>()

    suspend fun CreateInvoiceCtx.loadDefaults() {
        if (snapshotState().uiState.defaultsLoaded) return

        getInvoiceNumberPreview()
            .onSuccess { preview ->
                updateInvoice { it.copy(invoiceNumberPreview = preview) }
            }
            .onFailure { logger.w { "Could not load invoice number preview: ${it.message}" } }

        val tenantSettingsResult = getTenantSettings()
        val currentTenantResult = getCurrentTenant()

        tenantSettingsResult.onFailure { logger.w { "Could not load tenant defaults: ${it.message}" } }
        currentTenantResult.onFailure { logger.w { "Could not load current tenant: ${it.message}" } }

        val settings = tenantSettingsResult.getOrNull()
        val currentTenant = requireNotNull(currentTenantResult.getOrNull()) {
            "Current tenant must be available on Create Invoice screen."
        }

        updateInvoice { state ->
            val withDefaults = settings?.let { tenantSettings ->
                synchronizeDueDate(
                    state.formState.copy(
                        paymentTermsDays = tenantSettings.defaultPaymentTerms,
                        senderIban = tenantSettings.companyIban?.value.orEmpty(),
                        senderBic = tenantSettings.companyBic?.value.orEmpty(),
                        notes = state.formState.notes.ifBlank { tenantSettings.paymentTermsText.orEmpty() }
                    )
                )
            } ?: state.formState

            state.copy(
                formState = withDefaults,
                uiState = state.uiState.copy(
                    senderCompanyName = currentTenant.legalName.value,
                    senderCompanyVat = currentTenant.vatNumber.formatted
                )
            )
        }

        updateInvoice { state ->
            val updated = if (state.formState.structuredCommunication.isNotBlank()) {
                state
            } else {
                state.copy(
                    formState = state.formState.copy(
                        structuredCommunication = generateStructuredCommunication()
                    )
                )
            }
            updated.copy(uiState = updated.uiState.copy(defaultsLoaded = true))
        }
    }

    suspend fun CreateInvoiceCtx.submitInvoice(deliveryMethod: InvoiceDeliveryMethod?) {
        val current = snapshotState()
        if (current.formState.isSaving) return

        val validation = validateInvoice(current.formState)
        if (!validation.isValid) {
            val firstError = validation.errors.entries.firstOrNull()
            val firstInvalidSection = sectionForError(firstError?.key)
            updateInvoice { state ->
                state.copy(
                    formState = state.formState.copy(errors = validation.errors),
                    uiState = state.uiState.copy(
                        expandedSections = state.uiState.expandedSections + firstInvalidSection,
                        suggestedSection = firstInvalidSection
                    )
                )
            }
            action(
                CreateInvoiceAction.ShowValidationError(
                    firstError?.value?.message ?: "Invoice is missing required fields."
                )
            )
            return
        }

        updateInvoice {
            it.copy(formState = it.formState.copy(isSaving = true, errors = emptyMap()))
        }

        // Persist the user's delivery preference for UX recall, but the actual
        // delivery action is determined by `deliveryMethod` (null = draft, no delivery).
        val persistedPreference = current.uiState.selectedDeliveryPreference
        val request = current.formState.toCreateInvoiceRequest(persistedPreference)
        submitInvoiceWithDelivery(request, deliveryMethod).fold(
            onSuccess = { result ->
                updateInvoice { it.copy(formState = it.formState.copy(isSaving = false)) }
                when (result) {
                    is SubmitInvoiceWithDeliveryResult.DraftSaved -> {
                        action(CreateInvoiceAction.ShowSuccess("Draft saved."))
                        action(CreateInvoiceAction.NavigateBack)
                    }
                    is SubmitInvoiceWithDeliveryResult.PeppolQueued -> {
                        action(CreateInvoiceAction.ShowSuccess("Invoice queued for PEPPOL."))
                        action(CreateInvoiceAction.NavigateBack)
                    }
                    is SubmitInvoiceWithDeliveryResult.PdfReady -> {
                        action(CreateInvoiceAction.OpenExternalUrl(result.downloadUrl))
                        action(CreateInvoiceAction.ShowSuccess("Invoice PDF is ready."))
                        action(CreateInvoiceAction.NavigateBack)
                    }
                    is SubmitInvoiceWithDeliveryResult.DeliveryFailed -> {
                        action(CreateInvoiceAction.ShowError("Invoice saved but delivery failed: ${result.error}"))
                        action(CreateInvoiceAction.NavigateBack)
                    }
                }
            },
            onFailure = { error ->
                updateInvoice {
                    it.copy(formState = it.formState.copy(isSaving = false))
                }
                action(CreateInvoiceAction.ShowError(error.message ?: "Failed to submit invoice."))
            }
        )
    }

    private fun sectionForError(field: String?): InvoiceSection {
        return when (field) {
            ValidateInvoiceUseCase.FIELD_CLIENT -> InvoiceSection.Client
            ValidateInvoiceUseCase.FIELD_ITEMS -> InvoiceSection.LineItems
            ValidateInvoiceUseCase.FIELD_DUE_DATE -> InvoiceSection.DatesTerms
            else -> InvoiceSection.PaymentDelivery
        }
    }
}
