package tech.dokus.features.contacts.mvi

import pro.respawn.flowmvi.api.MVIIntent
import tech.dokus.domain.model.contact.ContactDto

internal sealed interface ContactMergeIntent : MVIIntent {
    data class UpdateSearchQuery(val query: String) : ContactMergeIntent
    data class SelectTarget(val contact: ContactDto) : ContactMergeIntent
    data class UpdateConflict(val index: Int, val keepSource: Boolean) : ContactMergeIntent

    data object Continue : ContactMergeIntent
    data object Back : ContactMergeIntent
    data object ConfirmMerge : ContactMergeIntent
    data object Complete : ContactMergeIntent
    data object Dismiss : ContactMergeIntent
}
