package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
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
        withState<DocumentReviewState.Content, _> {
            updateState {
                copy(
                    showContactSheet = true,
                    contactSheetSearchQuery = "",
                    contactSheetContacts = DokusState.Loading,
                )
            }
        }
        // Loading contacts is handled externally via ListContactsUseCase
        // The UI will trigger loading when the sheet opens
    }

    suspend fun DocumentReviewCtx.handleCloseContactSheet() {
        withState<DocumentReviewState.Content, _> {
            updateState {
                copy(
                    showContactSheet = false,
                    contactSheetSearchQuery = "",
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateContactSheetSearch(query: String) {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(contactSheetSearchQuery = query) }
        }
    }

    suspend fun DocumentReviewCtx.handleSelectContact(contactId: ContactId) {
        withState<DocumentReviewState.Content, _> {
            bindContact(documentId, contactId)
        }
    }

    suspend fun DocumentReviewCtx.handleAcceptSuggestedContact() {
        withState<DocumentReviewState.Content, _> {
            val suggested = contactSelectionState as? ContactSelectionState.Suggested
                ?: return@withState
            bindContact(documentId, suggested.contactId)
        }
    }

    suspend fun DocumentReviewCtx.handleClearSelectedContact() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(isBindingContact = true) }
        }

        withState<DocumentReviewState.Content, _> {
            updateDocumentDraftContact(documentId, null, CounterpartyIntent.None)
                .fold(
                    onSuccess = {
                        val newState = document.draft?.suggestedContactId?.let { suggestedId ->
                            ContactSelectionState.Suggested(
                                contactId = suggestedId,
                                name = document.draft?.extractedData?.invoice?.clientName
                                    ?: document.draft?.extractedData?.bill?.supplierName
                                    ?: "",
                                vatNumber = document.draft?.extractedData?.invoice?.clientVatNumber
                                    ?: document.draft?.extractedData?.bill?.supplierVatNumber,
                                confidence = document.draft?.contactSuggestionConfidence ?: 0f,
                                reason = document.draft?.contactSuggestionReason
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { ContactSuggestionReason.Custom(it) }
                                    ?: ContactSuggestionReason.AiSuggested,
                            )
                        } ?: ContactSelectionState.NoContact

                        updateState {
                            copy(
                                selectedContactId = null,
                                selectedContactSnapshot = null,
                                contactSelectionState = newState,
                                counterpartyIntent = CounterpartyIntent.None,
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
        withState<DocumentReviewState.Content, _> {
            bindContact(documentId, contactId)
        }
    }

    suspend fun DocumentReviewCtx.handleSetCounterpartyIntent(intent: CounterpartyIntent) {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(isBindingContact = true) }
        }

        withState<DocumentReviewState.Content, _> {
            updateDocumentDraftContact(documentId, null, intent)
                .fold(
                    onSuccess = {
                        val isPending = intent == CounterpartyIntent.Pending
                        updateState {
                            copy(
                                counterpartyIntent = intent,
                                selectedContactId = if (isPending) null else selectedContactId,
                                selectedContactSnapshot = if (isPending) null else selectedContactSnapshot,
                                contactSelectionState = if (isPending) {
                                    ContactSelectionState.NoContact
                                } else {
                                    contactSelectionState
                                },
                                isBindingContact = false,
                                contactValidationError = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to update counterparty intent" }
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
        withState<DocumentReviewState.Content, _> {
            updateState { copy(isBindingContact = true, contactValidationError = null) }
        }

        updateDocumentDraftContact(documentId, contactId, CounterpartyIntent.None)
            .fold(
                onSuccess = {
                    getContact(contactId).fold(
                        onSuccess = { contact ->
                            withState<DocumentReviewState.Content, _> {
                                updateState {
                                    copy(
                                        selectedContactId = contactId,
                                        selectedContactSnapshot = ContactSnapshot(
                                            id = contact.id,
                                            name = contact.name.value,
                                            vatNumber = contact.vatNumber?.value,
                                            email = contact.email?.value,
                                        ),
                                        contactSelectionState = ContactSelectionState.Selected,
                                        counterpartyIntent = CounterpartyIntent.None,
                                        isBindingContact = false,
                                    )
                                }
                            }
                        },
                        onFailure = { error ->
                            logger.w(error) { "Contact bound but fetch failed" }
                            withState<DocumentReviewState.Content, _> {
                                updateState {
                                    copy(
                                        selectedContactId = contactId,
                                        selectedContactSnapshot = null,
                                        contactSelectionState = ContactSelectionState.Selected,
                                        counterpartyIntent = CounterpartyIntent.None,
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
                    withState<DocumentReviewState.Content, _> {
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
