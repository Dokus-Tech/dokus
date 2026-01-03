@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for constants (Kotlin convention)

package tech.dokus.features.auth.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.TenantPlan
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.auth.presentation.auth.model.AddressFormState
import tech.dokus.features.auth.presentation.auth.model.EntityConfirmationState
import tech.dokus.features.auth.presentation.auth.model.LookupState
import tech.dokus.features.auth.presentation.auth.model.WorkspaceWizardStep
import tech.dokus.features.auth.repository.AuthRepository
import tech.dokus.foundation.platform.Logger

private const val MinCompanyNameLength = 3

internal typealias WorkspaceCreateCtx =
    PipelineContext<WorkspaceCreateState, WorkspaceCreateIntent, WorkspaceCreateAction>

/**
 * Container for Workspace Creation wizard using FlowMVI.
 * Manages the multi-step workspace creation flow.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class WorkspaceCreateContainer(
    private val authRepository: AuthRepository,
    private val searchCompanyUseCase: SearchCompanyUseCase,
) : Container<WorkspaceCreateState, WorkspaceCreateIntent, WorkspaceCreateAction> {

    private val logger = Logger.forClass<WorkspaceCreateContainer>()

    override val store: Store<WorkspaceCreateState, WorkspaceCreateIntent, WorkspaceCreateAction> =
        store(WorkspaceCreateState.Loading) {
            init {
                handleLoadUserInfo()
            }
            reduce { intent ->
                when (intent) {
                    is WorkspaceCreateIntent.LoadUserInfo -> handleLoadUserInfo()
                    is WorkspaceCreateIntent.SelectType -> handleSelectType(intent.type)
                    is WorkspaceCreateIntent.UpdateCompanyName -> handleUpdateCompanyName(intent.name)
                    is WorkspaceCreateIntent.LookupCompany -> handleLookupCompany()
                    is WorkspaceCreateIntent.SelectEntity -> handleSelectEntity(intent.entity)
                    is WorkspaceCreateIntent.EnterManually -> handleEnterManually()
                    is WorkspaceCreateIntent.DismissConfirmation -> handleDismissConfirmation()
                    is WorkspaceCreateIntent.UpdateVatNumber -> handleUpdateVatNumber(intent.vatNumber)
                    is WorkspaceCreateIntent.UpdateAddress -> handleUpdateAddress(intent.address)
                    is WorkspaceCreateIntent.NextClicked -> handleNext()
                    is WorkspaceCreateIntent.BackClicked -> handleBack()
                    is WorkspaceCreateIntent.RetryCreation -> handleRetryCreation()
                }
            }
        }

    private suspend fun WorkspaceCreateCtx.handleLoadUserInfo() {
        updateState { WorkspaceCreateState.Loading }

        logger.d { "Loading user info for workspace creation" }

        // First check if user has freelancer workspace
        val hasFreelancer = authRepository.hasFreelancerTenant().fold(
            onSuccess = { it },
            onFailure = { error ->
                logger.e(error) { "Failed to check freelancer workspace status" }
                updateState {
                    WorkspaceCreateState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(WorkspaceCreateIntent.LoadUserInfo) }
                    )
                }
                return
            }
        )

        // Then get user name
        val userName = authRepository.getCurrentUser().fold(
            onSuccess = { user ->
                listOfNotNull(
                    user.firstName?.value,
                    user.lastName?.value
                ).joinToString(" ")
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load user info" }
                updateState {
                    WorkspaceCreateState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(WorkspaceCreateIntent.LoadUserInfo) }
                    )
                }
                return
            }
        )

        logger.i { "User info loaded: hasFreelancer=$hasFreelancer" }

        updateState {
            WorkspaceCreateState.Wizard(
                hasFreelancerWorkspace = hasFreelancer,
                userName = userName,
                // If user has freelancer, default to Company
                tenantType = if (hasFreelancer) TenantType.Company else TenantType.Company
            )
        }
    }

    private suspend fun WorkspaceCreateCtx.handleSelectType(type: TenantType) {
        withState<WorkspaceCreateState.Wizard, _> {
            updateState { copy(tenantType = type) }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleUpdateCompanyName(name: String) {
        withState<WorkspaceCreateState.Wizard, _> {
            updateState {
                copy(
                    companyName = name,
                    lookupState = LookupState.Idle
                )
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleLookupCompany() {
        withState<WorkspaceCreateState.Wizard, _> {
            val name = companyName.trim()
            if (name.length < MinCompanyNameLength) return@withState

            val currentState = this

            updateState { copy(lookupState = LookupState.Loading) }

            logger.d { "Looking up company: $name" }
            searchCompanyUseCase(name).fold(
                onSuccess = { response ->
                    logger.d { "Company lookup returned ${response.results.size} results" }
                    updateState {
                        val newConfirmation = when {
                            response.results.size == 1 -> EntityConfirmationState.SingleResult(response.results.first())
                            response.results.isNotEmpty() -> EntityConfirmationState.MultipleResults(response.results)
                            else -> EntityConfirmationState.Hidden
                        }
                        copy(
                            lookupState = LookupState.Success(response.results),
                            confirmationState = newConfirmation
                        )
                    }

                    // If no results, proceed to manual entry
                    if (response.results.isEmpty()) {
                        updateState {
                            copy(step = WorkspaceWizardStep.VatAndAddress)
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Company lookup failed" }
                    updateState {
                        copy(
                            lookupState = LookupState.Error(DokusException.CompanyLookupFailed),
                            // Still allow manual entry
                            step = WorkspaceWizardStep.VatAndAddress
                        )
                    }
                }
            )
        }
    }

    private suspend fun WorkspaceCreateCtx.handleSelectEntity(entity: EntityLookup) {
        withState<WorkspaceCreateState.Wizard, _> {
            // Prefill state with entity data
            val currentState = this
            val newAddress = entity.address?.let { addr ->
                AddressFormState(
                    streetLine1 = addr.streetLine1,
                    streetLine2 = addr.streetLine2 ?: "",
                    city = addr.city,
                    postalCode = addr.postalCode,
                    country = addr.country,
                )
            } ?: address

            updateState {
                copy(
                    selectedEntity = entity,
                    companyName = entity.name,
                    vatNumber = entity.vatNumber ?: VatNumber(""),
                    address = newAddress,
                    confirmationState = EntityConfirmationState.Hidden
                )
            }

            // Create workspace directly with selected entity
            createWorkspace()
        }
    }

    private suspend fun WorkspaceCreateCtx.handleEnterManually() {
        withState<WorkspaceCreateState.Wizard, _> {
            updateState {
                copy(
                    confirmationState = EntityConfirmationState.Hidden,
                    step = WorkspaceWizardStep.VatAndAddress
                )
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleDismissConfirmation() {
        withState<WorkspaceCreateState.Wizard, _> {
            updateState { copy(confirmationState = EntityConfirmationState.Hidden) }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleUpdateVatNumber(vatNumber: VatNumber) {
        withState<WorkspaceCreateState.Wizard, _> {
            updateState { copy(vatNumber = vatNumber) }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleUpdateAddress(address: AddressFormState) {
        withState<WorkspaceCreateState.Wizard, _> {
            updateState { copy(address = address) }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleNext() {
        withState<WorkspaceCreateState.Wizard, _> {
            when (step) {
                WorkspaceWizardStep.TypeSelection -> {
                    val nextStep = if (tenantType == TenantType.Company) {
                        WorkspaceWizardStep.CompanyName
                    } else {
                        WorkspaceWizardStep.VatAndAddress
                    }
                    updateState { copy(step = nextStep) }
                }
                WorkspaceWizardStep.CompanyName -> {
                    // Trigger lookup, dialog will handle navigation
                    handleLookupCompany()
                }
                WorkspaceWizardStep.VatAndAddress -> {
                    createWorkspace()
                }
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleBack() {
        withState<WorkspaceCreateState.Wizard, _> {
            val steps = WorkspaceWizardStep.stepsForType(tenantType)
            val currentIndex = steps.indexOf(step)
            if (currentIndex > 0) {
                updateState { copy(step = steps[currentIndex - 1]) }
            } else {
                action(WorkspaceCreateAction.NavigateBack)
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleRetryCreation() {
        withState<WorkspaceCreateState.Error, _> {
            val previousState = previousWizardState
            if (previousState != null) {
                updateState { previousState }
                createWorkspace()
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.createWorkspace() {
        withState<WorkspaceCreateState.Wizard, _> {
            val currentState = this

            logger.d { "Creating workspace: type=$tenantType" }

            updateState {
                WorkspaceCreateState.Creating(
                    tenantType = tenantType,
                    companyName = companyName,
                    userName = userName,
                    vatNumber = vatNumber,
                    address = address,
                    selectedEntity = selectedEntity
                )
            }

            // For freelancer, use user's name as legal name
            val effectiveLegalName = if (currentState.tenantType.legalNameFromUser) {
                LegalName(currentState.userName)
            } else {
                LegalName(currentState.companyName)
            }

            // For freelancer, display name equals legal name
            val effectiveDisplayName = if (!currentState.tenantType.requiresDisplayName) {
                DisplayName(effectiveLegalName.value)
            } else {
                DisplayName(currentState.companyName)
            }

            val addressRequest = UpsertTenantAddressRequest(
                streetLine1 = currentState.address.streetLine1,
                streetLine2 = currentState.address.streetLine2.ifBlank { null },
                city = currentState.address.city,
                postalCode = currentState.address.postalCode,
                country = currentState.address.country,
            )

            authRepository.createTenant(
                type = currentState.tenantType,
                legalName = effectiveLegalName,
                displayName = effectiveDisplayName,
                plan = TenantPlan.Free,
                language = Language.En,
                vatNumber = currentState.vatNumber,
                address = addressRequest,
            ).fold(
                onSuccess = { tenant ->
                    logger.i { "Workspace created successfully: ${tenant.id}" }
                    action(WorkspaceCreateAction.NavigateHome)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create workspace" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.WorkspaceCreateFailed
                    } else {
                        exception
                    }
                    action(
                        WorkspaceCreateAction.ShowCreationError(
                            displayException
                        )
                    )
                    updateState {
                        WorkspaceCreateState.Error(
                            exception = error.asDokusException,
                            retryHandler = { intent(WorkspaceCreateIntent.RetryCreation) },
                            previousWizardState = currentState
                        )
                    }
                }
            )
        }
    }
}
