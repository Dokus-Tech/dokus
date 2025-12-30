package ai.dokus.app.contacts.viewmodel

import ai.dokus.app.contacts.usecases.CreateContactUseCase
import ai.dokus.app.contacts.usecases.DeleteContactUseCase
import ai.dokus.app.contacts.usecases.GetContactUseCase
import ai.dokus.app.contacts.usecases.ListContactsUseCase
import ai.dokus.app.contacts.usecases.UpdateContactUseCase
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_create_success
import ai.dokus.app.resources.generated.contacts_delete_failed
import ai.dokus.app.resources.generated.contacts_delete_success
import ai.dokus.app.resources.generated.contacts_invalid_email
import ai.dokus.app.resources.generated.contacts_invalid_peppol_id
import ai.dokus.app.resources.generated.contacts_name_required
import ai.dokus.app.resources.generated.contacts_peppol_id_required
import ai.dokus.app.resources.generated.contacts_update_success
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.CreateContactRequest
import tech.dokus.domain.model.contact.UpdateContactRequest
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce

internal typealias ContactFormCtx = PipelineContext<ContactFormState, ContactFormIntent, ContactFormAction>

/**
 * Container for the Contact Form screen (Create/Edit) using FlowMVI.
 *
 * Manages form state, validation, duplicate detection, and CRUD operations.
 *
 * Features:
 * - Form field validation (name required, email format, Peppol ID format)
 * - Duplicate detection with debouncing
 * - Country and business type pickers
 * - Delete confirmation dialog
 * - Create and update contact operations
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class ContactFormContainer(
    contactId: ContactId?,
    private val getContact: GetContactUseCase,
    private val listContacts: ListContactsUseCase,
    private val createContact: CreateContactUseCase,
    private val updateContact: UpdateContactUseCase,
    private val deleteContact: DeleteContactUseCase,
) : Container<ContactFormState, ContactFormIntent, ContactFormAction> {

    companion object {
        data class Params(
            val contactId: ContactId?
        )

        private const val DUPLICATE_CHECK_DEBOUNCE_MS = 300L
    }

    private val logger = Logger.forClass<ContactFormContainer>()
    private var duplicateCheckJob: Job? = null

    override val store: Store<ContactFormState, ContactFormIntent, ContactFormAction> =
        store(
            if (contactId != null) {
                ContactFormState.LoadingContact(contactId)
            } else {
                ContactFormState.Editing()
            }
        ) {
            reduce { intent ->
                when (intent) {
                    // Initialization
                    is ContactFormIntent.InitForCreate -> handleInitForCreate()
                    is ContactFormIntent.InitForEdit -> handleInitForEdit(intent.contactId)

                    // Basic Info Field Updates
                    is ContactFormIntent.UpdateName -> handleUpdateName(intent.value)
                    is ContactFormIntent.UpdateEmail -> handleUpdateEmail(intent.value)
                    is ContactFormIntent.UpdatePhone -> handleUpdatePhone(intent.value)
                    is ContactFormIntent.UpdateContactPerson -> handleUpdateContactPerson(intent.value)

                    // Business Info Field Updates
                    is ContactFormIntent.UpdateVatNumber -> handleUpdateVatNumber(intent.value)
                    is ContactFormIntent.UpdateCompanyNumber -> handleUpdateCompanyNumber(intent.value)
                    is ContactFormIntent.UpdateBusinessType -> handleUpdateBusinessType(intent.value)

                    // Address Field Updates
                    is ContactFormIntent.UpdateAddressLine1 -> handleUpdateAddressLine1(intent.value)
                    is ContactFormIntent.UpdateAddressLine2 -> handleUpdateAddressLine2(intent.value)
                    is ContactFormIntent.UpdateCity -> handleUpdateCity(intent.value)
                    is ContactFormIntent.UpdatePostalCode -> handleUpdatePostalCode(intent.value)
                    is ContactFormIntent.UpdateCountry -> handleUpdateCountry(intent.value)

                    // Peppol Field Updates
                    is ContactFormIntent.UpdatePeppolId -> handleUpdatePeppolId(intent.value)
                    is ContactFormIntent.UpdatePeppolEnabled -> handleUpdatePeppolEnabled(intent.value)

                    // Defaults Field Updates
                    is ContactFormIntent.UpdateDefaultPaymentTerms -> handleUpdateDefaultPaymentTerms(intent.value)
                    is ContactFormIntent.UpdateDefaultVatRate -> handleUpdateDefaultVatRate(intent.value)

                    // Tags and Notes Field Updates
                    is ContactFormIntent.UpdateTags -> handleUpdateTags(intent.value)
                    is ContactFormIntent.UpdateInitialNote -> handleUpdateInitialNote(intent.value)

                    // Status Field Updates
                    is ContactFormIntent.UpdateIsActive -> handleUpdateIsActive(intent.value)

                    // Country Picker
                    is ContactFormIntent.ShowCountryPicker -> handleShowCountryPicker()
                    is ContactFormIntent.HideCountryPicker -> handleHideCountryPicker()
                    is ContactFormIntent.UpdateCountrySearchQuery -> handleUpdateCountrySearchQuery(intent.value)

                    // Business Type Picker
                    is ContactFormIntent.ShowBusinessTypePicker -> handleShowBusinessTypePicker()
                    is ContactFormIntent.HideBusinessTypePicker -> handleHideBusinessTypePicker()

                    // Delete Confirmation
                    is ContactFormIntent.ShowDeleteConfirmation -> handleShowDeleteConfirmation()
                    is ContactFormIntent.HideDeleteConfirmation -> handleHideDeleteConfirmation()

                    // Duplicate Detection
                    is ContactFormIntent.DismissDuplicateWarnings -> handleDismissDuplicateWarnings()

                    // Form Actions
                    is ContactFormIntent.Save -> handleSave()
                    is ContactFormIntent.Delete -> handleDelete()
                    is ContactFormIntent.Cancel -> action(ContactFormAction.NavigateBack)
                }
            }
        }

    // ============================================================================
    // INITIALIZATION HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleInitForCreate() {
        logger.d { "Initializing form for create" }
        updateState { ContactFormState.Editing() }
    }

    private suspend fun ContactFormCtx.handleInitForEdit(contactId: ContactId) {
        logger.d { "Initializing form for edit: $contactId" }

        updateState { ContactFormState.LoadingContact(contactId) }

        getContact(contactId).fold(
            onSuccess = { contact ->
                logger.i { "Loaded contact for editing: ${contact.name}" }
                updateState {
                    ContactFormState.Editing(
                        contactId = contactId,
                        originalContact = contact,
                        formData = contact.toFormData()
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load contact for editing" }
                updateState {
                    ContactFormState.Error(
                        contactId = contactId,
                        formData = ContactFormData(),
                        exception = error.asDokusException,
                        retryHandler = { intent(ContactFormIntent.InitForEdit(contactId)) }
                    )
                }
            }
        )
    }

    // ============================================================================
    // BASIC INFO FIELD HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleUpdateName(value: String) {
        updateFormData { copy(name = value, errors = errors - "name") }
        checkDuplicatesDebounced()
    }

    private suspend fun ContactFormCtx.handleUpdateEmail(value: String) {
        updateFormData {
            val errors = if (value.isNotBlank() && !value.contains("@")) {
                errors + ("email" to getString(Res.string.contacts_invalid_email))
            } else {
                errors - "email"
            }
            copy(email = value, errors = errors)
        }
        checkDuplicatesDebounced()
    }

    private suspend fun ContactFormCtx.handleUpdatePhone(value: String) {
        updateFormData { copy(phone = value) }
    }

    private suspend fun ContactFormCtx.handleUpdateContactPerson(value: String) {
        updateFormData { copy(contactPerson = value) }
    }

    // ============================================================================
    // BUSINESS INFO FIELD HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleUpdateVatNumber(value: String) {
        updateFormData { copy(vatNumber = value, errors = errors - "vatNumber") }
        checkDuplicatesDebounced()
    }

    private suspend fun ContactFormCtx.handleUpdateCompanyNumber(value: String) {
        updateFormData { copy(companyNumber = value) }
    }

    private suspend fun ContactFormCtx.handleUpdateBusinessType(value: ClientType) {
        updateFormData { copy(businessType = value) }
        handleHideBusinessTypePicker()
    }

    // ============================================================================
    // ADDRESS FIELD HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleUpdateAddressLine1(value: String) {
        updateFormData { copy(addressLine1 = value) }
    }

    private suspend fun ContactFormCtx.handleUpdateAddressLine2(value: String) {
        updateFormData { copy(addressLine2 = value) }
    }

    private suspend fun ContactFormCtx.handleUpdateCity(value: String) {
        updateFormData { copy(city = value) }
    }

    private suspend fun ContactFormCtx.handleUpdatePostalCode(value: String) {
        updateFormData { copy(postalCode = value) }
    }

    private suspend fun ContactFormCtx.handleUpdateCountry(value: String) {
        updateFormData { copy(country = value) }
        handleHideCountryPicker()
        checkDuplicatesDebounced()
    }

    // ============================================================================
    // PEPPOL FIELD HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleUpdatePeppolId(value: String) {
        updateFormData {
            val errors = if (value.isNotBlank() && !value.contains(":")) {
                errors + ("peppolId" to getString(Res.string.contacts_invalid_peppol_id))
            } else {
                errors - "peppolId"
            }
            copy(peppolId = value, errors = errors)
        }
    }

    private suspend fun ContactFormCtx.handleUpdatePeppolEnabled(value: Boolean) {
        updateFormData { copy(peppolEnabled = value) }
    }

    // ============================================================================
    // DEFAULTS FIELD HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleUpdateDefaultPaymentTerms(value: Int) {
        updateFormData { copy(defaultPaymentTerms = value.coerceIn(0, 365)) }
    }

    private suspend fun ContactFormCtx.handleUpdateDefaultVatRate(value: String) {
        updateFormData { copy(defaultVatRate = value) }
    }

    // ============================================================================
    // TAGS AND NOTES FIELD HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleUpdateTags(value: String) {
        updateFormData { copy(tags = value) }
    }

    private suspend fun ContactFormCtx.handleUpdateInitialNote(value: String) {
        updateFormData { copy(initialNote = value) }
    }

    // ============================================================================
    // STATUS FIELD HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleUpdateIsActive(value: Boolean) {
        updateFormData { copy(isActive = value) }
    }

    // ============================================================================
    // COUNTRY PICKER HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleShowCountryPicker() {
        withState<ContactFormState.Editing, _> {
            updateState {
                copy(ui = ui.copy(showCountryPicker = true, countrySearchQuery = ""))
            }
        }
    }

    private suspend fun ContactFormCtx.handleHideCountryPicker() {
        withState<ContactFormState.Editing, _> {
            updateState {
                copy(ui = ui.copy(showCountryPicker = false, countrySearchQuery = ""))
            }
        }
    }

    private suspend fun ContactFormCtx.handleUpdateCountrySearchQuery(value: String) {
        withState<ContactFormState.Editing, _> {
            updateState {
                copy(ui = ui.copy(countrySearchQuery = value))
            }
        }
    }

    // ============================================================================
    // BUSINESS TYPE PICKER HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleShowBusinessTypePicker() {
        withState<ContactFormState.Editing, _> {
            updateState {
                copy(ui = ui.copy(showBusinessTypePicker = true))
            }
        }
    }

    private suspend fun ContactFormCtx.handleHideBusinessTypePicker() {
        withState<ContactFormState.Editing, _> {
            updateState {
                copy(ui = ui.copy(showBusinessTypePicker = false))
            }
        }
    }

    // ============================================================================
    // DELETE CONFIRMATION HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleShowDeleteConfirmation() {
        withState<ContactFormState.Editing, _> {
            updateState {
                copy(ui = ui.copy(showDeleteConfirmation = true))
            }
        }
    }

    private suspend fun ContactFormCtx.handleHideDeleteConfirmation() {
        withState<ContactFormState.Editing, _> {
            updateState {
                copy(ui = ui.copy(showDeleteConfirmation = false))
            }
        }
    }

    // ============================================================================
    // DUPLICATE DETECTION HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleDismissDuplicateWarnings() {
        withState<ContactFormState.Editing, _> {
            updateState { copy(duplicates = emptyList()) }
        }
    }

    private suspend fun ContactFormCtx.checkDuplicatesDebounced() {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = launch {
            delay(DUPLICATE_CHECK_DEBOUNCE_MS)
            checkDuplicates()
        }
    }

    private suspend fun ContactFormCtx.checkDuplicates() {
        withState<ContactFormState.Editing, _> {
            val form = formData
            val editingId = contactId

            // Skip check if no meaningful data to search
            if (form.name.length < 2 && form.email.isBlank() && form.vatNumber.isBlank()) {
                updateState { copy(duplicates = emptyList(), isDuplicateCheckInProgress = false) }
                return@withState
            }

            updateState { copy(isDuplicateCheckInProgress = true) }

            val foundDuplicates = mutableListOf<PotentialDuplicate>()

            // Check by VAT number (highest confidence)
            if (form.vatNumber.isNotBlank()) {
                listContacts(search = form.vatNumber, limit = 5).fold(
                    onSuccess = { contacts ->
                        contacts
                            .filter { it.id != editingId && it.vatNumber?.value == form.vatNumber }
                            .forEach { foundDuplicates.add(PotentialDuplicate(it, DuplicateReason.VatNumber)) }
                    },
                    onFailure = { /* ignore errors during duplicate check */ }
                )
            }

            // Check by email (high confidence)
            if (form.email.isNotBlank() && form.email.contains("@")) {
                listContacts(search = form.email, limit = 5).fold(
                    onSuccess = { contacts ->
                        contacts
                            .filter { it.id != editingId && it.email?.value.equals(form.email, ignoreCase = true) }
                            .filter { dup -> foundDuplicates.none { it.contact.id == dup.id } }
                            .forEach { foundDuplicates.add(PotentialDuplicate(it, DuplicateReason.Email)) }
                    },
                    onFailure = { /* ignore errors during duplicate check */ }
                )
            }

            // Check by name + country (medium confidence)
            if (form.name.length >= 3 && form.country.isNotBlank()) {
                listContacts(search = form.name, limit = 10).fold(
                    onSuccess = { contacts ->
                        contacts
                            .filter { it.id != editingId }
                            .filter {
                                it.name.value.equals(form.name, ignoreCase = true) &&
                                    it.country.equals(form.country, ignoreCase = true)
                            }
                            .filter { dup -> foundDuplicates.none { it.contact.id == dup.id } }
                            .forEach { foundDuplicates.add(PotentialDuplicate(it, DuplicateReason.NameAndCountry)) }
                    },
                    onFailure = { /* ignore errors during duplicate check */ }
                )
            }

            updateState {
                copy(duplicates = foundDuplicates, isDuplicateCheckInProgress = false)
            }
        }
    }

    // ============================================================================
    // VALIDATION
    // ============================================================================

    private suspend fun validateForm(formData: ContactFormData): Map<String, String> {
        val errors = mutableMapOf<String, String>()

        // Required field: name
        if (formData.name.isBlank()) {
            errors["name"] = getString(Res.string.contacts_name_required)
        }

        // Optional validation: email format
        if (formData.email.isNotBlank() && !formData.email.contains("@")) {
            errors["email"] = getString(Res.string.contacts_invalid_email)
        }

        // Optional validation: Peppol ID format
        if (formData.peppolId.isNotBlank() && !formData.peppolId.contains(":")) {
            errors["peppolId"] = getString(Res.string.contacts_invalid_peppol_id)
        }

        // Optional validation: Peppol ID required if enabled
        if (formData.peppolEnabled && formData.peppolId.isBlank()) {
            errors["peppolId"] = getString(Res.string.contacts_peppol_id_required)
        }

        return errors
    }

    // ============================================================================
    // SAVE HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleSave() {
        withState<ContactFormState.Editing, _> {
            val errors = validateForm(formData)

            if (errors.isNotEmpty()) {
                logger.w { "Form validation failed: $errors" }
                updateState { copy(formData = formData.copy(errors = errors)) }
                // Show first error to user
                errors.values.firstOrNull()?.let { errorMessage ->
                    action(ContactFormAction.ShowError(errorMessage))
                }
                return@withState
            }

            if (isEditMode) {
                handleUpdateContact(contactId!!)
            } else {
                handleCreateContact()
            }
        }
    }

    private suspend fun ContactFormCtx.handleCreateContact() {
        withState<ContactFormState.Editing, _> {
            updateState { copy(isSaving = true) }

            logger.d { "Creating contact: ${formData.name}" }

            val request = formData.toCreateRequest()

            createContact(request).fold(
                onSuccess = { contact ->
                    logger.i { "Contact created: ${contact.id}" }
                    updateState { copy(isSaving = false) }
                    action(ContactFormAction.ShowSuccess(getString(Res.string.contacts_create_success)))
                    action(ContactFormAction.NavigateToContact(contact.id))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create contact" }
                    updateState {
                        ContactFormState.Error(
                            contactId = null,
                            formData = formData,
                            exception = error.asDokusException,
                            retryHandler = { intent(ContactFormIntent.Save) }
                        )
                    }
                }
            )
        }
    }

    private suspend fun ContactFormCtx.handleUpdateContact(contactId: ContactId) {
        withState<ContactFormState.Editing, _> {
            updateState { copy(isSaving = true) }

            logger.d { "Updating contact: $contactId" }

            val request = formData.toUpdateRequest()

            updateContact(contactId, request).fold(
                onSuccess = { contact ->
                    logger.i { "Contact updated: ${contact.id}" }
                    updateState { copy(isSaving = false) }
                    action(ContactFormAction.ShowSuccess(getString(Res.string.contacts_update_success)))
                    action(ContactFormAction.NavigateToContact(contact.id))
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update contact: $contactId" }
                    updateState {
                        ContactFormState.Error(
                            contactId = contactId,
                            formData = formData,
                            exception = error.asDokusException,
                            retryHandler = { intent(ContactFormIntent.Save) }
                        )
                    }
                }
            )
        }
    }

    // ============================================================================
    // DELETE HANDLERS
    // ============================================================================

    private suspend fun ContactFormCtx.handleDelete() {
        withState<ContactFormState.Editing, _> {
            val id = contactId ?: return@withState

            updateState {
                copy(
                    isDeleting = true,
                    ui = ui.copy(showDeleteConfirmation = false)
                )
            }

            logger.d { "Deleting contact: $id" }

            deleteContact(id).fold(
                onSuccess = {
                    logger.i { "Contact deleted: $id" }
                    updateState { copy(isDeleting = false) }
                    action(ContactFormAction.ShowSuccess(getString(Res.string.contacts_delete_success)))
                    action(ContactFormAction.NavigateBack)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete contact: $id" }
                    updateState { copy(isDeleting = false) }
                    action(ContactFormAction.ShowError(getString(Res.string.contacts_delete_failed)))
                }
            )
        }
    }

    // ============================================================================
    // HELPER FUNCTIONS
    // ============================================================================

    /**
     * Helper to update form data in the Editing state.
     */
    private suspend fun ContactFormCtx.updateFormData(
        transform: ContactFormData.() -> ContactFormData
    ) {
        updateState {
            when (this) {
                is ContactFormState.Editing -> copy(formData = formData.transform())
                is ContactFormState.Error -> ContactFormState.Editing(
                    contactId = contactId,
                    formData = formData.transform()
                )
                else -> this
            }
        }
    }
}

