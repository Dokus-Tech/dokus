package ai.dokus.foundation.design.components.navigation

import ai.dokus.foundation.design.constrains.Constrains
import ai.dokus.foundation.design.model.HomeItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
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
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        val mainItems = navItems.dropLast(1)
        mainItems.forEach { item ->
            val isSelected = selectedItem == item
            SelectableCard(
                title = stringResource(item.titleRes),
                icon = painterResource(item.iconRes),
                isSelected = isSelected,
                onClick = { onSelectedItemChange(item) }
            )
        }
    }

    Spacer(modifier = Modifier.weight(1f))

    // Profile/Settings Section
    val profileItem = navItems.last()
    SelectableOutlineCard(
        stringResource(profileItem.titleRes),
        painterResource(profileItem.iconRes),
        selectedItem == profileItem
    ) { onSelectedItemChange(profileItem) }
}
