package tech.dokus.foundation.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

interface HomeShellTopBarHost {
    fun update(route: String, config: HomeShellTopBarConfig)
    fun clear(route: String)
}

data class HomeShellTopBarConfig(
    val enabled: Boolean = true,
    val mode: HomeShellTopBarMode,
    val actions: List<HomeShellTopBarAction> = emptyList(),
)

sealed interface HomeShellTopBarMode {
    data class Title(
        val title: String,
        val subtitle: String? = null,
    ) : HomeShellTopBarMode
}

sealed interface HomeShellTopBarAction {
    data class Icon(
        val icon: ImageVector,
        val contentDescription: String,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
    ) : HomeShellTopBarAction

    data class Text(
        val label: String,
        val onClick: () -> Unit,
        val enabled: Boolean = true,
    ) : HomeShellTopBarAction
}

private object NoOpHomeShellTopBarHost : HomeShellTopBarHost {
    override fun update(route: String, config: HomeShellTopBarConfig) = Unit
    override fun clear(route: String) = Unit
}

val LocalHomeShellTopBarHost = staticCompositionLocalOf<HomeShellTopBarHost> {
    NoOpHomeShellTopBarHost
}

@Composable
fun RegisterHomeShellTopBar(
    route: String,
    config: HomeShellTopBarConfig?,
) {
    val host = LocalHomeShellTopBarHost.current

    DisposableEffect(host, route) {
        onDispose {
            host.clear(route)
        }
    }

    SideEffect {
        if (config == null) {
            host.clear(route)
        } else {
            host.update(route, config)
        }
    }
}
