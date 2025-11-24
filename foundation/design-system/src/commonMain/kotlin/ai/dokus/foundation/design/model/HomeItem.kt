package ai.dokus.foundation.design.model

import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Immutable
data class HomeItem(
    val titleRes: StringResource,
    val iconRes: DrawableResource,
    val destination: HomeDestination,
    val showTopBar: Boolean = true,
    val priority: HomeItemPriority = HomeItemPriority.Medium,
)

enum class HomeItemPriority {
    High,
    Medium,
    Low
}