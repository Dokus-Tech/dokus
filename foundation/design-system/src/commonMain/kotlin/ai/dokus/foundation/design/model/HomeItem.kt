package ai.dokus.foundation.design.model

import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

@Immutable
data class HomeItem(
    val title: StringResource,
    val icon: ImageVector,
    val destination: HomeDestination,
    val showTopBar: Boolean = true,
    val priority: HomeItemPriority = HomeItemPriority.Medium,
)

enum class HomeItemPriority {
    High,
    Medium,
    Low
}