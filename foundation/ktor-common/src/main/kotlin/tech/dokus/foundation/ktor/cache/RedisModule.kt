package tech.dokus.foundation.ktor.cache

import tech.dokus.foundation.ktor.config.AppBaseConfig
import org.koin.dsl.module

fun redisModule(appConfig: AppBaseConfig, redistNamespace: RedisNamespace) = module {
    single<RedisClient> {
        redis {
            config = appConfig.caching.redis
            namespace = redistNamespace
        }
    }
}