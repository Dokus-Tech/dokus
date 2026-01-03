package tech.dokus.foundation.aura.components.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
fun ColumnScope.NavigationRail(
    selectedItem: TabNavItem,
    navItems: List<TabNavItem>,
    onSelectedItemChange: (TabNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        val mainItems = navItems.dropLast(1)
        mainItems.forEach { item ->
            val isSelected = selectedItem == item
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectedItemChange(item) },
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
                        .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Icon(
                        painter = item.icon,
                        contentDescription = item.title,
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.size(Constrains.IconSize.small)
                    )
                    Spacer(modifier = Modifier.width(Constrains.Spacing.small))
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    // Profile Section
    val profileItem = navItems.last()
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectedItemChange(profileItem) },
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            Constrains.Stroke.thin,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                painter = profileItem.icon,
                contentDescription = profileItem.title,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(Constrains.IconSize.smallMedium)
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text(
                text = profileItem.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
