package tech.dokus.app

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.app.notifications.CashflowInvoiceLookupDataSource
import tech.dokus.app.notifications.InvoiceLookupDataSource
import tech.dokus.app.notifications.NotificationRemoteDataSource
import tech.dokus.app.notifications.NotificationRemoteDataSourceImpl
import tech.dokus.app.infrastructure.ServerConfigManagerImpl
import tech.dokus.app.local.DefaultLocalDatabaseCleaner
import tech.dokus.app.share.ShareImportAction
import tech.dokus.app.share.ShareImportContainer
import tech.dokus.app.share.ShareImportIntent
import tech.dokus.app.share.ShareImportState
import tech.dokus.app.viewmodel.BootstrapAction
import tech.dokus.app.viewmodel.BootstrapContainer
import tech.dokus.app.viewmodel.BootstrapIntent
import tech.dokus.app.viewmodel.BootstrapState
import tech.dokus.app.viewmodel.HomeAction
import tech.dokus.app.viewmodel.HomeContainer
import tech.dokus.app.viewmodel.HomeIntent
import tech.dokus.app.viewmodel.HomeState
import tech.dokus.app.viewmodel.NotificationPreferencesAction
import tech.dokus.app.viewmodel.NotificationPreferencesContainer
import tech.dokus.app.viewmodel.NotificationPreferencesIntent
import tech.dokus.app.viewmodel.NotificationPreferencesState
import tech.dokus.app.viewmodel.SettingsAction
import tech.dokus.app.viewmodel.SettingsContainer
import tech.dokus.app.viewmodel.SettingsIntent
import tech.dokus.app.viewmodel.SettingsState
import tech.dokus.app.viewmodel.TeamSettingsAction
import tech.dokus.app.viewmodel.TeamSettingsContainer
import tech.dokus.app.viewmodel.TeamSettingsIntent
import tech.dokus.app.viewmodel.TeamSettingsState
import tech.dokus.app.viewmodel.TeamSettingsUseCases
import tech.dokus.app.viewmodel.TodayAction
import tech.dokus.app.viewmodel.TodayContainer
import tech.dokus.app.viewmodel.TodayIntent
import tech.dokus.app.viewmodel.TodayState
import tech.dokus.app.viewmodel.WorkspaceSettingsAction
import tech.dokus.app.viewmodel.WorkspaceSettingsContainer
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.flags.FeatureFlagService
import tech.dokus.foundation.app.database.LocalDatabaseCleaner
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.style.ThemeManager
import tech.dokus.foundation.aura.style.ThemeManagerImpl

internal val diModuleApp = module {
    // Server configuration management (bridges platform settings with domain types)
    singleOf(::ServerConfigManagerImpl) bind ServerConfigManager::class

    // Dynamic endpoint provider (bridges server config to HTTP clients)
    singleOf(::DynamicDokusEndpointProvider)

    // Note: ServerConnectionMonitor is now registered in AppDataMainModuleDi
    // and wired into HTTP clients for event-driven connection tracking

    // Theme management (singleton)
    singleOf(::ThemeManagerImpl) bind ThemeManager::class

    // FlowMVI Containers
    container<BootstrapContainer, BootstrapState, BootstrapIntent, BootstrapAction> {
        BootstrapContainer(
            authInitializer = get(),
            tokenManager = get(),
            serverConfigManager = get(),
        )
    }
    container<TodayContainer, TodayState, TodayIntent, TodayAction> {
        TodayContainer(
            getCurrentTenantUseCase = get(),
            watchPendingDocuments = get(),
            notificationRemoteDataSource = get(),
            invoiceLookupDataSource = get(),
        )
    }
    container<HomeContainer, HomeState, HomeIntent, HomeAction> {
        HomeContainer(
            watchCurrentTenantUseCase = get(),
            watchCurrentUserUseCase = get(),
            logoutUseCase = get(),
        )
    }
    container<SettingsContainer, SettingsState, SettingsIntent, SettingsAction> {
        SettingsContainer(getCurrentTenantUseCase = get())
    }
    container<NotificationPreferencesContainer, NotificationPreferencesState, NotificationPreferencesIntent, NotificationPreferencesAction> {
        NotificationPreferencesContainer(
            notificationRemoteDataSource = get()
        )
    }
    container<WorkspaceSettingsContainer, WorkspaceSettingsState, WorkspaceSettingsIntent, WorkspaceSettingsAction> {
        WorkspaceSettingsContainer(
            getCurrentTenantUseCase = get(),
            getTenantSettings = get(),
            getTenantAddress = get(),
            updateTenantSettings = get(),
            uploadWorkspaceAvatar = get(),
            deleteWorkspaceAvatar = get(),
            watchCurrentTenantUseCase = get(),
            getPeppolRegistration = get(),
            getPeppolActivity = get(),
        )
    }
    container<TeamSettingsContainer, TeamSettingsState, TeamSettingsIntent, TeamSettingsAction> {
        TeamSettingsContainer(
            useCases = TeamSettingsUseCases(
                listTeamMembers = get(),
                listPendingInvitations = get(),
                createInvitation = get(),
                cancelInvitation = get(),
                updateTeamMemberRole = get(),
                removeTeamMember = get(),
                transferWorkspaceOwnership = get(),
                getCurrentUser = get(),
                getCurrentTenant = get(),
            )
        )
    }
    container<ShareImportContainer, ShareImportState, ShareImportIntent, ShareImportAction> {
        ShareImportContainer(
            tokenManager = get(),
            getLastSelectedTenantIdUseCase = get(),
            listMyTenantsUseCase = get(),
            selectTenantUseCase = get(),
            uploadDocumentUseCase = get()
        )
    }

    singleOf(::CashflowInvoiceLookupDataSource) bind InvoiceLookupDataSource::class
    single<FeatureFlagService> { FeatureFlagService.defaultsOnly }
    singleOf(::NotificationRemoteDataSourceImpl) bind NotificationRemoteDataSource::class
    singleOf(::DefaultLocalDatabaseCleaner) bind LocalDatabaseCleaner::class
}

internal val diModuleUseCases = module {
    // UseCases are now wired in their respective feature modules
}
