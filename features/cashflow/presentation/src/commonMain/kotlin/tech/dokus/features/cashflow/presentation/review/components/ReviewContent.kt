package tech.dokus.features.cashflow.presentation.review.components

import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ContactSelectionSection
import tech.dokus.features.cashflow.presentation.review.DocumentReviewFooter
import tech.dokus.features.cashflow.presentation.review.PdfPreviewBottomSheet
import tech.dokus.features.cashflow.presentation.review.PdfPreviewPane
import tech.dokus.features.cashflow.presentation.review.PdfPreviewRow
import tech.dokus.features.cashflow.presentation.review.components.details.AmountsCard
import tech.dokus.features.cashflow.presentation.review.components.details.CounterpartyCard
import tech.dokus.features.cashflow.presentation.review.components.details.InvoiceDetailsCard
import tech.dokus.features.cashflow.presentation.review.components.forms.UnsavedChangesBar
import tech.dokus.features.cashflow.presentation.review.models.CounterpartyInfo
import tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_counterparty_details_title
import tech.dokus.aura.resources.cashflow_loading_document
import tech.dokus.aura.resources.cashflow_unknown_document_type
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
internal fun ReviewContent(
    state: DocumentReviewState,
    isLargeScreen: Boolean,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
    onLinkExistingContact: () -> Unit,
    onCreateNewContact: (CounterpartyInfo) -> Unit,
) {
    when (state) {
        is DocumentReviewState.Loading -> {
            LoadingContent(contentPadding)
        }

        is DocumentReviewState.Content -> {
            val counterparty = remember(state.editableData) { counterpartyInfo(state) }
            if (isLargeScreen) {
                DesktopReviewContent(
                    state = state,
                    contentPadding = contentPadding,
                    onIntent = onIntent,
                    onLinkExistingContact = onLinkExistingContact,
                    onCreateNewContact = { onCreateNewContact(counterparty) },
                )
            } else {
                MobileReviewContent(
                    state = state,
                    contentPadding = contentPadding,
                    onIntent = onIntent,
                    onLinkExistingContact = onLinkExistingContact,
                    onCreateNewContact = { onCreateNewContact(counterparty) },
                )
            }
        }

        is DocumentReviewState.Error -> {
            ErrorContent(
                error = state,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.cashflow_loading_document),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    error: DocumentReviewState.Error,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constrains.Spacing.large),
        contentAlignment = Alignment.Center
    ) {
        DokusErrorContent(
            exception = error.exception,
            retryHandler = error.retryHandler
        )
    }
}

@Composable
private fun DesktopReviewContent(
    state: DocumentReviewState.Content,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
    onLinkExistingContact: () -> Unit,
    onCreateNewContact: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constrains.Spacing.large),
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.large)
    ) {
        DocumentPreviewPane(
            previewState = state.previewState,
            selectedFieldPath = state.selectedFieldPath,
            onLoadMore = { maxPages -> onIntent(DocumentReviewIntent.LoadMorePages(maxPages)) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        ReviewDetailsPane(
            state = state,
            onIntent = onIntent,
            onLinkExistingContact = onLinkExistingContact,
            onCreateNewContact = onCreateNewContact,
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun DocumentPreviewPane(
    previewState: DocumentPreviewState,
    selectedFieldPath: String?,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier,
    ) {
        PdfPreviewPane(
            state = previewState,
            selectedFieldPath = selectedFieldPath,
            onLoadMore = onLoadMore,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ReviewDetailsPane(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    onLinkExistingContact: () -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.cashflow_counterparty_details_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = Constrains.Spacing.small),
        )

        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = Constrains.Spacing.large),
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium),
            ) {
                CounterpartyCard(
                    state = state,
                    onIntent = onIntent,
                    onLinkExistingContact = onLinkExistingContact,
                    onCreateNewContact = onCreateNewContact,
                    modifier = Modifier.fillMaxWidth(),
                )
                InvoiceDetailsCard(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxWidth(),
                )
                AmountsCard(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (scrollState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.background,
                                ),
                            )
                        )
                )
            }
        }
    }
}


@Composable
private fun MobileReviewContent(
    state: DocumentReviewState.Content,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
    onLinkExistingContact: () -> Unit,
    onCreateNewContact: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(Constrains.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            AnimatedVisibility(
                visible = state.hasUnsavedChanges && !state.isDocumentConfirmed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                UnsavedChangesBar(
                    isSaving = state.isSaving,
                    onSave = { onIntent(DocumentReviewIntent.SaveDraft) },
                    onDiscard = { onIntent(DocumentReviewIntent.DiscardChanges) }
                )
            }

            DokusCardSurface(
                modifier = Modifier.fillMaxWidth(),
            ) {
                PdfPreviewRow(
                    previewState = state.previewState,
                    onClick = { onIntent(DocumentReviewIntent.OpenPreviewSheet) },
                )
            }

            CounterpartyCard(
                state = state,
                onIntent = onIntent,
                onLinkExistingContact = onLinkExistingContact,
                onCreateNewContact = onCreateNewContact,
                modifier = Modifier.fillMaxWidth(),
            )

            InvoiceDetailsCard(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxWidth(),
            )

            AmountsCard(
                state = state,
                onIntent = onIntent,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        DocumentReviewFooter(
            canConfirm = state.canConfirm,
            isConfirming = state.isConfirming,
            isSaving = state.isSaving,
            isBindingContact = state.isBindingContact,
            hasUnsavedChanges = state.hasUnsavedChanges,
            isDocumentConfirmed = state.isDocumentConfirmed,
            confirmBlockedReason = state.confirmBlockedReason,
            onConfirm = { onIntent(DocumentReviewIntent.Confirm) },
            onSaveChanges = { onIntent(DocumentReviewIntent.SaveDraft) },
            onReject = { onIntent(DocumentReviewIntent.Reject) },
            onOpenChat = { onIntent(DocumentReviewIntent.OpenChat) },
        )
    }

    PdfPreviewBottomSheet(
        isVisible = state.showPreviewSheet,
        onDismiss = { onIntent(DocumentReviewIntent.ClosePreviewSheet) },
        previewState = state.previewState,
        onLoadMore = { maxPages -> onIntent(DocumentReviewIntent.LoadMorePages(maxPages)) },
    )
}
