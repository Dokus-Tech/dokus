package ai.dokus.app

import ai.dokus.foundation.ui.constrains.Constrains
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Dokus",
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