package tech.dokus.features.contacts.presentation.contacts.components.merge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.contacts.mvi.ContactMergeAction
import tech.dokus.features.contacts.mvi.ContactMergeContainer
import tech.dokus.features.contacts.mvi.ContactMergeIntent
import tech.dokus.features.contacts.mvi.ContactMergeState
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult
import tech.dokus.foundation.app.mvi.container

@Composable
internal fun ContactMergeDialogRoute(
    sourceContact: ContactDto,
    sourceActivity: ContactActivitySummary?,
    preselectedTarget: ContactDto? = null,
    onMergeComplete: (ContactMergeResult) -> Unit,
    onDismiss: () -> Unit,
    container: ContactMergeContainer = container {
        org.koin.core.parameter.parametersOf(
            ContactMergeContainer.Params(
                sourceContact = sourceContact,
                sourceActivity = sourceActivity,
                preselectedTarget = preselectedTarget
            )
        )
    },
) {
    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ContactMergeAction.MergeCompleted -> onMergeComplete(action.result)
            ContactMergeAction.DismissRequested -> onDismiss()
        }
    }

    ContactMergeDialog(
        state = state,
        onIntent = { intent -> container.store.intent(intent) }
    )
}
