package tech.dokus.features.cashflow.presentation.review.components.mobile

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import tech.dokus.foundation.aura.components.common.DokusLoader
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertCircle
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_no_preview
import tech.dokus.aura.resources.cashflow_preview_error
import tech.dokus.aura.resources.cashflow_preview_page_failed
import tech.dokus.aura.resources.cashflow_preview_page_label
import tech.dokus.domain.model.DocumentPagePreviewDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ScanningLineOverlay
import tech.dokus.features.cashflow.presentation.review.components.AnalysisFailedBanner
import tech.dokus.features.cashflow.presentation.review.components.details.AmountsCard
import tech.dokus.features.cashflow.presentation.review.components.details.CounterpartyCard
import tech.dokus.features.cashflow.presentation.review.components.details.InvoiceDetailsCard
import tech.dokus.features.cashflow.presentation.review.components.details.PeppolStatusCard
import tech.dokus.features.cashflow.presentation.review.components.details.SourcesCard
import tech.dokus.features.cashflow.presentation.review.rememberAuthenticatedImageLoader
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints

private const val A4_ASPECT_RATIO = 0.707f

/**
 * Preview tab content - shows PDF pages with lazy loading.
 *
 * Performance requirements (critical for mobile):
 * - Lazy load pages (only render visible + buffer)
 * - Don't render all pages at once
 */
@Composable
internal fun PreviewTabContent(
    previewState: DocumentPreviewState,
    modifier: Modifier = Modifier,
    showScanAnimation: Boolean = false
) {
    val imageLoader = rememberAuthenticatedImageLoader()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (previewState) {
            is DocumentPreviewState.Loading -> {
                DokusLoader()
            }
            is DocumentPreviewState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = FeatherIcons.AlertCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(Res.string.cashflow_preview_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is DocumentPreviewState.Ready -> {
                val pages = previewState.pages
                if (pages.isEmpty()) {
                    NoPreviewPlaceholder()
                } else {
                    // Use LazyColumn for page list - only renders visible pages
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Constraints.Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
                    ) {
                        itemsIndexed(pages, key = { _, page -> "page_${page.page}" }) { index, page ->
                            MobilePdfPageImage(
                                page = page,
                                imageLoader = imageLoader,
                                modifier = Modifier.fillMaxWidth(),
                                showScanAnimation = showScanAnimation
                            )
                        }
                    }
                }
            }
            DocumentPreviewState.NotPdf,
            DocumentPreviewState.NoPreview -> {
                NoPreviewPlaceholder()
            }
        }
    }
}

@Composable
private fun MobilePdfPageImage(
    page: DocumentPagePreviewDto,
    imageLoader: coil3.ImageLoader,
    modifier: Modifier = Modifier,
    showScanAnimation: Boolean = false
) {
    DokusCardSurface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SubcomposeAsyncImage(
                model = page.imageUrl,
                contentDescription = stringResource(Res.string.cashflow_preview_page_label, page.page),
                imageLoader = imageLoader,
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(A4_ASPECT_RATIO)
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusLoader()
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(A4_ASPECT_RATIO)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(Res.string.cashflow_preview_page_failed, page.page),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )

            if (showScanAnimation) {
                ScanningLineOverlay(modifier = Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun NoPreviewPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Constraints.Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.padding(bottom = Constraints.Spacing.medium),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.cashflow_no_preview),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Details tab content - fact validation, not form filling.
 *
 * Critical: Keep fact blocks with micro-labels (not big "section cards").
 * Only missing/uncertain fields render as editable inputs.
 * Everything else stays as text.
 */
@Composable
internal fun DetailsTabContent(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(Constraints.Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large)
    ) {
        // Show failure banner when extraction failed
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

        // Contact section (fact block with micro-label)
        CounterpartyCard(
            state = state,
            onIntent = onIntent,
            onCorrectContact = onCorrectContact,
            onCreateContact = onCreateContact,
            modifier = Modifier.fillMaxWidth()
        )

        // Document details section (fact display, not form)
        InvoiceDetailsCard(
            state = state,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth()
        )
        PeppolStatusCard(
            state = state,
            modifier = Modifier.fillMaxWidth()
        )
        SourcesCard(
            state = state,
            onResolveSame = { onIntent(DocumentReviewIntent.ResolvePossibleMatchSame) },
            onResolveDifferent = { onIntent(DocumentReviewIntent.ResolvePossibleMatchDifferent) },
            modifier = Modifier.fillMaxWidth()
        )

        // Amounts section (text with tabular nums, not inputs)
        AmountsCard(
            state = state,
            onIntent = onIntent,
            modifier = Modifier.fillMaxWidth()
        )

        // Line items (if invoice with items)
        if (state.draftData is InvoiceDraftData &&
            state.draftData.lineItems.isNotEmpty()) {
            // LineItemsSection would go here if needed
        }

        // Bottom padding for keyboard
        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))
    }
}
