package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewContactBinder(
    private val updateDocumentDraftContact: UpdateDocumentDraftContactUseCase,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
    // Contact sheet handlers

    suspend fun DocumentReviewCtx.handleOpenContactSheet() {
        withState {
            updateState {
                copy(
                    showContactSheet = true,
                    contactSheetSearchQuery = "",
                    contactSheetContacts = DokusState.loading(),
                )
            }
        }
        // Loading contacts is handled externally via ListContactsUseCase
        // The UI will trigger loading when the sheet opens
    }

    suspend fun DocumentReviewCtx.handleCloseContactSheet() {
        withState {
            updateState {
                copy(
                    showContactSheet = false,
                    contactSheetSearchQuery = "",
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateContactSheetSearch(query: String) {
        withState {
            updateState { copy(contactSheetSearchQuery = query) }
        }
    }

    suspend fun DocumentReviewCtx.handleSelectContact(contactId: ContactId) {
        withState {
            val activeDocumentId = documentId ?: return@withState
            bindContact(activeDocumentId, contactId)
        }
    }

    suspend fun DocumentReviewCtx.handleAcceptSuggestedContact() {
        withState {
            val activeDocumentId = documentId ?: return@withState
            val suggested = effectiveContact as? ResolvedContact.Suggested
                ?: return@withState
            bindContact(activeDocumentId, suggested.contactId)
        }
    }

    suspend fun DocumentReviewCtx.handleClearSelectedContact() {
        withState {
            updateState { copy(isBindingContact = true) }
        }

        withState {
            val activeDocumentId = documentId ?: return@withState
            updateDocumentDraftContact(activeDocumentId, null)
                .fold(
                    onSuccess = {
                        updateState {
                            copy(
                                selectedContactOverride = null,
                                isBindingContact = false,
                                contactValidationError = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to clear contact" }
                        updateState { copy(isBindingContact = false) }
                        val exception = error.asDokusException
                        val displayException = if (exception is DokusException.Unknown) {
                            DokusException.DocumentContactClearFailed
                        } else {
                            exception
                        }
                        action(DocumentReviewAction.ShowError(displayException))
                    }
                )
        }
    }

    suspend fun DocumentReviewCtx.handleContactCreated(contactId: ContactId) {
        withState {
            val activeDocumentId = documentId ?: return@withState
            bindContact(activeDocumentId, contactId)
        }
    }

    suspend fun DocumentReviewCtx.handleSetPendingCreation() {
        withState {
            updateState { copy(isBindingContact = true) }
        }

        withState {
            val activeDocumentId = documentId ?: return@withState
            updateDocumentDraftContact(activeDocumentId, null, pendingCreation = true)
                .fold(
                    onSuccess = {
                        updateState {
                            copy(
                                selectedContactOverride = null,
                                isBindingContact = false,
                                contactValidationError = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to set pending creation" }
                        updateState { copy(isBindingContact = false) }
                        val exception = error.asDokusException
                        val displayException = if (exception is DokusException.Unknown) {
                            DokusException.DocumentContactSaveFailed
                        } else {
                            exception
                        }
                        action(DocumentReviewAction.ShowError(displayException))
                    }
                )
        }
    }

    private suspend fun DocumentReviewCtx.bindContact(documentId: DocumentId, contactId: ContactId) {
        withState {
            updateState { copy(isBindingContact = true, contactValidationError = null) }
        }

        updateDocumentDraftContact(documentId, contactId)
            .fold(
                onSuccess = {
                    getContact(contactId).fold(
                        onSuccess = { contact ->
                            withState {
                                updateState {
                                    copy(
                                        selectedContactOverride = ResolvedContact.Linked(
                                            contactId = contactId,
                                            name = contact.name.value,
                                            vatNumber = contact.vatNumber?.value,
                                            email = contact.email?.value,
                                            avatarPath = contact.avatar?.small,
                                        ),
                                        isBindingContact = false,
                                    )
                                }
                            }
                        },
                        onFailure = { error ->
                            logger.w(error) { "Contact bound but fetch failed" }
                            withState {
                                updateState {
                                    copy(
                                        selectedContactOverride = ResolvedContact.Linked(
                                            contactId = contactId,
                                            name = "",
                                            vatNumber = null,
                                            email = null,
                                            avatarPath = null,
                                        ),
                                        isBindingContact = false,
                                    )
                                }
                            }
                        }
                    )
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to bind contact to draft" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.DocumentContactSaveFailed
                    } else {
                        exception
                    }
                    withState {
                        updateState {
                            copy(
                                isBindingContact = false,
                                contactValidationError = displayException,
                            )
                        }
                    }
                    val bindException = if (exception is DokusException.Unknown) {
                        DokusException.DocumentContactBindFailed
                    } else {
                        exception
                    }
                    action(DocumentReviewAction.ShowError(bindException))
                }
            )
    }
}
