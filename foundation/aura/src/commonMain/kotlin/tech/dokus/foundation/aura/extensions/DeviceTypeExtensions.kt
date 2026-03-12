package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Laptop
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Smartphone
import com.composables.icons.lucide.Tablet
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.device_type_android
import tech.dokus.aura.resources.device_type_desktop
import tech.dokus.aura.resources.device_type_ios
import tech.dokus.aura.resources.device_type_tablet
import tech.dokus.aura.resources.device_type_web
import tech.dokus.domain.DeviceType

val DeviceType.iconized: ImageVector
    get() = when (this) {
        DeviceType.Android, DeviceType.Ios -> Lucide.Smartphone
        DeviceType.Tablet -> Lucide.Tablet
        DeviceType.Web -> Lucide.Globe
        DeviceType.Desktop -> Lucide.Laptop
    }

val DeviceType.localized: String
    @Composable get() = when (this) {
        DeviceType.Android -> stringResource(Res.string.device_type_android)
        DeviceType.Ios -> stringResource(Res.string.device_type_ios)
        DeviceType.Desktop -> stringResource(Res.string.device_type_desktop)
        DeviceType.Web -> stringResource(Res.string.device_type_web)
        DeviceType.Tablet -> stringResource(Res.string.device_type_tablet)
    }
