package tech.dokus.features.contacts.mvi

import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult
import tech.dokus.features.contacts.presentation.contacts.model.MergeDialogStep
import tech.dokus.features.contacts.presentation.contacts.model.MergeFieldConflict

internal data class ContactMergeState(
    val step: MergeDialogStep,
    val sourceContact: ContactDto,
    val sourceActivity: ContactActivitySummary?,
    val targetContact: ContactDto?,
    val conflicts: List<MergeFieldConflict>,
    val searchQuery: String,
    val searchResults: List<ContactDto>,
    val isSearching: Boolean,
    val isMerging: Boolean,
    val mergeResult: ContactMergeResult?,
    val mergeError: DokusException?,
    val hasPreselectedTarget: Boolean,
) : MVIState
