package ai.dokus.foundation.design.tooling

import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Settings
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.home_dashboard
import ai.dokus.app.resources.generated.home_settings
import ai.dokus.app.resources.generated.home_users

// Simple mock values for previews
val mockEmail = Email("john.doe@dokus.be")
val mockName = Name("John")
val mockLastName = Name("Doe")
val mockUserId = UserId("preview-user-id")

val homeItemMocks = listOf(
    HomeItem(Res.string.home_dashboard, Icons.Default.Dashboard, HomeDestination.Dashboard),
    HomeItem(Res.string.home_settings, Icons.Default.Settings, HomeDestination.Settings)
)