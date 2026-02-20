package tech.dokus.app

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.app_name
import tech.dokus.foundation.aura.constrains.Constraints

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
        state = rememberWindowState(
            size = DpSize(
                width = Constraints.largeScreenDefaultWidth,
                height = Constraints.largeScreenHeight
            )
        )
    ) {
        window.styleForMacOs()

        App()
    }
}

private fun ComposeWindow.styleForMacOs() {
    // macOS-only: content under titlebar + transparent titlebar
    with(rootPane) {
        putClientProperty("apple.awt.fullWindowContent", true)
        putClientProperty("apple.awt.transparentTitleBar", true)
        putClientProperty("apple.awt.windowTitleVisible", false)
    }

    // Optional: keep title empty
    title = ""
}
