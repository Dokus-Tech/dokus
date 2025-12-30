package tech.dokus.foundation.aura.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.foundation.aura.style.brandGold

@Composable
fun ColumnScope.DokusNavigationRail(
    selectedItem: HomeItem,
    navItems: List<HomeItem>,
    onSelectedItemChange: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        val mainItems = navItems.dropLast(1)
        mainItems.forEach { item ->
            val isSelected = selectedItem == item

            RailItemContainer(isSelected = isSelected) {
                SelectableCard(
                    title = stringResource(item.titleRes),
                    icon = painterResource(item.iconRes),
                    isSelected = isSelected,
                    onClick = { onSelectedItemChange(item) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    // Profile / Settings (kept visually separated and slightly more "outlined")
    val profileItem = navItems.last()
    val isProfileSelected = selectedItem == profileItem
    RailItemContainer(isSelected = isProfileSelected) {
        SelectableOutlineCard(
            stringResource(profileItem.titleRes),
            painterResource(profileItem.iconRes),
            isProfileSelected
        ) { onSelectedItemChange(profileItem) }
    }
}

@Composable
private fun RailItemContainer(
    isSelected: Boolean,
    content: @Composable () -> Unit
) {
    // A thin brand-gold indicator for selection. We do not paint the whole item gold.
    // This keeps Dokus calm and premium.
    val indicatorColor =
        MaterialTheme.colorScheme.brandGold.copy(alpha = if (isSelected) 0.9f else 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // Left selection indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(3.dp)
                .height(22.dp)
                .background(indicatorColor, shape = MaterialTheme.shapes.extraSmall)
        )

        // Content with spacing from the indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp)
        ) {
            content()
        }
    }
}
