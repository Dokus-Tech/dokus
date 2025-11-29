package ai.dokus.app.auth.viewmodel

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.domain.enums.Language
import ai.dokus.foundation.domain.enums.TenantPlan
import ai.dokus.foundation.domain.enums.TenantType
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

internal class CompanyCreateViewModel(
    private val authRepository: AuthRepository,
) : BaseViewModel<DokusState<Unit>>(DokusState.idle()) {

    private val logger = Logger.forClass<CompanyCreateViewModel>()
    private val mutableEffect = MutableSharedFlow<Effect>()
    val effect = mutableEffect.asSharedFlow()

    fun createTenant(
        type: TenantType,
        legalName: LegalName,
        displayName: DisplayName,
        plan: TenantPlan = TenantPlan.Free,
        country: Country,
        language: Language = Language.En,
        vatNumber: VatNumber
    ) {
        scope.launch {
            mutableState.value = DokusState.loading()
            runCatching {
                authRepository.createTenant(
                    type = type,
                    legalName = legalName,
                    displayName = displayName,
                    plan = plan,
                    country = country,
                    language = language,
                    vatNumber = vatNumber
                ).getOrThrow()
            }.onSuccess {
                mutableState.value = DokusState.success(Unit)
                mutableEffect.emit(Effect.NavigateHome)
            }.onFailure { error ->
                logger.e(error) { "Failed to create tenant" }
                mutableState.value = DokusState.error(
                    exception = error,
                    retryHandler = { createTenant(type, legalName, displayName, plan, country, language, vatNumber) }
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
