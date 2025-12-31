package tech.dokus.contacts.viewmodel

import pro.respawn.flowmvi.api.MVIState
import tech.dokus.contacts.models.MergeDialogStep
import tech.dokus.contacts.models.MergeFieldConflict
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult

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
