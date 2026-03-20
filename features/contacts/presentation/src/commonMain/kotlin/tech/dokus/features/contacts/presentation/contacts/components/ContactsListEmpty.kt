package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.UserPlus
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_add_first
import tech.dokus.aura.resources.contacts_add_first_hint
import tech.dokus.aura.resources.contacts_empty
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// UI dimension constants
private val ListItemSpacing = Constraints.Spacing.medium
private val EmptyStatePadding = Constraints.Spacing.xxLarge
private val EmptyStateIconSize = Constraints.IconSize.xxLarge
private val EmptyStateSpacingMedium = Constraints.Spacing.large
private val EmptyStateSpacingSmall = Constraints.Spacing.small
private val EmptyStateSpacingLarge = Constraints.Spacing.xLarge
private val CtaCardPadding = Constraints.Spacing.large
private val CtaIconSpacing = Constraints.Spacing.small
private val SkeletonShimmerLineHeight = Constraints.IconSize.smallMedium
private val SkeletonShimmerBoxWidth = Constraints.IconSize.xLarge + Constraints.Spacing.xxSmall
private val SkeletonShimmerBoxHeight = Constraints.Spacing.large
private val SkeletonCornerRadius = Constraints.CornerRadius.badge
private val SkeletonRowSpacing = Constraints.Spacing.large
private val SkeletonSpacingSmall = Constraints.Spacing.small
private val SkeletonEmailHeight = Constraints.Height.shimmerLine
private val SkeletonTagWidth = Constraints.IconSize.xxLarge - Constraints.Spacing.xSmall
private val SkeletonTagWidthSmall = Constraints.IconSize.xLarge + Constraints.Spacing.xxSmall
private val SkeletonTagHeight = Constraints.IconSize.smallMedium
private val SkeletonTagSpacing = Constraints.Spacing.xSmall
private val LoadingMorePadding = Constraints.Spacing.large
private const val SkeletonItemCount = 6
private const val EmptyStateIconAlpha = 0.6f
private const val EmptyStateHintAlpha = 0.7f
private const val SkeletonNameWeight = 0.6f
private const val SkeletonEmailWidthFraction = 0.7f

// =============================================================================
// Empty State
// =============================================================================

/**
 * Empty state when no contacts exist.
 * Shows a call-to-action to add the first contact.
 */
@Composable
internal fun ContactsEmptyState(
    onAddContactClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(EmptyStatePadding)
        ) {
            Icon(
                imageVector = Lucide.UserPlus,
                contentDescription = null,
                modifier = Modifier
                    .height(EmptyStateIconSize)
                    .width(EmptyStateIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = EmptyStateIconAlpha)
            )

            Spacer(modifier = Modifier.height(EmptyStateSpacingMedium))

            Text(
                text = stringResource(Res.string.contacts_empty),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(EmptyStateSpacingSmall))

            Text(
                text = stringResource(Res.string.contacts_add_first_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = EmptyStateHintAlpha),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(EmptyStateSpacingLarge))

            if (onAddContactClick != null) {
                DokusCardSurface(
                    onClick = onAddContactClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CtaCardPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Lucide.UserPlus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(CtaIconSpacing))
                        Text(
                            text = stringResource(Res.string.contacts_add_first),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Skeletons
// =============================================================================

/**
 * Skeleton loading state for the contacts list.
 */
@Composable
internal fun ContactsListSkeleton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemSpacing)
    ) {
        items(SkeletonItemCount) {
            ContactCardSkeleton()
        }
    }
}

/**
 * Skeleton for a single contact card.
 */
@Composable
internal fun ContactCardSkeleton(
    modifier: Modifier = Modifier
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium)
        ) {
            // Name and status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerLine(
                    modifier = Modifier.weight(SkeletonNameWeight),
                    height = SkeletonShimmerLineHeight
                )
                Spacer(modifier = Modifier.width(SkeletonRowSpacing))
                ShimmerBox(
                    modifier = Modifier
                        .width(SkeletonShimmerBoxWidth)
                        .height(SkeletonShimmerBoxHeight),
                    shape = RoundedCornerShape(SkeletonCornerRadius)
                )
            }

            Spacer(modifier = Modifier.height(SkeletonSpacingSmall))

            // Email line
            ShimmerLine(
                modifier = Modifier.fillMaxWidth(SkeletonEmailWidthFraction),
                height = SkeletonEmailHeight
            )

            Spacer(modifier = Modifier.height(SkeletonSpacingSmall))

            // Tags row
            Row(
                horizontalArrangement = Arrangement.spacedBy(SkeletonTagSpacing)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(SkeletonTagWidth)
                        .height(SkeletonTagHeight),
                    shape = RoundedCornerShape(SkeletonCornerRadius)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(SkeletonTagWidthSmall)
                        .height(SkeletonTagHeight),
                    shape = RoundedCornerShape(SkeletonCornerRadius)
                )
            }
        }
    }
}

/**
 * Loading indicator for infinite scroll pagination.
 */
@Composable
internal fun ContactsLoadingMoreIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = LoadingMorePadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DokusLoader(size = DokusLoaderSize.Small)
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@Preview
@Composable
private fun ContactsListPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 1, 15, 10, 0)
    val contacts = listOf(
        ContactDto(
            id = ContactId.generate(),
            tenantId = TenantId.generate(),
            name = Name("Acme Corporation"),
            email = Email("info@acme.be"),
            vatNumber = VatNumber("BE0123456789"),
            isActive = true,
            derivedRoles = DerivedContactRoles(
                isCustomer = true,
                isSupplier = false
            ),
            createdAt = now,
            updatedAt = now
        ),
        ContactDto(
            id = ContactId.generate(),
            tenantId = TenantId.generate(),
            name = Name("TechStart BVBA"),
            email = Email("hello@techstart.be"),
            isActive = true,
            derivedRoles = DerivedContactRoles(
                isCustomer = true,
                isSupplier = true
            ),
            createdAt = now,
            updatedAt = now
        )
    )
    TestWrapper(parameters) {
        ContactsList(
            state = DokusState.success(
                PaginationState(
                    data = contacts,
                    currentPage = 1,
                    hasMorePages = false
                )
            ),
            onContactClick = {},
            onLoadMore = {},
            onAddContactClick = {}
        )
    }
}
