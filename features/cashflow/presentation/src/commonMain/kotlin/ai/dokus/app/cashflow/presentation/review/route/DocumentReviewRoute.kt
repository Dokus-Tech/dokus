package ai.dokus.app.cashflow.presentation.review.route

import ai.dokus.app.cashflow.presentation.review.DocumentReviewAction
import ai.dokus.app.cashflow.presentation.review.DocumentReviewContainer
import ai.dokus.app.cashflow.presentation.review.DocumentReviewIntent
import ai.dokus.app.cashflow.presentation.review.DocumentReviewState
import ai.dokus.app.cashflow.presentation.review.DocumentReviewSuccess
import ai.dokus.app.cashflow.presentation.review.screen.DocumentReviewScreen
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_document_confirmed
import tech.dokus.aura.resources.cashflow_draft_saved
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
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
                // TODO: Navigate to chat screen with processingId
            }
            is DocumentReviewAction.NavigateToEntity -> navController.popBackStack()
            is DocumentReviewAction.ShowError -> pendingError = action.error
            is DocumentReviewAction.ShowSuccess -> pendingSuccess = action.success
            is DocumentReviewAction.ShowDiscardConfirmation -> {
                // TODO: Show discard confirmation dialog
            }
            is DocumentReviewAction.ShowRejectConfirmation -> {
                // TODO: Show reject confirmation dialog
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
}
