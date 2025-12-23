package ai.dokus.app.contacts.viewmodel

import ai.dokus.app.contacts.repository.ContactRepository
import ai.dokus.foundation.domain.enums.ClientType
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.CreateContactRequest
import ai.dokus.foundation.domain.model.UpdateContactRequest
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import tech.dokus.foundation.app.viewmodel.BaseViewModel

// ============================================================================
// FORM STATE
// ============================================================================

/**
 * State representing the contact form data.
 * Holds all editable contact fields with validation errors.
 */
data class ContactFormState(
    // Basic info
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val contactPerson: String = "",

    // Business info
    val vatNumber: String = "",
    val companyNumber: String = "",
    val businessType: ClientType = ClientType.Business,

    // Address
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val postalCode: String = "",
    val country: String = "",

    // Peppol settings
    val peppolId: String = "",
    val peppolEnabled: Boolean = false,

    // Defaults
    val defaultPaymentTerms: Int = 30,
    val defaultVatRate: String = "",

    // Tags and notes
    val tags: String = "",
    val initialNote: String = "",

    // Status
    val isActive: Boolean = true,

    // Form state
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val errors: Map<String, String> = emptyMap()
) {
    /**
     * Check if the form has the minimum required data.
     */
    val isValid: Boolean
        get() = name.isNotBlank() && errors.isEmpty()

    /**
     * Check if email format is valid (if provided).
     */
    val isEmailValid: Boolean
        get() = email.isBlank() || email.contains("@")

    /**
     * Check if Peppol ID format is valid (if provided).
     * Expected format: scheme:identifier (e.g., "0208:BE0123456789")
     */
    val isPeppolIdValid: Boolean
        get() = peppolId.isBlank() || peppolId.contains(":")
}

/**
 * UI state for the contact form screen.
 * Separate from form data to keep concerns isolated.
 */
data class ContactFormUiState(
    val isEditMode: Boolean = false,
    val editingContactId: ContactId? = null,
    val showDeleteConfirmation: Boolean = false,
    val showCountryPicker: Boolean = false,
    val showBusinessTypePicker: Boolean = false,
    val countrySearchQuery: String = "",
    val isContactDeleted: Boolean = false
)

/**
 * Represents a potential duplicate contact detected during form entry.
 */
data class DuplicateContact(
    val contact: ContactDto,
    val matchReason: DuplicateMatchReason
)

/**
 * Reason for duplicate detection match.
 */
enum class DuplicateMatchReason(val displayName: String) {
    VatNumber("Matching VAT number"),
    Email("Matching email"),
    NameAndCountry("Similar name and country")
}

// ============================================================================
// VIEW MODEL
// ============================================================================

/**
 * ViewModel for creating and editing contacts.
 * Manages form state, validation, duplicate detection, and save/update/delete operations.
 */
