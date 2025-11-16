package ai.dokus.foundation.design.components.navigation

import ai.dokus.foundation.design.model.HomeItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource

@Composable
fun DokusNavigationBar(
    navItems: List<HomeItem>,
    selectedItem: HomeItem,
    onSelectedItemChange: (HomeItem) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        navItems.forEach {
            NavigationBarItem(
                icon = { Icon(it.icon, contentDescription = null) },
                label = {
                    Text(
                        stringResource(it.title),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                selected = selectedItem == it,
                alwaysShowLabel = false,
                onClick = { onSelectedItemChange(it) }
            )
        }
    }
}
