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
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.auth.CreateFirmRequest
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.auth.presentation.auth.model.AddressFormState
import tech.dokus.features.auth.presentation.auth.model.LookupState
import tech.dokus.features.auth.presentation.auth.model.WorkspaceCreateType
import tech.dokus.features.auth.presentation.auth.model.WorkspaceWizardStep
import tech.dokus.features.auth.usecases.CreateFirmUseCase
import tech.dokus.features.auth.usecases.CreateTenantUseCase
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.HasFreelancerTenantUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

typealias WorkspaceCreateCtx = PipelineContext<WorkspaceCreateState, WorkspaceCreateIntent, WorkspaceCreateAction>

/**
 * Container for Workspace Creation wizard using FlowMVI.
 * Manages the multistep workspace creation flow.
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class WorkspaceCreateContainer(
    private val hasFreelancerTenant: HasFreelancerTenantUseCase,
    private val getCurrentUser: GetCurrentUserUseCase,
    private val createTenant: CreateTenantUseCase,
    private val createFirm: CreateFirmUseCase,
    private val searchCompanyUseCase: SearchCompanyUseCase,
) : Container<WorkspaceCreateState, WorkspaceCreateIntent, WorkspaceCreateAction> {

    private val logger = Logger.forClass<WorkspaceCreateContainer>()

    override val store: Store<WorkspaceCreateState, WorkspaceCreateIntent, WorkspaceCreateAction> =
        store(WorkspaceCreateState.initial) {
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
                    is WorkspaceCreateIntent.UpdateVatNumber -> handleUpdateVatNumber(intent.vatNumber)
                    is WorkspaceCreateIntent.UpdateAddress -> handleUpdateAddress(intent.address)
                    is WorkspaceCreateIntent.NextClicked -> handleNext()
                    is WorkspaceCreateIntent.BackClicked -> handleBack()
                    is WorkspaceCreateIntent.RetryCreation -> handleRetryCreation()
                }
            }
        }

    private suspend fun WorkspaceCreateCtx.handleLoadUserInfo() {
        updateState { copy(userInfo = userInfo.asLoading, error = null) }

        logger.d { "Loading user info for workspace creation" }

        // First check if user has freelancer workspace
        val hasFreelancer = hasFreelancerTenant().fold(
            onSuccess = { it },
            onFailure = { error ->
                logger.e(error) { "Failed to check freelancer workspace status" }
                updateState {
                    copy(
                        userInfo = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(WorkspaceCreateIntent.LoadUserInfo) }
                        )
                    )
                }
                return
            }
        )

        // Then get user name
        val userName = getCurrentUser().fold(
            onSuccess = { user ->
                listOfNotNull(
                    user.firstName?.value,
                    user.lastName?.value
                ).joinToString(" ")
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load user info" }
                updateState {
                    copy(
                        userInfo = DokusState.error(
                            exception = error.asDokusException,
                            retryHandler = { intent(WorkspaceCreateIntent.LoadUserInfo) }
                        )
                    )
                }
                return
            }
        )

        logger.i { "User info loaded: hasFreelancer=$hasFreelancer" }

        updateState {
            copy(
                userInfo = DokusState.success(
                    WorkspaceCreateUserInfo(
                        hasFreelancerWorkspace = hasFreelancer,
                        userName = userName,
                    )
                ),
                workspaceType = WorkspaceCreateType.Company,
            )
        }
    }

    private suspend fun WorkspaceCreateCtx.handleSelectType(type: WorkspaceCreateType) {
        updateState { copy(workspaceType = type) }
    }

    private suspend fun WorkspaceCreateCtx.handleUpdateCompanyName(name: LegalName) {
        updateState {
            copy(
                companyName = name,
                lookupState = LookupState.Idle
            )
        }
    }

    private suspend fun WorkspaceCreateCtx.handleLookupCompany() {
        withState {
            if (!companyName.isValid) return@withState

            updateState { copy(lookupState = LookupState.Loading) }

            logger.d { "Looking up company: $companyName" }
            searchCompanyUseCase(companyName, null).fold(
                onSuccess = { response ->
                    logger.d { "Company lookup returned ${response.results.size} results" }
                    updateState {
                        copy(
                            lookupState = LookupState.Success(response.results),
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Company lookup failed" }
                    updateState {
                        copy(
                            lookupState = LookupState.Error(DokusException.CompanyLookupFailed),
                        )
                    }
                }
            )
        }
    }

    private suspend fun WorkspaceCreateCtx.handleSelectEntity(entity: EntityLookup) {
        withState {
            val newAddress = entity.address?.let { address ->
                AddressFormState(
                    streetLine1 = address.streetLine1,
                    streetLine2 = address.streetLine2 ?: "",
                    city = address.city,
                    postalCode = address.postalCode,
                    country = address.country,
                )
            } ?: address

            updateState {
                copy(
                    selectedEntity = entity,
                    companyName = entity.name,
                    vatNumber = entity.vatNumber,
                    address = newAddress,
                )
            }

            // Create workspace directly with selected entity
            createWorkspace()
        }
    }

    private suspend fun WorkspaceCreateCtx.handleEnterManually() {
        updateState {
            copy(step = WorkspaceWizardStep.VatAndAddress)
        }
    }

    private suspend fun WorkspaceCreateCtx.handleUpdateVatNumber(vatNumber: VatNumber) {
        updateState { copy(vatNumber = vatNumber) }
    }

    private suspend fun WorkspaceCreateCtx.handleUpdateAddress(address: AddressFormState) {
        updateState { copy(address = address) }
    }

    private suspend fun WorkspaceCreateCtx.handleNext() {
        withState {
            when (step) {
                WorkspaceWizardStep.TypeSelection -> {
                    val nextStep = if (workspaceType == WorkspaceCreateType.Freelancer) {
                        WorkspaceWizardStep.VatAndAddress
                    } else {
                        WorkspaceWizardStep.CompanyName
                    }
                    updateState { copy(step = nextStep) }
                }

                WorkspaceWizardStep.CompanyName -> {
                    handleLookupCompany()
                }

                WorkspaceWizardStep.VatAndAddress -> {
                    createWorkspace()
                }
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleBack() {
        withState {
            val steps = WorkspaceWizardStep.stepsForType(workspaceType)
            val currentIndex = steps.indexOf(step)
            if (currentIndex > 0) {
                updateState { copy(step = steps[currentIndex - 1]) }
            } else {
                action(WorkspaceCreateAction.NavigateBack)
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.handleRetryCreation() {
        withState {
            if (error != null) {
                updateState { copy(error = null) }
                createWorkspace()
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.createWorkspace() {
        withState {
            logger.d { "Creating workspace: type=$workspaceType" }

            updateState { copy(isCreating = true, error = null) }

            when (workspaceType) {
                WorkspaceCreateType.Company,
                WorkspaceCreateType.Freelancer,
                -> createTenantWorkspace()

                WorkspaceCreateType.Bookkeeper -> createBookkeeperWorkspace()
            }
        }
    }

    private suspend fun WorkspaceCreateCtx.createTenantWorkspace() {
        withState {
            val tenantType = when (workspaceType) {
                WorkspaceCreateType.Company -> TenantType.Company
                WorkspaceCreateType.Freelancer -> TenantType.Freelancer
                WorkspaceCreateType.Bookkeeper -> return@withState
            }

            // For freelancer, use user's name as legal name.
            val effectiveLegalName = if (tenantType.legalNameFromUser) {
                LegalName(userName)
            } else {
                companyName
            }

            // For freelancer, display name equals legal name.
            val effectiveDisplayName = if (!tenantType.requiresDisplayName) {
                DisplayName(effectiveLegalName.value)
            } else {
                DisplayName(companyName.value)
            }

            val addressRequest = UpsertTenantAddressRequest(
                streetLine1 = address.streetLine1,
                streetLine2 = address.streetLine2.ifBlank { null },
                city = address.city,
                postalCode = address.postalCode,
                country = address.country,
            )

            createTenant(
                type = tenantType,
                legalName = effectiveLegalName,
                displayName = effectiveDisplayName,
                plan = SubscriptionTier.default,
                language = Language.En,
                vatNumber = vatNumber,
                address = addressRequest,
            ).fold(
                onSuccess = { tenant ->
                    logger.i { "Workspace created successfully: ${tenant.id}" }
                    action(WorkspaceCreateAction.NavigateHome)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create workspace" }
                    handleCreationFailure(error)
                }
            )
        }
    }

    private suspend fun WorkspaceCreateCtx.createBookkeeperWorkspace() {
        withState {
            createFirm(
                CreateFirmRequest(
                    name = DisplayName(companyName.value),
                    vatNumber = vatNumber,
                ),
            ).fold(
                onSuccess = { firm ->
                    logger.i { "Bookkeeper workspace created successfully: ${firm.id}" }
                    action(WorkspaceCreateAction.NavigateToBookkeeperConsole(firm.id))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create bookkeeper workspace" }
                    handleCreationFailure(error)
                },
            )
        }
    }

    private suspend fun WorkspaceCreateCtx.handleCreationFailure(error: Throwable) {
        val exception = error.asDokusException
        val displayException = if (exception is DokusException.Unknown) {
            DokusException.WorkspaceCreateFailed
        } else {
            exception
        }
        action(WorkspaceCreateAction.ShowCreationError(displayException))
        updateState {
            copy(
                isCreating = false,
                error = exception,
            )
        }
    }
}
