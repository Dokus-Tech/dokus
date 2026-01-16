package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Check
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.cashflow_somethings_wrong
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.style.textMuted

/**
 * Simplified mobile footer for document review.
 *
 * Philosophy: Confirm is the action. Save is implementation detail.
 * - NO "Save" button on mobile (edits auto-apply to draft state)
 * - Only shows: "Something's wrong" link + "Confirm" button
 *
 * Must be used with imePadding() + navigationBarsPadding() for keyboard/safe-area handling.
 */
@Composable
internal fun MobileFooter(
    canConfirm: Boolean,
    isConfirming: Boolean,
    isBindingContact: Boolean,
    onConfirm: () -> Unit,
    onSomethingsWrong: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = isConfirming || isBindingContact

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "Something's wrong" text link (left side)
            TextButton(
                onClick = onSomethingsWrong,
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(Res.string.cashflow_somethings_wrong),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }

            // Confirm button only (right side) - NO Save button on mobile
            Button(
                onClick = onConfirm,
                enabled = canConfirm && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    PIcon(
                        icon = FeatherIcons.Check,
                        description = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(Res.string.action_confirm))
            }
        }
    }
}
