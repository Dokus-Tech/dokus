package tech.dokus.foundation.aura.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@Composable
internal actual fun SystemBarEffect(isDark: Boolean) {
    val style = if (isDark) UIUserInterfaceStyle.UIUserInterfaceStyleDark else UIUserInterfaceStyle.UIUserInterfaceStyleLight
    SideEffect {
        val windowScene = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull() ?: return@SideEffect
        for (window in windowScene.windows) {
            (window as? UIWindow)?.overrideUserInterfaceStyle = style
        }
    }
}
