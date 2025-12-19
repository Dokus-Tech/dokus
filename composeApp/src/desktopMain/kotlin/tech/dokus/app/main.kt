package tech.dokus.app

import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "D[#]kus",
        state = rememberWindowState(
            size = DpSize(
                width = Constrains.largeScreenDefaultWidth,
                height = Constrains.largeScreenHeight
            )
        )
    ) {
        App()
    }
}