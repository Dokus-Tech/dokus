package tech.dokus.features.contacts.mvi

import tech.dokus.foundation.platform.Logger

/**
 * Handles merge and enrichment intents for the Contact Details screen.
 */
internal class ContactDetailsMergeEnrichmentHandlers {

    private val logger = Logger.forClass<ContactDetailsMergeEnrichmentHandlers>()

    // ========================================================================
    // MERGE
    // ========================================================================

    suspend fun ContactDetailsCtx.handleShowMergeDialog() {
        updateState {
            copy(uiState = uiState.copy(showMergeDialog = true))
        }
    }

    suspend fun ContactDetailsCtx.handleHideMergeDialog() {
        updateState {
            copy(uiState = uiState.copy(showMergeDialog = false))
        }
    }

    // ========================================================================
    // ENRICHMENT
    // ========================================================================

    suspend fun ContactDetailsCtx.handleShowEnrichmentPanel() {
        updateState {
            copy(uiState = uiState.copy(showEnrichmentPanel = true))
        }
    }

    suspend fun ContactDetailsCtx.handleHideEnrichmentPanel() {
        updateState {
            copy(uiState = uiState.copy(showEnrichmentPanel = false))
        }
    }

    suspend fun ContactDetailsCtx.handleApplyEnrichmentSuggestions(
        suggestions: List<EnrichmentSuggestion>,
    ) {
        if (suggestions.isEmpty()) {
            logger.w { "No enrichment suggestions selected" }
            return
        }

        // Future implementation: Build UpdateContactRequest from selected suggestions
        // and call contactRepository.updateContact
        logger.d { "Applying ${suggestions.size} enrichment suggestions" }

        updateState {
            val remainingSuggestions = enrichmentSuggestions.filterNot { it in suggestions }
            copy(
                enrichmentSuggestions = remainingSuggestions,
                uiState = uiState.copy(showEnrichmentPanel = false)
            )
        }

        action(
            ContactDetailsAction.ShowSuccess(
                ContactDetailsSuccess.EnrichmentApplied(suggestions.size)
            )
        )
    }
}
