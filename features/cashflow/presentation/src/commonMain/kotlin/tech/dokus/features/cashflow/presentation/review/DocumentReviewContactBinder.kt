package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewContactBinder(
    private val dataSource: CashflowRemoteDataSource,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
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
            dataSource.updateDocumentDraftContact(documentId, null)
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

    private suspend fun DocumentReviewCtx.bindContact(documentId: DocumentId, contactId: ContactId) {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(isBindingContact = true, contactValidationError = null) }
        }

        dataSource.updateDocumentDraftContact(documentId, contactId)
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
