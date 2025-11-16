package ai.dokus.foundation.design.components.navigation

import ai.dokus.foundation.design.model.HomeItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

@Composable
fun ColumnScope.DokusNavigationRail(
    selectedItem: HomeItem,
    navItems: List<HomeItem>,
    onSelectedItemChange: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val mainItems = navItems.dropLast(1)
        mainItems.forEach { item ->
            val isSelected = selectedItem == item
            SelectableCard(
                stringResource(item.title),
                item.icon,
                isSelected = isSelected,
                onClick = { onSelectedItemChange(item) }
            )
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    // Profile/Settings Section
    val profileItem = navItems.last()
    SelectableOutlineCard(
        stringResource(profileItem.title),
        profileItem.icon,
        selectedItem == profileItem
    ) { onSelectedItemChange(profileItem) }
}
