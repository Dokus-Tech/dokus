package tech.dokus.features.cashflow.presentation.review.components.bankstatement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bank_statement_duplicate_banner_subtitle
import tech.dokus.aura.resources.bank_statement_duplicate_banner_title
import tech.dokus.aura.resources.bank_statement_excluded
import tech.dokus.aura.resources.bank_statement_to_import
import org.jetbrains.compose.resources.stringResource
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.foundation.layout.Box

@Composable
internal fun BankStatementDuplicateBanner(
    duplicateCount: Int,
    totalCount: Int,
    includedCount: Int,
    excludedCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Constraints.Spacing.small))
            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
            .padding(Constraints.Spacing.large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = Constraints.Spacing.xSmall)
                    .size(Constraints.Spacing.small)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)) {
                Text(
                    text = stringResource(Res.string.bank_statement_duplicate_banner_title, duplicateCount, totalCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.bank_statement_duplicate_banner_subtitle, includedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large)) {
            Text(
                text = stringResource(Res.string.bank_statement_to_import, includedCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.bank_statement_excluded, excludedCount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
