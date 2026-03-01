package tech.dokus.features.auth

import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.qualifier
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module
import tech.dokus.domain.asbtractions.AuthManager
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.model.common.Feature
import tech.dokus.domain.usecases.SearchCompanyUseCase
import tech.dokus.features.auth.database.AuthDatabase
import tech.dokus.features.auth.database.AuthDb
import tech.dokus.features.auth.datasource.AccountRemoteDataSource
import tech.dokus.features.auth.datasource.AccountRemoteDataSourceImpl
import tech.dokus.features.auth.datasource.IdentityRemoteDataSource
import tech.dokus.features.auth.datasource.IdentityRemoteDataSourceImpl
import tech.dokus.features.auth.datasource.LookupRemoteDataSource
import tech.dokus.features.auth.datasource.LookupRemoteDataSourceImpl
import tech.dokus.features.auth.datasource.TeamRemoteDataSource
import tech.dokus.features.auth.datasource.TeamRemoteDataSourceImpl
import tech.dokus.features.auth.datasource.TenantRemoteDataSource
import tech.dokus.features.auth.datasource.TenantRemoteDataSourceImpl
import tech.dokus.features.auth.gateway.AuthGateway
import tech.dokus.features.auth.gateway.TeamInvitationsGateway
import tech.dokus.features.auth.gateway.TeamInvitationsGatewayImpl
import tech.dokus.features.auth.gateway.TeamMembersGateway
import tech.dokus.features.auth.gateway.TeamMembersGatewayImpl
import tech.dokus.features.auth.gateway.TeamOwnershipGateway
import tech.dokus.features.auth.gateway.TeamOwnershipGatewayImpl
import tech.dokus.features.auth.gateway.WorkspaceSettingsGateway
import tech.dokus.features.auth.gateway.WorkspaceSettingsGatewayImpl
import tech.dokus.features.auth.initializer.AuthDataInitializer
import tech.dokus.features.auth.initializer.AuthDataInitializerImpl
import tech.dokus.features.auth.manager.AuthManagerImpl
import tech.dokus.features.auth.manager.AuthManagerMutable
import tech.dokus.features.auth.manager.TokenManagerImpl
import tech.dokus.features.auth.manager.TokenManagerMutable
import tech.dokus.features.auth.repository.AuthRepository
import tech.dokus.features.auth.storage.TokenStorage
import tech.dokus.features.auth.usecases.AuthSessionUseCase
import tech.dokus.features.auth.usecases.AuthSessionUseCaseImpl
import tech.dokus.features.auth.usecases.CancelInvitationUseCase
import tech.dokus.features.auth.usecases.CancelInvitationUseCaseImpl
import tech.dokus.features.auth.usecases.ConnectToServerUseCase
import tech.dokus.features.auth.usecases.ConnectToServerUseCaseImpl
import tech.dokus.features.auth.usecases.CreateInvitationUseCase
import tech.dokus.features.auth.usecases.CreateInvitationUseCaseImpl
import tech.dokus.features.auth.usecases.CreateTenantUseCase
import tech.dokus.features.auth.usecases.CreateTenantUseCaseImpl
import tech.dokus.features.auth.usecases.ChangePasswordUseCase
import tech.dokus.features.auth.usecases.ChangePasswordUseCaseImpl
import tech.dokus.features.auth.usecases.DeleteWorkspaceAvatarUseCase
import tech.dokus.features.auth.usecases.DeleteWorkspaceAvatarUseCaseImpl
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantIdUseCaseImpl
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCaseImpl
import tech.dokus.features.auth.usecases.GetAccountMeUseCase
import tech.dokus.features.auth.usecases.GetAccountMeUseCaseImpl
import tech.dokus.features.auth.usecases.GetCurrentUserUseCase
import tech.dokus.features.auth.usecases.GetCurrentUserUseCaseImpl
import tech.dokus.features.auth.usecases.GetInvoiceNumberPreviewUseCase
import tech.dokus.features.auth.usecases.GetInvoiceNumberPreviewUseCaseImpl
import tech.dokus.features.auth.usecases.GetLastSelectedTenantIdUseCase
import tech.dokus.features.auth.usecases.GetLastSelectedTenantIdUseCaseImpl
import tech.dokus.features.auth.usecases.GetTenantAddressUseCase
import tech.dokus.features.auth.usecases.GetTenantAddressUseCaseImpl
import tech.dokus.features.auth.usecases.GetTenantSettingsUseCase
import tech.dokus.features.auth.usecases.GetTenantSettingsUseCaseImpl
import tech.dokus.features.auth.usecases.HasFreelancerTenantUseCase
import tech.dokus.features.auth.usecases.HasFreelancerTenantUseCaseImpl
import tech.dokus.features.auth.usecases.ListMyTenantsUseCase
import tech.dokus.features.auth.usecases.ListMyTenantsUseCaseImpl
import tech.dokus.features.auth.usecases.ListSessionsUseCase
import tech.dokus.features.auth.usecases.ListSessionsUseCaseImpl
import tech.dokus.features.auth.usecases.ListPendingInvitationsUseCase
import tech.dokus.features.auth.usecases.ListPendingInvitationsUseCaseImpl
import tech.dokus.features.auth.usecases.ListTeamMembersUseCase
import tech.dokus.features.auth.usecases.ListTeamMembersUseCaseImpl
import tech.dokus.features.auth.usecases.LoginUseCase
import tech.dokus.features.auth.usecases.LoginUseCaseImpl
import tech.dokus.features.auth.usecases.LogoutUseCase
import tech.dokus.features.auth.usecases.LogoutUseCaseImpl
import tech.dokus.features.auth.usecases.RegisterAndLoginUseCase
import tech.dokus.features.auth.usecases.RegisterAndLoginUseCaseImpl
import tech.dokus.features.auth.usecases.RequestPasswordResetUseCase
import tech.dokus.features.auth.usecases.RequestPasswordResetUseCaseImpl
import tech.dokus.features.auth.usecases.ResetPasswordUseCase
import tech.dokus.features.auth.usecases.ResetPasswordUseCaseImpl
import tech.dokus.features.auth.usecases.RemoveTeamMemberUseCase
import tech.dokus.features.auth.usecases.RemoveTeamMemberUseCaseImpl
import tech.dokus.features.auth.usecases.ResendVerificationEmailUseCase
import tech.dokus.features.auth.usecases.ResendVerificationEmailUseCaseImpl
import tech.dokus.features.auth.usecases.RevokeOtherSessionsUseCase
import tech.dokus.features.auth.usecases.RevokeOtherSessionsUseCaseImpl
import tech.dokus.features.auth.usecases.RevokeSessionUseCase
import tech.dokus.features.auth.usecases.RevokeSessionUseCaseImpl
import tech.dokus.features.auth.usecases.SearchCompanyUseCaseImpl
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCaseImpl
import tech.dokus.features.auth.usecases.TransferWorkspaceOwnershipUseCase
import tech.dokus.features.auth.usecases.TransferWorkspaceOwnershipUseCaseImpl
import tech.dokus.features.auth.usecases.UpdateProfileUseCase
import tech.dokus.features.auth.usecases.UpdateProfileUseCaseImpl
import tech.dokus.features.auth.usecases.UpdateTeamMemberRoleUseCase
import tech.dokus.features.auth.usecases.UpdateTeamMemberRoleUseCaseImpl
import tech.dokus.features.auth.usecases.UpdateTenantSettingsUseCase
import tech.dokus.features.auth.usecases.UpdateTenantSettingsUseCaseImpl
import tech.dokus.features.auth.usecases.UploadWorkspaceAvatarUseCase
import tech.dokus.features.auth.usecases.UploadWorkspaceAvatarUseCaseImpl
import tech.dokus.features.auth.usecases.ValidateServerUseCase
import tech.dokus.features.auth.usecases.ValidateServerUseCaseImpl
import tech.dokus.features.auth.usecases.VerifyEmailUseCase
import tech.dokus.features.auth.usecases.VerifyEmailUseCaseImpl
import tech.dokus.features.auth.usecases.WatchCurrentTenantUseCase
import tech.dokus.features.auth.usecases.WatchCurrentTenantUseCaseImpl
import tech.dokus.features.auth.usecases.WatchCurrentUserUseCase
import tech.dokus.features.auth.usecases.WatchCurrentUserUseCaseImpl
import tech.dokus.features.auth.utils.JwtDecoder
import tech.dokus.foundation.app.SharedQualifiers
import tech.dokus.foundation.sstorage.SecureStorage

