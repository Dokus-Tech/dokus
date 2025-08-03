package ai.thepredict.ui.navigation

import ai.thepredict.ui.Themed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun NavigationBarPreview() {
    Themed {
        NavigationBar(
            navigationItems = NavigationItem.all,
            fabItem = NavigationItem.AddDocument,
            selectedIndex = 0
        ) {}
    }
}