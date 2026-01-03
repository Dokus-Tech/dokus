package tech.dokus.navigation.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController

val LocalNavController = staticCompositionLocalOf<NavController> { error("No NavController provided") }

@Composable
fun NavControllerProvided(navController: NavController, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalNavController provides navController) {
        content()
    }
}
