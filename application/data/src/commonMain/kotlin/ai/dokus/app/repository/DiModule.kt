package ai.dokus.app.repository

import ai.dokus.foundation.apispec.AuthApi
import ai.dokus.foundation.apispec.CompanyApi
import ai.dokus.foundation.apispec.CompanyMembersApi
import ai.dokus.foundation.apispec.DocumentApi
import ai.dokus.foundation.apispec.DocumentExtractionApi
import ai.dokus.foundation.apispec.DocumentFileApi
import ai.dokus.foundation.apispec.InfoApi
import ai.dokus.foundation.apispec.MatchingApi
import ai.dokus.foundation.apispec.TransactionApi
import ai.dokus.foundation.apispec.TransactionExtractionApi
import ai.dokus.foundation.apispec.TransactionFileApi
import ai.dokus.foundation.apispec.TransactionMatchingApi
import ai.dokus.foundation.apispec.UserApi
import ai.dokus.foundation.domain.configuration.ServerEndpoint
import ai.dokus.app.repository.api.UnifiedApi
import org.koin.dsl.module

val repositoryDiModule = module {
    single<UnifiedApi> { UnifiedApi.create(ServerEndpoint) }

    single<AuthApi> { get<UnifiedApi>() }
    single<CompanyApi> { get<UnifiedApi>() }
    single<CompanyMembersApi> { get<UnifiedApi>() }
    single<UserApi> { get<UnifiedApi>() }
    single<DocumentApi> { get<UnifiedApi>() }
    single<DocumentExtractionApi> { get<UnifiedApi>() }
    single<DocumentFileApi> { get<UnifiedApi>() }
    single<MatchingApi> { get<UnifiedApi>() }
    single<TransactionApi> { get<UnifiedApi>() }
    single<TransactionExtractionApi> { get<UnifiedApi>() }
    single<TransactionFileApi> { get<UnifiedApi>() }
    single<TransactionMatchingApi> { get<UnifiedApi>() }
    single<InfoApi> { get<UnifiedApi>() }
}