package tech.dokus.foundation.backend.cache

import org.koin.dsl.module
import tech.dokus.foundation.backend.config.AppBaseConfig

fun redisModule(appConfig: AppBaseConfig, redistNamespace: RedisNamespace) = module {
    single<RedisClient> {
        redis {
            config = appConfig.caching.redis
            namespace = redistNamespace
        }
    }
}
