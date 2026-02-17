package tech.dokus.foundation.aura.style

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import platform.Foundation.NSNumber
import platform.Foundation.setValue
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import platform.UIKit.UIWindowScene

@Composable
internal actual fun SystemBarEffect(isDark: Boolean) {
    // UIUserInterfaceStyle values: 1 = Light, 2 = Dark
    val styleValue = NSNumber(long = if (isDark) 2L else 1L)
    SideEffect {
        val windowScene = UIApplication.sharedApplication.connectedScenes
            .filterIsInstance<UIWindowScene>()
            .firstOrNull() ?: return@SideEffect
        for (window in windowScene.windows) {
            (window as? UIView)?.setValue(styleValue, forKey = "overrideUserInterfaceStyle")
        }
    }
}
