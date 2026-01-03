package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_activity_summary
import tech.dokus.aura.resources.contacts_bills
import tech.dokus.aura.resources.contacts_expenses
import tech.dokus.aura.resources.contacts_invoices
import tech.dokus.aura.resources.contacts_last_activity_value
import tech.dokus.aura.resources.contacts_pending_approval_plural
import tech.dokus.aura.resources.contacts_pending_approval_single
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.ShimmerLine

// UI dimension constants
private val SectionSpacing = 16.dp
private val ContentSpacing = 12.dp
private val StatCardIconSize = 24.dp
private val StatCardSpacing = 4.dp
private val LastActivityIconSize = 16.dp
private val LastActivitySpacing = 8.dp
private val PendingPadding = 12.dp
private val SkeletonIconSize = 24.dp
private val SkeletonCountWidth = 40.dp
private val SkeletonCountHeight = 28.dp
private val SkeletonLabelWidth = 50.dp
private val SkeletonLabelHeight = 12.dp
private val SkeletonTotalWidth = 60.dp
private val SkeletonTotalHeight = 12.dp
private val ErrorPaddingVertical = 32.dp
private const val SkeletonRepeatCount = 3

@Composable
internal fun ActivitySummarySection(
    state: DokusState<ContactActivitySummary>,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            Text(
                text = stringResource(Res.string.contacts_activity_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            when (state) {
                is DokusState.Loading, is DokusState.Idle -> {
                    ActivitySummarySkeleton()
                }
                is DokusState.Success -> {
                    ActivitySummaryContent(activity = state.data)
                }
                is DokusState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
    }
}

@Composable
private fun ActivitySummaryContent(
    activity: ContactActivitySummary
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ContentSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            ActivityStatCard(
                icon = Icons.Default.Description,
                title = stringResource(Res.string.contacts_invoices),
                count = activity.invoiceCount.toString(),
                total = activity.invoiceTotal,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            ActivityStatCard(
                icon = Icons.Default.Receipt,
                title = stringResource(Res.string.contacts_bills),
                count = activity.billCount.toString(),
                total = activity.billTotal,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            ActivityStatCard(
                icon = Icons.Default.ShoppingCart,
                title = stringResource(Res.string.contacts_expenses),
                count = activity.expenseCount.toString(),
                total = activity.expenseTotal,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        activity.lastActivityDate?.let { lastActivity ->
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LastActivitySpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(LastActivityIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(Res.string.contacts_last_activity_value, formatDateTime(lastActivity)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (activity.pendingApprovalCount > 0) {
            val pendingCount = activity.pendingApprovalCount
            val pendingText = if (pendingCount == 1L) {
                stringResource(Res.string.contacts_pending_approval_single, pendingCount)
            } else {
                stringResource(Res.string.contacts_pending_approval_plural, pendingCount)
            }
            DokusCardSurface(
                variant = DokusCardVariant.Soft,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PendingPadding),
                    horizontalArrangement = Arrangement.spacedBy(LastActivitySpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pendingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityStatCard(
    icon: ImageVector,
    title: String,
    count: String,
    total: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier,
        padding = DokusCardPadding.Dense,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(StatCardSpacing)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(StatCardIconSize),
                tint = color
            )
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = total,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivitySummarySkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SectionSpacing)
    ) {
        repeat(SkeletonRepeatCount) {
            DokusCardSurface(
                modifier = Modifier.weight(1f),
                variant = DokusCardVariant.Soft,
            ) {
                Column(
                    modifier = Modifier.padding(PendingPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(StatCardSpacing)
                ) {
                    ShimmerLine(modifier = Modifier.size(SkeletonIconSize), height = SkeletonIconSize)
                    ShimmerLine(modifier = Modifier.width(SkeletonCountWidth), height = SkeletonCountHeight)
                    ShimmerLine(modifier = Modifier.width(SkeletonLabelWidth), height = SkeletonLabelHeight)
                    ShimmerLine(modifier = Modifier.width(SkeletonTotalWidth), height = SkeletonTotalHeight)
                }
            }
        }
    }
}
