package tech.dokus.foundation.backend.cache

import org.koin.dsl.module
import tech.dokus.foundation.backend.config.CachingConfig

fun redisModule(cachingConfig: CachingConfig, redistNamespace: RedisNamespace) = module {
    single<RedisClient> {
        redis {
            config = cachingConfig.redis
            namespace = redistNamespace
        }
    }
}
