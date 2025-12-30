package tech.dokus.foundation.aura.tooling

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.chart_bar_trend_up
import ai.dokus.app.resources.generated.home_dashboard
import ai.dokus.app.resources.generated.home_settings
import ai.dokus.app.resources.generated.user
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.foundation.aura.model.HomeItemPriority
import ai.dokus.foundation.navigation.destinations.HomeDestination
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.ids.UserId

// Simple mock values for previews
val mockEmail = Email("john.doe@dokus.be")
val mockName = Name("John")
val mockLastName = Name("Doe")
val mockUserId = UserId("preview-user-id")

val homeItemMocks = listOf(
    HomeItem(
        titleRes = Res.string.home_dashboard,
        iconRes = Res.drawable.chart_bar_trend_up,
        destination = HomeDestination.Dashboard,
        priority = HomeItemPriority.High,
    ),
    HomeItem(
        titleRes = Res.string.home_settings,
        iconRes = Res.drawable.user,
        destination = HomeDestination.Settings,
        showTopBar = true,
        priority = HomeItemPriority.Low,
    ),
)