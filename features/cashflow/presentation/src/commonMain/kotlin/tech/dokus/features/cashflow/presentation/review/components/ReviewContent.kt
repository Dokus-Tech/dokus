package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_awaiting_extraction
import tech.dokus.aura.resources.cashflow_loading_document
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.components.mobile.MobileCanonicalContent
import tech.dokus.features.cashflow.presentation.review.components.mobile.MobileCanonicalHeader
import tech.dokus.features.cashflow.presentation.review.components.mobile.MobileDocumentDetailTopBar
import tech.dokus.features.cashflow.presentation.review.components.mobile.PreviewTabContent
import tech.dokus.features.cashflow.presentation.review.models.CounterpartyInfo
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.constrains.withContentPadding
import tech.dokus.foundation.aura.extensions.dismissKeyboardOnTapOutside
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun ReviewContent(
    state: DocumentReviewState,
    isLargeScreen: Boolean,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: (CounterpartyInfo) -> Unit,
    onCreateContact: (CounterpartyInfo) -> Unit,
    onBackClick: () -> Unit,
    onOpenSource: (DocumentSourceId) -> Unit,
) {
    when (state) {
        is DocumentReviewState.Loading -> LoadingContent(contentPadding)
        is DocumentReviewState.AwaitingExtraction -> AwaitingExtractionContent(
            state = state,
            contentPadding = contentPadding,
            isLargeScreen = isLargeScreen
        )
        is DocumentReviewState.Content -> {
            if (isLargeScreen) {
                DesktopReviewContent(
                    state = state,
                    contentPadding = contentPadding,
                    onIntent = onIntent,
                )
            } else {
                MobileReviewContent(
                    state = state,
                    contentPadding = contentPadding,
                    onIntent = onIntent,
                    onBackClick = onBackClick,
                    onOpenSource = onOpenSource,
                )
            }
        }
        is DocumentReviewState.Error -> ErrorContent(state, contentPadding)
    }
}

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            DokusLoader()
            Text(
                text = stringResource(Res.string.cashflow_loading_document),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                .padding(Constraints.Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        ) {
            DokusCardSurface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                tech.dokus.features.cashflow.presentation.review.PdfPreviewPane(
                    state = state.previewState,
                    selectedFieldPath = null,
                    onLoadMore = {},
                    modifier = Modifier.fillMaxSize(),
                    showScanAnimation = true
                )
            }
            DokusCardSurface(
                modifier = Modifier
                    .width(Constraints.DocumentDetail.inspectorWidth)
                    .fillMaxHeight(),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.cashflow_awaiting_extraction),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }
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
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                    .padding(Constraints.Spacing.medium),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.cashflow_awaiting_extraction),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

@Composable
private fun DesktopReviewContent(
    state: DocumentReviewState.Content,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constraints.Spacing.small),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        CanonicalCenterPane(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )
        ReviewInspectorPane(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .width(Constraints.DocumentDetail.inspectorWidth)
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun MobileReviewContent(
    state: DocumentReviewState.Content,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
    onBackClick: () -> Unit,
    onOpenSource: (DocumentSourceId) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val adjustedContentPadding = contentPadding.withTopRemoved(layoutDirection)

    if (state.shouldUsePdfFallback) {
        MobileFallbackContent(
            state = state,
            contentPadding = adjustedContentPadding,
            onBackClick = onBackClick,
        )
        return
    }

    MobileCanonicalContent(
        state = state,
        onIntent = onIntent,
        onBackClick = onBackClick,
        onOpenSource = onOpenSource,
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(adjustedContentPadding, layoutDirection)
            .imePadding()
            .dismissKeyboardOnTapOutside()
            .navigationBarsPadding(),
    )
}

@Composable
private fun MobileFallbackContent(
    state: DocumentReviewState.Content,
    contentPadding: PaddingValues,
    onBackClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .withContentPadding(contentPadding, androidx.compose.ui.platform.LocalLayoutDirection.current),
    ) {
        MobileDocumentDetailTopBar(
            state = state,
            onBackClick = onBackClick,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Constraints.Spacing.medium)
                .padding(top = Constraints.Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            MobileCanonicalHeader(state = state)
            Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))
            DokusCardSurface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = Constraints.Spacing.small),
            ) {
                PreviewTabContent(
                    previewState = state.previewState,
                    showScanAnimation = state.isProcessing,
                    modifier = Modifier.fillMaxSize(),
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
            .padding(Constraints.Spacing.large),
        contentAlignment = Alignment.Center,
    ) {
        DokusErrorContent(
            exception = error.exception,
            retryHandler = error.retryHandler
        )
    }
}

private fun PaddingValues.withTopRemoved(layoutDirection: LayoutDirection): PaddingValues = PaddingValues(
    start = calculateStartPadding(layoutDirection),
    top = 0.dp,
    end = calculateEndPadding(layoutDirection),
    bottom = calculateBottomPadding(),
)
