package ai.dokus.app.contacts.components.create

import ai.dokus.app.contacts.viewmodel.DuplicateVatUi
import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Banner shown when a contact with the same VAT number already exists.
 * This is a hard block - user cannot create duplicate VAT contacts.
 */
@Composable
fun DuplicateVatBanner(
    duplicate: DuplicateVatUi,
    onViewContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(Constrains.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(Constrains.Spacing.medium))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Contact already exists",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "${duplicate.displayName} (${duplicate.vatNumber})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }

        TextButton(onClick = onViewContact) {
            Text(
                text = "View",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