internal object Qualifiers {
    val secureStorageAuth: Qualifier = qualifier(Feature.Auth)
}

expect val authPlatformModule: Module

val authNetworkModule = module {
    // Non Authenticated
    single {
        IdentityRemoteDataSourceImpl(get<HttpClient>(SharedQualifiers.httpClientNoAuth))
    } bind IdentityRemoteDataSource::class

    // Authenticated
    singleOf(::AccountRemoteDataSourceImpl) bind AccountRemoteDataSource::class
    singleOf(::TenantRemoteDataSourceImpl) bind TenantRemoteDataSource::class
    singleOf(::TeamRemoteDataSourceImpl) bind TeamRemoteDataSource::class
    singleOf(::LookupRemoteDataSourceImpl) bind LookupRemoteDataSource::class
}

val authDataModule = module {
    // Database
    single { AuthDb.create() }
    single<AuthDatabase> { get<AuthDb>().get() }

    singleOf(::AuthManagerImpl) binds arrayOf(AuthManager::class, AuthManagerMutable::class)

    // JWT utilities
    singleOf(::JwtDecoder)

    // Token storage and management
    single<TokenStorage> {
        TokenStorage(get<SecureStorage>(Qualifiers.secureStorageAuth))
    }
    singleOf(::TokenManagerImpl) binds arrayOf(TokenManager::class, TokenManagerMutable::class)

    // Initialization
    singleOf(::AuthDataInitializerImpl) bind AuthDataInitializer::class

    // Repositories
    singleOf(::AuthRepository) bind AuthGateway::class
    singleOf(::WorkspaceSettingsGatewayImpl) bind WorkspaceSettingsGateway::class
    singleOf(::TeamMembersGatewayImpl) bind TeamMembersGateway::class
    singleOf(::TeamInvitationsGatewayImpl) bind TeamInvitationsGateway::class
    singleOf(::TeamOwnershipGatewayImpl) bind TeamOwnershipGateway::class
}

