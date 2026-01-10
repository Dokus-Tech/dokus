package tech.dokus.foundation.aura.tooling

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chart_bar_trend_up
import tech.dokus.aura.resources.home_today
import tech.dokus.aura.resources.home_settings
import tech.dokus.aura.resources.user
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.aura.model.HomeItem
import tech.dokus.foundation.aura.model.HomeItemPriority
import tech.dokus.navigation.destinations.HomeDestination

// Simple mock values for previews
val mockEmail = Email("john.doe@dokus.be")
val mockName = Name("John")
val mockLastName = Name("Doe")
val mockUserId = UserId("preview-user-id")

val homeItemMocks = listOf(
    HomeItem(
        titleRes = Res.string.home_today,
        iconRes = Res.drawable.chart_bar_trend_up,
        destination = HomeDestination.Today,
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
