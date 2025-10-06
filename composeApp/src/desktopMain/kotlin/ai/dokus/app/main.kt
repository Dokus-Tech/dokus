package ai.dokus.app

import ai.dokus.app.core.constrains.Constrains
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "The Predict",
        state = rememberWindowState(
            size = DpSize(
                width = Constrains.largeScreenWidth,
                height = Constrains.largeScreenHeight
            )
        )
    ) {
        App()
    }
}