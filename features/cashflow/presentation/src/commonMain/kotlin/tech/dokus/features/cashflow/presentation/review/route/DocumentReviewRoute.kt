package tech.dokus.features.cashflow.presentation.review.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_document_confirmed
import tech.dokus.aura.resources.cashflow_draft_saved
import tech.dokus.aura.resources.cashflow_reject_prompt
import tech.dokus.aura.resources.cashflow_reject_title
import tech.dokus.aura.resources.cashflow_reject_reason_duplicate
import tech.dokus.aura.resources.cashflow_reject_reason_not_my_business
import tech.dokus.aura.resources.cashflow_reject_reason_other
import tech.dokus.aura.resources.cashflow_reject_reason_spam
import tech.dokus.aura.resources.cashflow_reject_reason_test
import tech.dokus.aura.resources.cashflow_discard_changes_title
import tech.dokus.aura.resources.cashflow_discard_changes_message
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_confirm
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.features.cashflow.presentation.review.DocumentReviewAction
import tech.dokus.features.cashflow.presentation.review.DocumentReviewContainer
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewSuccess
import tech.dokus.features.cashflow.presentation.review.screen.DocumentReviewScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactCreateOrigin
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

private const val CONTACT_RESULT_KEY = "documentReview_contactId"

@Composable
internal fun DocumentReviewRoute(
    documentId: DocumentId,
    container: DocumentReviewContainer = container(),
) {
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val pendingContactId = backStackEntry?.savedStateHandle?.get<String>(CONTACT_RESULT_KEY)

    LaunchedEffect(pendingContactId) {
        if (pendingContactId != null) {
            container.store.intent(
                DocumentReviewIntent.ContactCreated(ContactId.parse(pendingContactId))
            )
            backStackEntry?.savedStateHandle?.remove<String>(CONTACT_RESULT_KEY)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<DocumentReviewSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedRejectReason by remember { mutableStateOf(DocumentRejectReason.NotMyBusiness) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            DocumentReviewSuccess.DraftSaved ->
                stringResource(Res.string.cashflow_draft_saved)
            DocumentReviewSuccess.DocumentConfirmed ->
                stringResource(Res.string.cashflow_document_confirmed)
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is DocumentReviewAction.NavigateBack -> navController.popBackStack()
            is DocumentReviewAction.NavigateToChat -> {
                navController.navigateTo(CashFlowDestination.DocumentChat(action.documentId.toString()))
            }
            is DocumentReviewAction.NavigateToEntity -> navController.popBackStack()
            is DocumentReviewAction.ShowError -> pendingError = action.error
            is DocumentReviewAction.ShowSuccess -> pendingSuccess = action.success
            is DocumentReviewAction.ShowDiscardConfirmation -> {
                showDiscardDialog = true
            }
            is DocumentReviewAction.ShowRejectConfirmation -> {
                showRejectDialog = true
            }
        }
    }

    LaunchedEffect(documentId) {
        container.store.intent(DocumentReviewIntent.LoadDocument(documentId))
    }

    val isLargeScreen = LocalScreenSize.isLarge
    DocumentReviewScreen(
        state = state,
        isLargeScreen = isLargeScreen,
        onIntent = { container.store.intent(it) },
        onBackClick = { navController.popBackStack() },
        onOpenChat = { container.store.intent(DocumentReviewIntent.OpenChat) },
        onLinkExistingContact = {
            navController.navigateTo(
                ContactsDestination.CreateContact(
                    origin = ContactCreateOrigin.DocumentReview.name
                )
            )
        },
        onCreateNewContact = { counterparty ->
            container.store.intent(
                DocumentReviewIntent.SetCounterpartyIntent(CounterpartyIntent.Pending)
            )
            navController.navigateTo(
                ContactsDestination.CreateContact(
                    prefillCompanyName = counterparty.name,
                    prefillVat = counterparty.vatNumber,
                    prefillAddress = counterparty.address,
                    origin = ContactCreateOrigin.DocumentReview.name
                )
            )
        },
        snackbarHostState = snackbarHostState,
    )

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(Res.string.cashflow_discard_changes_title)) },
            text = { Text(stringResource(Res.string.cashflow_discard_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        container.store.intent(DocumentReviewIntent.ConfirmDiscardChanges)
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text(stringResource(Res.string.cashflow_reject_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.cashflow_reject_prompt))
                    RejectReasonOption(
                        label = stringResource(Res.string.cashflow_reject_reason_not_my_business),
                        reason = DocumentRejectReason.NotMyBusiness,
                        selected = selectedRejectReason == DocumentRejectReason.NotMyBusiness,
                        onSelected = { selectedRejectReason = it }
                    )
                    RejectReasonOption(
                        label = stringResource(Res.string.cashflow_reject_reason_duplicate),
                        reason = DocumentRejectReason.Duplicate,
                        selected = selectedRejectReason == DocumentRejectReason.Duplicate,
                        onSelected = { selectedRejectReason = it }
                    )
                    RejectReasonOption(
                        label = stringResource(Res.string.cashflow_reject_reason_spam),
                        reason = DocumentRejectReason.Spam,
                        selected = selectedRejectReason == DocumentRejectReason.Spam,
                        onSelected = { selectedRejectReason = it }
                    )
                    RejectReasonOption(
                        label = stringResource(Res.string.cashflow_reject_reason_test),
                        reason = DocumentRejectReason.Test,
                        selected = selectedRejectReason == DocumentRejectReason.Test,
                        onSelected = { selectedRejectReason = it }
                    )
                    RejectReasonOption(
                        label = stringResource(Res.string.cashflow_reject_reason_other),
                        reason = DocumentRejectReason.Other,
                        selected = selectedRejectReason == DocumentRejectReason.Other,
                        onSelected = { selectedRejectReason = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRejectDialog = false
                        container.store.intent(
                            DocumentReviewIntent.ConfirmReject(selectedRejectReason)
                        )
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun RejectReasonOption(
    label: String,
    reason: DocumentRejectReason,
    selected: Boolean,
    onSelected: (DocumentRejectReason) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = { onSelected(reason) }
        )
        Text(text = label)
    }
}
