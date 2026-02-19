package tech.dokus.features.cashflow.presentation.review.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.components.ReviewContent
import tech.dokus.features.cashflow.presentation.review.components.ReviewTopBar
import tech.dokus.features.cashflow.presentation.review.models.CounterpartyInfo
import tech.dokus.foundation.app.shell.LocalIsInDocDetailMode

@Composable
internal fun DocumentReviewScreen(
    state: DocumentReviewState,
    isLargeScreen: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onBackClick: () -> Unit,
    onOpenChat: () -> Unit,
    onCorrectContact: (CounterpartyInfo) -> Unit,
    onCreateContact: (CounterpartyInfo) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val isInDetailMode = LocalIsInDocDetailMode.current

    Scaffold(
        topBar = {
            // In detail mode, DocumentDetailMode provides its own title bar
            if (isLargeScreen && !isInDetailMode) {
                ReviewTopBar(
                    state = state,
                    isLargeScreen = isLargeScreen,
                    onBackClick = onBackClick,
                    onChatClick = onOpenChat,
                    onConfirmClick = { onIntent(DocumentReviewIntent.Confirm) },
                    onRejectClick = { onIntent(DocumentReviewIntent.ShowFeedbackDialog) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // In detail mode, use transparent so the glass content Surface shows through
        containerColor = if (isInDetailMode) Color.Transparent
            else MaterialTheme.colorScheme.background,
        modifier = Modifier,
    ) { contentPadding ->
        ReviewContent(
            state = state,
            isLargeScreen = isLargeScreen,
            contentPadding = contentPadding,
            onIntent = onIntent,
            onCorrectContact = onCorrectContact,
            onCreateContact = onCreateContact,
            onBackClick = onBackClick,
        )
    }
}
