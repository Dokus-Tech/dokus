package tech.dokus.app.navigation.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

internal val LocalHomeNavController = staticCompositionLocalOf<NavController> {
    error("No home NavController provided")
}

@Composable
internal fun HomeNavControllerProvided(
    navController: NavController,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalHomeNavController provides navController) {
        content()
    }
}
