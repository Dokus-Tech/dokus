package tech.dokus.contacts.viewmodel

import ai.dokus.app.contacts.usecases.ListContactsUseCase
import ai.dokus.app.contacts.usecases.MergeContactsUseCase
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.contacts.models.MergeDialogStep
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult

internal typealias ContactMergeCtx = PipelineContext<ContactMergeState, ContactMergeIntent, ContactMergeAction>

internal class ContactMergeContainer(
    private val sourceContact: ContactDto,
    private val sourceActivity: ContactActivitySummary?,
    private val preselectedTarget: ContactDto?,
    private val listContacts: ListContactsUseCase,
    private val mergeContacts: MergeContactsUseCase,
) : Container<ContactMergeState, ContactMergeIntent, ContactMergeAction> {

    data class Params(
        val sourceContact: ContactDto,
        val sourceActivity: ContactActivitySummary?,
        val preselectedTarget: ContactDto?,
    )

    private val logger = Logger.forClass<ContactMergeContainer>()
    private var searchJob: Job? = null

    override val store: Store<ContactMergeState, ContactMergeIntent, ContactMergeAction> =
        store(initialState()) {
            init {
                if (preselectedTarget != null) {
                    updateState {
                        copy(
                            targetContact = preselectedTarget,
                            conflicts = ContactMergeConflictCalculator.compute(sourceContact, preselectedTarget)
                        )
                    }
                }
            }

            reduce { intent ->
                when (intent) {
                    is ContactMergeIntent.UpdateSearchQuery -> handleUpdateSearchQuery(intent.query)
                    is ContactMergeIntent.SelectTarget -> handleSelectTarget(intent.contact)
                    is ContactMergeIntent.UpdateConflict -> handleUpdateConflict(intent.index, intent.keepSource)
                    ContactMergeIntent.Continue -> handleContinue()
                    ContactMergeIntent.Back -> handleBack()
                    ContactMergeIntent.ConfirmMerge -> handleConfirmMerge()
                    ContactMergeIntent.Complete -> handleComplete()
                    ContactMergeIntent.Dismiss -> handleDismiss()
                }
            }
        }

    private fun initialState(): ContactMergeState {
        val initialStep = if (preselectedTarget != null) {
            MergeDialogStep.CompareFields
        } else {
            MergeDialogStep.SelectTarget
        }
        return ContactMergeState(
            step = initialStep,
            sourceContact = sourceContact,
            sourceActivity = sourceActivity,
            targetContact = preselectedTarget,
            conflicts = preselectedTarget?.let { ContactMergeConflictCalculator.compute(sourceContact, it) } ?: emptyList(),
            searchQuery = "",
            searchResults = emptyList(),
            isSearching = false,
            isMerging = false,
            mergeResult = null,
            mergeError = null,
            hasPreselectedTarget = preselectedTarget != null,
        )
    }

    private suspend fun ContactMergeCtx.handleUpdateSearchQuery(query: String) {
        updateState { copy(searchQuery = query, mergeError = null) }
        searchJob?.cancel()

        if (query.length < 2) {
            updateState { copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = launch {
            delay(300)
            updateState { copy(isSearching = true) }
            listContacts(
                search = query,
                isActive = true,
                limit = 20
            ).fold(
                onSuccess = { contacts ->
                    val filtered = contacts.filter { it.id != sourceContact.id }
                    updateState {
                        copy(
                            searchResults = filtered,
                            isSearching = false
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Search failed" }
                    updateState {
                        copy(
                            searchResults = emptyList(),
                            isSearching = false
                        )
                    }
                }
            )
        }
    }

    private suspend fun ContactMergeCtx.handleSelectTarget(contact: ContactDto) {
        updateState {
            copy(
                targetContact = contact,
                conflicts = ContactMergeConflictCalculator.compute(sourceContact, contact),
                step = MergeDialogStep.CompareFields,
                mergeError = null,
            )
        }
    }

    private suspend fun ContactMergeCtx.handleUpdateConflict(index: Int, keepSource: Boolean) {
        withState<ContactMergeState, _> {
            if (index !in conflicts.indices) return@withState
            val updated = conflicts.toMutableList()
            updated[index] = updated[index].copy(keepSource = keepSource)
            updateState { copy(conflicts = updated) }
        }
    }

    private suspend fun ContactMergeCtx.handleContinue() {
        withState<ContactMergeState, _> {
            if (targetContact == null) return@withState
            updateState { copy(step = MergeDialogStep.Confirmation, mergeError = null) }
        }
    }

    private suspend fun ContactMergeCtx.handleBack() {
        withState<ContactMergeState, _> {
            when (step) {
                MergeDialogStep.SelectTarget -> action(ContactMergeAction.DismissRequested)
                MergeDialogStep.CompareFields -> {
                    if (hasPreselectedTarget) {
                        action(ContactMergeAction.DismissRequested)
                    } else {
                        updateState { copy(step = MergeDialogStep.SelectTarget) }
                    }
                }
                MergeDialogStep.Confirmation -> updateState { copy(step = MergeDialogStep.CompareFields) }
                MergeDialogStep.Result -> {}
            }
        }
    }

    private suspend fun ContactMergeCtx.handleDismiss() {
        withState<ContactMergeState, _> {
            if (!isMerging) {
                action(ContactMergeAction.DismissRequested)
            }
        }
    }

    private suspend fun ContactMergeCtx.handleConfirmMerge() {
        withState<ContactMergeState, _> {
            val target = targetContact ?: return@withState
            updateState { copy(isMerging = true, mergeError = null) }

            mergeContacts(
                sourceContactId = sourceContact.id,
                targetContactId = target.id
            ).fold(
                onSuccess = { result ->
                    logger.i {
                        "Merge successful: ${'$'}{result.invoicesReassigned} invoices, " +
                            "${'$'}{result.billsReassigned} bills reassigned"
                    }
                    updateState {
                        copy(
                            mergeResult = result,
                            step = MergeDialogStep.Result,
                            isMerging = false
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Merge failed" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.ContactMergeFailed
                    } else {
                        exception
                    }
                    updateState { copy(isMerging = false, mergeError = displayException) }
                }
            )
        }
    }

    private suspend fun ContactMergeCtx.handleComplete() {
        withState<ContactMergeState, _> {
            val result = mergeResult ?: return@withState
            action(ContactMergeAction.MergeCompleted(result))
            action(ContactMergeAction.DismissRequested)
        }
    }
}