// ============================================================================
// EXTENSION FUNCTIONS
// ============================================================================

/**
 * Convert ContactDto to ContactFormData for editing.
 */
private fun ContactDto.toFormData(): ContactFormData = ContactFormData(
    name = name.value,
    email = email?.value ?: "",
    phone = phone ?: "",
    contactPerson = contactPerson ?: "",
    vatNumber = vatNumber?.value ?: "",
    companyNumber = companyNumber ?: "",
    businessType = businessType,
    addressLine1 = addressLine1 ?: "",
    addressLine2 = addressLine2 ?: "",
    city = city ?: "",
    postalCode = postalCode ?: "",
    country = country ?: "",
    peppolId = peppolId ?: "",
    peppolEnabled = peppolEnabled,
    defaultPaymentTerms = defaultPaymentTerms,
    defaultVatRate = defaultVatRate?.toString() ?: "",
    tags = tags ?: "",
    isActive = isActive
)

/**
 * Convert ContactFormData to CreateContactRequest.
 */
private fun ContactFormData.toCreateRequest(): CreateContactRequest = CreateContactRequest(
    name = name,
    email = email.takeIf { it.isNotBlank() },
    phone = phone.takeIf { it.isNotBlank() },
    vatNumber = vatNumber.takeIf { it.isNotBlank() },
    businessType = businessType,
    addressLine1 = addressLine1.takeIf { it.isNotBlank() },
    addressLine2 = addressLine2.takeIf { it.isNotBlank() },
    city = city.takeIf { it.isNotBlank() },
    postalCode = postalCode.takeIf { it.isNotBlank() },
    country = country.takeIf { it.isNotBlank() },
    contactPerson = contactPerson.takeIf { it.isNotBlank() },
    companyNumber = companyNumber.takeIf { it.isNotBlank() },
    defaultPaymentTerms = defaultPaymentTerms,
    defaultVatRate = defaultVatRate.takeIf { it.isNotBlank() },
    peppolId = peppolId.takeIf { it.isNotBlank() },
    peppolEnabled = peppolEnabled,
    tags = tags.takeIf { it.isNotBlank() },
    initialNote = initialNote.takeIf { it.isNotBlank() }
)

/**
 * Convert ContactFormData to UpdateContactRequest.
 */
private fun ContactFormData.toUpdateRequest(): UpdateContactRequest = UpdateContactRequest(
    name = name,
    email = email.takeIf { it.isNotBlank() },
    phone = phone.takeIf { it.isNotBlank() },
    vatNumber = vatNumber.takeIf { it.isNotBlank() },
    businessType = businessType,
    addressLine1 = addressLine1.takeIf { it.isNotBlank() },
    addressLine2 = addressLine2.takeIf { it.isNotBlank() },
    city = city.takeIf { it.isNotBlank() },
    postalCode = postalCode.takeIf { it.isNotBlank() },
    country = country.takeIf { it.isNotBlank() },
    contactPerson = contactPerson.takeIf { it.isNotBlank() },
    companyNumber = companyNumber.takeIf { it.isNotBlank() },
    defaultPaymentTerms = defaultPaymentTerms,
    defaultVatRate = defaultVatRate.takeIf { it.isNotBlank() },
    peppolId = peppolId.takeIf { it.isNotBlank() },
    peppolEnabled = peppolEnabled,
    tags = tags.takeIf { it.isNotBlank() },
    isActive = isActive
)
