package tech.dokus.features.cashflow.presentation.detail.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.components.ReviewContent
import tech.dokus.features.cashflow.presentation.detail.components.ReviewTopBar
import tech.dokus.features.cashflow.presentation.detail.components.previewReviewContentState
import tech.dokus.features.cashflow.presentation.detail.components.previewSourceEvidenceViewerState
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.foundation.app.shell.LocalIsInDocDetailMode
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun DocumentDetailScreen(
    state: DocumentDetailState,
    isLargeScreen: Boolean,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentDetailIntent) -> Unit,
    onBackClick: () -> Unit,
    backLabel: String = "",
    onOpenSource: (DocumentSourceId) -> Unit,
    onCorrectContact: (ResolvedContact) -> Unit,
    onCreateContact: (ResolvedContact) -> Unit,
) {
    val isInDetailMode = LocalIsInDocDetailMode.current

    Scaffold(
        topBar = {
            // In detail mode, DocumentDetailMode provides its own title bar
            if (isLargeScreen && !isInDetailMode) {
                ReviewTopBar(
                    state = state,
                    onBackClick = onBackClick,
                    backLabel = backLabel,
                )
            }
        },
        // In detail mode, use transparent so the glass content Surface shows through
        containerColor = if (isInDetailMode) {
            Color.Transparent
        } else {
            MaterialTheme.colorScheme.background
        },
        modifier = Modifier,
    ) { contentPadding ->
        Column {
            state.actionError?.let { error ->
                DokusErrorBanner(
                    exception = error,
                    retryHandler = null,
                    modifier = Modifier.padding(horizontal = Constraints.Spacing.large),
                    onDismiss = { onIntent(DocumentDetailIntent.DismissActionError) },
                )
            }

            ReviewContent(
                state = state,
                isLargeScreen = isLargeScreen,
                isAccountantReadOnly = isAccountantReadOnly,
                contentPadding = contentPadding,
                onIntent = onIntent,
                onCorrectContact = onCorrectContact,
                onCreateContact = onCreateContact,
                onBackClick = onBackClick,
                onOpenSource = onOpenSource,
            )
        }
    }
}

@Preview(name = "Document Review - Mobile Loading", widthDp = 430, heightDp = 900)
@Composable
private fun DocumentDetailScreenLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentDetailScreen(
            state = DocumentDetailState(),
            isLargeScreen = false,
            isAccountantReadOnly = false,
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}

@Preview(name = "Document Review - Desktop Open", widthDp = 1366, heightDp = 900)
@Composable
private fun DocumentDetailScreenDesktopOpenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentDetailScreen(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Open),
            isLargeScreen = true,
            isAccountantReadOnly = false,
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}

@Preview(name = "Document Review - Desktop Source", widthDp = 1366, heightDp = 900)
@Composable
private fun DocumentDetailScreenDesktopSourcePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        val baseState = previewReviewContentState(entryStatus = CashflowEntryStatus.Open)
        val sources = baseState.documentRecord?.sources.orEmpty()
        val peppolSourceId = sources
            .firstOrNull { it.sourceChannel == DocumentSource.Peppol }
            ?.id
            ?: sources.first().id
        val sourceViewerState = previewSourceEvidenceViewerState(sourceType = DocumentSource.Peppol)
            .copy(sourceId = peppolSourceId)

        DocumentDetailScreen(
            state = baseState.copy(sourceViewerState = sourceViewerState),
            isLargeScreen = true,
            isAccountantReadOnly = false,
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}

@Preview(name = "Document Review - Desktop Paid", widthDp = 1366, heightDp = 900)
@Composable
private fun DocumentDetailScreenDesktopPaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentDetailScreen(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Paid),
            isLargeScreen = true,
            isAccountantReadOnly = false,
            onIntent = {},
            onBackClick = {},
            onOpenSource = {},
            onCorrectContact = {},
            onCreateContact = {},
        )
    }
}
