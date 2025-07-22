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
import ai.thepredict.configuration.ServerEndpoint
import ai.thepredict.repository.api.UnifiedApi
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

val repositoryDiModule by DI.Module("repository") {
    bind<UnifiedApi>() with singleton { UnifiedApi.create(ServerEndpoint.Local) }

    bind<AuthApi>() with singleton { instance<UnifiedApi>() }
    bind<CompanyApi>() with singleton { instance<UnifiedApi>() }
    bind<CompanyMembersApi>() with singleton { instance<UnifiedApi>() }
    bind<UserApi>() with singleton { instance<UnifiedApi>() }
    bind<DocumentApi>() with singleton { instance<UnifiedApi>() }
    bind<DocumentExtractionApi>() with singleton { instance<UnifiedApi>() }
    bind<DocumentFileApi>() with singleton { instance<UnifiedApi>() }
    bind<MatchingApi>() with singleton { instance<UnifiedApi>() }
    bind<TransactionApi>() with singleton { instance<UnifiedApi>() }
    bind<TransactionExtractionApi>() with singleton { instance<UnifiedApi>() }
    bind<TransactionFileApi>() with singleton { instance<UnifiedApi>() }
    bind<TransactionMatchingApi>() with singleton { instance<UnifiedApi>() }
    bind<InfoApi>() with singleton { instance<UnifiedApi>() }
}