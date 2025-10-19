package ai.dokus.foundation.ktor.cache

import ai.dokus.foundation.ktor.AppBaseConfig
import org.koin.dsl.module

fun redisModule(appConfig: AppBaseConfig, redistNamespace: RedisNamespace) = module {
    single<RedisClient> {
        redis {
            config = appConfig.caching.redis
            namespace = redistNamespace
        }
    }
}