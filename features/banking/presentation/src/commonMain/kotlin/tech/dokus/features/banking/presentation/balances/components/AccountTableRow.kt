package tech.dokus.features.banking.presentation.balances.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_col_account
import tech.dokus.aura.resources.banking_balances_col_balance
import tech.dokus.aura.resources.banking_balances_col_last_sync
import tech.dokus.aura.resources.banking_balances_col_status
import tech.dokus.aura.resources.banking_balances_col_type
import tech.dokus.aura.resources.banking_balances_status_inactive
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankAccountProvider
import tech.dokus.domain.enums.BankAccountStatus
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.layout.DokusHeaderColumn
import tech.dokus.foundation.aura.components.layout.DokusTableHeader
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.positionPositive
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// =============================================================================
// Table column specs
// =============================================================================

@Immutable
internal object BalancesTableColumns {
    val Account = DokusTableColumnSpec(weight = 1f)
    val Type = DokusTableColumnSpec(width = 90.dp)
    val Balance = DokusTableColumnSpec(width = 140.dp)
    val LastSync = DokusTableColumnSpec(width = 90.dp)
    val Status = DokusTableColumnSpec(width = 80.dp)
}

// =============================================================================
// Desktop: accounts table
// =============================================================================

@Composable
internal fun DesktopAccountsTable(
    accounts: List<BankAccountDto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        DokusTableHeader(
            columns = listOf(
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_account),
                    weight = 1f,
                ),
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_type),
                    width = 90.dp,
                ),
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_balance),
                    width = 140.dp,
                ),
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_last_sync),
                    width = 90.dp,
                ),
                DokusHeaderColumn(
                    label = stringResource(Res.string.banking_balances_col_status),
                    width = 80.dp,
                ),
            ),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        accounts.forEachIndexed { index, account ->
            AccountTableRow(account = account)
            if (index < accounts.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
internal fun AccountTableRow(
    account: BankAccountDto,
    modifier: Modifier = Modifier,
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = Constraints.Height.input,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large),
    ) {
        // Account: name + IBAN
        DokusTableCell(BalancesTableColumns.Account) {
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = formatIban(account.iban.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                )
            }
        }

        // Type badge
        DokusTableCell(BalancesTableColumns.Type) {
            AccountTypeBadge(type = account.accountType)
        }

        // Balance + provider
        DokusTableCell(BalancesTableColumns.Balance) {
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)) {
                account.balance?.let { balance ->
                    Amt(minorUnits = balance.minor)
                }
                Text(
                    text = account.provider.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                )
            }
        }

        // Last sync
        DokusTableCell(BalancesTableColumns.LastSync) {
            account.balanceUpdatedAt?.let { updatedAt ->
                Text(
                    text = formatRelativeTime(updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                )
            }
        }

        // Status
        DokusTableCell(BalancesTableColumns.Status) {
            StatusIndicator(status = account.status, isActive = account.isActive)
        }
    }
}

// =============================================================================
// Mobile: card list
// =============================================================================

@Composable
internal fun MobileAccountsList(
    accounts: List<BankAccountDto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        accounts.forEachIndexed { index, account ->
            MobileAccountCard(account = account)
            if (index < accounts.size - 1) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
internal fun MobileAccountCard(
    account: BankAccountDto,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = formatIban(account.iban.value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AccountTypeBadge(type = account.accountType)
                StatusIndicator(status = account.status, isActive = account.isActive)
            }
        }

        account.balance?.let { balance ->
            Amt(minorUnits = balance.minor)
        }
    }
}

// =============================================================================
// Shared components
// =============================================================================

@Composable
internal fun AccountTypeBadge(
    type: BankAccountType,
    modifier: Modifier = Modifier,
) {
    val (backgroundColor, textColor) = remember(type) {
        when (type) {
            BankAccountType.Current -> Color(0xFF3B82F6).copy(alpha = 0.12f) to Color(0xFF3B82F6)
            BankAccountType.Savings -> Color(0xFF10B981).copy(alpha = 0.12f) to Color(0xFF10B981)
            BankAccountType.CreditCard -> Color(0xFFF59E0B).copy(alpha = 0.12f) to Color(0xFFF59E0B)
        }
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.extraSmall,
            )
            .padding(
                horizontal = Constraints.Spacing.small,
                vertical = Constraints.Spacing.xxSmall,
            ),
    ) {
        Text(
            text = type.name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
internal fun StatusIndicator(
    status: BankAccountStatus,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = when {
        !isActive -> MaterialTheme.colorScheme.textMuted
        status == BankAccountStatus.PendingReview -> MaterialTheme.colorScheme.statusWarning
        else -> MaterialTheme.colorScheme.positionPositive
    }
    val label = if (!isActive) {
        stringResource(Res.string.banking_balances_status_inactive)
    } else {
        status.localized
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(Constraints.StatusDot.size)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
            maxLines = 1,
        )
    }
}

/**
 * Formats an IBAN with spaces every 4 characters for display.
 */
internal fun formatIban(iban: String): String {
    val clean = iban.replace(" ", "")
    return clean.chunked(4).joinToString(" ")
}

// =============================================================================
// Previews
// =============================================================================

private val PreviewAccount = BankAccountDto(
    id = BankAccountId.generate(),
    tenantId = TenantId.generate(),
    iban = Iban("BE68539007547034"),
    name = "KBC Business",
    institutionName = "KBC",
    accountType = BankAccountType.Current,
    currency = Currency.Eur,
    provider = BankAccountProvider.Tink,
    balance = Money(1438042),
    balanceUpdatedAt = LocalDateTime(2026, 3, 9, 15, 0),
    status = BankAccountStatus.Confirmed,
    isActive = true,
    createdAt = LocalDateTime(2026, 1, 1, 0, 0),
)

private val PreviewAccountInactive = BankAccountDto(
    id = BankAccountId.generate(),
    tenantId = TenantId.generate(),
    iban = Iban("BE42063012345678"),
    name = "Belfius Savings",
    institutionName = "Belfius",
    accountType = BankAccountType.Savings,
    currency = Currency.Eur,
    provider = BankAccountProvider.Coda,
    balance = Money(340000),
    balanceUpdatedAt = LocalDateTime(2026, 3, 8, 10, 0),
    status = BankAccountStatus.Confirmed,
    isActive = false,
    createdAt = LocalDateTime(2026, 1, 1, 0, 0),
)

@Preview(name = "Account Table Row", widthDp = 800)
@Composable
private fun AccountTableRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column {
                AccountTableRow(account = PreviewAccount)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AccountTableRow(account = PreviewAccountInactive)
            }
        }
    }
}

@Preview(name = "Mobile Account Card", widthDp = 390)
@Composable
private fun MobileAccountCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column {
                MobileAccountCard(account = PreviewAccount)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                MobileAccountCard(account = PreviewAccountInactive)
            }
        }
    }
}
