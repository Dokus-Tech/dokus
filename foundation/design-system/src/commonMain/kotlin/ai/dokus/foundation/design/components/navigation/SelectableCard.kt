package ai.dokus.foundation.design.components.navigation

import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun SelectableCard(
    title: String,
    icon: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.Transparent
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Constrains.Elevation.none)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.size(Constrains.IconSize.small)
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun SelectableOutlineCard(
    title: String,
    icon: Painter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            Constrains.Stroke.thin,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.Transparent
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Constrains.IconSize.small),
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
