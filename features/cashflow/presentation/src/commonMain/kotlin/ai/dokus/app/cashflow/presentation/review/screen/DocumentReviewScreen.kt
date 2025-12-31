package ai.dokus.app.cashflow.presentation.review.screen

import ai.dokus.app.cashflow.presentation.review.DocumentReviewIntent
import ai.dokus.app.cashflow.presentation.review.DocumentReviewState
import ai.dokus.app.cashflow.presentation.review.components.ReviewContent
import ai.dokus.app.cashflow.presentation.review.components.ReviewTopBar
import ai.dokus.app.cashflow.presentation.review.models.CounterpartyInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun DocumentReviewScreen(
    state: DocumentReviewState,
    isLargeScreen: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onBackClick: () -> Unit,
    onOpenChat: () -> Unit,
    onLinkExistingContact: () -> Unit,
    onCreateNewContact: (CounterpartyInfo) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        topBar = {
            ReviewTopBar(
                state = state,
                isLargeScreen = isLargeScreen,
                onBackClick = onBackClick,
                onChatClick = onOpenChat,
                onConfirmClick = { onIntent(DocumentReviewIntent.Confirm) },
                onRejectClick = { onIntent(DocumentReviewIntent.Reject) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier,
    ) { contentPadding ->
        ReviewContent(
            state = state,
            isLargeScreen = isLargeScreen,
            contentPadding = contentPadding,
            onIntent = onIntent,
            onLinkExistingContact = onLinkExistingContact,
            onCreateNewContact = onCreateNewContact,
        )
    }
}
