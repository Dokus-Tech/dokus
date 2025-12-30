package tech.dokus.app

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.app_name
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.stringResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = stringResource(Res.string.app_name),
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
