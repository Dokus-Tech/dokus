package tech.dokus.app.navigation.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

internal val LocalRootNavController = staticCompositionLocalOf<NavController> {
    error("No root NavController provided")
}

@Composable
internal fun RootNavControllerProvided(
    navController: NavController,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRootNavController provides navController) {
        content()
    }
}
