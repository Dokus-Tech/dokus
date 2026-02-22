package tech.dokus.features.cashflow.presentation.review.screen

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.components.ReviewContent
import tech.dokus.features.cashflow.presentation.review.components.ReviewTopBar
import tech.dokus.features.cashflow.presentation.review.models.CounterpartyInfo
import tech.dokus.foundation.app.shell.LocalIsInDocDetailMode
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun DocumentReviewScreen(
    state: DocumentReviewState,
    isLargeScreen: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    onBackClick: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenSource: (DocumentSourceId) -> Unit,
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
        containerColor = if (isInDetailMode) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.background
        },
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
            onOpenSource = onOpenSource,
        )
    }
}

@Preview
@Composable
private fun DocumentReviewScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentReviewScreen(
            state = DocumentReviewState.Loading(),
            isLargeScreen = false,
            onIntent = {},
            onBackClick = {},
            onOpenChat = {},
            onOpenSource = {},
            onCorrectContact = {},
            onCreateContact = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
