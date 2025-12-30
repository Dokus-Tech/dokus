package tech.dokus.foundation.backend.cache

import tech.dokus.foundation.backend.config.AppBaseConfig
import org.koin.dsl.module

fun redisModule(appConfig: AppBaseConfig, redistNamespace: RedisNamespace) = module {
    single<RedisClient> {
        redis {
            config = appConfig.caching.redis
            namespace = redistNamespace
        }
    }
}