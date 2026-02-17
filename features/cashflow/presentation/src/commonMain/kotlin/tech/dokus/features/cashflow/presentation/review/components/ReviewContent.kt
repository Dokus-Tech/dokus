package tech.dokus.features.cashflow.presentation.review.components

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import tech.dokus.foundation.aura.components.common.DokusLoader
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_awaiting_extraction
import tech.dokus.aura.resources.cashflow_contact_details_title
import tech.dokus.aura.resources.cashflow_loading_document
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewFooter
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.PdfPreviewPane
import tech.dokus.features.cashflow.presentation.review.components.details.AmountsCard
import tech.dokus.features.cashflow.presentation.review.components.details.CounterpartyCard
import tech.dokus.features.cashflow.presentation.review.components.details.InvoiceDetailsCard
import tech.dokus.features.cashflow.presentation.review.components.details.PeppolStatusCard
import tech.dokus.features.cashflow.presentation.review.components.details.SourcesCard
import tech.dokus.features.cashflow.presentation.review.components.mobile.DetailsTabContent
import tech.dokus.features.cashflow.presentation.review.components.mobile.DocumentDetailMobileHeader
import tech.dokus.features.cashflow.presentation.review.components.mobile.DocumentDetailTabBar
import tech.dokus.features.cashflow.presentation.review.components.mobile.MobileFooter
import tech.dokus.features.cashflow.presentation.review.components.mobile.PreviewTabContent
import tech.dokus.features.cashflow.presentation.review.components.mobile.TAB_DETAILS
import tech.dokus.features.cashflow.presentation.review.components.mobile.TAB_PREVIEW
import tech.dokus.features.cashflow.presentation.review.models.CounterpartyInfo
import tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.constrains.withContentPadding