internal class ContactFormViewModel :
    BaseViewModel<DokusState<ContactDto>>(DokusState.idle()),
    KoinComponent {

    private val logger = Logger.forClass<ContactFormViewModel>()
    private val contactRepository: ContactRepository by inject()

    // Form state
    private val _formState = MutableStateFlow(ContactFormState())
    val formState: StateFlow<ContactFormState> = _formState.asStateFlow()

    // UI state
    private val _uiState = MutableStateFlow(ContactFormUiState())
    val uiState: StateFlow<ContactFormUiState> = _uiState.asStateFlow()

    // Duplicate detection
    private val _duplicates = MutableStateFlow<List<DuplicateContact>>(emptyList())
    val duplicates: StateFlow<List<DuplicateContact>> = _duplicates.asStateFlow()

    private val _isDuplicateCheckInProgress = MutableStateFlow(false)
    val isDuplicateCheckInProgress: StateFlow<Boolean> = _isDuplicateCheckInProgress.asStateFlow()

    // Track the created/updated contact ID
    private val _savedContactId = MutableStateFlow<ContactId?>(null)
    val savedContactId: StateFlow<ContactId?> = _savedContactId.asStateFlow()

    // Job for debounced duplicate checking
    private var duplicateCheckJob: Job? = null

    // ============================================================================
    // INITIALIZATION
    // ============================================================================

    /**
     * Initialize the form for creating a new contact.
     */
    fun initForCreate() {
        logger.d { "Initializing form for create" }
        _formState.value = ContactFormState()
        _uiState.value = ContactFormUiState(isEditMode = false)
        _duplicates.value = emptyList()
        _savedContactId.value = null
        mutableState.value = DokusState.idle()
    }

    /**
     * Initialize the form for editing an existing contact.
     * Loads contact data and populates the form.
     */
    fun initForEdit(contactId: ContactId) {
        logger.d { "Initializing form for edit: $contactId" }
        _uiState.update { it.copy(isEditMode = true, editingContactId = contactId) }
        _savedContactId.value = null

        scope.launch {
            mutableState.emitLoading()

            contactRepository.getContact(contactId).fold(
                onSuccess = { contact ->
                    logger.i { "Loaded contact for editing: ${contact.name}" }
                    _formState.value = ContactFormState(
                        name = contact.name.value,
                        email = contact.email?.value ?: "",
                        phone = contact.phone ?: "",
                        contactPerson = contact.contactPerson ?: "",
                        vatNumber = contact.vatNumber?.value ?: "",
                        companyNumber = contact.companyNumber ?: "",
                        businessType = contact.businessType,
                        addressLine1 = contact.addressLine1 ?: "",
                        addressLine2 = contact.addressLine2 ?: "",
                        city = contact.city ?: "",
                        postalCode = contact.postalCode ?: "",
                        country = contact.country ?: "",
                        peppolId = contact.peppolId ?: "",
                        peppolEnabled = contact.peppolEnabled,
                        defaultPaymentTerms = contact.defaultPaymentTerms,
                        defaultVatRate = contact.defaultVatRate?.toString() ?: "",
                        tags = contact.tags ?: "",
                        isActive = contact.isActive
                    )
                    mutableState.value = DokusState.success(contact)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load contact for editing" }
                    mutableState.emit(error) { initForEdit(contactId) }
                }
            )
        }
    }

    // ============================================================================
    // FIELD UPDATES
    // ============================================================================

    fun updateName(name: String) {
        _formState.update { it.copy(name = name, errors = it.errors - "name") }
        // Check for duplicates by name after debounce
        checkDuplicatesDebounced()
    }

    fun updateEmail(email: String) {
        _formState.update {
            val errors = if (email.isNotBlank() && !email.contains("@")) {
                it.errors + ("email" to "Invalid email format")
            } else {
                it.errors - "email"
            }
            it.copy(email = email, errors = errors)
        }
        // Check for duplicates by email after debounce
        checkDuplicatesDebounced()
    }

    fun updatePhone(phone: String) {
        _formState.update { it.copy(phone = phone) }
    }

    fun updateContactPerson(contactPerson: String) {
        _formState.update { it.copy(contactPerson = contactPerson) }
    }

    fun updateVatNumber(vatNumber: String) {
        _formState.update { it.copy(vatNumber = vatNumber, errors = it.errors - "vatNumber") }
        // Check for duplicates by VAT after debounce
        checkDuplicatesDebounced()
    }

    fun updateCompanyNumber(companyNumber: String) {
        _formState.update { it.copy(companyNumber = companyNumber) }
    }

    fun updateBusinessType(businessType: ClientType) {
        _formState.update { it.copy(businessType = businessType) }
        closeBusinessTypePicker()
    }

    fun updateAddressLine1(addressLine1: String) {
        _formState.update { it.copy(addressLine1 = addressLine1) }
    }

    fun updateAddressLine2(addressLine2: String) {
        _formState.update { it.copy(addressLine2 = addressLine2) }
    }

    fun updateCity(city: String) {
        _formState.update { it.copy(city = city) }
    }

    fun updatePostalCode(postalCode: String) {
        _formState.update { it.copy(postalCode = postalCode) }
    }

    fun updateCountry(country: String) {
        _formState.update { it.copy(country = country) }
        closeCountryPicker()
        // Check for duplicates by name+country after debounce
        checkDuplicatesDebounced()
    }

    fun updatePeppolId(peppolId: String) {
        _formState.update {
            val errors = if (peppolId.isNotBlank() && !peppolId.contains(":")) {
                it.errors + ("peppolId" to "Invalid Peppol ID format (expected scheme:identifier)")
            } else {
                it.errors - "peppolId"
            }
            it.copy(peppolId = peppolId, errors = errors)
        }
    }

    fun updatePeppolEnabled(enabled: Boolean) {
        _formState.update { it.copy(peppolEnabled = enabled) }
    }

    fun updateDefaultPaymentTerms(terms: Int) {
        _formState.update { it.copy(defaultPaymentTerms = terms.coerceIn(0, 365)) }
    }

    fun updateDefaultVatRate(vatRate: String) {
        _formState.update { it.copy(defaultVatRate = vatRate) }
    }

    fun updateTags(tags: String) {
        _formState.update { it.copy(tags = tags) }
    }

    fun updateInitialNote(note: String) {
        _formState.update { it.copy(initialNote = note) }
    }

    fun updateIsActive(isActive: Boolean) {
        _formState.update { it.copy(isActive = isActive) }
    }

    // ============================================================================
    // UI STATE UPDATES
    // ============================================================================

    fun showCountryPicker() {
        _uiState.update { it.copy(showCountryPicker = true, countrySearchQuery = "") }
    }

    fun closeCountryPicker() {
        _uiState.update { it.copy(showCountryPicker = false, countrySearchQuery = "") }
    }

    fun updateCountrySearchQuery(query: String) {
        _uiState.update { it.copy(countrySearchQuery = query) }
    }

    fun showBusinessTypePicker() {
        _uiState.update { it.copy(showBusinessTypePicker = true) }
    }

    fun closeBusinessTypePicker() {
        _uiState.update { it.copy(showBusinessTypePicker = false) }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    // ============================================================================
    // DUPLICATE DETECTION
    // ============================================================================

    /**
     * Check for duplicate contacts with debouncing.
     * Searches by VAT number, email, and name+country.
     */
    private fun checkDuplicatesDebounced() {
        duplicateCheckJob?.cancel()
        duplicateCheckJob = scope.launch {
            delay(DUPLICATE_CHECK_DEBOUNCE_MS)
            checkDuplicates()
        }
    }

    /**
     * Perform duplicate detection check.
     */
    private suspend fun checkDuplicates() {
        val form = _formState.value
        val editingId = _uiState.value.editingContactId

        // Skip check if no meaningful data to search
        if (form.name.length < 2 && form.email.isBlank() && form.vatNumber.isBlank()) {
            _duplicates.value = emptyList()
            return
        }

        _isDuplicateCheckInProgress.value = true

        val foundDuplicates = mutableListOf<DuplicateContact>()

        // Check by VAT number (highest confidence)
        if (form.vatNumber.isNotBlank()) {
            contactRepository.listContacts(search = form.vatNumber, limit = 5).fold(
                onSuccess = { contacts ->
                    contacts
                        .filter { it.id != editingId && it.vatNumber?.value == form.vatNumber }
                        .forEach { foundDuplicates.add(DuplicateContact(it, DuplicateMatchReason.VatNumber)) }
                },
                onFailure = { /* ignore errors during duplicate check */ }
            )
        }

        // Check by email (high confidence)
        if (form.email.isNotBlank() && form.email.contains("@")) {
            contactRepository.listContacts(search = form.email, limit = 5).fold(
                onSuccess = { contacts ->
                    contacts
                        .filter { it.id != editingId && it.email?.value.equals(form.email, ignoreCase = true) }
                        .filter { dup -> foundDuplicates.none { it.contact.id == dup.id } }
                        .forEach { foundDuplicates.add(DuplicateContact(it, DuplicateMatchReason.Email)) }
                },
                onFailure = { /* ignore errors during duplicate check */ }
            )
        }

        // Check by name + country (medium confidence)
        if (form.name.length >= 3 && form.country.isNotBlank()) {
            contactRepository.listContacts(search = form.name, limit = 10).fold(
                onSuccess = { contacts ->
                    contacts
                        .filter { it.id != editingId }
                        .filter { it.name.value.equals(form.name, ignoreCase = true) && it.country.equals(form.country, ignoreCase = true) }
                        .filter { dup -> foundDuplicates.none { it.contact.id == dup.id } }
                        .forEach { foundDuplicates.add(DuplicateContact(it, DuplicateMatchReason.NameAndCountry)) }
                },
                onFailure = { /* ignore errors during duplicate check */ }
            )
        }

        _duplicates.value = foundDuplicates
        _isDuplicateCheckInProgress.value = false
    }

    /**
     * Dismiss duplicate warnings and continue with save.
     */
    fun dismissDuplicateWarnings() {
        _duplicates.value = emptyList()
    }

    // ============================================================================
    // VALIDATION
    // ============================================================================

    /**
     * Validate the form before saving.
     * Returns true if form is valid, false otherwise.
     */
    private fun validateForm(): Boolean {
        val form = _formState.value
        val errors = mutableMapOf<String, String>()

        // Required field: name
        if (form.name.isBlank()) {
            errors["name"] = "Name is required"
        }

        // Optional validation: email format
        if (form.email.isNotBlank() && !form.email.contains("@")) {
            errors["email"] = "Invalid email format"
        }

        // Optional validation: Peppol ID format
        if (form.peppolId.isNotBlank() && !form.peppolId.contains(":")) {
            errors["peppolId"] = "Invalid Peppol ID format (expected scheme:identifier)"
        }

        // Optional validation: Peppol ID required if enabled
        if (form.peppolEnabled && form.peppolId.isBlank()) {
            errors["peppolId"] = "Peppol ID is required when Peppol is enabled"
        }

        _formState.update { it.copy(errors = errors) }
        return errors.isEmpty()
    }

    // ============================================================================
    // SAVE OPERATIONS
    // ============================================================================

    /**
     * Save the contact (create or update depending on mode).
     */
    fun save() {
        if (!validateForm()) {
            logger.w { "Form validation failed" }
            return
        }

        val uiState = _uiState.value
        if (uiState.isEditMode && uiState.editingContactId != null) {
            updateContact(uiState.editingContactId)
        } else {
            createContact()
        }
    }

    /**
     * Create a new contact.
     */
    private fun createContact() {
        val form = _formState.value
        _formState.update { it.copy(isSaving = true) }

        scope.launch {
            mutableState.emitLoading()
            logger.d { "Creating contact: ${form.name}" }

            val request = CreateContactRequest(
                name = form.name,
                email = form.email.takeIf { it.isNotBlank() },
                phone = form.phone.takeIf { it.isNotBlank() },
                vatNumber = form.vatNumber.takeIf { it.isNotBlank() },
                businessType = form.businessType,
                addressLine1 = form.addressLine1.takeIf { it.isNotBlank() },
                addressLine2 = form.addressLine2.takeIf { it.isNotBlank() },
                city = form.city.takeIf { it.isNotBlank() },
                postalCode = form.postalCode.takeIf { it.isNotBlank() },
                country = form.country.takeIf { it.isNotBlank() },
                contactPerson = form.contactPerson.takeIf { it.isNotBlank() },
                companyNumber = form.companyNumber.takeIf { it.isNotBlank() },
                defaultPaymentTerms = form.defaultPaymentTerms,
                defaultVatRate = form.defaultVatRate.takeIf { it.isNotBlank() },
                peppolId = form.peppolId.takeIf { it.isNotBlank() },
                peppolEnabled = form.peppolEnabled,
                tags = form.tags.takeIf { it.isNotBlank() },
                initialNote = form.initialNote.takeIf { it.isNotBlank() }
            )

            contactRepository.createContact(request).fold(
                onSuccess = { contact ->
                    logger.i { "Contact created: ${contact.id}" }
                    _savedContactId.value = contact.id
                    _formState.update { it.copy(isSaving = false) }
                    mutableState.value = DokusState.success(contact)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to create contact" }
                    _formState.update {
                        it.copy(
                            isSaving = false,
                            errors = it.errors + ("general" to (error.message ?: "Failed to create contact"))
                        )
                    }
                    mutableState.emit(error) { createContact() }
                }
            )
        }
    }

    /**
     * Update an existing contact.
     */
    private fun updateContact(contactId: ContactId) {
        val form = _formState.value
        _formState.update { it.copy(isSaving = true) }

        scope.launch {
            mutableState.emitLoading()
            logger.d { "Updating contact: $contactId" }

            val request = UpdateContactRequest(
                name = form.name,
                email = form.email.takeIf { it.isNotBlank() },
                phone = form.phone.takeIf { it.isNotBlank() },
                vatNumber = form.vatNumber.takeIf { it.isNotBlank() },
                businessType = form.businessType,
                addressLine1 = form.addressLine1.takeIf { it.isNotBlank() },
                addressLine2 = form.addressLine2.takeIf { it.isNotBlank() },
                city = form.city.takeIf { it.isNotBlank() },
                postalCode = form.postalCode.takeIf { it.isNotBlank() },
                country = form.country.takeIf { it.isNotBlank() },
                contactPerson = form.contactPerson.takeIf { it.isNotBlank() },
                companyNumber = form.companyNumber.takeIf { it.isNotBlank() },
                defaultPaymentTerms = form.defaultPaymentTerms,
                defaultVatRate = form.defaultVatRate.takeIf { it.isNotBlank() },
                peppolId = form.peppolId.takeIf { it.isNotBlank() },
                peppolEnabled = form.peppolEnabled,
                tags = form.tags.takeIf { it.isNotBlank() },
                isActive = form.isActive
            )

            contactRepository.updateContact(contactId, request).fold(
                onSuccess = { contact ->
                    logger.i { "Contact updated: ${contact.id}" }
                    _savedContactId.value = contact.id
                    _formState.update { it.copy(isSaving = false) }
                    mutableState.value = DokusState.success(contact)
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to update contact: $contactId" }
                    _formState.update {
                        it.copy(
                            isSaving = false,
                            errors = it.errors + ("general" to (error.message ?: "Failed to update contact"))
                        )
                    }
                    mutableState.emit(error) { updateContact(contactId) }
                }
            )
        }
    }

    /**
     * Delete the contact being edited.
     */
    fun deleteContact() {
        val contactId = _uiState.value.editingContactId ?: return

        _formState.update { it.copy(isDeleting = true) }
        hideDeleteConfirmation()

        scope.launch {
            mutableState.emitLoading()
            logger.d { "Deleting contact: $contactId" }

            contactRepository.deleteContact(contactId).fold(
                onSuccess = {
                    logger.i { "Contact deleted: $contactId" }
                    _formState.update { it.copy(isDeleting = false) }
                    // Set savedContactId to null to indicate successful deletion
                    _savedContactId.value = null
                    // Signal deletion completion for navigation
                    _uiState.update { it.copy(isContactDeleted = true) }
                    // Emit idle state to signal completion
                    mutableState.value = DokusState.idle()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to delete contact: $contactId" }
                    _formState.update {
                        it.copy(
                            isDeleting = false,
                            errors = it.errors + ("general" to (error.message ?: "Failed to delete contact"))
                        )
                    }
                    mutableState.emit(error) { deleteContact() }
                }
            )
        }
    }

    companion object {
        private const val DUPLICATE_CHECK_DEBOUNCE_MS = 300L
    }
}
