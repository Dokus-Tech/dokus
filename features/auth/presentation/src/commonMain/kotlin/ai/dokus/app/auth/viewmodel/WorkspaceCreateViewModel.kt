package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class WorkspaceCreateViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<DokusState<Unit>>(DokusState.idle()) {

    private val logger = Logger.forClass<WorkspaceCreateViewModel>()
    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    private val mutableHasFreelancerWorkspace = MutableStateFlow(false)
    val hasFreelancerWorkspace = mutableHasFreelancerWorkspace.asStateFlow()

    private val mutableUserName = MutableStateFlow("")
    val userName = mutableUserName.asStateFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        scope.launch {
            // Check if the user already has a freelancer workspace
            authRepository.hasFreelancerTenant()
                .onSuccess { hasFreelancer ->
                    mutableHasFreelancerWorkspace.value = hasFreelancer
                }.onFailure { error ->
                    logger.e(error) { "Failed to check freelancer workspace status" }
                }

            // Get a user's name for freelancer autofill
            authRepository.getCurrentUser()
                .onSuccess { user ->
                    val fullName = listOfNotNull(
                        user.firstName?.value,
                        user.lastName?.value
                    ).joinToString(" ")
                    mutableUserName.value = fullName
                }.onFailure { error ->
                    logger.e(error) { "Failed to load user info" }
                }
        }
    }

    fun createWorkspace(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: TenantPlan = TenantPlan.Free,
        language: Language = Language.En,
        vatNumber: VatNumber
    ) {
        scope.launch {
            mutableState.value = DokusState.loading()

            // For freelancer, use the user's name as legal name
            val effectiveLegalName = if (type.legalNameFromUser) {
                LegalName(userName.value)
            } else {
                legalName
            }

            // For freelancer, display name equals legal name
            val effectiveDisplayName = if (!type.requiresDisplayName) {
                DisplayName(effectiveLegalName.value)
            } else {
                displayName
            }

            authRepository.createTenant(
                type = type,
                legalName = effectiveLegalName,
                displayName = effectiveDisplayName,
                plan = plan,
                language = language,
                vatNumber = vatNumber
            ).onSuccess {
                mutableState.value = DokusState.success(Unit)
                mutableEffect.emit(Effect.NavigateHome)
            }.onFailure { error ->
                logger.e(error) { "Failed to create workspace" }
                mutableState.value = DokusState.error(
                    exception = error,
                    retryHandler = { createWorkspace(type, legalName, displayName, plan, language, vatNumber) }
                )
                mutableEffect.emit(Effect.CreationFailed(error))
            }
        }
    }

    sealed interface Effect {
        data object NavigateHome : Effect
        data class CreationFailed(val error: Throwable) : Effect
    }
}
