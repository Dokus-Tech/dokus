package tech.dokus.features.banking.presentation.payments.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_filter_all_accounts
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun AccountFilterDropdown(
    accounts: Map<BankAccountId, String>,
    selectedAccountId: BankAccountId?,
    onSelect: (BankAccountId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (accounts.size < 2) return

    var expanded by remember { mutableStateOf(false) }
    val displayText = selectedAccountId?.let { accounts[it] }
        ?: stringResource(Res.string.banking_filter_all_accounts)

    Surface(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable { expanded = true },
        shape = RoundedCornerShape(Constraints.Spacing.small),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Constraints.Stroke.thin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Constraints.Spacing.medium,
                vertical = Constraints.Spacing.xSmall,
            ),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "\u25BE",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(Res.string.banking_filter_all_accounts),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (selectedAccountId == null) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            accounts.forEach { (id, name) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (id == selectedAccountId) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    },
                )
            }
        }
    }
}
