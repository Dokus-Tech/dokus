package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_add_first
import tech.dokus.aura.resources.contacts_add_first_hint
import tech.dokus.aura.resources.contacts_empty
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.MonogramAvatar
import tech.dokus.foundation.aura.components.badges.ContactRole as UiContactRole
import tech.dokus.foundation.aura.components.badges.RoleBadge
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textMuted

// UI dimension constants
private val ErrorPaddingVertical = Constraints.Spacing.xxxLarge
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
private const val InfiniteScrollThreshold = 5
private const val EmptyStateIconAlpha = 0.6f
private const val EmptyStateHintAlpha = 0.7f
private const val SkeletonNameWeight = 0.6f
private const val SkeletonEmailWidthFraction = 0.7f

/**
 * ContactsList component with scrollable list and empty state.
 * Handles loading, success, error, and empty states.
 *
 * @param state The DokusState containing pagination state for contacts
 * @param onContactClick Callback when a contact is clicked
 * @param onLoadMore Callback for infinite scroll pagination
 * @param onAddContactClick Callback when the empty state add button is clicked
 * @param contentPadding Optional content padding
 * @param modifier Optional modifier
 * @param selectedContactId Currently selected contact for highlight (desktop)
 * @param isDesktop Desktop mode renders simple list rows instead of cards
 */
@Composable
internal fun ContactsList(
    state: DokusState<PaginationState<ContactDto>>,
    onContactClick: (ContactDto) -> Unit,
    onLoadMore: () -> Unit,
    onAddContactClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(Constraints.Elevation.none),
    modifier: Modifier = Modifier,
    selectedContactId: ContactId? = null,
    isDesktop: Boolean = false,
) {
    val listState = rememberLazyListState()

    // Extract pagination state for infinite scroll (if available)
    val paginationState = (state as? DokusState.Success)?.data

    // Infinite scroll trigger
    LaunchedEffect(listState, paginationState?.hasMorePages, paginationState?.isLoadingMore) {
        if (paginationState == null) return@LaunchedEffect
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }
            .distinctUntilChanged()
            .filter { (last, total) ->
                (last + 1) > (total - InfiniteScrollThreshold) &&
                    paginationState.hasMorePages &&
                    !paginationState.isLoadingMore
            }
            .collect { onLoadMore() }
    }

    when (state) {
        is DokusState.Loading, is DokusState.Idle -> {
            ContactsListSkeleton(
                contentPadding = contentPadding,
                modifier = modifier
            )
        }

        is DokusState.Success -> {
            if (state.data.data.isEmpty()) {
                ContactsEmptyState(
                    onAddContactClick = onAddContactClick,
                    modifier = modifier.padding(contentPadding)
                )
            } else {
                ContactsListContent(
                    contacts = state.data.data,
                    listState = listState,
                    isLoadingMore = state.data.isLoadingMore,
                    onContactClick = onContactClick,
                    contentPadding = contentPadding,
                    modifier = modifier,
                    selectedContactId = selectedContactId,
                    isDesktop = isDesktop,
                )
            }
        }

        is DokusState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .padding(vertical = ErrorPaddingVertical),
                contentAlignment = Alignment.Center
            ) {
                DokusErrorContent(
                    exception = state.exception,
                    retryHandler = state.retryHandler
                )
            }
        }
    }
}

/**
 * The actual list content showing contact items.
 * Desktop: compact rows with avatar. Mobile: cards.
 */
@Composable
private fun ContactsListContent(
    contacts: List<ContactDto>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    onContactClick: (ContactDto) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    selectedContactId: ContactId? = null,
    isDesktop: Boolean = false,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = if (isDesktop) Arrangement.Top else Arrangement.spacedBy(ListItemSpacing)
    ) {
        items(
            items = contacts,
            key = { it.id.toString() }
        ) { contact ->
            if (isDesktop) {
                ContactListItem(
                    contact = contact,
                    isSelected = contact.id == selectedContactId,
                    onClick = { onContactClick(contact) },
                )
            } else {
                ContactCard(
                    contact = contact,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContactClick(contact) }
                )
            }
        }

        // Loading more indicator
        if (isLoadingMore) {
            item {
                ContactsLoadingMoreIndicator()
            }
        }
    }
}

// =============================================================================
// Desktop List Item
// =============================================================================

/**
 * Desktop master list row: MonogramAvatar + name + RoleBadge + doc count.
 * Selected: warm bg + 2dp amber right border.
 */
@Composable
private fun ContactListItem(
    contact: ContactDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val surfaceHover = MaterialTheme.colorScheme.surfaceHover
    val borderAmberColor = MaterialTheme.colorScheme.borderAmber

    val bgColor = when {
        isSelected || isHovered -> surfaceHover
        else -> Color.Transparent
    }

    val initials = remember(contact.name.value) { extractInitials(contact.name.value) }
    val uiRole = remember(contact.derivedRoles) { mapToUiRole(contact.derivedRoles) }
    val docCount = contact.invoiceCount + contact.inboundInvoiceCount + contact.expenseCount

    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .background(bgColor)
            .then(
                if (isSelected) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(
                            color = borderAmberColor,
                            topLeft = Offset(size.width - 2.dp.toPx(), 0f),
                            size = Size(2.dp.toPx(), size.height)
                        )
                    }
                } else Modifier
            )
            .padding(horizontal = Constraints.Spacing.medium, vertical = Constraints.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        MonogramAvatar(
            initials = initials,
            size = 32.dp,
            radius = 8.dp,
            selected = isSelected,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.5.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (uiRole != null) {
                RoleBadge(role = uiRole)
            }
        }

        if (docCount > 0) {
            Text(
                text = docCount.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                ),
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
    }
}

// =============================================================================
// Empty State
// =============================================================================

/**
 * Empty state when no contacts exist.
 * Shows a call-to-action to add the first contact.
 */
@Composable
private fun ContactsEmptyState(
    onAddContactClick: () -> Unit,
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
                imageVector = Icons.Default.PersonAdd,
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

            // Empty state CTA card
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
                        imageVector = Icons.Default.PersonAdd,
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

// =============================================================================
// Skeletons
// =============================================================================

/**
 * Skeleton loading state for the contacts list.
 */
@Composable
private fun ContactsListSkeleton(
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
private fun ContactCardSkeleton(
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
private fun ContactsLoadingMoreIndicator(
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactsListPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    val now = kotlinx.datetime.LocalDateTime(2026, 1, 15, 10, 0)
    val contacts = listOf(
        ContactDto(
            id = tech.dokus.domain.ids.ContactId.generate(),
            tenantId = tech.dokus.domain.ids.TenantId.generate(),
            name = tech.dokus.domain.Name("Acme Corporation"),
            email = tech.dokus.domain.Email("info@acme.be"),
            vatNumber = tech.dokus.domain.ids.VatNumber("BE0123456789"),
            isActive = true,
            derivedRoles = tech.dokus.domain.model.contact.DerivedContactRoles(
                isCustomer = true,
                isSupplier = false
            ),
            createdAt = now,
            updatedAt = now
        ),
        ContactDto(
            id = tech.dokus.domain.ids.ContactId.generate(),
            tenantId = tech.dokus.domain.ids.TenantId.generate(),
            name = tech.dokus.domain.Name("TechStart BVBA"),
            email = tech.dokus.domain.Email("hello@techstart.be"),
            isActive = true,
            derivedRoles = tech.dokus.domain.model.contact.DerivedContactRoles(
                isCustomer = true,
                isSupplier = true
            ),
            createdAt = now,
            updatedAt = now
        )
    )
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
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