val authDomainModule = module {
    singleOf(::AuthSessionUseCaseImpl) bind AuthSessionUseCase::class
    singleOf(::LoginUseCaseImpl) bind LoginUseCase::class
    singleOf(::RegisterAndLoginUseCaseImpl) bind RegisterAndLoginUseCase::class
    singleOf(::LogoutUseCaseImpl) bind LogoutUseCase::class
    singleOf(::RequestPasswordResetUseCaseImpl) bind RequestPasswordResetUseCase::class
    singleOf(::ResetPasswordUseCaseImpl) bind ResetPasswordUseCase::class
    singleOf(::VerifyEmailUseCaseImpl) bind VerifyEmailUseCase::class
    singleOf(::ResendVerificationEmailUseCaseImpl) bind ResendVerificationEmailUseCase::class
    singleOf(::GetAccountMeUseCaseImpl) bind GetAccountMeUseCase::class
    singleOf(::GetCurrentUserUseCaseImpl) bind GetCurrentUserUseCase::class
    singleOf(::WatchCurrentUserUseCaseImpl) bind WatchCurrentUserUseCase::class
    singleOf(::UpdateProfileUseCaseImpl) bind UpdateProfileUseCase::class
    singleOf(::ChangePasswordUseCaseImpl) bind ChangePasswordUseCase::class
    singleOf(::ListSessionsUseCaseImpl) bind ListSessionsUseCase::class
    singleOf(::RevokeSessionUseCaseImpl) bind RevokeSessionUseCase::class
    singleOf(::RevokeOtherSessionsUseCaseImpl) bind RevokeOtherSessionsUseCase::class
    singleOf(::HasFreelancerTenantUseCaseImpl) bind HasFreelancerTenantUseCase::class
    singleOf(::CreateTenantUseCaseImpl) bind CreateTenantUseCase::class
    singleOf(::ListMyTenantsUseCaseImpl) bind ListMyTenantsUseCase::class
    singleOf(::GetInvoiceNumberPreviewUseCaseImpl) bind GetInvoiceNumberPreviewUseCase::class
    singleOf(::GetTenantSettingsUseCaseImpl) bind GetTenantSettingsUseCase::class
    singleOf(::GetTenantAddressUseCaseImpl) bind GetTenantAddressUseCase::class
    singleOf(::UpdateTenantSettingsUseCaseImpl) bind UpdateTenantSettingsUseCase::class
    singleOf(::UploadWorkspaceAvatarUseCaseImpl) bind UploadWorkspaceAvatarUseCase::class
    singleOf(::DeleteWorkspaceAvatarUseCaseImpl) bind DeleteWorkspaceAvatarUseCase::class
    singleOf(::ListTeamMembersUseCaseImpl) bind ListTeamMembersUseCase::class
    singleOf(::ListPendingInvitationsUseCaseImpl) bind ListPendingInvitationsUseCase::class
    singleOf(::CreateInvitationUseCaseImpl) bind CreateInvitationUseCase::class
    singleOf(::CancelInvitationUseCaseImpl) bind CancelInvitationUseCase::class
    singleOf(::UpdateTeamMemberRoleUseCaseImpl) bind UpdateTeamMemberRoleUseCase::class
    singleOf(::RemoveTeamMemberUseCaseImpl) bind RemoveTeamMemberUseCase::class
    singleOf(::TransferWorkspaceOwnershipUseCaseImpl) bind TransferWorkspaceOwnershipUseCase::class
    singleOf(::GetCurrentTenantUseCaseImpl) bind GetCurrentTenantUseCase::class
    singleOf(::WatchCurrentTenantUseCaseImpl) bind WatchCurrentTenantUseCase::class
    singleOf(::GetCurrentTenantIdUseCaseImpl) bind GetCurrentTenantIdUseCase::class
    singleOf(::GetLastSelectedTenantIdUseCaseImpl) bind GetLastSelectedTenantIdUseCase::class
    singleOf(::SelectTenantUseCaseImpl) bind SelectTenantUseCase::class

    // Server connection use cases
    singleOf(::ValidateServerUseCaseImpl) bind ValidateServerUseCase::class
    singleOf(::ConnectToServerUseCaseImpl) bind ConnectToServerUseCase::class

    // Company lookup use case (CBE API)
    singleOf(::SearchCompanyUseCaseImpl) bind SearchCompanyUseCase::class
}
