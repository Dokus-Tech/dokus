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
import ai.dokus.app.media.navigation.MediaHomeNavigationProvider
import ai.dokus.foundation.design.model.HomeItem
import ai.dokus.foundation.navigation.NavigationProvider

object MediaAppModule : AppModule {
    override val navigationProvider: NavigationProvider? = null
    override val homeNavigationProvider: NavigationProvider = MediaHomeNavigationProvider
    override val homeItems: List<HomeItem> = listOf()
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