@Composable
internal fun ReviewContent(
    state: DocumentReviewState,
    isLargeScreen: Boolean,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: (CounterpartyInfo) -> Unit,
    onCreateContact: (CounterpartyInfo) -> Unit,
    onBackClick: () -> Unit,
) {
    when (state) {
        is DocumentReviewState.Loading -> {
            LoadingContent(contentPadding)
        }

        is DocumentReviewState.AwaitingExtraction -> {
            AwaitingExtractionContent(state, contentPadding, isLargeScreen)
        }

        is DocumentReviewState.Content -> {
            val counterparty = remember(state.draftData) { counterpartyInfo(state) }
            if (isLargeScreen) {
                DesktopReviewContent(
                    state = state,
                    contentPadding = contentPadding,
                    onIntent = onIntent,
                    onCorrectContact = { onCorrectContact(counterparty) },
                    onCreateContact = { onCreateContact(counterparty) },
                )
            } else {
                MobileReviewContent(
                    state = state,
                    contentPadding = contentPadding,
                    onIntent = onIntent,
                    onCorrectContact = { onCorrectContact(counterparty) },
                    onCreateContact = { onCreateContact(counterparty) },
                    onBackClick = onBackClick,
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
private fun AwaitingExtractionContent(
    state: DocumentReviewState.AwaitingExtraction,
    contentPadding: PaddingValues,
    isLargeScreen: Boolean,
) {
    if (isLargeScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(Constrains.Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.large)
        ) {
            DokusCardSurface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                PdfPreviewPane(
                    state = state.previewState,
                    selectedFieldPath = null,
                    onLoadMore = {},
                    modifier = Modifier.fillMaxSize(),
                    showScanAnimation = true
                )
            }

            AwaitingExtractionStatusPanel(
                filename = state.document.document.filename,
                modifier = Modifier
                    .width(420.dp)
                    .fillMaxHeight()
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            PreviewTabContent(
                previewState = state.previewState,
                showScanAnimation = true,
            )

            // Gradient overlay with status at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface,
                            ),
                        )
                    )
                    .padding(Constrains.Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(Res.string.cashflow_awaiting_extraction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.document.document.filename?.let { filename ->
                    Text(
                        text = filename,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AwaitingExtractionStatusPanel(
    filename: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.cashflow_awaiting_extraction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            filename?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
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
            isProcessing = state.isProcessing,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        ReviewDetailsPane(
            state = state,
            onIntent = onIntent,
            onCorrectContact = onCorrectContact,
            onCreateContact = onCreateContact,
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
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier,
    ) {
        PdfPreviewPane(
            state = previewState,
            selectedFieldPath = selectedFieldPath,
            onLoadMore = onLoadMore,
            modifier = Modifier.fillMaxSize(),
            showScanAnimation = isProcessing
        )
    }
}

@Composable
private fun ReviewDetailsPane(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.cashflow_contact_details_title),
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
                // Show failure banner at top when extraction failed
                AnimatedVisibility(
                    visible = state.isFailed && !state.failureBannerDismissed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    AnalysisFailedBanner(
                        reason = state.failureReason,
                        isRetrying = state.isProcessing,
                        onRetry = { onIntent(DocumentReviewIntent.RetryAnalysis) },
                        onContinueManually = { onIntent(DocumentReviewIntent.DismissFailureBanner) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                CounterpartyCard(
                    state = state,
                    onIntent = onIntent,
                    onCorrectContact = onCorrectContact,
                    onCreateContact = onCreateContact,
                    modifier = Modifier.fillMaxWidth(),
                )
                InvoiceDetailsCard(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.fillMaxWidth(),
                )
                PeppolStatusCard(
                    state = state,
                    modifier = Modifier.fillMaxWidth(),
                )
                SourcesCard(
                    state = state,
                    onResolveSame = { onIntent(DocumentReviewIntent.ResolvePossibleMatchSame) },
                    onResolveDifferent = { onIntent(DocumentReviewIntent.ResolvePossibleMatchDifferent) },
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

/**
 * Mobile tabbed layout for document review.
 *
 * Two tabs: Preview (document view) | Details (fact validation)
 * - Default tab is Details if hasAttention (reactive-once, after state loads)
 * - User's manual tab selection is respected (not overridden)
 * - Footer has only "Something's wrong" + "Confirm" (no Save on mobile)
 */
@Composable
private fun MobileReviewContent(
    state: DocumentReviewState.Content,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
    onBackClick: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    // Track if user has manually changed tab (respect their choice)
    var userChangedTab by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(TAB_PREVIEW) }

    // Auto-switch to Details on first load if attention needed
    // Only if user hasn't manually selected a tab yet
    LaunchedEffect(state.hasAttention) {
        if (!userChangedTab && state.hasAttention) {
            selectedTab = TAB_DETAILS
        }
    }

    // Show post-confirmation footer if document is confirmed or rejected
    val showPostConfirmation = state.isDocumentConfirmed || state.isDocumentRejected

    Column(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(contentPadding, layoutDirection)
    ) {
        // Header (fixed)
        DocumentDetailMobileHeader(
            description = state.description,
            total = state.totalAmount,
            hasAttention = state.hasAttention,
            isBlocking = state.isBlocking,
            onBackClick = onBackClick
        )

        // Tab bar (fixed) - only show when not confirmed/rejected
        if (!showPostConfirmation) {
            DocumentDetailTabBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    userChangedTab = true // Mark that user took control
                    selectedTab = tab
                }
            )
        }

        // Tab content (flexible, fills remaining space)
        Box(modifier = Modifier.weight(1f)) {
            if (showPostConfirmation) {
                // After confirmation, show details only
                DetailsTabContent(
                    state = state,
                    onIntent = onIntent,
                    onCorrectContact = onCorrectContact,
                    onCreateContact = onCreateContact
                )
            } else {
                when (selectedTab) {
                    TAB_PREVIEW -> PreviewTabContent(
                        previewState = state.previewState,
                        showScanAnimation = state.isProcessing
                    )
                    TAB_DETAILS -> DetailsTabContent(
                        state = state,
                        onIntent = onIntent,
                        onCorrectContact = onCorrectContact,
                        onCreateContact = onCreateContact
                    )
                }
            }
        }

        // Footer (fixed, with keyboard/safe-area handling)
        if (showPostConfirmation) {
            // Use the existing post-confirmation footer
            DocumentReviewFooter(
                canConfirm = state.canConfirm,
                isConfirming = state.isConfirming,
                isSaving = state.isSaving,
                isBindingContact = state.isBindingContact,
                isRejecting = state.isRejecting,
                hasUnsavedChanges = state.hasUnsavedChanges,
                isDocumentConfirmed = state.isDocumentConfirmed,
                isDocumentRejected = state.isDocumentRejected,
                hasCashflowEntry = state.confirmedCashflowEntryId != null,
                confirmBlockedReason = state.confirmBlockedReason,
                onConfirm = { onIntent(DocumentReviewIntent.Confirm) },
                onSaveChanges = { onIntent(DocumentReviewIntent.SaveDraft) },
                onReject = { onIntent(DocumentReviewIntent.ShowFeedbackDialog) },
                onOpenChat = { onIntent(DocumentReviewIntent.OpenChat) },
                onViewEntity = { onIntent(DocumentReviewIntent.ViewEntity) },
                onViewCashflowEntry = { onIntent(DocumentReviewIntent.ViewCashflowEntry) },
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
            )
        } else {
            // Simplified mobile footer (no Save button)
            MobileFooter(
                canConfirm = state.canConfirm,
                isConfirming = state.isConfirming,
                isBindingContact = state.isBindingContact,
                onConfirm = { onIntent(DocumentReviewIntent.Confirm) },
                onSomethingsWrong = { onIntent(DocumentReviewIntent.ShowFeedbackDialog) },
                modifier = Modifier
                    .imePadding()
                    .navigationBarsPadding()
            )
        }
    }
}
