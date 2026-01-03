package tech.dokus.foundation.aura.model

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import tech.dokus.navigation.destinations.HomeDestination

@Immutable
data class HomeItem(
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    val destination: HomeDestination,
    val showTopBar: Boolean = false,
    val priority: HomeItemPriority = HomeItemPriority.Medium,
)

enum class HomeItemPriority {
    High,
    Medium,
    Low
}
