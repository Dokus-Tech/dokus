package tech.dokus.features.contacts.mvi

import pro.respawn.flowmvi.api.MVIAction
import tech.dokus.domain.model.contact.ContactMergeResult

internal sealed interface ContactMergeAction : MVIAction {
    data class MergeCompleted(val result: ContactMergeResult) : ContactMergeAction
    data object DismissRequested : ContactMergeAction
}
