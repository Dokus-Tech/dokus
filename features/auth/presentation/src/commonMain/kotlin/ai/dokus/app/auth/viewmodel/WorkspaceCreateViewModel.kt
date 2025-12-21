package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.model.AddressFormState
import ai.dokus.app.auth.model.EntityConfirmationState
import ai.dokus.app.auth.model.LookupState
import ai.dokus.app.auth.model.WorkspaceWizardState
import ai.dokus.app.auth.model.WorkspaceWizardStep
import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.auth.repository.LookupRepository
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.EntityLookup
import ai.dokus.foundation.domain.model.UpsertTenantAddressRequest
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel

internal class WorkspaceCreateViewModel(
    private val authRepository: AuthRepository,
    private val lookupRepository: LookupRepository,
) : BaseViewModel<DokusState<Unit>>(DokusState.idle()) {

    private val logger = Logger.forClass<WorkspaceCreateViewModel>()

    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    private val mutableHasFreelancerWorkspace = MutableStateFlow(false)
    val hasFreelancerWorkspace = mutableHasFreelancerWorkspace.asStateFlow()

    private val mutableUserName = MutableStateFlow("")
    val userName = mutableUserName.asStateFlow()

    private val mutableWizardState = MutableStateFlow(WorkspaceWizardState())
    val wizardState = mutableWizardState.asStateFlow()

    private val mutableConfirmationState = MutableStateFlow<EntityConfirmationState>(EntityConfirmationState.Hidden)
    val confirmationState = mutableConfirmationState.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        scope.launch {
            mutableState.emitLoading()

            // Check if user already has a freelancer workspace
            authRepository.hasFreelancerTenant()
                .onSuccess { hasFreelancer ->
                    mutableHasFreelancerWorkspace.value = hasFreelancer
                    // If user has freelancer, default to Company
                    if (hasFreelancer) {
                        mutableWizardState.update { it.copy(tenantType = TenantType.Company) }
                    }
                    mutableState.emit(Unit)
                }.onFailure { error ->
                    logger.e(error) { "Failed to check freelancer workspace status" }
                    mutableState.emit(error) { loadUserInfo() }
                }

            // Get user's name for freelancer autofill
            authRepository.getCurrentUser()
                .onSuccess { user ->
                    val fullName = listOfNotNull(
                        user.firstName?.value,
                        user.lastName?.value
                    ).joinToString(" ")
                    mutableUserName.value = fullName
                    mutableState.emit(Unit)
                }.onFailure { error ->
                    logger.e(error) { "Failed to load user info" }
                    mutableState.emit(error) { loadUserInfo() }
                }
        }
    }

    // ===== Type Selection Step =====

    fun onTypeSelected(type: TenantType) {
        mutableWizardState.update { it.copy(tenantType = type) }
    }

    // ===== Company Name Step =====

    fun onCompanyNameChanged(name: String) {
        mutableWizardState.update { it.copy(companyName = name, lookupState = LookupState.Idle) }
    }

    fun lookupCompany() {
        val name = wizardState.value.companyName.trim()
        if (name.length < 3) return

        scope.launch {
            mutableWizardState.update { it.copy(lookupState = LookupState.Loading) }

            lookupRepository.searchCompany(name)
                .onSuccess { response ->
                    mutableWizardState.update { it.copy(lookupState = LookupState.Success(response.results)) }

                    // Show confirmation dialog based on results
                    when {
                        response.results.size == 1 -> {
                            mutableConfirmationState.value = EntityConfirmationState.SingleResult(response.results.first())
                        }
                        response.results.isNotEmpty() -> {
                            mutableConfirmationState.value = EntityConfirmationState.MultipleResults(response.results)
                        }
                        else -> {
                            // No results - proceed to manual entry
                            goToStep(WorkspaceWizardStep.VatAndAddress)
                        }
                    }
                }
                .onFailure { error ->
                    logger.e(error) { "Company lookup failed" }
                    mutableWizardState.update {
                        it.copy(lookupState = LookupState.Error(error.message ?: "Lookup failed"))
                    }
                    // Still allow manual entry
                    goToStep(WorkspaceWizardStep.VatAndAddress)
                }
        }
    }

    fun onEntitySelected(entity: EntityLookup) {
        // Prefill state with entity data and create workspace directly
        mutableWizardState.update { state ->
            state.copy(
                selectedEntity = entity,
                companyName = entity.name,
                vatNumber = entity.vatNumber ?: VatNumber(""),
                address = entity.address?.let { addr ->
                    AddressFormState(
                        streetLine1 = addr.streetLine1,
                        streetLine2 = addr.streetLine2 ?: "",
                        city = addr.city,
                        postalCode = addr.postalCode,
                        country = addr.country,
                    )
                } ?: AddressFormState(),
            )
        }
        dismissConfirmation()
        createWorkspace()
    }

    fun onEnterManually() {
        dismissConfirmation()
        goToStep(WorkspaceWizardStep.VatAndAddress)
    }

    fun dismissConfirmation() {
        mutableConfirmationState.value = EntityConfirmationState.Hidden
    }

    // ===== VAT and Address Step =====

    fun onVatNumberChanged(vatNumber: VatNumber) {
        mutableWizardState.update { it.copy(vatNumber = vatNumber) }
    }

    fun onAddressChanged(address: AddressFormState) {
        mutableWizardState.update { it.copy(address = address) }
    }

    // ===== Navigation =====

    fun goToStep(step: WorkspaceWizardStep) {
        mutableWizardState.update { it.copy(step = step) }
    }

    fun goBack() {
        val state = wizardState.value
        val steps = WorkspaceWizardStep.stepsForType(state.tenantType)
        val currentIndex = steps.indexOf(state.step)
        if (currentIndex > 0) {
            goToStep(steps[currentIndex - 1])
        }
    }

    fun goNext() {
        val state = wizardState.value

        when (state.step) {
            WorkspaceWizardStep.TypeSelection -> {
                val nextStep = if (state.tenantType == TenantType.Company) {
                    WorkspaceWizardStep.CompanyName
                } else {
                    WorkspaceWizardStep.VatAndAddress
                }
                goToStep(nextStep)
            }
            WorkspaceWizardStep.CompanyName -> {
                // Trigger lookup, dialog will handle navigation
                lookupCompany()
            }
            WorkspaceWizardStep.VatAndAddress -> {
                createWorkspace()
            }
        }
    }

    fun canGoBack(): Boolean {
        val state = wizardState.value
        val steps = WorkspaceWizardStep.stepsForType(state.tenantType)
        return steps.indexOf(state.step) > 0
    }

    // ===== Workspace Creation =====

    private fun createWorkspace() {
        val state = wizardState.value

        scope.launch {
            mutableWizardState.update { it.copy(isCreating = true) }
            mutableState.emitLoading()

            // For freelancer, use user's name as legal name
            val effectiveLegalName = if (state.tenantType.legalNameFromUser) {
                LegalName(userName.value)
            } else {
                LegalName(state.companyName)
            }

            // For freelancer, display name equals legal name
            val effectiveDisplayName = if (!state.tenantType.requiresDisplayName) {
                DisplayName(effectiveLegalName.value)
            } else {
                DisplayName(state.companyName)
            }

            val addressRequest = UpsertTenantAddressRequest(
                streetLine1 = state.address.streetLine1,
                streetLine2 = state.address.streetLine2.ifBlank { null },
                city = state.address.city,
                postalCode = state.address.postalCode,
                country = state.address.country,
            )

            authRepository.createTenant(
                type = state.tenantType,
                legalName = effectiveLegalName,
                displayName = effectiveDisplayName,
                plan = TenantPlan.Free,
                language = Language.En,
                vatNumber = state.vatNumber,
                address = addressRequest,
            ).onSuccess {
                mutableWizardState.update { it.copy(isCreating = false) }
                mutableState.emit(Unit)
                mutableEffect.emit(Effect.NavigateHome)
            }.onFailure { error ->
                logger.e(error) { "Failed to create workspace" }
                mutableWizardState.update { it.copy(isCreating = false) }
                mutableState.emit(error) { createWorkspace() }
                mutableEffect.emit(Effect.CreationFailed(error))
            }
        }
    }

    sealed interface Effect {
        data object NavigateHome : Effect
        data class CreationFailed(val error: Throwable) : Effect
    }
}
