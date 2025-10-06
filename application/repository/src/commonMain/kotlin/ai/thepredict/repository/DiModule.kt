package ai.thepredict.repository

import ai.thepredict.apispec.AuthApi
import ai.thepredict.apispec.CompanyApi
import ai.thepredict.apispec.CompanyMembersApi
import ai.thepredict.apispec.DocumentApi
import ai.thepredict.apispec.DocumentExtractionApi
import ai.thepredict.apispec.DocumentFileApi
import ai.thepredict.apispec.InfoApi
import ai.thepredict.apispec.MatchingApi
import ai.thepredict.apispec.TransactionApi
import ai.thepredict.apispec.TransactionExtractionApi
import ai.thepredict.apispec.TransactionFileApi
import ai.thepredict.apispec.TransactionMatchingApi
import ai.thepredict.apispec.UserApi
import ai.thepredict.domain.configuration.ServerEndpoint
import ai.thepredict.repository.api.UnifiedApi
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