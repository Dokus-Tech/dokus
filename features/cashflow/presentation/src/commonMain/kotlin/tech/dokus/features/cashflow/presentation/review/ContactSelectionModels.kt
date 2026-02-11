package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import tech.dokus.domain.ids.ContactId

@Immutable
data class ContactSnapshot(
    val id: ContactId,
    val name: String,
    val vatNumber: String?,
    val email: String?,
)

@Immutable
sealed interface ContactSelectionState {
    data object NoContact : ContactSelectionState
    data object Selected : ContactSelectionState

    data class Suggested(
        val contactId: ContactId,
        val name: String,
        val vatNumber: String?,
    ) : ContactSelectionState
}

@Immutable
data class ContactSuggestion(
    val contactId: ContactId,
    val name: String,
    val vatNumber: String?,
)
