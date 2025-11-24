package ai.dokus.app.media

import ai.dokus.app.core.AppDataModuleDi
import ai.dokus.app.core.AppDomainModuleDi
import ai.dokus.app.core.AppModule
import ai.dokus.app.core.AppPresentationModuleDi
import ai.dokus.app.core.DashboardWidget
import ai.dokus.app.core.ModuleSettingsGroup
import ai.dokus.app.media.di.mediaDataModule
import ai.dokus.app.media.di.mediaDomainModule
import ai.dokus.app.media.di.mediaPresentationModule
import ai.dokus.app.media.di.mediaViewModelModule
import ai.dokus.app.media.navigation.MediaNavigationProvider
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.inbox
import ai.dokus.app.resources.generated.media_title
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.design.model.HomeItemPriority
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination

object MediaAppModule : AppModule {
    override val navigationProvider: NavigationProvider? = null
    override val homeNavigationProvider: NavigationProvider = MediaNavigationProvider
    override val homeItems: List<HomeItem> = listOf(
        HomeItem(
            destination = HomeDestination.Media,
            titleRes = Res.string.media_title,
            iconRes = Res.drawable.inbox,
            priority = HomeItemPriority.Medium,
            showTopBar = false
        )
    )
    override val settingsGroups: List<ModuleSettingsGroup> = emptyList()
    override val dashboardWidgets: List<DashboardWidget> = emptyList()

    override val presentationDi: AppPresentationModuleDi = object : AppPresentationModuleDi {
        override val viewModels = mediaViewModelModule
        override val presentation = mediaPresentationModule
    }

    override val dataDi: AppDataModuleDi = object : AppDataModuleDi {
        override val platform = null
        override val network = mediaDataModule
        override val data = null
    }

    override val domainDi: AppDomainModuleDi = object : AppDomainModuleDi {
        override val useCases = mediaDomainModule
    }
}
