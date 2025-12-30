package tech.dokus.contacts.viewmodel

import ai.dokus.app.contacts.usecases.CreateContactUseCase
import ai.dokus.app.contacts.usecases.ListContactsUseCase
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.Country
import tech.dokus.domain.enums.Language
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.domain.usecases.SearchCompanyUseCase

internal typealias CreateContactCtx = PipelineContext<CreateContactState, CreateContactIntent, CreateContactAction>

/**
 * Container for Create Contact flow using FlowMVI.
 *
 * Implements a VAT-first, split-step approach:
 * 1. LookupStep - Search by company name or VAT number
 * 2. ConfirmStep - Review fetched company data + add billing email
 * 3. ManualStep - Manual business/individual entry
 *
 * Features:
 * - VAT duplicate detection (hard block) - checks local before remote
 * - Debounced name search (300ms), immediate VAT lookup
 * - Soft duplicate warning on manual submit (name+country)
 */
internal class CreateContactContainer(
    private val searchCompanyUseCase: SearchCompanyUseCase,
    private val listContacts: ListContactsUseCase,
    private val createContact: CreateContactUseCase,
) : Container<CreateContactState, CreateContactIntent, CreateContactAction> {

    private val logger = Logger.forClass<CreateContactContainer>()
    private var searchJob: Job? = null

    override val store: Store<CreateContactState, CreateContactIntent, CreateContactAction> =
        store(CreateContactState.LookupStep()) {
            reduce { intent ->
                when (intent) {
                    // Lookup step
                    is CreateContactIntent.Search -> handleSearch(intent.query)
                    is CreateContactIntent.ClearSearch -> handleClearSearch()
                    is CreateContactIntent.SelectResult -> handleSelectResult(intent.entity)
                    is CreateContactIntent.GoToManualEntry -> handleGoToManualEntry()

                    // Confirm step
                    is CreateContactIntent.BillingEmailChanged -> handleBillingEmailChanged(intent.email)
                    is CreateContactIntent.PhoneChanged -> handlePhoneChanged(intent.phone)
                    is CreateContactIntent.LanguageChanged -> handleLanguageChanged(intent.language)
                    is CreateContactIntent.ToggleAddressDetails -> handleToggleAddressDetails()
                    is CreateContactIntent.ConfirmAndCreate -> handleConfirmAndCreate()
                    is CreateContactIntent.BackToLookup -> handleBackToLookup()

                    // Manual step
                    is CreateContactIntent.ManualTypeChanged -> handleManualTypeChanged(intent.type)
                    is CreateContactIntent.ManualFieldChanged -> handleManualFieldChanged(
                        intent.field,
                        intent.value
                    )

                    is CreateContactIntent.ManualCountryChanged -> handleManualCountryChanged(intent.country)
                    is CreateContactIntent.ShowCountryPicker -> handleShowCountryPicker()
                    is CreateContactIntent.HideCountryPicker -> handleHideCountryPicker()
                    is CreateContactIntent.CreateManualContact -> handleCreateManualContact()
                    is CreateContactIntent.ConfirmCreateDespiteDuplicates -> handleConfirmCreateDespiteDuplicates()
                    is CreateContactIntent.DismissSoftDuplicates -> handleDismissSoftDuplicates()
                    is CreateContactIntent.BackFromManual -> handleBackToLookup()

                    // Common
                    is CreateContactIntent.Cancel -> action(CreateContactAction.NavigateBack)
                    is CreateContactIntent.ViewExistingContact -> action(
                        CreateContactAction.NavigateToContact(
                            intent.contactId
                        )
                    )
                }
            }
        }

    // ============================================================================
    // LOOKUP STEP HANDLERS
    // ============================================================================

    /**
     * Handle search request (already debounced by UI).
     * Called from snapshotFlow after debounce delay.
     */
    private suspend fun CreateContactCtx.handleSearch(query: String) {
        // Cancel any previous search job
        searchJob?.cancel()

        withState<CreateContactState.LookupStep, _> {
            // Clear previous duplicate warning
            if (duplicateVat != null) {
                updateState { copy(duplicateVat = null) }
            }

            // Use VatNumber for validation and normalization
            val vatNumber = VatNumber(query)

            if (vatNumber.isValid) {
                // Valid VAT pattern - check local first, then remote
                handleVatQuery(vatNumber.normalized)
            } else {
                // Name search - execute immediately (debounce already happened in UI)
                updateState { copy(lookupState = LookupUiState.Loading) }
                searchJob = launch {
                    searchRemote(query)
                }
            }
        }
    }

    /**
     * Clear search results when query is empty or too short.
     */
    private suspend fun CreateContactCtx.handleClearSearch() {
        searchJob?.cancel()
        withState<CreateContactState.LookupStep, _> {
            updateState { copy(lookupState = LookupUiState.Idle, duplicateVat = null) }
        }
    }

    private suspend fun CreateContactCtx.handleVatQuery(normalizedVat: String) {
        withState<CreateContactState.LookupStep, _> {
            updateState { copy(lookupState = LookupUiState.Loading) }

            // Check local contacts first
            val existingContact = findContactByVat(normalizedVat)
            if (existingContact != null) {
                logger.d { "Found existing contact with VAT: $normalizedVat" }
                updateState {
                    copy(
                        lookupState = LookupUiState.Idle,
                        duplicateVat = DuplicateVatUi(
                            contactId = existingContact.first,
                            displayName = existingContact.second,
                            vatNumber = normalizedVat
                        )
                    )
                }
                return@withState
            }

            // No local match - do remote lookup
            searchRemote(normalizedVat)
        }
    }

    private suspend fun CreateContactCtx.searchRemote(query: String) {
        logger.d { "Searching remote for: $query" }

        searchCompanyUseCase(query).fold(
            onSuccess = { response ->
                logger.d { "Found ${response.results.size} results" }
                updateState {
                    when (this) {
                        is CreateContactState.LookupStep -> copy(
                            lookupState = if (response.results.isEmpty()) {
                                LookupUiState.Empty
                            } else {
                                LookupUiState.Success(response.results)
                            }
                        )

                        else -> this
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Search failed" }
                updateState {
                    when (this) {
                        is CreateContactState.LookupStep -> {
                            val exception = error.asDokusException
                            val displayException = if (exception is DokusException.Unknown) {
                                DokusException.ContactLookupFailed
                            } else {
                                exception
                            }
                            copy(lookupState = LookupUiState.Error(displayException))
                        }

                        else -> this
                    }
                }
            }
        )
    }

    private suspend fun findContactByVat(vatNumber: String): Pair<ContactId, String>? {
        return listContacts(search = vatNumber, limit = 5).fold(
            onSuccess = { contacts ->
                contacts.find { it.vatNumber?.value == vatNumber }?.let { it.id to it.name.value }
            },
            onFailure = { null }
        )
    }

    private suspend fun CreateContactCtx.handleSelectResult(entity: EntityLookup) {
        logger.d { "Selected entity: ${entity.name}" }
        updateState {
            CreateContactState.ConfirmStep(selectedEntity = entity)
        }
    }

    private suspend fun CreateContactCtx.handleGoToManualEntry() {
        logger.d { "Going to manual entry" }
        updateState {
            CreateContactState.ManualStep()
        }
    }

    // ============================================================================
    // CONFIRM STEP HANDLERS
    // ============================================================================

    private suspend fun CreateContactCtx.handleBillingEmailChanged(email: String) {
        withState<CreateContactState.ConfirmStep, _> {
            val error = if (email.isNotBlank() && !email.contains("@")) {
                DokusException.Validation.InvalidEmail
            } else {
                null
            }
            updateState { copy(billingEmail = email, emailError = error) }
        }
    }

    private suspend fun CreateContactCtx.handlePhoneChanged(phone: String) {
        withState<CreateContactState.ConfirmStep, _> {
            updateState { copy(phone = phone) }
        }
    }

    private suspend fun CreateContactCtx.handleLanguageChanged(language: Language?) {
        withState<CreateContactState.ConfirmStep, _> {
            updateState { copy(language = language) }
        }
    }

    private suspend fun CreateContactCtx.handleToggleAddressDetails() {
        withState<CreateContactState.ConfirmStep, _> {
            updateState { copy(showAddressDetails = !showAddressDetails) }
        }
    }

    private suspend fun CreateContactCtx.handleConfirmAndCreate() {
        withState<CreateContactState.ConfirmStep, _> {
            // Validate email format only if provided
            if (billingEmail.isNotBlank() && !billingEmail.contains("@")) {
                updateState { copy(emailError = DokusException.Validation.InvalidEmail) }
                return@withState
            }

            updateState { copy(isSubmitting = true) }

            val entity = selectedEntity
            val request = CreateContactRequest(
                name = entity.name,
                email = billingEmail,
                phone = phone.takeIf { it.isNotBlank() },
                vatNumber = entity.vatNumber?.value,
                businessType = ClientType.Business,
                addressLine1 = entity.address?.streetLine1,
                addressLine2 = entity.address?.streetLine2,
                city = entity.address?.city,
                postalCode = entity.address?.postalCode,
                country = entity.address?.country?.dbValue,
            )

            createContact(request).fold(
                onSuccess = { contact ->
                    logger.i { "Contact created from lookup: ${contact.id}" }
                    action(CreateContactAction.ContactCreated(contact.id, contact.name.value))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create contact" }
                    updateState { copy(isSubmitting = false) }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.ContactCreateFailed
                    } else {
                        exception
                    }
                    action(CreateContactAction.ShowError(displayException))
                }
            )
        }
    }

    private suspend fun CreateContactCtx.handleBackToLookup() {
        updateState { CreateContactState.LookupStep() }
    }

    // ============================================================================
    // MANUAL STEP HANDLERS
    // ============================================================================

    private suspend fun CreateContactCtx.handleManualTypeChanged(type: ClientType) {
        withState<CreateContactState.ManualStep, _> {
            updateState { copy(contactType = type) }
        }
    }

    private suspend fun CreateContactCtx.handleManualFieldChanged(field: String, value: String) {
        withState<CreateContactState.ManualStep, _> {
            val newFormData = when (field) {
                "companyName" -> formData.copy(
                    companyName = value,
                    errors = formData.errors - "companyName"
                )

                "vatNumber" -> formData.copy(vatNumber = value)
                "email" -> formData.copy(email = value)
                "fullName" -> formData.copy(fullName = value, errors = formData.errors - "fullName")
                "personEmail" -> formData.copy(
                    personEmail = value,
                    errors = formData.errors - "contact"
                )

                "personPhone" -> formData.copy(
                    personPhone = value,
                    errors = formData.errors - "contact"
                )

                else -> formData
            }
            updateState { copy(formData = newFormData) }
        }
    }

    private suspend fun CreateContactCtx.handleManualCountryChanged(country: Country) {
        withState<CreateContactState.ManualStep, _> {
            updateState {
                copy(
                    formData = formData.copy(country = country),
                    showCountryPicker = false
                )
            }
        }
    }

    private suspend fun CreateContactCtx.handleShowCountryPicker() {
        withState<CreateContactState.ManualStep, _> {
            updateState { copy(showCountryPicker = true) }
        }
    }

    private suspend fun CreateContactCtx.handleHideCountryPicker() {
        withState<CreateContactState.ManualStep, _> {
            updateState { copy(showCountryPicker = false) }
        }
    }

    private suspend fun CreateContactCtx.handleCreateManualContact() {
        withState<CreateContactState.ManualStep, _> {
            // Validate form
            val errors = validateManualForm(contactType, formData)
            if (errors.isNotEmpty()) {
                updateState { copy(formData = formData.copy(errors = errors)) }
                return@withState
            }

            // Check for soft duplicates (name + country)
            val duplicates = findSoftDuplicates(contactType, formData)
            if (duplicates.isNotEmpty() && softDuplicates == null) {
                logger.d { "Found ${duplicates.size} potential duplicates" }
                updateState { copy(softDuplicates = duplicates) }
                return@withState
            }

            // Proceed with creation
            createManualContact()
        }
    }

    private suspend fun CreateContactCtx.handleConfirmCreateDespiteDuplicates() {
        withState<CreateContactState.ManualStep, _> {
            updateState { copy(softDuplicates = null) }
            createManualContact()
        }
    }

    private suspend fun CreateContactCtx.handleDismissSoftDuplicates() {
        withState<CreateContactState.ManualStep, _> {
            updateState { copy(softDuplicates = null) }
        }
    }

    private suspend fun CreateContactCtx.createManualContact() {
        withState<CreateContactState.ManualStep, _> {
            updateState { copy(isSubmitting = true) }

            val request = if (contactType == ClientType.Business) {
                CreateContactRequest(
                    name = formData.companyName,
                    email = formData.email.takeIf { it.isNotBlank() },
                    vatNumber = formData.vatNumber.takeIf { it.isNotBlank() },
                    businessType = ClientType.Business,
                    country = formData.country.dbValue,
                )
            } else {
                CreateContactRequest(
                    name = formData.fullName,
                    email = formData.personEmail.takeIf { it.isNotBlank() },
                    phone = formData.personPhone.takeIf { it.isNotBlank() },
                    businessType = ClientType.Individual,
                )
            }

            createContact(request).fold(
                onSuccess = { contact ->
                    logger.i { "Contact created manually: ${contact.id}" }
                    action(CreateContactAction.ContactCreated(contact.id, contact.name.value))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create contact" }
                    updateState { copy(isSubmitting = false) }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.ContactCreateFailed
                    } else {
                        exception
                    }
                    action(CreateContactAction.ShowError(displayException))
                }
            )
        }
    }

    // ============================================================================
    // VALIDATION HELPERS
    // ============================================================================

    private suspend fun validateManualForm(
        type: ClientType,
        data: ManualContactFormData
    ): Map<String, DokusException> {
        val errors = mutableMapOf<String, DokusException>()

        if (type == ClientType.Business) {
            if (data.companyName.isBlank()) {
                errors["companyName"] = DokusException.Validation.CompanyNameRequired
            }
        } else {
            if (data.fullName.isBlank()) {
                errors["fullName"] = DokusException.Validation.FullNameRequired
            }
            if (data.personEmail.isBlank() && data.personPhone.isBlank()) {
                errors["contact"] = DokusException.Validation.ContactEmailOrPhoneRequired
            }
        }

        return errors
    }

    private suspend fun findSoftDuplicates(
        type: ClientType,
        data: ManualContactFormData
    ): List<SoftDuplicateUi> {
        val name = if (type == ClientType.Business) data.companyName else data.fullName
        if (name.length < 3) return emptyList()

        return listContacts(search = name, limit = 5).fold(
            onSuccess = { contacts ->
                contacts
                    .filter { contact ->
                        contact.name.value.equals(name, ignoreCase = true) &&
                                (type != ClientType.Business || contact.country == data.country.dbValue)
                    }
                    .map { contact ->
                        SoftDuplicateUi(
                            contactId = contact.id,
                            displayName = contact.name.value,
                            matchReason = if (type == ClientType.Business) {
                                SoftDuplicateReason.NameAndCountry
                            } else {
                                SoftDuplicateReason.Name
                            }
                        )
                    }
            },
            onFailure = { emptyList() }
        )
    }

}
