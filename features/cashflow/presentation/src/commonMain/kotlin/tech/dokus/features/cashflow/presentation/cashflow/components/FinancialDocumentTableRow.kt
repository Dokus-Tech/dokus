package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.EllipsisVertical
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.document_table_more_options
import tech.dokus.aura.resources.document_table_view_details
import tech.dokus.foundation.aura.components.CashflowTypeBadge
import tech.dokus.foundation.aura.style.dokusSizing
import tech.dokus.foundation.aura.style.dokusSpacing

/**
 * Data row for the financial document table.
 */
@Composable
internal fun FinancialDocumentTableRow(
    row: FinancialDocumentRow,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = spacing.xLarge, vertical = spacing.xLarge),
        horizontalArrangement = Arrangement.spacedBy(spacing.xLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Invoice column with alert indicator
        Row(
            modifier = Modifier.width(spacing.large * 8.75f),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alert dot if needed
            if (row.hasAlert) {
                Box(
                    modifier = Modifier
                        .size(sizing.strokeCropGuide * 2f)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error) // Red alert indicator
                )
            }

            Text(
                text = row.invoiceNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Contact column with avatar and info
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(sizing.buttonHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer), // Light background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Lucide.User,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer, // Icon color
                    modifier = Modifier.size(sizing.buttonLoadingIcon)
                )
            }

            // Name and email
            Column(
                verticalArrangement = Arrangement.spacedBy(spacing.xxSmall)
            ) {
                Text(
                    text = row.contactName.ifBlank {
                        stringResource(Res.string.common_unknown)
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (row.contactEmail.isNotEmpty()) {
                    Text(
                        text = row.contactEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Amount column
        Text(
            text = row.amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(spacing.large * 6.25f)
        )

        // Date column
        Column(
            modifier = Modifier.width(spacing.large * 7.5f),
            verticalArrangement = Arrangement.spacedBy(spacing.xSmall)
        ) {
            Text(
                text = row.date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Type badge
        Box(
            modifier = Modifier.width(spacing.large * 6.25f),
            contentAlignment = Alignment.CenterStart
        ) {
            CashflowTypeBadge(type = row.cashflowType)
        }

        // Actions column
        Row(
            modifier = Modifier.width(spacing.large * 4.5f),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // More menu button
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(sizing.iconMedium)
            ) {
                Icon(
                    imageVector = Lucide.EllipsisVertical,
                    contentDescription = stringResource(Res.string.document_table_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron right button
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(sizing.iconMedium)
            ) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = stringResource(Res.string.document_table_view_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
