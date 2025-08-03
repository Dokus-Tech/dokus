package ai.thepredict.ui.navigation

import ai.thepredict.ui.Themed
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun NavigationBarPreview() {
    Themed {
        NavigationBar(
            tabNavItems = TabNavItem.all,
            fabItem = TabNavItem.AddDocuments,
            selectedIndex = 0
        ) {}
    }
}