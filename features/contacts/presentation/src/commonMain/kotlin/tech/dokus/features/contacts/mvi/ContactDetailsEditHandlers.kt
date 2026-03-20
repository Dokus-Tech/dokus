package tech.dokus.features.contacts.mvi

import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.contacts.usecases.CacheContactsUseCase
import tech.dokus.features.contacts.usecases.UpdateContactUseCase
import tech.dokus.features.contacts.mvi.extensions.toFormData
import tech.dokus.features.contacts.mvi.extensions.toUpdateRequest
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger

/**
 * Handles inline-edit intents for the Contact Details screen.
 *
 * Covers start/cancel/save editing and form data updates.
 */
internal class ContactDetailsEditHandlers(
    private val updateContact: UpdateContactUseCase,
    private val getCurrentTenantId: GetCurrentTenantIdUseCase,
    private val cacheContacts: CacheContactsUseCase,
) {

    private val logger = Logger.forClass<ContactDetailsEditHandlers>()

    suspend fun ContactDetailsCtx.handleStartEditing() {
        withState {
            if (!contact.isSuccess()) return@withState
            val formData = contact.data.toFormData()
            updateState { copy(editFormData = formData) }
        }
    }

    suspend fun ContactDetailsCtx.handleCancelEditing() {
        updateState { copy(editFormData = null) }
    }

    suspend fun ContactDetailsCtx.handleUpdateEditFormData(formData: ContactFormData) {
        updateState { copy(editFormData = formData) }
    }

    suspend fun ContactDetailsCtx.handleSaveEdit() {
        var capturedContactId: ContactId? = null
        var capturedFormData: ContactFormData? = null

        withState {
            capturedContactId = contactId
            capturedFormData = editFormData
        }

        val editContactId = capturedContactId ?: return
        val form = capturedFormData ?: return

        if (form.name.value.isBlank()) {
            updateState {
                copy(
                    editFormData = form.copy(
                        errors = mapOf("name" to DokusException.Validation.ContactNameRequired)
                    )
                )
            }
            return
        }

        updateState { copy(isSavingEdit = true) }
        logger.d { "Saving inline edit for contact $editContactId" }

        val request = form.toUpdateRequest()
        updateContact(editContactId, request).fold(
            onSuccess = { updatedContact ->
                logger.i { "Contact updated: ${updatedContact.id}" }
                updateState {
                    copy(
                        contact = DokusState.success(updatedContact),
                        editFormData = null,
                        isSavingEdit = false,
                    )
                }
                cacheContact(updatedContact)
                action(ContactDetailsAction.ShowSuccess(ContactDetailsSuccess.ContactUpdated))
            },
            onFailure = { error ->
                logger.e(error) { "Failed to update contact: $editContactId" }
                updateState { copy(isSavingEdit = false) }
                action(ContactDetailsAction.ShowError(error.asDokusException))
            }
        )
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun cacheContact(contact: ContactDto) {
        val tenantId = getCurrentTenantId() ?: return
        try {
            cacheContacts(tenantId, listOf(contact))
            logger.d { "Cached contact: ${contact.name}" }
        } catch (e: Exception) {
            logger.w(e) { "Failed to cache contact" }
        }
    }
}
