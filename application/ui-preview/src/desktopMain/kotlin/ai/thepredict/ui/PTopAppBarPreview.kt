package ai.thepredict.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator

private class PTopAppBarPreviewScreen : Screen {
    @Composable
    override fun Content() {
    }
}

@Composable
@Preview
fun PTopAppBarPreview() {
    Navigator(PTopAppBarPreviewScreen()) {
        Scaffold(
            topBar = { PTopAppBar("Preview") }
        ) {
            CurrentScreen()
        }
    }
}